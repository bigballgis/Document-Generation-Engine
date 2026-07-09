package com.bank.docgen.rendering;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
class PdfConversionInstrumentationConfig {

    @Bean
    @Primary
    PdfConversionService instrumentedPdfConversionService(
            PdfConversionMetrics metrics,
            ObjectProvider<LibreOfficePdfConversionService> cliProvider,
            ObjectProvider<DockerExecPdfConversionService> dockerProvider
    ) {
        PdfConversionService delegate = cliProvider.getIfAvailable();
        if (delegate == null) {
            delegate = dockerProvider.getIfAvailable(() -> {
                throw new IllegalStateException("No PdfConversionService implementation available");
            });
        }
        return new InstrumentedPdfConversionService(delegate, metrics);
    }
}
