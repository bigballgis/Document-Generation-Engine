package com.bank.docgen.infrastructure.async;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.api.TraceIdConstants;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * LR-D4 Scenario A: sync request MDC traceId T must reach a real async worker thread.
 * Must use {@link ThreadPoolTaskExecutor} + {@link MdcTaskDecorator} — the {@code test}
 * profile {@code Runnable::run} executor would falsely pass without a decorator.
 */
class MdcTaskDecoratorTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void scenarioA_asyncWorkerRetainsCallerTraceId() throws Exception {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("mdc-scenario-a-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();

        String expectedTraceId = "trace-scenario-a-001";
        MDC.put(TraceIdConstants.MDC_KEY, expectedTraceId);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> workerTraceId = new AtomicReference<>();
        AtomicReference<String> workerThreadName = new AtomicReference<>();

        try {
            executor.execute(() -> {
                workerTraceId.set(MDC.get(TraceIdConstants.MDC_KEY));
                workerThreadName.set(Thread.currentThread().getName());
                latch.countDown();
            });

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(workerThreadName.get()).startsWith("mdc-scenario-a-");
            assertThat(workerTraceId.get()).isEqualTo(expectedTraceId);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void decoratorClearsMdcAfterTaskCompletes() throws Exception {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("mdc-clear-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();

        MDC.put(TraceIdConstants.MDC_KEY, "trace-to-clear");

        CountDownLatch firstDone = new CountDownLatch(1);
        CountDownLatch secondDone = new CountDownLatch(1);
        AtomicReference<String> secondTaskTraceId = new AtomicReference<>("unset");

        try {
            executor.execute(firstDone::countDown);
            assertThat(firstDone.await(5, TimeUnit.SECONDS)).isTrue();

            MDC.clear();
            executor.execute(() -> {
                secondTaskTraceId.set(MDC.get(TraceIdConstants.MDC_KEY));
                secondDone.countDown();
            });

            assertThat(secondDone.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondTaskTraceId.get()).isNull();
        } finally {
            executor.shutdown();
        }
    }
}
