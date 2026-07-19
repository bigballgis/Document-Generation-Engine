package com.bank.docgen.template.port;

import com.bank.docgen.template.persistence.TemplateVersionEntity;
import java.util.Map;
import java.util.UUID;

/**
 * In-flight version and pinned module structures needed for preview generation.
 */
public interface TemplateRenderContextPort {

    TemplateVersionEntity requireInFlightDevVersion(UUID templateId);

    /**
     * Resolves pinned CM structures after ADR-0063 Composition Inclusion evaluation
     * using the same evaluator as runtime generate (E2-C9).
     */
    Map<String, String> resolvePinnedContentStructures(
            UUID templateVersionId,
            CompositionInclusionAxes inclusionAxes
    );
}
