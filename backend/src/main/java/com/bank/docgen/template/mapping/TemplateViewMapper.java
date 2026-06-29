package com.bank.docgen.template.mapping;

import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TemplateViewMapper {

    private final TemplateVersionRepository templateVersionRepository;
    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final ObjectMapper objectMapper;

    public TemplateViewMapper(
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            ObjectMapper objectMapper
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.objectMapper = objectMapper;
    }

    public TemplateSummaryView toSummary(TemplateEntity template) {
        int releaseVersionCount = (int) templateVersionRepository
                .findByTemplateIdOrderByDevVersionNumberDesc(template.getId())
                .stream()
                .filter(version -> version.getReleaseVersion() != null && !version.getReleaseVersion().isBlank())
                .count();
        return new TemplateSummaryView(
                template.getId().toString(),
                template.getExternalId(),
                template.getGroupCode(),
                template.getName(),
                template.getLifecycleStatus(),
                template.getReleaseVersion(),
                releaseVersionCount,
                template.getMasterId().toString(),
                template.getUpdatedBy(),
                template.getUpdatedAt()
        );
    }

    public TemplateDetailView toDetail(TemplateEntity template) {
        TemplateVersionEntity version = currentDevVersion(template.getId());
        List<VariableSchemaView> variables = variableSchemaRepository
                .findByTemplateVersionIdOrderByVariableKeyAsc(version.getId())
                .stream()
                .map(this::toVariableView)
                .toList();
        List<AnchorBindingView> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId())
                .stream()
                .map(this::toBindingView)
                .toList();
        return new TemplateDetailView(
                template.getId().toString(),
                template.getExternalId(),
                template.getGroupCode(),
                template.getName(),
                template.getDescription(),
                template.getMasterId().toString(),
                template.getLifecycleStatus(),
                resolveApprovalSubState(template),
                template.getReleaseVersion(),
                version.getId().toString(),
                version.getDevVersionNumber(),
                variables,
                bindings,
                loadRules(version),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    public List<CompositionRuleView> loadRules(TemplateVersionEntity version) {
        if (version.getRulesJson() == null || version.getRulesJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    version.getRulesJson(),
                    new TypeReference<List<CompositionRuleView>>() {
                    }
            );
        } catch (JsonProcessingException exception) {
            throw new TemplateValidationException("api.error.template.invalidRulesJson");
        }
    }

    public VariableSchemaView toVariableView(VariableSchemaEntity entity) {
        return new VariableSchemaView(
                entity.getId().toString(),
                entity.getVariableKey(),
                entity.getVariableType(),
                entity.isRequired(),
                entity.getDefaultValue(),
                entity.getEnumValues(),
                entity.getDescription()
        );
    }

    public AnchorBindingView toBindingView(AnchorBindingEntity entity) {
        return new AnchorBindingView(
                entity.getId().toString(),
                entity.getAnchorId(),
                entity.getDeclaredContentType().name(),
                entity.getStructuredContentJson(),
                entity.getValidationStatus()
        );
    }

    private TemplateVersionEntity currentDevVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1)
                .orElseThrow(TemplateNotFoundException::new);
    }

    private ApprovalSubState resolveApprovalSubState(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.APPROVAL) {
            return null;
        }
        List<TemplateLifecycleRecordEntity> records =
                lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(template.getId());
        for (TemplateLifecycleRecordEntity record : records) {
            if (record.getAction() == LifecycleAction.SUBMIT_FOR_APPROVAL) {
                return ApprovalSubState.PENDING_DECISION;
            }
            if (record.getAction() == LifecycleAction.RECORD_TEST_DECISION
                    && record.getToStatus() == TemplateLifecycleStatus.APPROVAL) {
                return ApprovalSubState.PENDING_SUBMIT;
            }
        }
        return ApprovalSubState.PENDING_SUBMIT;
    }
}
