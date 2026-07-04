package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

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
    public ContractResultView {
        paths = DefensiveCopies.copyList(paths);
        callableVersions = DefensiveCopies.copyList(callableVersions);
        schemas = DefensiveCopies.copyList(schemas);
        errorCodes = DefensiveCopies.copyList(errorCodes);
        examples = DefensiveCopies.copyList(examples);
    }

}
