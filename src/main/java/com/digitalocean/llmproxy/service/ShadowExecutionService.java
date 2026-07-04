package com.digitalocean.llmproxy.service;

import com.digitalocean.llmproxy.client.LlmCallResult;
import com.digitalocean.llmproxy.client.LlmClient;
import com.digitalocean.llmproxy.model.chat.ChatCompletionRequest;
import com.digitalocean.llmproxy.model.metrics.ComparisonBreakdown;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;

/**
 * Fire-and-forget shadow routing to the candidate LLM.
 * All work runs off the request thread; failures are swallowed after metrics/logging.
 */
@Service
public class ShadowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ShadowExecutionService.class);

    private final LlmClient candidateClient;
    private final ComparisonService comparisonService;
    private final MetricsStore metricsStore;
    private final MeterRegistry meterRegistry;
    private final BoundedShadowExecutor shadowExecutor;
    private final ObjectMapper objectMapper;

    public ShadowExecutionService(
            @Qualifier("candidateLlmClient") LlmClient candidateClient,
            ComparisonService comparisonService,
            MetricsStore metricsStore,
            MeterRegistry meterRegistry,
            BoundedShadowExecutor shadowExecutor,
            ObjectMapper objectMapper) {
        this.candidateClient = candidateClient;
        this.comparisonService = comparisonService;
        this.metricsStore = metricsStore;
        this.meterRegistry = meterRegistry;
        this.shadowExecutor = shadowExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * Schedule candidate shadow work without blocking the caller.
     * Never throws — primary response must not be affected.
     */
    public void dispatchShadow(
            String requestId,
            ChatCompletionRequest request,
            LlmCallResult primaryResult,
            double primaryElapsedMs) {
        boolean accepted = shadowExecutor.tryDispatch(() -> {
            ChatCompletionRequest shadowRequest = copyRequest(request);
            runShadowCandidate(requestId, shadowRequest, primaryResult, primaryElapsedMs);
        });

        if (!accepted) {
            log.warn(
                    "Shadow evaluation shed for request_id={} (customer already served)",
                    requestId);
        }
    }

    private void runShadowCandidate(
            String requestId,
            ChatCompletionRequest request,
            LlmCallResult primaryResult,
            double primaryElapsedMs) {
        metricsStore.markPending(1);
        try {
            long candidateStarted = System.nanoTime();
            LlmCallResult candidateResult = candidateClient.chatCompletions(request);
            double candidateElapsedMs = (System.nanoTime() - candidateStarted) / 1_000_000.0;

            String primaryContent = extractContent(primaryResult);
            String candidateContent = extractContent(candidateResult);

            ComparisonBreakdown breakdown = comparisonService.compareResponses(primaryContent, candidateContent);

            metricsStore.recordComparison(
                    meterRegistry,
                    new MetricsStore.ComparisonRecord(
                            requestId,
                            System.currentTimeMillis() / 1000.0,
                            primaryResult.response().getModel(),
                            candidateResult.response().getModel(),
                            primaryElapsedMs,
                            candidateElapsedMs,
                            breakdown));

            log.info(
                    "Comparison request_id={} primary_valid_json={} candidate_valid_json={} action_match={} primary_action={} candidate_action={}",
                    requestId,
                    breakdown.isPrimaryValidJson(),
                    breakdown.isCandidateValidJson(),
                    breakdown.isActionExactMatch(),
                    breakdown.getPrimaryAction(),
                    breakdown.getCandidateAction());
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                metricsStore.recordShadowTimeout();
                log.error(
                        "Candidate LLM timed out request_id={} (customer already served)",
                        requestId,
                        ex);
            } else {
                metricsStore.recordShadowError();
                log.error(
                        "Candidate LLM call failed request_id={} (customer already served)",
                        requestId,
                        ex);
            }
        } finally {
            metricsStore.markPending(-1);
        }
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            if (current instanceof WebClientRequestException webClientEx) {
                String message = webClientEx.getMessage();
                if (message != null && message.toLowerCase().contains("timeout")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private ChatCompletionRequest copyRequest(ChatCompletionRequest request) {
        return objectMapper.convertValue(request, ChatCompletionRequest.class);
    }

    private static String extractContent(LlmCallResult result) {
        if (result.response().getChoices() == null || result.response().getChoices().isEmpty()) {
            return "";
        }
        String content = result.response().getChoices().getFirst().getMessage().getContent();
        return content != null ? content : "";
    }
}
