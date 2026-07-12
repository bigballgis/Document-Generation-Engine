package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.storage.StorageProperties;
import com.bank.docgen.sharedkernel.security.JwtProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecretGuardTest {

    private static final String LOCAL_DEV_JWT = "local-dev-only-change-me-please-32bytes-min";
    private static final String PROD_CHANGE_ME_JWT = "prod-change-me-32-bytes-minimum-secret";
    private static final String SAFE_JWT = "prod-secret-at-least-32-bytes-long!!";

    @Test
    void productionEnvironmentRejectsDefaultSecrets() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties(LOCAL_DEV_JWT, "PT5M"),
                minioWithLocalDefaults(),
                "docgen_local_pwd"
        );

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default")
                .hasMessageNotContaining(LOCAL_DEV_JWT);
    }

    @Test
    void productionEnvironmentRejectsProdChangeMeJwtSecret() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties(PROD_CHANGE_ME_JWT, "PT5M"),
                safeStorage(),
                "prod-db-secret"
        );

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure")
                .hasMessageNotContaining(PROD_CHANGE_ME_JWT);
    }

    @Test
    void acceptancePathWithProdAndDevProfilesRejectsKnownInsecureJwt() {
        // docker-compose.prod.yml historically used SPRING_PROFILES_ACTIVE=prod,dev
        // and APP_ENVIRONMENT=dev — JWT must still fail-closed (BDD-OPS-JWT-SECRET-001 / S2b).
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "dev");
        environment.setActiveProfiles("prod", "dev");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties(PROD_CHANGE_ME_JWT, "PT5M"),
                minioWithLocalDefaults(),
                "docgen_local_pwd"
        );

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure")
                .hasMessageNotContaining(PROD_CHANGE_ME_JWT);
    }

    @Test
    void acceptancePathWithProdAndDevProfilesRejectsLocalDevJwt() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "dev");
        environment.setActiveProfiles("prod", "dev");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties(LOCAL_DEV_JWT, "PT5M"),
                minioWithLocalDefaults(),
                "docgen_local_pwd"
        );

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure")
                .hasMessageNotContaining(LOCAL_DEV_JWT);
    }

    @Test
    void acceptancePathAllowsNonDefaultJwtEvenWhenEnvironmentIsDev() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "dev");
        environment.setActiveProfiles("prod", "dev");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties(SAFE_JWT, "PT5M"),
                minioWithLocalDefaults(),
                "docgen_local_pwd"
        );

        assertThatCode(guard::verifyOrThrow).doesNotThrowAnyException();
    }

    @Test
    void productionEnvironmentRejectsBlankJwtSecret() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties("   ", "PT5M"),
                safeStorage(),
                "prod-db-secret"
        );

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure");
    }

    @Test
    void testProfileAllowsLocalDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");
        environment.setActiveProfiles("test");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties(LOCAL_DEV_JWT, "PT5M"),
                minioWithLocalDefaults(),
                "docgen_local_pwd"
        );

        guard.verifyOrThrow();
    }

    @Test
    void devProfileAloneAllowsDocumentedLocalJwt() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "dev");
        environment.setActiveProfiles("dev");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties(LOCAL_DEV_JWT, "PT5M"),
                minioWithLocalDefaults(),
                "docgen_local_pwd"
        );

        assertThatCode(guard::verifyOrThrow).doesNotThrowAnyException();
    }

    @Test
    void productionEnvironmentAllowsNonDefaultSecrets() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");

        ProductionSecretGuard guard = new ProductionSecretGuard(
                environment,
                new JwtProperties(SAFE_JWT, "PT5M"),
                safeStorage(),
                "prod-db-secret"
        );

        guard.verifyOrThrow();
    }

    @Test
    void prodComposeFileRequiresExplicitJwtSecretWithoutDefaultFallback() throws Exception {
        Path compose = Path.of("..").resolve("docker-compose.prod.yml").toAbsolutePath().normalize();
        if (!Files.isRegularFile(compose)) {
            compose = Path.of("docker-compose.prod.yml").toAbsolutePath().normalize();
        }
        String content = Files.readString(compose);
        org.assertj.core.api.Assertions.assertThat(content)
                .doesNotContain("JWT_SECRET:-")
                .doesNotContain("prod-change-me-32-bytes-minimum-secret")
                .contains("JWT_SECRET:");
    }

    private static StorageProperties minioWithLocalDefaults() {
        return new StorageProperties(
                "minio",
                "docgen-artifacts",
                new StorageProperties.MinioProperties("http://localhost:9000", "docgen", "docgen_local_pwd")
        );
    }

    private static StorageProperties safeStorage() {
        return new StorageProperties(
                "minio",
                "docgen-artifacts",
                new StorageProperties.MinioProperties("http://localhost:9000", "prod-access", "prod-minio-secret")
        );
    }
}
