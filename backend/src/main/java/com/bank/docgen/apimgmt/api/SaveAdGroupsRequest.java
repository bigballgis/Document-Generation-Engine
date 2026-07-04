package com.bank.docgen.apimgmt.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record SaveAdGroupsRequest(List<String> allowedAdGroups, boolean confirmed) {
    public SaveAdGroupsRequest {
        allowedAdGroups = DefensiveCopies.copyList(allowedAdGroups);
    }

}
