package com.bank.docgen.template.port;

import java.util.Map;
import java.util.UUID;

/**
 * CE-K03 compute evaluation seam for rendering/runtime assembly (port — no template.service import).
 */
public interface VariableComputePort {

    Map<String, Object> applyCompute(
            UUID templateVersionId,
            Map<String, Object> inputVariables,
            String localeTag
    );
}
