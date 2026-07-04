package com.bank.docgen.apimgmt.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record SaveOutputPolicyRequest(List<String> outputFormats, List<String> outputModes, boolean confirmed) {
    public SaveOutputPolicyRequest {
        outputFormats = DefensiveCopies.copyList(outputFormats);
        outputModes = DefensiveCopies.copyList(outputModes);
    }

}
