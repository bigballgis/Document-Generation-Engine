package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.CoverageThresholdView;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.port.TemplateCoveragePort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TemplateCoverageAdapter implements TemplateCoveragePort {

    private final CoverageComputationService coverageComputationService;
    private final CoverageThresholdResolver coverageThresholdResolver;

    public TemplateCoverageAdapter(
            CoverageComputationService coverageComputationService,
            CoverageThresholdResolver coverageThresholdResolver
    ) {
        this.coverageComputationService = coverageComputationService;
        this.coverageThresholdResolver = coverageThresholdResolver;
    }

    @Override
    public CoverageSummaryView compute(UUID templateId, ManagementSessionClaims session) {
        return coverageComputationService.compute(templateId, session);
    }

    @Override
    public CoverageThresholdView resolveThreshold(String groupCode) {
        TemplateEntity template = new TemplateEntity(
                UUID.randomUUID(),
                "coverage-port",
                groupCode,
                "coverage-port",
                null,
                UUID.randomUUID(),
                "system"
        );
        return coverageThresholdResolver.resolveForTemplate(template);
    }
}
