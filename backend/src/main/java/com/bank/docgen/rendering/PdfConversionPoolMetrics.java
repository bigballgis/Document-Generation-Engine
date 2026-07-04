package com.bank.docgen.rendering;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Exposes PDF conversion executor queue depth and active workers (SOR-P03).
 */
@Component
@Profile("!test")
public class PdfConversionPoolMetrics implements MeterBinder {

    private final ThreadPoolTaskExecutor pdfConversionExecutor;

    public PdfConversionPoolMetrics(@Qualifier("pdfConversionExecutor") ThreadPoolTaskExecutor pdfConversionExecutor) {
        this.pdfConversionExecutor = pdfConversionExecutor;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(
                        "docgen.pdf.conversion.pool.active",
                        pdfConversionExecutor,
                        executor -> executor.getThreadPoolExecutor().getActiveCount()
                )
                .description("Active PDF conversion worker threads")
                .register(registry);
        Gauge.builder(
                        "docgen.pdf.conversion.pool.queue.size",
                        pdfConversionExecutor,
                        executor -> executor.getThreadPoolExecutor().getQueue().size()
                )
                .description("Queued PDF conversion tasks waiting for a worker")
                .register(registry);
        Gauge.builder(
                        "docgen.pdf.conversion.pool.queue.remaining",
                        pdfConversionExecutor,
                        executor -> executor.getThreadPoolExecutor().getQueue().remainingCapacity()
                )
                .description("Remaining PDF conversion queue capacity")
                .register(registry);
    }
}
