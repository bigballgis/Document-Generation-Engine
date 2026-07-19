package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * IBL-D4 / F22 — LibreOffice PDF conversion pool chaos / failover suite.
 *
 * <p>Deterministic harness (no real {@code soffice}): saturation, timeout, reject, and
 * post-pressure recovery, asserting IBL-B2 Micrometer names
 * ({@code docgen.pdf.conversion.pool.*}).
 *
 * <p>Focused lane: {@code -Plo-pool-chaos} (Surefire groups {@code lo-pool-chaos}).
 * Also included in default {@code mvn verify}.
 */
@Tag(PdfConversionPoolChaosTest.TAG)
class PdfConversionPoolChaosTest {

    static final String TAG = "lo-pool-chaos";

    private ThreadPoolTaskExecutor executor;
    private MeterRegistry registry;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        registry = null;
    }

    @Test
    void saturationFillsQueueGaugesThenRejectIncrementsB2RejectionCounter() throws Exception {
        // pool=2 / queue=2 — mirrors B2 absorb-then-Abort shape at small scale
        bindChaosPool(2, 2);
        Semaphore releaseWorkers = new Semaphore(0);
        PdfConversionPoolRejectionMetrics rejectionMetrics = new PdfConversionPoolRejectionMetrics(registry);

        for (int i = 0; i < 2; i++) {
            executor.execute(() -> {
                try {
                    releaseWorkers.acquire();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        awaitActiveCount(2);

        CountDownLatch callersEntered = new CountDownLatch(2);
        List<CompletableFuture<byte[]>> queuedCalls = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            queuedCalls.add(CompletableFuture.supplyAsync(() -> {
                callersEntered.countDown();
                return PdfConversionOffloadSupport.executeOffloaded(
                        executor,
                        30,
                        () -> {
                            try {
                                releaseWorkers.acquire();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                            return new byte[]{1};
                        },
                        rejectionMetrics::record
                );
            }));
        }

        assertThat(callersEntered.await(2, TimeUnit.SECONDS)).isTrue();
        awaitQueueSize(2);

        assertThat(gauge("docgen.pdf.conversion.pool.active")).isEqualTo(2.0);
        assertThat(gauge("docgen.pdf.conversion.pool.queue.size")).isEqualTo(2.0);
        assertThat(gauge("docgen.pdf.conversion.pool.queue.remaining")).isZero();
        assertThat(counter("docgen.pdf.conversion.pool.rejections")).isZero();

        assertThatThrownBy(() -> PdfConversionOffloadSupport.executeOffloaded(
                executor,
                5,
                () -> new byte[]{9},
                rejectionMetrics::record
        ))
                .isInstanceOf(PdfConversionCapacityExceededException.class);

        assertThat(counter("docgen.pdf.conversion.pool.rejections")).isEqualTo(1.0);

        releaseWorkers.release(4);
        for (CompletableFuture<byte[]> call : queuedCalls) {
            assertThat(call.get(5, TimeUnit.SECONDS)).containsExactly(1);
        }
    }

    @Test
    void hungConversionTimeoutDoesNotIncrementRejectionCounter() {
        bindChaosPool(1, 0);
        PdfConversionPoolRejectionMetrics rejectionMetrics = new PdfConversionPoolRejectionMetrics(registry);
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
                },
                rejectionMetrics::record
        ))
                .isInstanceOf(RenderingOperationException.class)
                .hasMessage("api.error.generation.pdfConversionFailed");

        assertThat(counter("docgen.pdf.conversion.pool.rejections")).isZero();
        release.release();
    }

    @Test
    void rejectPathRecordsB2CapacityExceededAndRejectionMetric() {
        bindChaosPool(1, 0);
        PdfConversionPoolRejectionMetrics rejectionMetrics = new PdfConversionPoolRejectionMetrics(registry);
        Semaphore hold = new Semaphore(0);

        executor.execute(() -> {
            try {
                hold.acquire();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        awaitActiveCount(1);

        assertThat(gauge("docgen.pdf.conversion.pool.active")).isEqualTo(1.0);
        assertThat(gauge("docgen.pdf.conversion.pool.queue.remaining")).isZero();

        assertThatThrownBy(() -> PdfConversionOffloadSupport.executeOffloaded(
                executor,
                5,
                () -> new byte[]{1},
                rejectionMetrics::record
        ))
                .isInstanceOf(PdfConversionCapacityExceededException.class);

        assertThat(counter("docgen.pdf.conversion.pool.rejections")).isEqualTo(1.0);
        hold.release();
    }

    @Test
    void failoverRecoversGaugesAndAcceptsWorkAfterPressureClears() throws Exception {
        bindChaosPool(1, 1);
        PdfConversionPoolRejectionMetrics rejectionMetrics = new PdfConversionPoolRejectionMetrics(registry);
        Semaphore holdWorker = new Semaphore(0);

        executor.execute(() -> {
            try {
                holdWorker.acquire();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        awaitActiveCount(1);

        CountDownLatch queuedEntered = new CountDownLatch(1);
        CompletableFuture<byte[]> queued = CompletableFuture.supplyAsync(() -> {
            queuedEntered.countDown();
            return PdfConversionOffloadSupport.executeOffloaded(
                    executor,
                    30,
                    () -> new byte[]{7},
                    rejectionMetrics::record
            );
        });
        assertThat(queuedEntered.await(2, TimeUnit.SECONDS)).isTrue();
        awaitQueueSize(1);

        assertThatThrownBy(() -> PdfConversionOffloadSupport.executeOffloaded(
                executor,
                5,
                () -> new byte[]{1},
                rejectionMetrics::record
        ))
                .isInstanceOf(PdfConversionCapacityExceededException.class);
        assertThat(counter("docgen.pdf.conversion.pool.rejections")).isEqualTo(1.0);

        holdWorker.release();
        assertThat(queued.get(5, TimeUnit.SECONDS)).containsExactly(7);

        awaitActiveCount(0);
        awaitQueueSizeAtMost(0);

        assertThat(gauge("docgen.pdf.conversion.pool.active")).isZero();
        assertThat(gauge("docgen.pdf.conversion.pool.queue.size")).isZero();
        assertThat(gauge("docgen.pdf.conversion.pool.queue.remaining")).isEqualTo(1.0);

        byte[] recovered = PdfConversionOffloadSupport.executeOffloaded(
                executor,
                5,
                () -> new byte[]{3},
                rejectionMetrics::record
        );
        assertThat(recovered).containsExactly(3);
        assertThat(counter("docgen.pdf.conversion.pool.rejections")).isEqualTo(1.0);
    }

    private void bindChaosPool(int poolSize, int queueCapacity) {
        registry = new SimpleMeterRegistry();
        executor = boundedExecutor(poolSize, queueCapacity);
        new PdfConversionPoolMetrics(executor).bindTo(registry);
    }

    private double gauge(String name) {
        Double value = registry.find(name).gauge().value();
        assertThat(value).as("gauge %s", name).isNotNull();
        return value;
    }

    private double counter(String name) {
        Double value = registry.find(name).counter().count();
        assertThat(value).as("counter %s", name).isNotNull();
        return value;
    }

    private void awaitActiveCount(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (executor.getThreadPoolExecutor().getActiveCount() == expected) {
                return;
            }
            sleepBriefly();
        }
        assertThat(executor.getThreadPoolExecutor().getActiveCount())
                .as("active count")
                .isEqualTo(expected);
    }

    private void awaitQueueSize(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (executor.getThreadPoolExecutor().getQueue().size() >= expected) {
                return;
            }
            sleepBriefly();
        }
        assertThat(executor.getThreadPoolExecutor().getQueue().size())
                .as("queue size >= %s", expected)
                .isGreaterThanOrEqualTo(expected);
    }

    private void awaitQueueSizeAtMost(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (executor.getThreadPoolExecutor().getQueue().size() <= expected) {
                return;
            }
            sleepBriefly();
        }
        assertThat(executor.getThreadPoolExecutor().getQueue().size())
                .as("queue size <= %s", expected)
                .isLessThanOrEqualTo(expected);
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadPoolTaskExecutor boundedExecutor(int poolSize, int queueCapacity) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(poolSize);
        taskExecutor.setMaxPoolSize(poolSize);
        taskExecutor.setQueueCapacity(queueCapacity);
        taskExecutor.setThreadNamePrefix("pdf-chaos-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }
}
