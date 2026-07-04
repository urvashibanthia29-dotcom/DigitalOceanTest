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
import com.digitalocean.llmproxy.service.MetricsStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestPropertySource(properties = "app.mock-llm=true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProxyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MetricsStore metricsStore;

    @MockBean
    @Qualifier("candidateLlmClient")
    private LlmClient candidateLlmClient;

    @BeforeEach
    void resetMetrics() {
        metricsStore.resetForTests();
    }

    @Test
    @Order(1)
    void chatReturnsPrimaryImmediately() throws Exception {
        stubMatchingCandidateJson();

        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"user","content":"What is the capital of France?"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("chat.completion"))
                .andExpect(jsonPath("$.choices[0].message.content").value(org.hamcrest.Matchers.containsString("\"action\"")));
    }

    @Test
    @Order(2)
    void primaryUnaffectedWhenCandidateFails() throws Exception {
        when(candidateLlmClient.chatCompletions(any())).thenThrow(new RuntimeException("candidate unavailable"));

        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"user","content":"Hello proxy"}]}
                                """))
                .andExpect(status().isOk());

        Thread.sleep(200);

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shadow_errors_or_timeouts").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.total_comparisons").value(0));
    }

    @Test
    @Order(3)
    void metricsReportsActionExactMatchRate() throws Exception {
        stubMatchingCandidateJson();

        mockMvc.perform(post("/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"messages":[{"role":"user","content":"Hello proxy"}]}
                        """));

        Thread.sleep(200);

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_requests_processed").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.total_comparisons").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.exact_match_rate_percent").value(100.0))
                .andExpect(jsonPath("$.pending_shadow_executions").value(0));
    }

    @Test
    @Order(4)
    void streamingRejected() throws Exception {
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"user","content":"Hi"}],"stream":true}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void primaryNotDelayedBySlowCandidate() throws Exception {
        when(candidateLlmClient.chatCompletions(any())).thenAnswer(invocation -> {
            Thread.sleep(2000);
            return stubCandidateJson("answer");
        });

        long started = System.currentTimeMillis();
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"user","content":"Speed test"}]}
                                """))
                .andExpect(status().isOk());
        long elapsed = System.currentTimeMillis() - started;

        org.junit.jupiter.api.Assertions.assertTrue(
                elapsed < 1000,
                "Primary response took " + elapsed + "ms; should not wait for candidate");
    }

    private void stubMatchingCandidateJson() {
        when(candidateLlmClient.chatCompletions(any())).thenAnswer(invocation -> stubCandidateJson("answer"));
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
