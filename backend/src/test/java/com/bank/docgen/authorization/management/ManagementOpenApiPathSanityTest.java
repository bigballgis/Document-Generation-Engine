package com.bank.docgen.authorization.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Lightweight sanity check: every {@code /api/management/v1} path documented in OpenAPI maps to an
 * expected controller module package. Does not assert a full authorization matrix.
 */
class ManagementOpenApiPathSanityTest {

    private static final Path OPENAPI = Path.of("..", "docs", "api", "openapi-v1.yaml");
    private static final Pattern MANAGEMENT_PATH_PATTERN =
            Pattern.compile("^  (/api/management/v1\\S*):\\s*$");

    @ParameterizedTest
    @MethodSource("managementOpenApiPaths")
    void managementOpenApiPathMapsToKnownControllerPackage(String path) {
        assertThat(resolveExpectedPackagePrefix(path))
                .as("Add OpenAPI path mapping for %s", path)
                .isPresent();
    }

    @Test
    void openApiDocumentsAtLeastOneManagementPath() throws IOException {
        assertThat(loadManagementOpenApiPaths()).isNotEmpty();
    }

    private static List<String> managementOpenApiPaths() throws IOException {
        return loadManagementOpenApiPaths();
    }

    private static List<String> loadManagementOpenApiPaths() throws IOException {
        return Files.lines(OPENAPI)
                .map(MANAGEMENT_PATH_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .sorted()
                .toList();
    }

    private static java.util.Optional<String> resolveExpectedPackagePrefix(String path) {
        if (path.startsWith("/api/management/v1/content-modules")) {
            return java.util.Optional.of("com.bank.docgen.contentmodule");
        }
        if (path.startsWith("/api/management/v1/dashboard")) {
            return java.util.Optional.of("com.bank.docgen.dashboard");
        }
        if (path.startsWith("/api/management/v1/library/")) {
            return java.util.Optional.of("com.bank.docgen.library");
        }
        if (path.startsWith("/api/management/v1/legal-holds")) {
            return java.util.Optional.of("com.bank.docgen.legalhold");
        }
        if (path.startsWith("/api/management/v1/collaboration-work-items")
                || path.startsWith("/api/management/v1/collaboration-timeout-config")
                || path.startsWith("/api/management/v1/collaboration-notifications")) {
            return java.util.Optional.of("com.bank.docgen.collaboration");
        }
        if (path.startsWith("/api/management/v1/masters")) {
            return java.util.Optional.of("com.bank.docgen.master");
        }
        if (path.contains("/previews") || path.contains("/batch-tests")) {
            return java.util.Optional.of("com.bank.docgen.rendering");
        }
        if (path.startsWith("/api/management/v1/templates")) {
            return java.util.Optional.of("com.bank.docgen.template");
        }
        if (path.startsWith("/api/management/v1/admin/audit")
                || path.startsWith("/api/management/v1/audit/")) {
            return java.util.Optional.of("com.bank.docgen.audit");
        }
        if (path.startsWith("/api/management/v1/users")
                || path.startsWith("/api/management/v1/groups")
                || path.startsWith("/api/management/v1/auth")
                || path.startsWith("/api/management/v1/security-audit")) {
            return java.util.Optional.of("com.bank.docgen.authorization.management");
        }
        if (path.startsWith("/api/management/v1/risk-prompt-config")) {
            return java.util.Optional.of("com.bank.docgen.template");
        }
        if (path.startsWith("/api/management/v1/api-access")
                || path.startsWith("/api/management/v1/api/policies")
                || path.startsWith("/api/management/v1/invocations")) {
            return java.util.Optional.of("com.bank.docgen.apimgmt");
        }
        return java.util.Optional.empty();
    }
}
