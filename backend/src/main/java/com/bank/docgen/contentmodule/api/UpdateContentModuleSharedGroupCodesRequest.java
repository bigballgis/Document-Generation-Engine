package com.bank.docgen.contentmodule.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateContentModuleSharedGroupCodesRequest(
        List<@NotBlank @Size(max = 64) String> sharedGroupCodes
) {
    public UpdateContentModuleSharedGroupCodesRequest {
        sharedGroupCodes = DefensiveCopies.copyList(sharedGroupCodes);
    }
}
