package com.bank.docgen.template.api;

public record TemplateExportResult(
        String format,
        TemplateExportBundleView bundle
) {
}
