package com.bank.docgen.authorization.management.api;

public record ManagementCapabilitiesView(
        boolean manageMasters,
        boolean reviewMasters,
        boolean authorTemplates,
        boolean decideTests,
        boolean decideApprovals,
        boolean publishTemplates,
        boolean stopTemplates,
        boolean restoreOrDeprecateTemplates,
        boolean manageApiPolicy,
        boolean deleteTemplates,
        boolean readAudit
) {
}
