package com.bank.docgen.template.service;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import java.util.UUID;

/**
 * Package-private command helper for cloning version-line data and lifecycle audit rows.
 */
final class TemplateVersionLineCloneSupport {

    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final TemplateContentModuleReferenceRepository contentModuleReferenceRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final MessageResolver messageResolver;

    TemplateVersionLineCloneSupport(
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            TemplateContentModuleReferenceRepository contentModuleReferenceRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            MessageResolver messageResolver
    ) {
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.contentModuleReferenceRepository = contentModuleReferenceRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.messageResolver = messageResolver;
    }

    void copyVariables(TemplateVersionEntity source, TemplateVersionEntity target) {
        for (VariableSchemaEntity variable : variableSchemaRepository
                .findByTemplateVersionIdOrderByVariableKeyAsc(source.getId())) {
            variableSchemaRepository.save(new VariableSchemaEntity(
                    UUID.randomUUID(),
                    target.getId(),
                    variable.getVariableKey(),
                    variable.getVariableType(),
                    variable.isRequired(),
                    variable.getDefaultValue(),
                    variable.getEnumValues(),
                    variable.getDescription(),
                    variable.getComputeExpression(),
                    variable.getPiiCategory()
            ));
        }
    }

    void copyBindings(TemplateVersionEntity source, TemplateVersionEntity target) {
        for (AnchorBindingEntity binding : anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(source.getId())) {
            AnchorBindingEntity copied = new AnchorBindingEntity(
                    UUID.randomUUID(),
                    target.getId(),
                    binding.getAnchorId(),
                    binding.getDeclaredContentType(),
                    binding.getStructuredContentJson(),
                    binding.getValidationStatus()
            );
            copied.setPasteCleaningEvidenceJson(binding.getPasteCleaningEvidenceJson());
            anchorBindingRepository.save(copied);
        }
    }

    void copyContentModuleReferences(TemplateVersionEntity source, TemplateVersionEntity target) {
        for (TemplateContentModuleReferenceEntity reference : contentModuleReferenceRepository
                .findByTemplateVersionIdOrderByReferenceKeyAsc(source.getId())) {
            TemplateContentModuleReferenceEntity copied = new TemplateContentModuleReferenceEntity(
                    UUID.randomUUID(),
                    target.getId(),
                    reference.getReferenceKey(),
                    reference.getContentModuleVersionId()
            );
            contentModuleReferenceRepository.save(copied);
        }
    }

    void copyVersionGraph(TemplateVersionEntity source, TemplateVersionEntity target) {
        copyVariables(source, target);
        copyBindings(source, target);
        copyContentModuleReferences(source, target);
    }

    void recordCloneLifecycle(
            TemplateEntity template,
            TemplateLifecycleStatus fromStatus,
            String sourceReleaseVersion,
            UUID newDevVersionId,
            int newDevVersionNumber,
            ManagementSessionClaims session
    ) {
        String comment = messageResolver.resolve(
                "api.audit.lifecycle.clonedRelease",
                sourceReleaseVersion,
                newDevVersionNumber,
                newDevVersionId
        );
        lifecycleRecordRepository.save(new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                template.getId(),
                LifecycleAction.CLONE_FROM_RELEASE,
                fromStatus,
                TemplateLifecycleStatus.DRAFT,
                null,
                comment,
                sourceReleaseVersion,
                session.username()
        ));
    }

    void recordAbandonLifecycle(
            TemplateEntity template,
            TemplateLifecycleStatus fromStatus,
            TemplateLifecycleStatus toStatus,
            int abandonedDevVersionNumber,
            UUID abandonedDevVersionId,
            ManagementSessionClaims session
    ) {
        String comment = messageResolver.resolve(
                "api.audit.lifecycle.abandonedInFlightDev",
                abandonedDevVersionNumber,
                abandonedDevVersionId
        );
        lifecycleRecordRepository.save(new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                template.getId(),
                LifecycleAction.ABANDON_IN_FLIGHT_DEV,
                fromStatus,
                toStatus,
                null,
                comment,
                null,
                session.username()
        ));
    }
}
