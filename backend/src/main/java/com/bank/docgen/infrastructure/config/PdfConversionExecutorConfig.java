package com.bank.docgen.infrastructure.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PdfConversionExecutorConfig {

    @Bean(name = "pdfConversionExecutor", destroyMethod = "shutdown")
    @Profile("!test")
    public ThreadPoolTaskExecutor pdfConversionExecutor(DocgenRenderingProperties renderingProperties) {
        int poolSize = renderingProperties.getConversionPoolSize();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(poolSize * 4);
        executor.setThreadNamePrefix("pdf-conversion-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
