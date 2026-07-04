package com.digitalocean.llmproxy.client;

import com.digitalocean.llmproxy.model.chat.ChatCompletionResponse;

public record LlmCallResult(ChatCompletionResponse response, double latencyMs) {
}
