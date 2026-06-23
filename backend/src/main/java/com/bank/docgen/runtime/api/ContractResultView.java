package com.bank.docgen.runtime.api;

import java.util.List;

public record ContractResultView(
        String templateId,
        List<String> paths,
        DefaultRouteSummaryView defaultRoute,
        ApiPolicySummaryView apiPolicy,
        List<CallableVersionView> callableVersions,
        List<String> schemas,
        List<ErrorCodeSummaryView> errorCodes,
        List<String> examples
) {
}
