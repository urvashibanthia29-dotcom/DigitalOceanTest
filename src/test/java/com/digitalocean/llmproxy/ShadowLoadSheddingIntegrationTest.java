package com.digitalocean.llmproxy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitalocean.llmproxy.client.LlmCallResult;
import com.digitalocean.llmproxy.client.LlmClient;
import com.digitalocean.llmproxy.model.chat.ChatCompletionChoice;
import com.digitalocean.llmproxy.model.chat.ChatCompletionResponse;
import com.digitalocean.llmproxy.model.chat.ChatMessage;
import com.digitalocean.llmproxy.service.BoundedShadowExecutor;
import com.digitalocean.llmproxy.service.MetricsStore;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "app.mock-llm=true",
            "app.shadow-core-pool-size=1",
            "app.shadow-max-pool-size=1",
            "app.shadow-queue-capacity=1",
            "app.max-comparison-records=50"
        })
class ShadowLoadSheddingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MetricsStore metricsStore;

    @Autowired
    private BoundedShadowExecutor shadowExecutor;

    @MockBean
    @Qualifier("candidateLlmClient")
    private LlmClient candidateLlmClient;

    @BeforeEach
    void resetMetrics() {
        metricsStore.resetForTests();
    }

    @Test
    void burstShedsShadowTasksWithoutBlockingPrimary() throws Exception {
        when(candidateLlmClient.chatCompletions(any())).thenAnswer(invocation -> {
            Thread.sleep(400);
            return stubCandidateJson("answer");
        });

        long started = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"messages":[{"role":"user","content":"burst-%d"}]}
                                    """.formatted(i)))
                    .andExpect(status().isOk());
        }
        long elapsed = System.currentTimeMillis() - started;

        org.junit.jupiter.api.Assertions.assertTrue(
                elapsed < 5000,
                "Primary burst took " + elapsed + "ms; should not wait for shadow backlog");

        Thread.sleep(1500);

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_requests_processed").value(20))
                .andExpect(jsonPath("$.shadow_tasks_dropped").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.total_comparisons").value(org.hamcrest.Matchers.lessThan(20)));

        org.junit.jupiter.api.Assertions.assertTrue(shadowExecutor.getDroppedTasks() > 0);
    }

    @Test
    void comparisonHistoryStaysWithinConfiguredCap() throws Exception {
        when(candidateLlmClient.chatCompletions(any())).thenAnswer(invocation -> stubCandidateJson("answer"));

        IntStream.range(0, 15).forEach(i -> {
            try {
                mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"user","content":"cap-%d"}]}
                                """.formatted(i)));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        Thread.sleep(800);

        org.junit.jupiter.api.Assertions.assertTrue(metricsStore.getComparisonRecordCount() <= 50);
    }

    private LlmCallResult stubCandidateJson(String action) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-test");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel("test-candidate");

        ChatCompletionChoice choice = new ChatCompletionChoice();
        choice.setIndex(0);
        choice.setMessage(new ChatMessage("assistant", "{\"action\":\"" + action + "\",\"role\":\"candidate\"}"));
        choice.setFinishReason("stop");
        response.setChoices(List.of(choice));

        return new LlmCallResult(response, 50.0);
    }
}
