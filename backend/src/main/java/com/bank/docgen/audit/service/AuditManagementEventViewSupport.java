package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.ManagementAuditEventView;
import com.bank.docgen.audit.api.ManagementAuditExportEventView;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TemplateService.TemplateDisplayInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Package-private management / runtime management-view mapping for AuditEventViewMapper.
 */
final class AuditManagementEventViewSupport {

    private final TemplateService templateService;
    private final AuditMaskingService auditMaskingService;
    private final ObjectMapper objectMapper;

    AuditManagementEventViewSupport(
            TemplateService templateService,
            AuditMaskingService auditMaskingService,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.auditMaskingService = auditMaskingService;
        this.objectMapper = objectMapper;
    }

    List<ManagementAuditEventView> toManagementViews(List<ManagementAuditEventEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<UUID> templateIds = entities.stream()
                .map(ManagementAuditEventEntity::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, TemplateDisplayInfo> templateDisplayInfo = templateService.lookupDisplayInfoByIds(templateIds);
        return entities.stream()
                .map(entity -> {
                    UUID templateId = entity.getTemplateId();
                    TemplateDisplayInfo displayInfo = templateId == null ? null : templateDisplayInfo.get(templateId);
                    return toManagementView(entity, displayInfo);
                })
                .toList();
    }

    List<ManagementAuditEventView> toManagementViewsFromRuntime(
            List<RuntimeGenerationAuditEventEntity> entities
    ) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<UUID> templateIds = entities.stream()
                .map(RuntimeGenerationAuditEventEntity::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, TemplateDisplayInfo> templateDisplayInfo = templateService.lookupDisplayInfoByIds(templateIds);
        return entities.stream()
                .map(entity -> {
                    UUID templateId = entity.getTemplateId();
                    TemplateDisplayInfo displayInfo = templateId == null ? null : templateDisplayInfo.get(templateId);
                    return toManagementViewFromRuntime(entity, displayInfo);
                })
                .toList();
    }

    ManagementAuditExportEventView toExportView(ManagementAuditEventEntity entity) {
        return new ManagementAuditExportEventView(
                entity.getEventAt(),
                entity.getEventType(),
                entity.getTemplateId() == null ? null : entity.getTemplateId().toString(),
                entity.getCredentialId() == null ? null : entity.getCredentialId().toString(),
                entity.getPreviousPolicyVersion(),
                entity.getPolicyVersion(),
                readStringList(entity.getChangedAreasJson()),
                entity.isRollback(),
                entity.getRollbackSourcePolicyVersion(),
                auditMaskingService.maskActorSummary(entity.getActorSummary()),
                auditMaskingService.maskCredentialFingerprint(entity.getCredentialFingerprint()),
                entity.getStatusSummary(),
                readStringList(entity.getWarningCodesJson())
        );
    }

    ManagementAuditExportEventView toRuntimeExportView(RuntimeGenerationAuditEventEntity entity) {
        return new ManagementAuditExportEventView(
                entity.getEventAt(),
                entity.getEventType(),
                entity.getTemplateId() == null ? null : entity.getTemplateId().toString(),
                entity.getCredentialId() == null ? null : entity.getCredentialId().toString(),
                null,
                null,
                List.of(),
                false,
                null,
                auditMaskingService.maskActorSummary(entity.getAccessAccount()),
                auditMaskingService.maskCredentialFingerprint(entity.getCredentialFingerprint()),
                resolveRuntimeStatusSummary(entity),
                List.of()
        );
    }

    private ManagementAuditEventView toManagementView(
            ManagementAuditEventEntity entity,
            TemplateDisplayInfo templateDisplayInfo
    ) {
        return new ManagementAuditEventView(
                entity.getEventAt(),
                entity.getEventType(),
                entity.getTemplateId() == null ? null : entity.getTemplateId().toString(),
                templateDisplayInfo == null ? null : templateDisplayInfo.name(),
                templateDisplayInfo == null ? null : templateDisplayInfo.externalId(),
                entity.getCredentialId() == null ? null : entity.getCredentialId().toString(),
                entity.getPreviousPolicyVersion(),
                entity.getPolicyVersion(),
                readStringList(entity.getChangedAreasJson()),
                entity.isRollback(),
                entity.getRollbackSourcePolicyVersion(),
                entity.getActorSummary(),
                entity.getCredentialFingerprint(),
                entity.getStatusSummary(),
                readStringList(entity.getWarningCodesJson()),
                null
        );
    }

    private ManagementAuditEventView toManagementViewFromRuntime(
            RuntimeGenerationAuditEventEntity entity,
            TemplateDisplayInfo templateDisplayInfo
    ) {
        return new ManagementAuditEventView(
                entity.getEventAt(),
                entity.getEventType(),
                entity.getTemplateId() == null ? null : entity.getTemplateId().toString(),
                templateDisplayInfo == null ? null : templateDisplayInfo.name(),
                templateDisplayInfo == null ? null : templateDisplayInfo.externalId(),
                entity.getCredentialId() == null ? null : entity.getCredentialId().toString(),
                null,
                null,
                List.of(),
                false,
                null,
                entity.getAccessAccount(),
                entity.getCredentialFingerprint(),
                resolveRuntimeStatusSummary(entity),
                List.of(),
                entity.getRequestId()
        );
    }

    static String resolveRuntimeStatusSummary(RuntimeGenerationAuditEventEntity entity) {
        String statusSummary = entity.getResultSummary();
        if (statusSummary == null || statusSummary.isBlank()) {
            return entity.getOutcome();
        }
        return statusSummary;
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            return values.stream().filter(Objects::nonNull).toList();
        } catch (JsonProcessingException | RuntimeException ex) {
            return List.of();
        }
    }
}
