package com.digitalocean.llmproxy.client;

import com.digitalocean.llmproxy.config.AppProperties;
import com.digitalocean.llmproxy.model.chat.ChatCompletionChoice;
import com.digitalocean.llmproxy.model.chat.ChatCompletionRequest;
import com.digitalocean.llmproxy.model.chat.ChatCompletionResponse;
import com.digitalocean.llmproxy.model.chat.ChatMessage;
import com.digitalocean.llmproxy.model.chat.Usage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class LlmClient {

    private final AppProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String role;
    private final String baseUrl;
    private final String defaultModel;
    private final String apiKey;

    public LlmClient(
            AppProperties properties,
            WebClient webClient,
            ObjectMapper objectMapper,
            String role,
            String baseUrl,
            String defaultModel,
            String apiKey) {
        this.properties = properties;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.role = role;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultModel = defaultModel;
        this.apiKey = apiKey;
    }

    public LlmCallResult chatCompletions(ChatCompletionRequest request) {
        String model = request.getModel() != null ? request.getModel() : defaultModel;
        if (properties.isMockLlm()) {
            return mockResponse(request, model);
        }

        long started = System.nanoTime();
        ObjectNode payload = objectMapper.valueToTree(request);
        payload.put("model", model);
        payload.remove("stream");

        WebClient.RequestBodySpec spec = webClient.post()
                .uri(baseUrl + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        if (apiKey != null && !apiKey.isBlank()) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        try {
            ChatCompletionResponse response = spec.bodyValue(payload)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block(Duration.ofMillis((long) (properties.getLlmTimeoutSeconds() * 1000)));

            double latencyMs = (System.nanoTime() - started) / 1_000_000.0;
            return new LlmCallResult(response, latencyMs);
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Upstream LLM returned " + ex.getStatusCode(), ex);
        }
    }

    private LlmCallResult mockResponse(ChatCompletionRequest request, String model) {
        long started = System.nanoTime();

        String userText = request.getMessages().stream()
                .filter(message -> "user".equals(message.getRole()))
                .map(message -> message.getContent() != null ? message.getContent() : "")
                .collect(Collectors.joining(" "))
                .trim();

        String seed = sha256(role + ":" + model + ":" + userText);
        String content;
        int promptTokens;
        int completionTokens;
        double latencyMs;

        if ("primary".equals(role)) {
            content = mockJsonContent("answer", role, model, userText, seed);
            promptTokens = Math.max(10, userText.isEmpty() ? 0 : userText.split("\\s+").length * 2);
            completionTokens = 40 + (Integer.parseInt(seed.substring(0, 4), 16) % 20);
            latencyMs = 120.0 + (Integer.parseInt(seed.substring(4, 8), 16) % 80);
        } else {
            content = mockJsonContent("answer", role, model, userText, seed);
            promptTokens = Math.max(10, userText.isEmpty() ? 0 : userText.split("\\s+").length * 2);
            completionTokens = 25 + (Integer.parseInt(seed.substring(0, 4), 16) % 15);
            latencyMs = 60.0 + (Integer.parseInt(seed.substring(4, 8), 16) % 40);
        }

        double elapsedMs = (System.nanoTime() - started) / 1_000_000.0;

        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-mock-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel(model);

        ChatCompletionChoice choice = new ChatCompletionChoice();
        choice.setIndex(0);
        choice.setMessage(new ChatMessage("assistant", content));
        choice.setFinishReason("stop");
        response.setChoices(List.of(choice));
        response.setUsage(new Usage(promptTokens, completionTokens, promptTokens + completionTokens));

        return new LlmCallResult(response, latencyMs + elapsedMs);
    }

    private static String mockJsonContent(
            String action, String role, String model, String userText, String seed) {
        String escapedMessage = userText.isEmpty() ? "empty prompt" : userText.replace("\"", "\\\"");
        return String.format(
                "{\"action\":\"%s\",\"role\":\"%s\",\"model\":\"%s\",\"message\":\"%s\",\"seed\":\"%s\"}",
                action,
                role,
                model,
                escapedMessage,
                seed.substring(0, 8));
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
