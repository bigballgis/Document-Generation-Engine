package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.domain.ManagementRoute;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RouteVisibilityServiceTest {

    private final RouteVisibilityService routeVisibilityService = new RouteVisibilityService();

    @Test
    void globalAdminSeesAllRoutesAndLandsOnGlobalHome() {
        Set<ManagementRole> roles = Set.of(ManagementRole.GLOBAL_ADMIN);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.GLOBAL_GOVERNANCE_HOME.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.GLOBAL_GOVERNANCE_HOME.routeKey(),
                        ManagementRoute.GROUP_GOVERNANCE_HOME.routeKey(),
                        ManagementRoute.TEMPLATE_AUTHORING_HOME.routeKey(),
                        ManagementRoute.MASTER_MANAGEMENT.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey(),
                        ManagementRoute.TESTER_WORKBENCH.routeKey(),
                        ManagementRoute.APPROVER_WORKBENCH.routeKey(),
                        ManagementRoute.API_POLICY_MANAGEMENT.routeKey(),
                        ManagementRoute.AUDIT_CONSOLE.routeKey(),
                        ManagementRoute.IDENTITY_ADMINISTRATION.routeKey()
                );
    }

    @Test
    void groupAdminSeesGroupScopedRoutes() {
        Set<ManagementRole> roles = Set.of(ManagementRole.GROUP_ADMIN);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.GROUP_GOVERNANCE_HOME.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.GROUP_GOVERNANCE_HOME.routeKey(),
                        ManagementRoute.TEMPLATE_AUTHORING_HOME.routeKey(),
                        ManagementRoute.MASTER_MANAGEMENT.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey(),
                        ManagementRoute.API_POLICY_MANAGEMENT.routeKey(),
                        ManagementRoute.AUDIT_CONSOLE.routeKey(),
                        ManagementRoute.IDENTITY_ADMINISTRATION.routeKey()
                );
    }

    @Test
    void templateAuthorSeesAuthoringAndTemplateManagement() {
        Set<ManagementRole> roles = Set.of(ManagementRole.TEMPLATE_AUTHOR);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.TEMPLATE_AUTHORING_HOME.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.TEMPLATE_AUTHORING_HOME.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey()
                );
    }

    @Test
    void masterDesignerLandsOnAuthoringHomeWithMasterAndTemplateManagement() {
        Set<ManagementRole> roles = Set.of(ManagementRole.MASTER_DESIGNER);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.TEMPLATE_AUTHORING_HOME.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.TEMPLATE_AUTHORING_HOME.routeKey(),
                        ManagementRoute.MASTER_MANAGEMENT.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey()
                );
    }

    @Test
    void templateTesterLandsOnTesterWorkbench() {
        Set<ManagementRole> roles = Set.of(ManagementRole.TEMPLATE_TESTER);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.TESTER_WORKBENCH.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.TESTER_WORKBENCH.routeKey(),
                        ManagementRoute.TEMPLATE_MANAGEMENT.routeKey()
                );
    }

    @Test
    void templateApproverLandsOnApproverWorkbench() {
        Set<ManagementRole> roles = Set.of(ManagementRole.TEMPLATE_APPROVER);

        assertThat(routeVisibilityService.resolveDefaultRoute(roles))
                .isEqualTo(ManagementRoute.APPROVER_WORKBENCH.routeKey());
        assertThat(routeVisibilityService.resolveVisibleRoutes(roles))
                .containsExactly(
                        ManagementRoute.APPROVER_WORKBENCH.routeKey(),
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
