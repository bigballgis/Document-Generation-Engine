package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

public class GroupNotFoundException extends ManagementNotFoundException {

    public GroupNotFoundException() {
        super(ApiErrorCodes.GROUP_NOT_FOUND, "api.error.notFound.groupNotFound");
    }
}
