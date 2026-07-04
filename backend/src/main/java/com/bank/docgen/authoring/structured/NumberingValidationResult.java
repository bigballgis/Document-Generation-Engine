package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record NumberingValidationResult(
        StructuredContentValidationResult fidelity,
        List<NumberingSequenceEntry> sequence
) {

    public NumberingValidationResult {
        sequence = DefensiveCopies.copyList(sequence);
    }

    public static NumberingValidationResult of(
            StructuredContentValidationResult fidelity,
            List<NumberingSequenceEntry> rawSequence
    ) {
        return new NumberingValidationResult(fidelity, rawSequence);
    }
}
