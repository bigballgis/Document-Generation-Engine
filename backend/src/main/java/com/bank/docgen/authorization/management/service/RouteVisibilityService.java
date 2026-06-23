package com.bank.docgen.authorization.management.service;

import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.domain.ManagementRoute;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RouteVisibilityService {

    public String resolveDefaultRoute(Set<ManagementRole> roles) {
        if (roles.contains(ManagementRole.AUDIT_ADMIN)) {
            return ManagementRoute.AUDIT_CONSOLE.routeKey();
        }
        if (roles.contains(ManagementRole.GLOBAL_ADMIN)
                || roles.contains(ManagementRole.GROUP_ADMIN)
                || roles.contains(ManagementRole.MASTER_DESIGNER)
                || roles.contains(ManagementRole.TEMPLATE_AUTHOR)
                || roles.contains(ManagementRole.TEMPLATE_TESTER)
                || roles.contains(ManagementRole.TEMPLATE_APPROVER)) {
            return ManagementRoute.DASHBOARD_HOME.routeKey();
        }
        throw new IllegalStateException("No default route for roles: " + roles);
    }

    public List<String> resolveVisibleRoutes(Set<ManagementRole> roles) {
        Set<String> visible = new LinkedHashSet<>();
        if (roles.contains(ManagementRole.GLOBAL_ADMIN)) {
            visible.add(ManagementRoute.DASHBOARD_HOME.routeKey());
            visible.add(ManagementRoute.MASTER_MANAGEMENT.routeKey());
            visible.add(ManagementRoute.TEMPLATE_MANAGEMENT.routeKey());
            visible.add(ManagementRoute.API_POLICY_MANAGEMENT.routeKey());
            visible.add(ManagementRoute.AUDIT_CONSOLE.routeKey());
            visible.add(ManagementRoute.IDENTITY_ADMINISTRATION.routeKey());
            return new ArrayList<>(visible);
        }
        if (roles.contains(ManagementRole.AUDIT_ADMIN)) {
            visible.add(ManagementRoute.AUDIT_CONSOLE.routeKey());
            return new ArrayList<>(visible);
        }
        if (roles.contains(ManagementRole.GROUP_ADMIN)) {
            visible.add(ManagementRoute.DASHBOARD_HOME.routeKey());
            visible.add(ManagementRoute.MASTER_MANAGEMENT.routeKey());
            visible.add(ManagementRoute.TEMPLATE_MANAGEMENT.routeKey());
            visible.add(ManagementRoute.API_POLICY_MANAGEMENT.routeKey());
            visible.add(ManagementRoute.AUDIT_CONSOLE.routeKey());
            visible.add(ManagementRoute.IDENTITY_ADMINISTRATION.routeKey());
        }
        if (roles.contains(ManagementRole.MASTER_DESIGNER)) {
            visible.add(ManagementRoute.DASHBOARD_HOME.routeKey());
            visible.add(ManagementRoute.MASTER_MANAGEMENT.routeKey());
            visible.add(ManagementRoute.TEMPLATE_MANAGEMENT.routeKey());
        }
        if (roles.contains(ManagementRole.TEMPLATE_AUTHOR)) {
            visible.add(ManagementRoute.DASHBOARD_HOME.routeKey());
            visible.add(ManagementRoute.TEMPLATE_MANAGEMENT.routeKey());
        }
        if (roles.contains(ManagementRole.TEMPLATE_TESTER)) {
            visible.add(ManagementRoute.DASHBOARD_HOME.routeKey());
            visible.add(ManagementRoute.TEMPLATE_MANAGEMENT.routeKey());
        }
        if (roles.contains(ManagementRole.TEMPLATE_APPROVER)) {
            visible.add(ManagementRoute.DASHBOARD_HOME.routeKey());
            visible.add(ManagementRoute.TEMPLATE_MANAGEMENT.routeKey());
        }
        return new ArrayList<>(visible);
    }

    public boolean canAccessRoute(Set<ManagementRole> roles, String routeKey) {
        return resolveVisibleRoutes(roles).contains(routeKey);
    }

    public Set<ManagementRole> normalizeRoles(Set<ManagementRole> roles) {
        return roles == null || roles.isEmpty() ? Set.of() : EnumSet.copyOf(roles);
    }
}
