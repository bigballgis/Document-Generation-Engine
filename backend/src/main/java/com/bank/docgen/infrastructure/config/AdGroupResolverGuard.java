package com.bank.docgen.infrastructure.config;

import com.bank.docgen.apimgmt.service.AdGroupResolverProperties;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Startup fail-closed guard for AD Group resolver source (BDD-OPS-AD-GROUP-STUB-001 / ADR-0054).
 *
 * <p>Mirrors {@link ProductionSecretGuard} JWT enforce: an active {@code prod} profile refuses
 * the config-file stub even when {@code APP_ENVIRONMENT=dev} (acceptance compose). Local docker
 * acceptance may set {@code docgen.ad-group-resolver.allow-config-stub-on-prod-profile=true}
 * explicitly (LAB ONLY — not production directory resolution).
 */
@Component
public class AdGroupResolverGuard implements ApplicationListener<ApplicationReadyEvent>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdGroupResolverGuard.class);

    private final Environment environment;
    private final AdGroupResolverProperties properties;

    public AdGroupResolverGuard(Environment environment, AdGroupResolverProperties properties) {
        this.environment = environment;
        this.properties = properties;
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
        if (!enforceAcceptanceOrProductionBoundary()) {
            return;
        }

        String resolverType = normalizeType(properties.getType());
        if (isConfigStubType(resolverType)) {
            if (properties.isAllowConfigStubOnProdProfile()) {
                LOGGER.warn(
                        "LAB ONLY: allowing config-file AD Group stub on prod-shaped path "
                                + "(docgen.ad-group-resolver.allow-config-stub-on-prod-profile=true). "
                                + "This is NOT production directory resolution."
                );
                return;
            }
            throw new IllegalStateException(
                    "Refusing to start with config-file AD Group stub on acceptance/production paths. "
                            + "Set a configured directory adapter SPI when available, or for local "
                            + "docker acceptance only set "
                            + "docgen.ad-group-resolver.allow-config-stub-on-prod-profile=true "
                            + "(LAB ONLY — not production AD)."
            );
        }

        // No real directory adapter ships in this slice — any non-config type fails closed.
        throw new IllegalStateException(
                "Refusing to start: AD Group resolver type '" + resolverType
                        + "' is unimplemented or not configured on acceptance/production paths. "
                        + "Company directory coordinates are UNKNOWN — do not invent LDAP hosts. "
                        + "Operator supplies a real directory adapter when available."
        );
    }

    /**
     * Same honesty as JWT: {@code prod} profile always enforces; otherwise enforce when outside
     * soft {@code dev}/{@code local}/{@code test} environments.
     */
    private boolean enforceAcceptanceOrProductionBoundary() {
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

    private static String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isConfigStubType(String normalizedType) {
        return normalizedType.isEmpty() || "config".equals(normalizedType);
    }
}
