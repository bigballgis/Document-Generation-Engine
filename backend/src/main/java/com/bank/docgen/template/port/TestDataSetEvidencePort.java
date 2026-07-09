package com.bank.docgen.template.port;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.Map;
import java.util.UUID;

/**
 * Test-data variable resolution and evidence locking for preview/batch runs.
 */
public interface TestDataSetEvidencePort {

    Map<String, Object> resolveVariables(UUID templateId, String externalId, ManagementSessionClaims session);

    void lockForEvidence(UUID templateId, String externalId);
}
