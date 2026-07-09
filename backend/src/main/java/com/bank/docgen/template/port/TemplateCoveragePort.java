package com.bank.docgen.template.port;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.CoverageThresholdView;
import java.util.UUID;

/**
 * Coverage computation and threshold resolution for batch-test orchestration.
 */
public interface TemplateCoveragePort {

    CoverageSummaryView compute(UUID templateId, ManagementSessionClaims session);

    CoverageThresholdView resolveThreshold(String groupCode);
}
