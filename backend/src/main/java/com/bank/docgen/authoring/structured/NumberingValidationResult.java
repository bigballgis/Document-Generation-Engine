package com.bank.docgen.authoring.structured;

import java.util.List;

public record NumberingValidationResult(
        StructuredContentValidationResult fidelity,
        List<NumberingSequenceEntry> sequence
) {

    public static NumberingValidationResult of(
            StructuredContentValidationResult fidelity,
            List<NumberingSequenceEntry> rawSequence
    ) {
        return new NumberingValidationResult(
                fidelity,
                rawSequence == null ? List.of() : List.copyOf(rawSequence)
        );
    }
}
