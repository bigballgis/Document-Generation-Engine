package com.bank.docgen.authoring.structured;

import java.util.List;

public record ReferenceNodeValidationResult(
        StructuredContentValidationResult fidelity,
        List<AttachmentListReferenceModel> attachmentLists
) {

    public static ReferenceNodeValidationResult of(
            StructuredContentValidationResult fidelity,
            List<AttachmentListReferenceModel> rawAttachmentLists
    ) {
        return new ReferenceNodeValidationResult(
                fidelity,
                rawAttachmentLists == null ? List.of() : List.copyOf(rawAttachmentLists)
        );
    }
}
