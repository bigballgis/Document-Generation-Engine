package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

/**
 * Fail-closed rejection when a client attempts to assign a retired or unknown
 * management role (ADR-0070 / BDD-SYS-NORM-ROLE-005). Maps to HTTP 422.
 */
public class RoleNotAssignableException extends RuntimeException {

    private final String errorCode;
    private final String messageKey;

    public RoleNotAssignableException(String roleCode) {
        super(roleCode == null || roleCode.isBlank()
                ? "Role code is required"
                : "Role not assignable: " + roleCode);
        this.errorCode = ApiErrorCodes.ROLE_NOT_ASSIGNABLE;
        this.messageKey = "api.error.authorization.roleNotAssignable";
    }

    public String errorCode() {
        return errorCode;
    }

    public String messageKey() {
        return messageKey;
    }
}
