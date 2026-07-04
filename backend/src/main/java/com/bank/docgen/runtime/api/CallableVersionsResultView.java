package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record CallableVersionsResultView(String templateId, List<CallableVersionView> versions) {
    public CallableVersionsResultView {
        versions = DefensiveCopies.copyList(versions);
    }
}
