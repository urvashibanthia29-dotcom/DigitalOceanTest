package com.digitalocean.llmproxy.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.digitalocean.llmproxy.client.LlmCallResult;
import com.digitalocean.llmproxy.client.LlmClient;
import com.digitalocean.llmproxy.model.chat.ChatCompletionChoice;
import com.digitalocean.llmproxy.model.chat.ChatCompletionRequest;
import com.digitalocean.llmproxy.model.chat.ChatCompletionResponse;
import com.digitalocean.llmproxy.model.chat.ChatMessage;
import com.digitalocean.llmproxy.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShadowExecutionServiceTest {

    @Mock
    private LlmClient candidateClient;

    private MetricsStore metricsStore;
    private BoundedShadowExecutor shadowExecutor;
    private ShadowExecutionService shadowExecutionService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setShadowCorePoolSize(1);
        properties.setShadowMaxPoolSize(1);
        properties.setShadowQueueCapacity(0);

        metricsStore = new MetricsStore(properties, new SimpleMeterRegistry());
        metricsStore.resetForTests();
        shadowExecutor = new BoundedShadowExecutor(properties, metricsStore);

        shadowExecutionService = new ShadowExecutionService(
                candidateClient,
                new ComparisonService(new ObjectMapper()),
                metricsStore,
                new SimpleMeterRegistry(),
                shadowExecutor,
                new ObjectMapper());
    }

    @Test
    void shedShadowSkipsCandidateCall() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch block = new CountDownLatch(1);

        shadowExecutor.tryDispatch(() -> {
            started.countDown();
            try {
                block.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        started.await(2, TimeUnit.SECONDS);

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setMessages(List.of(new ChatMessage("user", "hello")));
        shadowExecutionService.dispatchShadow("req-1", request, primaryResult(), 10);

        org.junit.jupiter.api.Assertions.assertEquals(1, metricsStore.getShadowDroppedCount());
        verify(candidateClient, never()).chatCompletions(any());

        block.countDown();
    }

    private static LlmCallResult primaryResult() {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setModel("primary");
        ChatCompletionChoice choice = new ChatCompletionChoice();
        choice.setMessage(new ChatMessage("assistant", "{\"action\":\"answer\"}"));
        response.setChoices(List.of(choice));
        return new LlmCallResult(response, 5);
    }
}
