package com.bank.docgen.demo;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRoute;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

final class DemoCatalogSessions {

    private DemoCatalogSessions() {
    }

    static ManagementSessionClaims groupAdminSession() {
        return session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));
    }

    static ManagementSessionClaims globalAdminSession() {
        return session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
    }

    static ManagementSessionClaims templateAuthorSession() {
        return session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
    }

    static ManagementSessionClaims templateTesterSession() {
        return session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
    }

    static ManagementSessionClaims templateApproverSession() {
        return session("10000007", List.of("TEMPLATE_APPROVER"), List.of("RETAIL"));
    }

    static ManagementSessionClaims legalReviewerSession() {
        return session("10000009", List.of("LEGAL_REVIEWER"), List.of("RETAIL"));
    }

    private static ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                "Demo Seeder",
                "demo-seeder@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                ManagementRoute.DASHBOARD_HOME.routeKey(),
                List.of(ManagementRoute.DASHBOARD_HOME.routeKey()),
                Instant.now().plus(Duration.ofHours(1))
        );
    }
}
