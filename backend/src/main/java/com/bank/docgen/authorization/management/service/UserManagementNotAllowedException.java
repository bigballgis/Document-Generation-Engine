package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

/**
 * Raised when a caller without any administrator role attempts to manage users.
 * Maps to the generic management access-denied envelope (fail-closed).
 */
public class UserManagementNotAllowedException extends ManagementForbiddenException {

    public UserManagementNotAllowedException() {
        super(ApiErrorCodes.ACCESS_DENIED, "api.error.authorization.accessDenied");
    }
}
