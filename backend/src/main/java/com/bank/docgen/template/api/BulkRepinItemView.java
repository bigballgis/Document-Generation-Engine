package com.bank.docgen.template.api;

public record BulkRepinItemView(
        String templateId,
        String templateVersionId,
        String referenceKey,
        String beforeSemanticVersion,
        String afterSemanticVersion,
        BulkRepinItemStatus status,
        String errorCode
) {
}
