package com.digitalocean.llmproxy.service;

import com.digitalocean.llmproxy.config.AppProperties;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Bounded, load-shedding executor for background shadow evaluations.
 * When the queue is saturated, tasks are dropped — never queued unboundedly and
 * never executed on the HTTP thread that serves primary traffic.
 */
@Component
public class BoundedShadowExecutor {

    private static final Logger log = LoggerFactory.getLogger(BoundedShadowExecutor.class);

    private final ThreadPoolTaskExecutor executor;
    private final AtomicLong droppedTasks = new AtomicLong(0);
    private final MetricsStore metricsStore;

    public BoundedShadowExecutor(AppProperties properties, MetricsStore metricsStore) {
        this.metricsStore = metricsStore;
        this.executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getShadowCorePoolSize());
        executor.setMaxPoolSize(properties.getShadowMaxPoolSize());
        executor.setQueueCapacity(properties.getShadowQueueCapacity());
        executor.setThreadNamePrefix("shadow-");
        executor.setRejectedExecutionHandler((task, pool) -> recordDrop("pool saturated at reject handler"));
        executor.initialize();
    }

    /**
     * Attempt to schedule shadow work. Returns {@code false} if load was shed.
     * The runnable is not executed when load is shed.
     */
    public boolean tryDispatch(Runnable task) {
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        if (!hasCapacity(pool)) {
            recordDrop("pre-check capacity exhausted");
            return false;
        }
        executor.execute(task);
        return true;
    }

    public boolean hasCapacity() {
        return hasCapacity(executor.getThreadPoolExecutor());
    }

    public long getDroppedTasks() {
        return droppedTasks.get();
    }

    public int getPendingQueueSize() {
        return executor.getThreadPoolExecutor().getQueue().size();
    }

    public int getActiveCount() {
        return executor.getThreadPoolExecutor().getActiveCount();
    }

    public int getQueueCapacity() {
        return executor.getThreadPoolExecutor().getQueue().remainingCapacity()
                + executor.getThreadPoolExecutor().getQueue().size();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private static boolean hasCapacity(ThreadPoolExecutor pool) {
        return pool.getQueue().remainingCapacity() > 0 || pool.getPoolSize() < pool.getMaximumPoolSize();
    }

    private void recordDrop(String reason) {
        droppedTasks.incrementAndGet();
        metricsStore.recordShadowDropped();
        log.warn("Shadow evaluation dropped ({})", reason);
    }
}
