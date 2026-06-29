package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.PublishGateCheckCode;

public record PublishGateItemView(
        PublishGateCheckCode checkCode,
        boolean ready,
        boolean blocker,
        String messageKey,
        String summary
) {
}
