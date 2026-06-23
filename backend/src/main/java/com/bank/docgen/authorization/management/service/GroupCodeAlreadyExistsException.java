package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class GroupCodeAlreadyExistsException extends ManagementConflictException {

    public GroupCodeAlreadyExistsException() {
        super(ApiErrorCodes.GROUP_CODE_ALREADY_EXISTS, "api.error.conflict.groupCodeAlreadyExists");
    }
}
