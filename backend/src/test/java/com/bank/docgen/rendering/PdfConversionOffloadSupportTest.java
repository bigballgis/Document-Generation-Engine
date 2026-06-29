package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.template.service.TemplateValidationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class PdfConversionOffloadSupportTest {

    private ThreadPoolTaskExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    void runsConversionOffCallingThread() {
        executor = boundedExecutor(1);
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> workerThread = new AtomicReference<>();
        Executor trackingExecutor = task -> executor.execute(() -> {
            workerThread.set(Thread.currentThread());
            task.run();
        });

        byte[] result = PdfConversionOffloadSupport.executeOffloaded(
                trackingExecutor,
                5,
                () -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    return new byte[]{1};
                }
        );

        assertThat(result).containsExactly(1);
        assertThat(workerThread.get()).isNotNull();
        assertThat(workerThread.get()).isNotEqualTo(callerThread);
    }

    @Test
    void propagatesTemplateValidationException() {
        executor = boundedExecutor(1);

        assertThatThrownBy(() -> PdfConversionOffloadSupport.executeOffloaded(
                executor,
                5,
                () -> {
                    throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
                }
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.generation.pdfConversionFailed");
    }

    @Test
    void timesOutWhenConversionDoesNotFinish() {
        executor = boundedExecutor(1);
        Semaphore release = new Semaphore(0);

        assertThatThrownBy(() -> PdfConversionOffloadSupport.executeOffloaded(
                executor,
                1,
                () -> {
                    try {
                        release.acquire();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    return new byte[]{1};
                }
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.generation.pdfConversionFailed");
    }

    @Test
    void mapsRejectedExecutionToPdfConversionFailed() {
        ThreadPoolExecutor saturatedPool = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new java.util.concurrent.ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy()
        );
        try {
            CompletableFuture<Void> blocker = CompletableFuture.runAsync(
                    () -> {
                        try {
                            Thread.sleep(5_000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    },
                    saturatedPool
            );
            saturatedPool.submit(() -> {
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });

            assertThatThrownBy(() -> PdfConversionOffloadSupport.executeOffloaded(
                    saturatedPool,
                    5,
                    () -> new byte[]{1}
            ))
                    .isInstanceOf(TemplateValidationException.class)
                    .hasMessage("api.error.generation.pdfConversionFailed");

            blocker.cancel(true);
        } finally {
            saturatedPool.shutdownNow();
        }
    }

    private ThreadPoolTaskExecutor boundedExecutor(int poolSize) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(poolSize);
        taskExecutor.setMaxPoolSize(poolSize);
        taskExecutor.setQueueCapacity(poolSize * 4);
        taskExecutor.setThreadNamePrefix("pdf-conversion-test-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }
}
