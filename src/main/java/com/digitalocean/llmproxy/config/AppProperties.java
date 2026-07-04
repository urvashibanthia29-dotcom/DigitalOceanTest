package com.digitalocean.llmproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String primaryLlmBaseUrl = "https://inference.do-ai.run/v1";
    private String primaryLlmModel = "llama3.3-70b-instruct";
    private String primaryLlmApiKey = "";
    private String candidateLlmBaseUrl = "https://inference.do-ai.run/v1";
    private String candidateLlmModel = "llama3-8b-instruct";
    private String candidateLlmApiKey = "";
    private boolean mockLlm = true;
    private double llmTimeoutSeconds = 60.0;

    private int shadowCorePoolSize = 4;
    private int shadowMaxPoolSize = 16;
    private int shadowQueueCapacity = 500;
    private int maxComparisonRecords = 1000;

    public String getPrimaryLlmBaseUrl() {
        return primaryLlmBaseUrl;
    }

    public void setPrimaryLlmBaseUrl(String primaryLlmBaseUrl) {
        this.primaryLlmBaseUrl = primaryLlmBaseUrl;
    }

    public String getPrimaryLlmModel() {
        return primaryLlmModel;
    }

    public void setPrimaryLlmModel(String primaryLlmModel) {
        this.primaryLlmModel = primaryLlmModel;
    }

    public String getPrimaryLlmApiKey() {
        return primaryLlmApiKey;
    }

    public void setPrimaryLlmApiKey(String primaryLlmApiKey) {
        this.primaryLlmApiKey = primaryLlmApiKey;
    }

    public String getCandidateLlmBaseUrl() {
        return candidateLlmBaseUrl;
    }

    public void setCandidateLlmBaseUrl(String candidateLlmBaseUrl) {
        this.candidateLlmBaseUrl = candidateLlmBaseUrl;
    }

    public String getCandidateLlmModel() {
        return candidateLlmModel;
    }

    public void setCandidateLlmModel(String candidateLlmModel) {
        this.candidateLlmModel = candidateLlmModel;
    }

    public String getCandidateLlmApiKey() {
        return candidateLlmApiKey;
    }

    public void setCandidateLlmApiKey(String candidateLlmApiKey) {
        this.candidateLlmApiKey = candidateLlmApiKey;
    }

    public boolean isMockLlm() {
        return mockLlm;
    }

    public void setMockLlm(boolean mockLlm) {
        this.mockLlm = mockLlm;
    }

    public double getLlmTimeoutSeconds() {
        return llmTimeoutSeconds;
    }

    public void setLlmTimeoutSeconds(double llmTimeoutSeconds) {
        this.llmTimeoutSeconds = llmTimeoutSeconds;
    }

    public int getShadowCorePoolSize() {
        return shadowCorePoolSize;
    }

    public void setShadowCorePoolSize(int shadowCorePoolSize) {
        this.shadowCorePoolSize = shadowCorePoolSize;
    }

    public int getShadowMaxPoolSize() {
        return shadowMaxPoolSize;
    }

    public void setShadowMaxPoolSize(int shadowMaxPoolSize) {
        this.shadowMaxPoolSize = shadowMaxPoolSize;
    }

    public int getShadowQueueCapacity() {
        return shadowQueueCapacity;
    }

    public void setShadowQueueCapacity(int shadowQueueCapacity) {
        this.shadowQueueCapacity = shadowQueueCapacity;
    }

    public int getMaxComparisonRecords() {
        return maxComparisonRecords;
    }

    public void setMaxComparisonRecords(int maxComparisonRecords) {
        this.maxComparisonRecords = maxComparisonRecords;
    }
}
