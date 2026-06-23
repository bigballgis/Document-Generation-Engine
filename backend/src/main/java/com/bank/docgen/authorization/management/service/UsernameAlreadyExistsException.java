package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class UsernameAlreadyExistsException extends ManagementConflictException {

    public UsernameAlreadyExistsException() {
        super(ApiErrorCodes.USERNAME_ALREADY_EXISTS, "api.error.conflict.usernameAlreadyExists");
    }
}
