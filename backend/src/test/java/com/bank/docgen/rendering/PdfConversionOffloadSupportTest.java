package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.rendering.RenderingOperationException;
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
    void propagatesRenderingOperationException() {
        executor = boundedExecutor(1);

        assertThatThrownBy(() -> PdfConversionOffloadSupport.executeOffloaded(
                executor,
                5,
                () -> {
                    throw new RenderingOperationException("api.error.generation.pdfConversionFailed");
                }
        ))
                .isInstanceOf(RenderingOperationException.class)
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
                .isInstanceOf(RenderingOperationException.class)
                .hasMessage("api.error.generation.pdfConversionFailed");
    }

    @Test
    void mapsRejectedExecutionToCapacityExceeded() {
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
                    .isInstanceOf(PdfConversionCapacityExceededException.class);

            blocker.cancel(true);
        } finally {
            saturatedPool.shutdownNow();
        }
    }

    @Test
    void failsFastWhenPoolAlreadySaturated() {
        executor = boundedExecutor(1);
        executor.execute(() -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        // Wait until worker is active.
        awaitActiveWorker(executor);

        assertThatThrownBy(() -> PdfConversionOffloadSupport.executeOffloaded(
                executor,
                5,
                () -> new byte[]{1}
        ))
                .isInstanceOf(PdfConversionCapacityExceededException.class);
    }

    @Test
    void recordsRejectionCallbackWhenPoolSaturated() {
        executor = boundedExecutor(1);
        executor.execute(() -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        awaitActiveWorker(executor);
        java.util.concurrent.atomic.AtomicInteger rejections = new java.util.concurrent.atomic.AtomicInteger();

        assertThatThrownBy(() -> PdfConversionOffloadSupport.executeOffloaded(
                executor,
                5,
                () -> new byte[]{1},
                rejections::incrementAndGet
        ))
                .isInstanceOf(PdfConversionCapacityExceededException.class);
        assertThat(rejections.get()).isEqualTo(1);
    }

    private void awaitActiveWorker(ThreadPoolTaskExecutor taskExecutor) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (taskExecutor.getThreadPoolExecutor().getActiveCount() > 0) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private ThreadPoolTaskExecutor boundedExecutor(int poolSize) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(poolSize);
        taskExecutor.setMaxPoolSize(poolSize);
        taskExecutor.setQueueCapacity(0);
        taskExecutor.setThreadNamePrefix("pdf-conversion-test-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }
}
