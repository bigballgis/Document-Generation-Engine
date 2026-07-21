package com.bank.docgen.sharedkernel.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.authorization.management.api.CreateUserRequest;
import com.bank.docgen.authorization.management.api.ManagementSessionView;
import com.bank.docgen.authorization.management.api.ManagementUserView;
import com.bank.docgen.authorization.management.api.UpdateUserRequest;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.runtime.api.ApiPolicySummaryView;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecuritySessionDtoImmutabilityTest {

    @Test
    void managementSessionClaimsDefensivelyCopiesMutableInputs() {
        List<String> roles = new ArrayList<>(List.of("GLOBAL_ADMIN"));
        List<String> groups = new ArrayList<>(List.of("RETAIL"));
        List<String> routes = new ArrayList<>(List.of("/templates"));

        ManagementSessionClaims claims = claims(roles, groups, routes);

        roles.add("DOCUMENT_AUTHOR");
        groups.add("CORP");
        routes.add("/masters");

        assertThat(claims.roles()).containsExactly("GLOBAL_ADMIN");
        assertThat(claims.authorizedGroupCodes()).containsExactly("RETAIL");
        assertThat(claims.visibleRoutes()).containsExactly("/templates");
        assertThatThrownBy(() -> claims.roles().add("X")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void managementSessionViewDefensivelyCopiesMutableInputs() {
        List<String> roles = new ArrayList<>(List.of("GROUP_ADMIN"));
        List<String> groups = new ArrayList<>(List.of("*"));
        List<String> routes = new ArrayList<>(List.of("/dashboard"));

        ManagementSessionView view = new ManagementSessionView(
                "10000001",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL.name(),
                roles,
                groups,
                "/dashboard",
                routes,
                null,
                Instant.parse("2030-01-01T00:00:00Z"),
                Instant.parse("2030-01-01T08:00:00Z")
        );

        roles.add("GLOBAL_ADMIN");
        groups.add("RETAIL");
        routes.add("/settings");

        assertThat(view.roles()).containsExactly("GROUP_ADMIN");
        assertThat(view.authorizedGroupCodes()).containsExactly("*");
        assertThat(view.visibleRoutes()).containsExactly("/dashboard");
        assertThatThrownBy(() -> view.roles().add("X")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void managementUserViewDefensivelyCopiesMutableInputs() {
        List<String> roles = new ArrayList<>(List.of("DOCUMENT_AUTHOR"));
        List<String> groups = new ArrayList<>(List.of("RETAIL"));

        ManagementUserView view = new ManagementUserView(
                UUID.randomUUID().toString(),
                "10000002",
                "Author",
                "author@example.com",
                AuthSource.LOCAL.name(),
                roles,
                groups,
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
        );

        roles.add("GLOBAL_ADMIN");
        groups.add("CORP");

        assertThat(view.roles()).containsExactly("DOCUMENT_AUTHOR");
        assertThat(view.authorizedGroupCodes()).containsExactly("RETAIL");
        assertThatThrownBy(() -> view.roles().add("X")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void runtimeSessionClaimsDefensivelyCopiesCallerAdGroups() {
        List<String> adGroups = new ArrayList<>(List.of("DOCGEN-API-RETAIL"));

        RuntimeSessionClaims claims = new RuntimeSessionClaims(
                UUID.randomUUID(),
                "cred-ext-1",
                UUID.randomUUID(),
                "tpl-ext-1",
                "svc-account",
                adGroups
        );

        adGroups.add("DOCGEN-API-CORP");

        assertThat(claims.callerAdGroups()).containsExactly("DOCGEN-API-RETAIL");
        assertThatThrownBy(() -> claims.callerAdGroups().add("X")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void apiPolicySummaryViewDefensivelyCopiesOutputPolicyLists() {
        List<String> formats = new ArrayList<>(List.of("PDF"));
        List<String> modes = new ArrayList<>(List.of("ATTACHMENT"));

        ApiPolicySummaryView view = new ApiPolicySummaryView(
                1,
                Instant.parse("2026-01-01T00:00:00Z"),
                "10000001",
                formats,
                modes,
                null,
                null,
                null,
                null
        );

        formats.add("DOCX");
        modes.add("INLINE");

        assertThat(view.allowedOutputFormats()).containsExactly("PDF");
        assertThat(view.allowedOutputModes()).containsExactly("ATTACHMENT");
        assertThatThrownBy(() -> view.allowedOutputFormats().add("X"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void createUserRequestDefensivelyCopiesRoleAndGroupLists() {
        List<ManagementRole> roles = new ArrayList<>(List.of(ManagementRole.GROUP_ADMIN));
        List<String> groups = new ArrayList<>(List.of("RETAIL"));

        CreateUserRequest request = new CreateUserRequest(
                "10000003",
                "New User",
                "new@example.com",
                "InitialPass123!",
                roles,
                groups
        );

        roles.add(ManagementRole.GLOBAL_ADMIN);
        groups.add("CORP");

        assertThat(request.roles()).containsExactly(ManagementRole.GROUP_ADMIN);
        assertThat(request.authorizedGroupCodes()).containsExactly("RETAIL");
        assertThatThrownBy(() -> request.roles().add(ManagementRole.TEMPLATE_TESTER))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void updateUserRequestDefensivelyCopiesRoleAndGroupLists() {
        List<ManagementRole> roles = new ArrayList<>(List.of(ManagementRole.TEMPLATE_TESTER));
        List<String> groups = new ArrayList<>(List.of("RETAIL"));

        UpdateUserRequest request = new UpdateUserRequest(
                "Tester",
                "tester@example.com",
                roles,
                groups
        );

        roles.add(ManagementRole.GLOBAL_ADMIN);
        groups.add("CORP");

        assertThat(request.roles()).containsExactly(ManagementRole.TEMPLATE_TESTER);
        assertThat(request.authorizedGroupCodes()).containsExactly("RETAIL");
        assertThatThrownBy(() -> request.authorizedGroupCodes().add("X"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static ManagementSessionClaims claims(
            List<String> roles,
            List<String> groups,
            List<String> routes
    ) {
        return new ManagementSessionClaims(
                "10000001",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "/dashboard",
                routes,
                Instant.parse("2030-01-01T00:00:00Z")
        );
    }
}
