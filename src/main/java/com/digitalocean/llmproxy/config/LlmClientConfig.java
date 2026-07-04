package com.digitalocean.llmproxy.config;

import com.digitalocean.llmproxy.client.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class LlmClientConfig {

    @Bean(name = "primaryLlmClient")
    public LlmClient primaryLlmClient(AppProperties properties, WebClient webClient, ObjectMapper objectMapper) {
        return new LlmClient(
                properties,
                webClient,
                objectMapper,
                "primary",
                properties.getPrimaryLlmBaseUrl(),
                properties.getPrimaryLlmModel(),
                properties.getPrimaryLlmApiKey());
    }

    @Bean(name = "candidateLlmClient")
    public LlmClient candidateLlmClient(AppProperties properties, WebClient webClient, ObjectMapper objectMapper) {
        return new LlmClient(
                properties,
                webClient,
                objectMapper,
                "candidate",
                properties.getCandidateLlmBaseUrl(),
                properties.getCandidateLlmModel(),
                properties.getCandidateLlmApiKey());
    }
}
