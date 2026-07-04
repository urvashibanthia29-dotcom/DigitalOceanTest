package com.digitalocean.llmproxy.service;

import com.digitalocean.llmproxy.config.AppProperties;
import com.digitalocean.llmproxy.model.metrics.ComparisonBreakdown;
import com.digitalocean.llmproxy.model.metrics.ObservabilitySummary;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class MetricsStore {

    private final int maxRecords;
    private final Object lock = new Object();
    private final Deque<ComparisonRecord> records = new ArrayDeque<>();
    private final AtomicInteger pending = new AtomicInteger(0);
    private final AtomicLong totalRequestsProcessed = new AtomicLong(0);
    private final AtomicLong shadowErrors = new AtomicLong(0);
    private final AtomicLong shadowTimeouts = new AtomicLong(0);
    private final AtomicLong shadowDropped = new AtomicLong(0);

    private final Counter primaryErrorCounter;
    private final Counter shadowErrorCounter;
    private final Counter shadowDroppedCounter;

    public MetricsStore(AppProperties properties, MeterRegistry registry) {
        this.maxRecords = properties.getMaxComparisonRecords();

        Gauge.builder("proxy_pending_comparisons", pending, AtomicInteger::get)
                .description("Background candidate comparisons in flight")
                .register(registry);

        Gauge.builder("proxy_shadow_tasks_dropped", shadowDropped, AtomicLong::get)
                .description("Shadow evaluations shed under load")
                .register(registry);

        this.primaryErrorCounter = Counter.builder("proxy_llm_errors_total")
                .tag("role", "primary")
                .description("Upstream LLM errors")
                .register(registry);

        this.shadowErrorCounter = Counter.builder("proxy_shadow_errors_total")
                .description("Shadow execution errors or timeouts")
                .register(registry);

        this.shadowDroppedCounter = Counter.builder("proxy_shadow_tasks_dropped_total")
                .description("Shadow evaluations dropped due to bounded queue")
                .register(registry);
    }

    public void recordRequestProcessed() {
        totalRequestsProcessed.incrementAndGet();
    }

    public void markPending(int delta) {
        pending.addAndGet(delta);
    }

    public void recordPrimaryError() {
        primaryErrorCounter.increment();
    }

    public void recordShadowError() {
        shadowErrors.incrementAndGet();
        shadowErrorCounter.increment();
    }

    public void recordShadowTimeout() {
        shadowTimeouts.incrementAndGet();
        shadowErrorCounter.increment();
    }

    public void recordShadowDropped() {
        shadowDropped.incrementAndGet();
        shadowDroppedCounter.increment();
    }

    public void recordComparison(MeterRegistry registry, ComparisonRecord record) {
        synchronized (lock) {
            records.addLast(record);
            while (records.size() > maxRecords) {
                records.removeFirst();
            }
        }

        if (record.breakdown().isActionExactMatch()) {
            Counter.builder("proxy_action_exact_match_total")
                    .register(registry)
                    .increment();
        } else {
            Counter.builder("proxy_action_mismatch_total")
                    .register(registry)
                    .increment();
        }
    }

    public ObservabilitySummary observabilitySummary() {
        synchronized (lock) {
            long comparisons = records.size();
            long matches = records.stream()
                    .filter(record -> record.breakdown().isActionExactMatch())
                    .count();
            double matchRate = comparisons == 0 ? 0.0 : round2((double) matches / comparisons * 100.0);

            return new ObservabilitySummary(
                    totalRequestsProcessed.get(),
                    shadowErrors.get() + shadowTimeouts.get(),
                    shadowDropped.get(),
                    matchRate,
                    comparisons,
                    matches,
                    pending.get());
        }
    }

    public int getMaxRecords() {
        return maxRecords;
    }

    public int getComparisonRecordCount() {
        synchronized (lock) {
            return records.size();
        }
    }

    public long getShadowDroppedCount() {
        return shadowDropped.get();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record ComparisonRecord(
            String requestId,
            double timestamp,
            String primaryModel,
            String candidateModel,
            double primaryLatencyMs,
            double candidateLatencyMs,
            ComparisonBreakdown breakdown) {
    }

    /** Test-only helper to isolate integration tests sharing one Spring context. */
    public void resetForTests() {
        synchronized (lock) {
            records.clear();
        }
        pending.set(0);
        totalRequestsProcessed.set(0);
        shadowErrors.set(0);
        shadowTimeouts.set(0);
        shadowDropped.set(0);
    }
}
