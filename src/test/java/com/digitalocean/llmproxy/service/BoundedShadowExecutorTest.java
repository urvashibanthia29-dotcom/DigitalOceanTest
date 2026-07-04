package com.digitalocean.llmproxy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalocean.llmproxy.config.AppProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoundedShadowExecutorTest {

    private MetricsStore metricsStore;
    private BoundedShadowExecutor executor;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setShadowCorePoolSize(1);
        properties.setShadowMaxPoolSize(1);
        properties.setShadowQueueCapacity(1);
        properties.setMaxComparisonRecords(100);

        metricsStore = new MetricsStore(properties, new SimpleMeterRegistry());
        metricsStore.resetForTests();
        executor = new BoundedShadowExecutor(properties, metricsStore);
    }

    @Test
    void acceptsTaskWhenCapacityAvailable() {
        assertTrue(executor.tryDispatch(() -> {}));
        assertEquals(0, executor.getDroppedTasks());
        assertEquals(0, metricsStore.getShadowDroppedCount());
    }

    @Test
    void shedsLoadWhenQueueAndThreadsSaturated() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch block = new CountDownLatch(1);

        assertTrue(executor.tryDispatch(() -> {
            started.countDown();
            try {
                block.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }));
        assertTrue(executor.tryDispatch(() -> {}));

        started.await(2, TimeUnit.SECONDS);
        assertFalse(executor.tryDispatch(() -> {}));

        block.countDown();
        Thread.sleep(100);

        assertTrue(executor.getDroppedTasks() >= 1);
        assertTrue(metricsStore.getShadowDroppedCount() >= 1);
    }

    @Test
    void droppedTaskNeverExecutes() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch block = new CountDownLatch(1);
        AtomicInteger droppedTaskRan = new AtomicInteger(0);

        assertTrue(executor.tryDispatch(() -> {
            started.countDown();
            try {
                block.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }));
        assertTrue(executor.tryDispatch(() -> {}));
        started.await(2, TimeUnit.SECONDS);

        assertFalse(executor.tryDispatch(droppedTaskRan::incrementAndGet));

        block.countDown();
        Thread.sleep(200);

        assertEquals(0, droppedTaskRan.get());
    }
}
