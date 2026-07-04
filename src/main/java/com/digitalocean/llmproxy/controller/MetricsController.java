package com.digitalocean.llmproxy.controller;

import com.digitalocean.llmproxy.model.metrics.ObservabilitySummary;
import com.digitalocean.llmproxy.service.MetricsStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    private final MetricsStore metricsStore;

    public MetricsController(MetricsStore metricsStore) {
        this.metricsStore = metricsStore;
    }

    @GetMapping("/metrics")
    public ObservabilitySummary metrics() {
        return metricsStore.observabilitySummary();
    }

    @GetMapping("/healthz")
    public HealthResponse healthz() {
        return new HealthResponse("ok");
    }

    public record HealthResponse(String status) {
    }
}
