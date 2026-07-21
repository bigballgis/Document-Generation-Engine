package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.domain.ManagementRoute;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RouteVisibilityServiceTest {

    private final RouteVisibilityService routeVisibilityService = new RouteVisibilityService();

    @Test
    void globalAdminSeesOrganizedConsoleRoutesAndLandsOnDashboard() {
        Set<ManagementRole> roles = Set.of(ManagementRole.GLOBAL_ADMIN);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.DASHBOARD_HOME.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.DASHBOARD_HOME.routeKey(),
                        ManagementRoute.MASTER_MANAGEMENT.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey(),
                        ManagementRoute.CONTENT_MODULE_MANAGEMENT.routeKey(),
                        ManagementRoute.ASSET_LIBRARY_MANAGEMENT.routeKey(),
                        ManagementRoute.API_POLICY_MANAGEMENT.routeKey(),
                        ManagementRoute.AUDIT_CONSOLE.routeKey(),
                        ManagementRoute.IDENTITY_ADMINISTRATION.routeKey(),
                        ManagementRoute.LEGAL_HOLD_ADMINISTRATION.routeKey()
                );
    }

    @Test
    void groupAdminSeesGroupScopedRoutes() {
        Set<ManagementRole> roles = Set.of(ManagementRole.GROUP_ADMIN);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.DASHBOARD_HOME.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.DASHBOARD_HOME.routeKey(),
                        ManagementRoute.MASTER_MANAGEMENT.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey(),
                        ManagementRoute.CONTENT_MODULE_MANAGEMENT.routeKey(),
                        ManagementRoute.ASSET_LIBRARY_MANAGEMENT.routeKey(),
                        ManagementRoute.API_POLICY_MANAGEMENT.routeKey(),
                        ManagementRoute.AUDIT_CONSOLE.routeKey(),
                        ManagementRoute.IDENTITY_ADMINISTRATION.routeKey()
                );
    }

    @Test
    void documentAuthorSeesMasterTemplateContentAndAssetRoutes() {
        Set<ManagementRole> roles = Set.of(ManagementRole.DOCUMENT_AUTHOR);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.DASHBOARD_HOME.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.DASHBOARD_HOME.routeKey(),
                        ManagementRoute.MASTER_MANAGEMENT.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey(),
                        ManagementRoute.CONTENT_MODULE_MANAGEMENT.routeKey(),
                        ManagementRoute.ASSET_LIBRARY_MANAGEMENT.routeKey()
                );
    }

    @Test
    void templateTesterLandsOnDashboardWithTemplateManagement() {
        Set<ManagementRole> roles = Set.of(ManagementRole.TEMPLATE_TESTER);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.DASHBOARD_HOME.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.DASHBOARD_HOME.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey(),
                        ManagementRoute.ASSET_LIBRARY_MANAGEMENT.routeKey()
                );
    }

    @Test
    void legalReviewerLandsOnDashboardWithTemplateManagement() {
        Set<ManagementRole> roles = Set.of(ManagementRole.LEGAL_REVIEWER);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.DASHBOARD_HOME.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.DASHBOARD_HOME.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey()
                );
    }

    @Test
    void auditAdminSeesAuditConsoleOnly() {
        Set<ManagementRole> roles = Set.of(ManagementRole.AUDIT_ADMIN);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.AUDIT_CONSOLE.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(ManagementRoute.AUDIT_CONSOLE.routeKey());
    }
}
