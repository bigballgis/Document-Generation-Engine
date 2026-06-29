package com.bank.docgen.template.api;

public record ChangeDiffModificationView(
        String key,
        String changeType,
        String summary
) {
}
