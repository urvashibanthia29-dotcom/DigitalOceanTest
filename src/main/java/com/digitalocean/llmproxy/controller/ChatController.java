package com.digitalocean.llmproxy.controller;

import com.digitalocean.llmproxy.model.chat.ChatCompletionRequest;
import com.digitalocean.llmproxy.model.chat.ChatCompletionResponse;
import com.digitalocean.llmproxy.service.MetricsStore;
import com.digitalocean.llmproxy.service.ProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ProxyService proxyService;
    private final MetricsStore metricsStore;

    public ChatController(ProxyService proxyService, MetricsStore metricsStore) {
        this.proxyService = proxyService;
        this.metricsStore = metricsStore;
    }

    @PostMapping("/v1/chat")
    public ChatCompletionResponse chat(@Valid @RequestBody ChatCompletionRequest request) {
        metricsStore.recordRequestProcessed();
        try {
            return proxyService.handleChat(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Upstream primary LLM failure", ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Primary LLM upstream failed",
                    ex);
        }
    }
}
