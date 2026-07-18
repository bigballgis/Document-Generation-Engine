package com.bank.docgen.template.port;

import java.util.Map;
import java.util.UUID;

/**
 * IBL-A1 VariableSchema fail-closed validation seam for rendering/runtime assembly
 * (port — no template.service import from rendering).
 */
public interface VariableSchemaValidationPort {

    /**
     * Validate request variables against the version schema before compute/assemble.
     *
     * @throws com.bank.docgen.sharedkernel.document.variable.VariableValidationException on failure
     */
    void validateForAssembly(UUID templateVersionId, Map<String, Object> variables);
}
