package com.bank.docgen.infrastructure.config;

import com.bank.docgen.infrastructure.storage.StorageProperties;
import com.bank.docgen.sharedkernel.security.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecretGuard implements ApplicationListener<ApplicationReadyEvent>, InitializingBean {

    private static final String DEFAULT_DATASOURCE_PASSWORD = "docgen_local_pwd";
    private static final String DEFAULT_JWT_SECRET = "local-dev-only-change-me-please-32bytes-min";
    private static final String DEFAULT_MINIO_SECRET = "docgen_local_pwd";

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
        if (!enforceGuard()) {
            return;
        }
        boolean defaultDatabasePassword = DEFAULT_DATASOURCE_PASSWORD.equals(datasourcePassword);
        boolean defaultJwtSecret = DEFAULT_JWT_SECRET.equals(jwtProperties.secret());
        boolean defaultMinioSecret = "minio".equalsIgnoreCase(storageProperties.provider())
                && storageProperties.minio() != null
                && DEFAULT_MINIO_SECRET.equals(storageProperties.minio().secretKey());
        if (defaultDatabasePassword || defaultJwtSecret || defaultMinioSecret) {
            throw new IllegalStateException(
                    "Refusing to start with default secrets outside dev/local/test profiles.");
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
