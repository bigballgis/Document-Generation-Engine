package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.GenerationAuditEventView;
import com.bank.docgen.audit.api.LifecycleAuditEventView;
import com.bank.docgen.audit.api.ManagementAuditEventView;
import com.bank.docgen.audit.api.ManagementAuditExportEventView;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
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
 * Package-private view / export mapping for audit query results.
 */
final class AuditEventViewMapper {

    private final TemplateService templateService;
    private final ManagementUserDisplayService managementUserDisplayService;
    private final AuditMaskingService auditMaskingService;
    private final ObjectMapper objectMapper;

    AuditEventViewMapper(
            TemplateService templateService,
            ManagementUserDisplayService managementUserDisplayService,
            AuditMaskingService auditMaskingService,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.managementUserDisplayService = managementUserDisplayService;
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

    List<LifecycleAuditEventView> toLifecycleViews(List<TemplateLifecycleRecordEntity> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Set<UUID> templateIds = records.stream()
                .map(TemplateLifecycleRecordEntity::getTemplateId)
                .collect(Collectors.toSet());
        Set<String> actorUsernames = records.stream()
                .map(TemplateLifecycleRecordEntity::getActorUsername)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<UUID, TemplateDisplayInfo> templateDisplayInfo = templateService.lookupDisplayInfoByIds(templateIds);
        Map<String, String> actorDisplayNames = managementUserDisplayService.lookupDisplayNames(actorUsernames);
        return records.stream()
                .map(record -> toLifecycleView(
                        record.getTemplateId(),
                        record,
                        templateDisplayInfo.get(record.getTemplateId()),
                        resolveActorDisplayName(record.getActorUsername(), actorDisplayNames)
                ))
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

    GenerationAuditEventView toGenerationAuditEventView(
            RuntimeGenerationAuditEventEntity entity,
            String templateExternalId
    ) {
        return new GenerationAuditEventView(
                entity.getEventAt(),
                entity.getEventType(),
                templateExternalId,
                entity.getRequestId(),
                entity.getOutcome(),
                mapGenerationAuditStatus(entity.getOutcome()),
                auditMaskingService.maskActorSummary(entity.getAccessAccount())
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

    private LifecycleAuditEventView toLifecycleView(
            UUID templateId,
            TemplateLifecycleRecordEntity record,
            TemplateDisplayInfo templateDisplayInfo,
            String actorDisplayName
    ) {
        return new LifecycleAuditEventView(
                record.getCreatedAt(),
                record.getAction().name(),
                templateId.toString(),
                templateDisplayInfo == null ? null : templateDisplayInfo.name(),
                templateDisplayInfo == null ? null : templateDisplayInfo.externalId(),
                record.getAction().name(),
                record.getFromStatus() == null ? null : record.getFromStatus().name(),
                record.getToStatus() == null ? null : record.getToStatus().name(),
                record.getActorUsername(),
                actorDisplayName,
                record.getCommentSummary(),
                List.of()
        );
    }

    private static String resolveRuntimeStatusSummary(RuntimeGenerationAuditEventEntity entity) {
        String statusSummary = entity.getResultSummary();
        if (statusSummary == null || statusSummary.isBlank()) {
            return entity.getOutcome();
        }
        return statusSummary;
    }

    private static String resolveActorDisplayName(String username, Map<String, String> actorDisplayNames) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return actorDisplayNames.get(username);
    }

    private static String mapGenerationAuditStatus(String outcome) {
        if (RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS.equals(outcome)
                || RuntimeGenerationAuditRecorder.OUTCOME_REPLAYED.equals(outcome)) {
            return "SUCCEEDED";
        }
        if (RuntimeGenerationAuditRecorder.OUTCOME_FAILURE.equals(outcome)) {
            return "FAILED";
        }
        return outcome;
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
