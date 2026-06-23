package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class GroupManagementNotAllowedException extends ManagementForbiddenException {

    public GroupManagementNotAllowedException() {
        super(ApiErrorCodes.GROUP_MANAGEMENT_NOT_ALLOWED, "api.error.authorization.groupManagementNotAllowed");
    }
}
