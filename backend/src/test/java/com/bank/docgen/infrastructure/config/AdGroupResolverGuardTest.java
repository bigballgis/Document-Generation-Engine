package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.apimgmt.service.AdGroupResolverProperties;
import com.bank.docgen.apimgmt.service.ConfigAdGroupResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * BDD-OPS-AD-GROUP-STUB-001 — S1 / S2 / S3 startup boundary for AD Group resolver.
 */
class AdGroupResolverGuardTest {

    @Test
    void s1_prodProfileRefusesConfigStubEvenWhenAppEnvironmentIsDev() {
        // docker-compose.prod.yml: SPRING_PROFILES_ACTIVE=prod,dev + APP_ENVIRONMENT=dev
        MockEnvironment environment = softDevEnvironment();
        environment.setActiveProfiles("prod", "dev");

        AdGroupResolverGuard guard = new AdGroupResolverGuard(environment, configProperties(false));

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("config")
                .hasMessageContaining("stub")
                .hasMessageNotContaining("password")
                .hasMessageNotContaining("secret")
                .hasMessageNotContaining("ldap://");
    }

    @Test
    void s1_productionEnvironmentRefusesConfigStubWithoutProdProfile() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");

        AdGroupResolverGuard guard = new AdGroupResolverGuard(environment, configProperties(false));

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("config")
                .hasMessageContaining("stub");
    }

    @Test
    void s1_blankTypeDefaultsToConfigStubAndIsRefusedOnProdProfile() {
        MockEnvironment environment = softDevEnvironment();
        environment.setActiveProfiles("prod", "dev");

        AdGroupResolverProperties properties = new AdGroupResolverProperties();
        properties.setType("  ");
        properties.setAllowConfigStubOnProdProfile(false);

        AdGroupResolverGuard guard = new AdGroupResolverGuard(environment, properties);

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stub");
    }

    @Test
    void s1_labOverrideAllowsConfigStubOnProdProfileForLocalAcceptance() {
        MockEnvironment environment = softDevEnvironment();
        environment.setActiveProfiles("prod", "dev");

        AdGroupResolverGuard guard = new AdGroupResolverGuard(environment, configProperties(true));

        assertThatCode(guard::verifyOrThrow).doesNotThrowAnyException();
    }

    @Test
    void s2_prodPathRefusesUnimplementedNonConfigType() {
        MockEnvironment environment = softDevEnvironment();
        environment.setActiveProfiles("prod", "dev");

        AdGroupResolverProperties properties = new AdGroupResolverProperties();
        properties.setType("ldap");
        properties.setAllowConfigStubOnProdProfile(false);

        AdGroupResolverGuard guard = new AdGroupResolverGuard(environment, properties);

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unimplemented")
                .hasMessageNotContaining("ldap://")
                .hasMessageNotContaining("password");
    }

    @Test
    void s2_labOverrideDoesNotAllowUnimplementedDirectoryType() {
        MockEnvironment environment = softDevEnvironment();
        environment.setActiveProfiles("prod", "dev");

        AdGroupResolverProperties properties = new AdGroupResolverProperties();
        properties.setType("directory");
        properties.setAllowConfigStubOnProdProfile(true);

        AdGroupResolverGuard guard = new AdGroupResolverGuard(environment, properties);

        assertThatThrownBy(guard::verifyOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unimplemented");
    }

    @Test
    void s3_devProfileAllowsConfigStub() {
        MockEnvironment environment = softDevEnvironment();
        environment.setActiveProfiles("dev");

        AdGroupResolverGuard guard = new AdGroupResolverGuard(environment, configProperties(false));

        assertThatCode(guard::verifyOrThrow).doesNotThrowAnyException();
    }

    @Test
    void s3_testProfileAllowsConfigStubEvenWhenDocgenEnvironmentIsProd() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("docgen.environment", "prod");
        environment.setActiveProfiles("test");

        AdGroupResolverGuard guard = new AdGroupResolverGuard(environment, configProperties(false));

        assertThatCode(guard::verifyOrThrow).doesNotThrowAnyException();
    }

    @Test
    void s3_configResolverReturnsKnownGroupsAndEmptyForUnknown() {
        AdGroupResolverProperties properties = new AdGroupResolverProperties();
        properties.setType("config");
        properties.setAccountGroups(Map.of(
                "svc-caller", List.of("RETAIL_API", "CORP_API")
        ));
        ConfigAdGroupResolver resolver = new ConfigAdGroupResolver(properties);

        org.assertj.core.api.Assertions.assertThat(resolver.resolveGroups("svc-caller"))
                .containsExactly("RETAIL_API", "CORP_API");
        org.assertj.core.api.Assertions.assertThat(resolver.resolveGroups("unknown-account"))
                .isEmpty();
        org.assertj.core.api.Assertions.assertThat(resolver.resolveGroups(" "))
                .isEmpty();
        org.assertj.core.api.Assertions.assertThat(resolver.resolveGroups(null))
                .isEmpty();
        org.assertj.core.api.Assertions.assertThat(
                resolver.isAuthorized("svc-caller", List.of("RETAIL_API"))
        ).isTrue();
        org.assertj.core.api.Assertions.assertThat(
                resolver.isAuthorized("unknown-account", List.of("RETAIL_API"))
        ).isFalse();
    }

    @Test
    void prodComposeFileDocumentsLabOnlyConfigStubOverride() throws Exception {
        Path compose = Path.of("..").resolve("docker-compose.prod.yml").toAbsolutePath().normalize();
        if (!Files.isRegularFile(compose)) {
            compose = Path.of("docker-compose.prod.yml").toAbsolutePath().normalize();
        }
        String content = Files.readString(compose);
        org.assertj.core.api.Assertions.assertThat(content)
                .contains("DOCGEN_AD_GROUP_RESOLVER_ALLOW_CONFIG_STUB_ON_PROD_PROFILE")
                .containsIgnoringCase("LAB ONLY");
    }

    private static MockEnvironment softDevEnvironment() {
        return new MockEnvironment().withProperty("docgen.environment", "dev");
    }

    private static AdGroupResolverProperties configProperties(boolean allowLabOverride) {
        AdGroupResolverProperties properties = new AdGroupResolverProperties();
        properties.setType("config");
        properties.setAllowConfigStubOnProdProfile(allowLabOverride);
        return properties;
    }
}
