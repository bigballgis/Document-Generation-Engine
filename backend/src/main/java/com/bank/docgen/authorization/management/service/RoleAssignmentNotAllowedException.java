package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class RoleAssignmentNotAllowedException extends ManagementForbiddenException {

    public RoleAssignmentNotAllowedException() {
        super(ApiErrorCodes.ROLE_ASSIGNMENT_NOT_ALLOWED, "api.error.authorization.roleAssignmentNotAllowed");
    }
}
