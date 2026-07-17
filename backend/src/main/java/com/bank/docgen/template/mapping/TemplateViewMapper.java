package com.bank.docgen.template.mapping;

import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.ApprovalSubStateResolver;
import com.bank.docgen.template.service.PasteCleaningEvidenceSupport;
import com.bank.docgen.template.service.TemplateCurrentVersionResolver;
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
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TemplateViewMapper {

    private final TemplateVersionRepository templateVersionRepository;
    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final ApprovalSubStateResolver approvalSubStateResolver;
    private final ObjectMapper objectMapper;
    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;

    public TemplateViewMapper(
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            ApprovalSubStateResolver approvalSubStateResolver,
            ObjectMapper objectMapper,
            TemplateCurrentVersionResolver templateCurrentVersionResolver
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.approvalSubStateResolver = approvalSubStateResolver;
        this.objectMapper = objectMapper;
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
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
                approvalSubStateResolver.resolve(template),
                template.getReleaseVersion(),
                releaseVersionCount,
                template.getMasterId().toString(),
                template.getUpdatedBy(),
                template.getUpdatedAt(),
                null
        );
    }

    public TemplateDetailView toDetail(TemplateEntity template) {
        Optional<TemplateVersionEntity> inFlight = templateCurrentVersionResolver
                .findInFlightDevVersion(template.getId());
        TemplateVersionEntity version = inFlight.orElseGet(() -> templateCurrentVersionResolver
                .findLatestPublishedVersion(template.getId())
                .orElseThrow(TemplateNotFoundException::new));
        return toDetailForVersion(template, version, false);
    }

    public TemplateDetailView toDetailForVersion(
            TemplateEntity template,
            TemplateVersionEntity version,
            boolean readOnly
    ) {
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
        String releaseVersion = templateCurrentVersionResolver.isInFlight(version)
                ? template.getReleaseVersion()
                : version.getReleaseVersion();
        return new TemplateDetailView(
                template.getId().toString(),
                template.getExternalId(),
                template.getGroupCode(),
                template.getName(),
                template.getDescription(),
                template.getMasterId().toString(),
                readOnly
                        ? version.getLifecycleStatus()
                        : template.getLifecycleStatus(),
                templateCurrentVersionResolver.isInFlight(version)
                        ? approvalSubStateResolver.resolve(template)
                        : null,
                releaseVersion,
                version.getId().toString(),
                version.getDevVersionNumber(),
                variables,
                bindings,
                loadRules(version),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                readOnly ? version.getCreatedBy() : null,
                null,
                readOnly,
                null
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
                entity.getDescription(),
                entity.getComputeExpression(),
                entity.getPiiCategory()
        );
    }

    public AnchorBindingView toBindingView(AnchorBindingEntity entity) {
        return new AnchorBindingView(
                entity.getId().toString(),
                entity.getAnchorId(),
                entity.getDeclaredContentType().name(),
                entity.getStructuredContentJson(),
                entity.getValidationStatus(),
                PasteCleaningEvidenceSupport.read(entity.getPasteCleaningEvidenceJson(), objectMapper),
                entity.getUpdatedAt()
        );
    }
}
