package com.bank.docgen.infrastructure.config;

import java.util.Locale;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Startup fail-closed guard for async transport (BDD-PRR-B01-TPC-006…008).
 *
 * <p>Mirrors {@link ProductionSecretGuard} JWT / {@link AdGroupResolverGuard}: an active
 * {@code prod} profile always requires {@code kafka}, even when a {@code dev} profile is also
 * active or {@code APP_ENVIRONMENT=dev}. Pure {@code dev}/{@code local}/{@code test} paths
 * (no {@code prod}) may keep in-process transport.
 */
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
                    "Refusing to start with non-kafka async transport on acceptance/production paths.");
        }
    }

    /**
     * Same honesty as JWT / AD guards: {@code prod} profile always enforces; otherwise enforce
     * when outside soft {@code dev}/{@code local}/{@code test} environments.
     */
    private boolean enforceGuard() {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            return true;
        }
        if (environment.acceptsProfiles(Profiles.of("dev", "local", "test"))) {
            return false;
        }
        String docgenEnvironment = environment.getProperty("docgen.environment", "dev");
        String normalized = docgenEnvironment.toLowerCase(Locale.ROOT);
        return !("dev".equals(normalized) || "local".equals(normalized) || "test".equals(normalized));
    }
}
