package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class GroupScopeOutOfRangeException extends ManagementForbiddenException {

    public GroupScopeOutOfRangeException() {
        super(ApiErrorCodes.GROUP_SCOPE_OUT_OF_RANGE, "api.error.authorization.groupScopeOutOfRange");
    }
}
