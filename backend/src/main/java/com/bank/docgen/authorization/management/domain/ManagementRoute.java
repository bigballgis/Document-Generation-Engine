package com.bank.docgen.authorization.management.domain;

public enum ManagementRoute {
    GLOBAL_GOVERNANCE_HOME("route.global-governance-home"),
    GROUP_GOVERNANCE_HOME("route.group-governance-home"),
    TEMPLATE_AUTHORING_HOME("route.template-authoring-home"),
    API_POLICY_MANAGEMENT("route.api-policy-management"),
    AUDIT_CONSOLE("route.audit-console");

    private final String routeKey;

    ManagementRoute(String routeKey) {
        this.routeKey = routeKey;
    }

    public String routeKey() {
        return routeKey;
    }
}
