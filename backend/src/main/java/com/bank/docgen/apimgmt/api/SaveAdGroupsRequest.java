package com.bank.docgen.apimgmt.api;

import java.util.List;

public record SaveAdGroupsRequest(List<String> allowedAdGroups, boolean confirmed) {
}
