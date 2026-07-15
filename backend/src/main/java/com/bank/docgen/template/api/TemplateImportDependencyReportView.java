package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record TemplateImportDependencyReportView(
        List<TemplateImportDependencyItemView> items,
        int blockingCount,
        int warningCount,
        int infoCount,
        boolean readyToCommit,
        String bundleFormat
) {
    public TemplateImportDependencyReportView {
        items = DefensiveCopies.copyList(items);
    }
}
