package com.digitalocean.llmproxy.model.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ObservabilitySummary {

    @JsonProperty("total_requests_processed")
    private long totalRequestsProcessed;

    @JsonProperty("shadow_errors_or_timeouts")
    private long shadowErrorsOrTimeouts;

    @JsonProperty("shadow_tasks_dropped")
    private long shadowTasksDropped;

    @JsonProperty("exact_match_rate_percent")
    private double exactMatchRatePercent;

    @JsonProperty("total_comparisons")
    private long totalComparisons;

    @JsonProperty("action_exact_matches")
    private long actionExactMatches;

    @JsonProperty("pending_shadow_executions")
    private int pendingShadowExecutions;

    public ObservabilitySummary() {
    }

    public ObservabilitySummary(
            long totalRequestsProcessed,
            long shadowErrorsOrTimeouts,
            long shadowTasksDropped,
            double exactMatchRatePercent,
            long totalComparisons,
            long actionExactMatches,
            int pendingShadowExecutions) {
        this.totalRequestsProcessed = totalRequestsProcessed;
        this.shadowErrorsOrTimeouts = shadowErrorsOrTimeouts;
        this.shadowTasksDropped = shadowTasksDropped;
        this.exactMatchRatePercent = exactMatchRatePercent;
        this.totalComparisons = totalComparisons;
        this.actionExactMatches = actionExactMatches;
        this.pendingShadowExecutions = pendingShadowExecutions;
    }

    public long getTotalRequestsProcessed() {
        return totalRequestsProcessed;
    }

    public long getShadowErrorsOrTimeouts() {
        return shadowErrorsOrTimeouts;
    }

    public long getShadowTasksDropped() {
        return shadowTasksDropped;
    }

    public double getExactMatchRatePercent() {
        return exactMatchRatePercent;
    }

    public long getTotalComparisons() {
        return totalComparisons;
    }

    public long getActionExactMatches() {
        return actionExactMatches;
    }

    public int getPendingShadowExecutions() {
        return pendingShadowExecutions;
    }
}
