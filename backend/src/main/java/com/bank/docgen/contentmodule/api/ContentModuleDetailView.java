package com.bank.docgen.contentmodule.api;

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
}
