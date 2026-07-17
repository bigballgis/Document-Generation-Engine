package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * BDD-PRR-B01-TPC-006…008 — async transport fail-closed aligned with other Production*Guards.
 */
class ProductionAsyncTransportGuardTest {

    @Test
    void productionEnvironmentRejectsInProcessAsyncTransport() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");
        environment.setActiveProfiles("prod");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("in-process");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kafka");
    }

    @Test
    void productionEnvironmentAllowsKafkaAsyncTransport() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");
        environment.setActiveProfiles("prod");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("kafka");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        guard.verifyOrThrow();
    }

    @Test
    void prodProfileRejectsInProcessEvenWhenAppEnvironmentIsDev() {
        // Historical LAB mix: prod profile + APP_ENVIRONMENT=dev must still force kafka
        // (BDD-PRR-B01-TPC-006 — align with ProductionSecretGuard / AdGroupResolverGuard).
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "dev");
        environment.setActiveProfiles("prod");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("in-process");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kafka");
    }

    @Test
    void prodPlusDevProfilesRejectInProcessAsyncTransport() {
        // Critical honesty gap: any active "dev" profile must NOT skip enforce when "prod" is also active.
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "dev");
        environment.setActiveProfiles("prod", "dev");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("in-process");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kafka");
    }

    @Test
    void prodPlusDevProfilesAllowKafkaAsyncTransport() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "dev");
        environment.setActiveProfiles("prod", "dev");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("kafka");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        assertThatCode(guard::verifyOrThrow).doesNotThrowAnyException();
    }

    @Test
    void pureDevProfileAllowsInProcessAsyncTransport() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "dev");
        environment.setActiveProfiles("dev");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("in-process");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        assertThatCode(guard::verifyOrThrow).doesNotThrowAnyException();
    }

    @Test
    void pureLocalProfileAllowsInProcessAsyncTransport() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "local");
        environment.setActiveProfiles("local");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("in-process");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        assertThatCode(guard::verifyOrThrow).doesNotThrowAnyException();
    }

    @Test
    void pureTestProfileAllowsInProcessAsyncTransport() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "test");
        environment.setActiveProfiles("test");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("in-process");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        assertThatCode(guard::verifyOrThrow).doesNotThrowAnyException();
    }

    @Test
    void nonLocalEnvironmentWithoutProdProfileRejectsInProcess() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "staging");

        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        properties.setTransport("in-process");

        ProductionAsyncTransportGuard guard = new ProductionAsyncTransportGuard(environment, properties);

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kafka");
    }

    @Test
    void prodComposeFileUsesClaimedProductionDefaults() throws Exception {
        // BDD-PRR-B01-TPC-001…005 — claimed production compose contract.
        Path compose = resolveRepoFile("docker-compose.prod.yml");
        String content = Files.readString(compose);
        assertThat(content)
                .contains("SPRING_PROFILES_ACTIVE: prod")
                .doesNotContain("SPRING_PROFILES_ACTIVE: prod,dev")
                .contains("APP_ENVIRONMENT: prod")
                .contains("ASYNC_TRANSPORT: kafka")
                .doesNotContain("ASYNC_TRANSPORT:-in-process")
                .contains("DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED:-false")
                .doesNotContain("DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED:-true")
                .contains("DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB:-false")
                .doesNotContain("DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB:-true");
    }

    @Test
    void labComposeOverlayIsExplicitAndNotProdDefault() throws Exception {
        // BDD-PRR-B01-TPC-009
        Path lab = resolveRepoFile("docker-compose.lab.yml");
        String content = Files.readString(lab);
        assertThat(content)
                .containsIgnoringCase("LAB ONLY")
                .contains("DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB:-true")
                .contains("DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED:-true");
    }

    private static Path resolveRepoFile(String fileName) {
        Path compose = Path.of("..").resolve(fileName).toAbsolutePath().normalize();
        if (!Files.isRegularFile(compose)) {
            compose = Path.of(fileName).toAbsolutePath().normalize();
        }
        return compose;
    }
}
