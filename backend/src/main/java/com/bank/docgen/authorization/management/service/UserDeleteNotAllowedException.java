package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class UserDeleteNotAllowedException extends ManagementForbiddenException {

    public UserDeleteNotAllowedException() {
        super(ApiErrorCodes.USER_DELETE_NOT_ALLOWED, "api.error.authorization.userDeleteNotAllowed");
    }
}
