package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class UserNotFoundException extends ManagementNotFoundException {

    public UserNotFoundException() {
        super(ApiErrorCodes.USER_NOT_FOUND, "api.error.notFound.userNotFound");
    }
}
