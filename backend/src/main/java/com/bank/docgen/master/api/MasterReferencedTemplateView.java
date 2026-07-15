package com.bank.docgen.master.api;

/**
 * Template package referencing a master (CE-K05 impact analysis entry).
 */
public record MasterReferencedTemplateView(
        String templateId,
        String name,
        String lifecycleStatus,
        String externalId
) {
}
