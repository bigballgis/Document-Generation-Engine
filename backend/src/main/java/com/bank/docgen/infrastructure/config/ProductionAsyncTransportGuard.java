package com.bank.docgen.infrastructure.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class ProductionAsyncTransportGuard implements ApplicationListener<ApplicationReadyEvent>, InitializingBean {

    private final Environment environment;
    private final DocgenAsyncProperties asyncProperties;

    public ProductionAsyncTransportGuard(Environment environment, DocgenAsyncProperties asyncProperties) {
        this.environment = environment;
        this.asyncProperties = asyncProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        verifyOrThrow();
    }

    @Override
    public void afterPropertiesSet() {
        verifyOrThrow();
    }

    void verifyOrThrow() {
        if (!enforceGuard()) {
            return;
        }
        if (!"kafka".equalsIgnoreCase(asyncProperties.getTransport())) {
            throw new IllegalStateException(
                    "Refusing to start with non-kafka async transport outside dev/local/test profiles.");
        }
    }

    private boolean enforceGuard() {
        if (environment.acceptsProfiles(Profiles.of("dev", "local", "test"))) {
            return false;
        }
        String docgenEnvironment = environment.getProperty("docgen.environment", "dev");
        return !("dev".equalsIgnoreCase(docgenEnvironment)
                || "local".equalsIgnoreCase(docgenEnvironment)
                || "test".equalsIgnoreCase(docgenEnvironment));
    }
}
