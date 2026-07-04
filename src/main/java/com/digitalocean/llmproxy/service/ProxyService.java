package com.digitalocean.llmproxy.service;

import com.digitalocean.llmproxy.client.LlmCallResult;
import com.digitalocean.llmproxy.client.LlmClient;
import com.digitalocean.llmproxy.model.chat.ChatCompletionRequest;
import com.digitalocean.llmproxy.model.chat.ChatCompletionResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);

    private final LlmClient primaryClient;
    private final MetricsStore metricsStore;
    private final ShadowExecutionService shadowExecutionService;

    public ProxyService(
            @Qualifier("primaryLlmClient") LlmClient primaryClient,
            MetricsStore metricsStore,
            ShadowExecutionService shadowExecutionService) {
        this.primaryClient = primaryClient;
        this.metricsStore = metricsStore;
        this.shadowExecutionService = shadowExecutionService;
    }

    public ChatCompletionResponse handleChat(ChatCompletionRequest request) {
        if (request.isStream()) {
            throw new IllegalArgumentException("Streaming is not supported on this proxy endpoint");
        }

        String requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long primaryStarted = System.nanoTime();

        LlmCallResult primaryResult;
        try {
            primaryResult = primaryClient.chatCompletions(request);
        } catch (Exception ex) {
            metricsStore.recordPrimaryError();
            log.error("Primary LLM call failed request_id={}", requestId, ex);
            throw ex;
        }

        double primaryElapsedMs = (System.nanoTime() - primaryStarted) / 1_000_000.0;

        // Fire-and-forget: candidate latency/errors never block this return path.
        shadowExecutionService.dispatchShadow(requestId, request, primaryResult, primaryElapsedMs);

        return primaryResult.response();
    }
}
