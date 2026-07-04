package com.digitalocean.llmproxy.model.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    private String model;

    @NotEmpty
    @Valid
    private List<ChatMessage> messages;

    private Double temperature;
    private Integer maxTokens;
    private Integer maxCompletionTokens;
    private Double topP;
    private boolean stream = false;
    private String user;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    @JsonProperty("max_tokens")
    public Integer getMaxTokens() {
        return maxTokens;
    }

    @JsonProperty("max_tokens")
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    @JsonProperty("max_completion_tokens")
    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    @JsonProperty("max_completion_tokens")
    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    @JsonProperty("top_p")
    public Double getTopP() {
        return topP;
    }

    @JsonProperty("top_p")
    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
