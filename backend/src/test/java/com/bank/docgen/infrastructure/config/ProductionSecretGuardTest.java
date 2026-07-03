package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.storage.StorageProperties;
import com.bank.docgen.sharedkernel.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecretGuardTest {

    @Test
    void productionEnvironmentRejectsDefaultSecrets() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties("local-dev-only-change-me-please-32bytes-min", "PT5M"),
                new StorageProperties(
                        "minio",
                        "docgen-artifacts",
                        new StorageProperties.MinioProperties("http://localhost:9000", "docgen", "docgen_local_pwd")
                ),
                "docgen_local_pwd"
        );

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default secrets");
    }

    @Test
    void testProfileAllowsLocalDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");
        environment.setActiveProfiles("test");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties("local-dev-only-change-me-please-32bytes-min", "PT5M"),
                new StorageProperties(
                        "minio",
                        "docgen-artifacts",
                        new StorageProperties.MinioProperties("http://localhost:9000", "docgen", "docgen_local_pwd")
                ),
                "docgen_local_pwd"
        );

        guard.verifyOrThrow();
    }

    @Test
    void productionEnvironmentAllowsNonDefaultSecrets() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties("prod-secret-at-least-32-bytes-long!!", "PT5M"),
                new StorageProperties(
                        "minio",
                        "docgen-artifacts",
                        new StorageProperties.MinioProperties("http://localhost:9000", "prod-access", "prod-minio-secret")
                ),
                "prod-db-secret"
        );

        guard.verifyOrThrow();
    }
}
