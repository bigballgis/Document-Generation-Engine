package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.INVOCATION_REGENERATED;

import com.bank.docgen.apimgmt.api.InvocationRegeneratedAuditDetail;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class InvocationRegenerationAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    InvocationRegenerationAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void record(InvocationRegeneratedAuditDetail detail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceInvocationId", detail.sourceInvocationId());
        payload.put("regenerationId", detail.regenerationId());
        payload.put("releaseBundleSnapshotId", detail.releaseBundleSnapshotId());
        payload.put("releaseBundleHash", detail.releaseBundleHash());
        payload.put("outputFormat", detail.outputFormat());
        payload.put("outcome", detail.outcome());
        payload.put("errorCode", detail.errorCode());
        payload.put("encryptionReapplied", detail.encryptionReapplied());
        String status = "INVOCATION_REGENERATED outcome=" + detail.outcome()
                + " source=" + detail.sourceInvocationId()
                + " regen=" + detail.regenerationId();
        eventWriter.persist(
                INVOCATION_REGENERATED,
                detail.templateId(),
                detail.groupCode(),
                null,
                null,
                null,
                eventWriter.writeJson(List.of()),
                false,
                null,
                detail.actorUsername(),
                detail.actorUsername(),
                null,
                eventWriter.truncate(status),
                eventWriter.writeJsonMap(payload)
        );
    }
}
