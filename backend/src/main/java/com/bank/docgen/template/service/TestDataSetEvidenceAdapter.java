package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.port.TestDataSetEvidencePort;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TestDataSetEvidenceAdapter implements TestDataSetEvidencePort {

    private final TestDataSetService testDataSetService;

    public TestDataSetEvidenceAdapter(TestDataSetService testDataSetService) {
        this.testDataSetService = testDataSetService;
    }

    @Override
    public Map<String, Object> resolveVariables(UUID templateId, String externalId, ManagementSessionClaims session) {
        return testDataSetService.resolveVariables(templateId, externalId, session);
    }

    @Override
    public void lockForEvidence(UUID templateId, String externalId) {
        testDataSetService.lockForEvidence(templateId, externalId);
    }
}
