package com.bank.docgen.contentmodule.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record ContentModuleDetailView(
        String moduleId,
        String moduleCode,
        String groupCode,
        String name,
        String description,
        List<String> sharedGroupCodes,
        List<ContentModuleVersionView> versions
) {
    public ContentModuleDetailView {
        sharedGroupCodes = DefensiveCopies.copyStringList(sharedGroupCodes);
        versions = DefensiveCopies.copyList(versions);
    }

}
