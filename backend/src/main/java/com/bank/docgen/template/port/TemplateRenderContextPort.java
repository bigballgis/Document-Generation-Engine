package com.bank.docgen.template.port;

import com.bank.docgen.template.persistence.TemplateVersionEntity;
import java.util.Map;
import java.util.UUID;

/**
 * In-flight version and pinned module structures needed for preview generation.
 */
public interface TemplateRenderContextPort {

    TemplateVersionEntity requireInFlightDevVersion(UUID templateId);

    Map<String, String> resolvePinnedContentStructures(UUID templateVersionId);
}
