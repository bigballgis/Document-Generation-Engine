package com.bank.docgen.infrastructure.config;

import com.bank.docgen.infrastructure.storage.StorageProperties;
import com.bank.docgen.sharedkernel.security.JwtProperties;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecretGuard implements ApplicationListener<ApplicationReadyEvent>, InitializingBean {

    private static final String DEFAULT_DATASOURCE_PASSWORD = "docgen_local_pwd";
    private static final String DEFAULT_MINIO_SECRET = "docgen_local_pwd";

    /**
     * Known insecure JWT values that must never run on acceptance/production-shaped paths
     * (BDD-OPS-JWT-SECRET-001). Documented local/test use remains allowed only on pure
     * {@code dev}/{@code local}/{@code test} paths without an active {@code prod} profile.
     */
    private static final Set<String> KNOWN_INSECURE_JWT_SECRETS = Set.of(
            "local-dev-only-change-me-please-32bytes-min",
            "prod-change-me-32-bytes-minimum-secret"
    );

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final StorageProperties storageProperties;
    private final String datasourcePassword;

    public ProductionSecretGuard(
            Environment environment,
            JwtProperties jwtProperties,
            StorageProperties storageProperties,
            @Value("${spring.datasource.password:}") String datasourcePassword
    ) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
        this.storageProperties = storageProperties;
        this.datasourcePassword = datasourcePassword;
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
        if (enforceJwtSecretGuard()) {
            String jwtSecret = normalizeSecret(jwtProperties.secret());
            if (jwtSecret.isEmpty() || KNOWN_INSECURE_JWT_SECRETS.contains(jwtSecret)) {
                throw new IllegalStateException(
                        "Refusing to start with default/insecure JWT secrets on acceptance/production paths.");
            }
        }

        if (!enforceInfrastructureSecretGuard()) {
            return;
        }
        boolean defaultDatabasePassword = DEFAULT_DATASOURCE_PASSWORD.equals(datasourcePassword);
        boolean defaultMinioSecret = "minio".equalsIgnoreCase(storageProperties.provider())
                && storageProperties.minio() != null
                && DEFAULT_MINIO_SECRET.equals(storageProperties.minio().secretKey());
        if (defaultDatabasePassword || defaultMinioSecret) {
            throw new IllegalStateException(
                    "Refusing to start with default secrets outside dev/local/test profiles.");
        }
    }

    /**
     * JWT blacklist applies whenever the {@code prod} profile is active (acceptance compose
     * uses {@code prod,dev} + {@code APP_ENVIRONMENT=dev}) or when the process is otherwise
     * outside soft local/dev/test environments.
     */
    private boolean enforceJwtSecretGuard() {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            return true;
        }
        return enforceInfrastructureSecretGuard();
    }

    private boolean enforceInfrastructureSecretGuard() {
        if (environment.acceptsProfiles(Profiles.of("dev", "local", "test"))) {
            return false;
        }
        String docgenEnvironment = environment.getProperty("docgen.environment", "dev");
        String normalized = docgenEnvironment.toLowerCase(Locale.ROOT);
        return !("dev".equals(normalized) || "local".equals(normalized) || "test".equals(normalized));
    }

    private static String normalizeSecret(String secret) {
        return secret == null ? "" : secret.trim();
    }
}
