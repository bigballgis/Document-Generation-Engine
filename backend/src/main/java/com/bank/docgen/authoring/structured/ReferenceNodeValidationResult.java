package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record ReferenceNodeValidationResult(
        StructuredContentValidationResult fidelity,
        List<AttachmentListReferenceModel> attachmentLists
) {

    public ReferenceNodeValidationResult {
        attachmentLists = DefensiveCopies.copyList(attachmentLists);
    }

    public static ReferenceNodeValidationResult of(
            StructuredContentValidationResult fidelity,
            List<AttachmentListReferenceModel> rawAttachmentLists
    ) {
        return new ReferenceNodeValidationResult(fidelity, rawAttachmentLists);
    }
}
