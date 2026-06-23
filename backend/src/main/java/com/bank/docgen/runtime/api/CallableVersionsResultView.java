package com.bank.docgen.runtime.api;

import java.util.List;

public record CallableVersionsResultView(String templateId, List<CallableVersionView> versions) {
}
