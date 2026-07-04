package com.digitalocean.llmproxy.model.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComparisonBreakdown {

    @JsonProperty("primary_valid_json")
    private boolean primaryValidJson;

    @JsonProperty("candidate_valid_json")
    private boolean candidateValidJson;

    @JsonProperty("primary_action")
    private String primaryAction;

    @JsonProperty("candidate_action")
    private String candidateAction;

    @JsonProperty("action_exact_match")
    private boolean actionExactMatch;

    public ComparisonBreakdown() {
    }

    public ComparisonBreakdown(
            boolean primaryValidJson,
            boolean candidateValidJson,
            String primaryAction,
            String candidateAction,
            boolean actionExactMatch) {
        this.primaryValidJson = primaryValidJson;
        this.candidateValidJson = candidateValidJson;
        this.primaryAction = primaryAction;
        this.candidateAction = candidateAction;
        this.actionExactMatch = actionExactMatch;
    }

    public boolean isPrimaryValidJson() {
        return primaryValidJson;
    }

    public boolean isCandidateValidJson() {
        return candidateValidJson;
    }

    public String getPrimaryAction() {
        return primaryAction;
    }

    public String getCandidateAction() {
        return candidateAction;
    }

    public boolean isActionExactMatch() {
        return actionExactMatch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComparisonBreakdown that)) {
            return false;
        }
        return primaryValidJson == that.primaryValidJson
                && candidateValidJson == that.candidateValidJson
                && actionExactMatch == that.actionExactMatch
                && java.util.Objects.equals(primaryAction, that.primaryAction)
                && java.util.Objects.equals(candidateAction, that.candidateAction);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                primaryValidJson, candidateValidJson, primaryAction, candidateAction, actionExactMatch);
    }
}
