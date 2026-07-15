package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.TEMPLATE_TEST_DATA_PII_EXPLICIT_CONFIRM;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class TestDataSetAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    TestDataSetAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void recordTestDataPiiExplicitConfirm(
            UUID templateId,
            String groupCode,
            String testDataSetId,
            int datasetVersion,
            String variablesHash,
            List<String> piiFieldKeys,
            Map<String, String> piiCategories,
            String piiConfirmReason,
            String actorUsername,
            String actorSummary
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("testDataSetId", testDataSetId);
        detail.put("datasetVersion", datasetVersion);
        detail.put("variablesHash", variablesHash);
        detail.put("piiFieldKeys", piiFieldKeys);
        detail.put("piiCategories", piiCategories);
        detail.put("piiHandling", "EXPLICIT_SENSITIVE");
        detail.put("piiConfirmReason", piiConfirmReason);

        eventWriter.persist(
                TEMPLATE_TEST_DATA_PII_EXPLICIT_CONFIRM,
                templateId,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate(
                        "Test data PII explicit confirm: " + testDataSetId + " keys=" + piiFieldKeys.size()
                ),
                eventWriter.writeJsonMap(detail)
        );
    }
}
