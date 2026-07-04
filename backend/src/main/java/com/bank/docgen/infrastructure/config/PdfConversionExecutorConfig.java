package com.bank.docgen.infrastructure.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PdfConversionExecutorConfig {

    // destroyMethod "shutdown" honors waitForTasksToCompleteOnShutdown/awaitTermination below.
    @Bean(name = "pdfConversionExecutor", destroyMethod = "shutdown")
    @Profile("!test")
    public ThreadPoolTaskExecutor pdfConversionExecutor(DocgenRenderingProperties renderingProperties) {
        int poolSize = renderingProperties.getConversionPoolSize();
        int queueCapacity = renderingProperties.getConversionQueueCapacity();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("pdf-conversion-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // LR-B5: let running LibreOffice conversions finish (bounded) instead of killing them.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(25);
        executor.initialize();
        return executor;
    }
}
