package com.digitalocean.llmproxy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalocean.llmproxy.config.AppProperties;
import com.digitalocean.llmproxy.model.metrics.ComparisonBreakdown;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsStoreTest {

    private MetricsStore metricsStore;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setMaxComparisonRecords(3);
        registry = new SimpleMeterRegistry();
        metricsStore = new MetricsStore(properties, registry);
        metricsStore.resetForTests();
    }

    @Test
    void comparisonRecordsBoundedToMax() {
        for (int i = 0; i < 5; i++) {
            metricsStore.recordComparison(registry, sampleRecord("req-" + i, i % 2 == 0));
        }
        assertEquals(3, metricsStore.getComparisonRecordCount());
    }

    @Test
    void observabilitySummaryTracksDroppedShadowTasks() {
        metricsStore.recordRequestProcessed();
        metricsStore.recordRequestProcessed();
        metricsStore.recordShadowDropped();
        metricsStore.recordShadowError();

        var summary = metricsStore.observabilitySummary();
        assertEquals(2, summary.getTotalRequestsProcessed());
        assertEquals(1, summary.getShadowTasksDropped());
        assertEquals(1, summary.getShadowErrorsOrTimeouts());
    }

    @Test
    void exactMatchRateComputedFromRetainedRecords() {
        metricsStore.recordComparison(registry, sampleRecord("a", true));
        metricsStore.recordComparison(registry, sampleRecord("b", true));
        metricsStore.recordComparison(registry, sampleRecord("c", false));

        var summary = metricsStore.observabilitySummary();
        assertEquals(3, summary.getTotalComparisons());
        assertEquals(2, summary.getActionExactMatches());
        assertTrue(summary.getExactMatchRatePercent() > 66.0 && summary.getExactMatchRatePercent() < 67.0);
    }

    private static MetricsStore.ComparisonRecord sampleRecord(String id, boolean match) {
        return new MetricsStore.ComparisonRecord(
                id,
                1.0,
                "primary",
                "candidate",
                10,
                20,
                new ComparisonBreakdown(true, true, "answer", "answer", match));
    }
}
