package com.bank.docgen.template.api;

/**
 * CE-E01 v2 master revision fingerprint embedded in the export bundle.
 */
public record TemplateExportMasterPinView(
        String masterRevisionId,
        String masterFileHash,
        Integer revisionSequence,
        String pinOrigin
) {
}
