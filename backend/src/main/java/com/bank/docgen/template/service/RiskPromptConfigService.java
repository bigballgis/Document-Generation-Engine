package com.bank.docgen.template.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.DecisionFormConfigView;
import com.bank.docgen.template.api.RiskPromptConfigView;
import com.bank.docgen.template.api.TemplateRiskPromptConfigView;
import com.bank.docgen.template.api.UpsertGlobalRiskPromptConfigRequest;
import com.bank.docgen.template.api.UpsertTemplateRiskPromptConfigRequest;
import com.bank.docgen.template.domain.RiskPromptScope;
import com.bank.docgen.template.persistence.RiskPromptConfigEntity;
import com.bank.docgen.template.persistence.RiskPromptConfigRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRiskPromptOverrideEntity;
import com.bank.docgen.template.persistence.TemplateRiskPromptOverrideRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskPromptConfigService {

    public static final String AUDIT_EVENT = "RISK_PROMPT_CONFIG_UPDATED";
    private static final String TEMPLATE_SCOPE = "TEMPLATE";

    private final RiskPromptConfigRepository riskPromptConfigRepository;
    private final TemplateRiskPromptOverrideRepository templateRiskPromptOverrideRepository;
    private final TemplateService templateService;
    private final ManagementAuditRecorder auditRecorder;
    private final ObjectMapper objectMapper;

    public RiskPromptConfigService(
            RiskPromptConfigRepository riskPromptConfigRepository,
            TemplateRiskPromptOverrideRepository templateRiskPromptOverrideRepository,
            TemplateService templateService,
            ManagementAuditRecorder auditRecorder,
            ObjectMapper objectMapper
    ) {
        this.riskPromptConfigRepository = riskPromptConfigRepository;
        this.templateRiskPromptOverrideRepository = templateRiskPromptOverrideRepository;
        this.templateService = templateService;
        this.auditRecorder = auditRecorder;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public RiskPromptConfigView getGlobal(ManagementSessionClaims session) {
        return toGlobalView(loadGlobalEntity());
    }

    @Transactional
    public RiskPromptConfigView upsertGlobal(UpsertGlobalRiskPromptConfigRequest request, ManagementSessionClaims session) {
        requireGlobalAdmin(session);

        String categoriesJson = writeJson(request.reasonCategories());
        String copyJson = writeJson(request.riskPromptCopy());
        RiskPromptConfigEntity entity = riskPromptConfigRepository
                .findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null)
                .orElseGet(() -> new RiskPromptConfigEntity(
                        UUID.randomUUID(),
                        RiskPromptScope.GLOBAL,
                        null,
                        categoriesJson,
                        copyJson
                ));
        entity.update(categoriesJson, copyJson);
        riskPromptConfigRepository.save(entity);

        auditRecorder.recordRiskPromptConfigUpdated(
                RiskPromptScope.GLOBAL.name(),
                null,
                session.username(),
                session.displayName(),
                "reasonCategories=" + request.reasonCategories().size()
        );
        return toGlobalView(entity);
    }

    @Transactional(readOnly = true)
    public TemplateRiskPromptConfigView getTemplateConfig(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        return buildTemplateView(templateId);
    }

    @Transactional
    public TemplateRiskPromptConfigView upsertTemplateConfig(
            UUID templateId,
            UpsertTemplateRiskPromptConfigRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        if (Boolean.TRUE.equals(request.useDefault())) {
            templateRiskPromptOverrideRepository.findById(templateId).ifPresent(existing -> {
                templateRiskPromptOverrideRepository.delete(existing);
                auditTemplateOverrideCleared(template, session);
            });
            return buildTemplateView(templateId);
        }

        validateOverrideCategories(request.reasonCategories());
        String categoriesJson = writeJson(request.reasonCategories());
        String copyJson = writeJson(normalizeCopy(request.riskPromptCopy()));
        TemplateRiskPromptOverrideEntity entity = templateRiskPromptOverrideRepository
                .findById(templateId)
                .orElseGet(() -> new TemplateRiskPromptOverrideEntity(templateId, categoriesJson, copyJson));
        entity.update(categoriesJson, copyJson);
        templateRiskPromptOverrideRepository.save(entity);

        auditRecorder.recordRiskPromptConfigUpdated(
                TEMPLATE_SCOPE,
                template.getGroupCode(),
                session.username(),
                session.displayName(),
                "templateId=" + templateId + ",reasonCategories=" + request.reasonCategories().size()
        );
        return buildTemplateView(templateId);
    }

    @Transactional(readOnly = true)
    public DecisionFormConfigView resolveDecisionFormConfig(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        EffectiveRiskPromptConfig effective = resolveEffective(templateId);
        return new DecisionFormConfigView(effective.reasonCategories(), effective.riskPromptCopy());
    }

    EffectiveRiskPromptConfig resolveEffective(UUID templateId) {
        return templateRiskPromptOverrideRepository.findById(templateId)
                .map(override -> new EffectiveRiskPromptConfig(
                        readList(override.getReasonCategoriesJson()),
                        readMap(override.getRiskPromptCopyJson()),
                        override.getUpdatedAt().toString()
                ))
                .orElseGet(() -> {
                    RiskPromptConfigEntity global = loadGlobalEntity();
                    return new EffectiveRiskPromptConfig(
                            readList(global.getReasonCategoriesJson()),
                            readMap(global.getRiskPromptCopyJson()),
                            global.getUpdatedAt().toString()
                    );
                });
    }

    private TemplateRiskPromptConfigView buildTemplateView(UUID templateId) {
        return templateRiskPromptOverrideRepository.findById(templateId)
                .map(override -> new TemplateRiskPromptConfigView(
                        false,
                        readList(override.getReasonCategoriesJson()),
                        readMap(override.getRiskPromptCopyJson()),
                        override.getUpdatedAt().toString()
                ))
                .orElseGet(() -> {
                    RiskPromptConfigEntity global = loadGlobalEntity();
                    return new TemplateRiskPromptConfigView(
                            true,
                            readList(global.getReasonCategoriesJson()),
                            readMap(global.getRiskPromptCopyJson()),
                            global.getUpdatedAt().toString()
                    );
                });
    }

    private RiskPromptConfigEntity loadGlobalEntity() {
        return riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null)
                .orElseThrow(() -> new TemplateValidationException("api.error.validation.requestBodyInvalid"));
    }

    private void validateOverrideCategories(List<String> reasonCategories) {
        if (reasonCategories == null || reasonCategories.isEmpty()) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private Map<String, String> normalizeCopy(Map<String, String> riskPromptCopy) {
        return riskPromptCopy == null ? Map.of() : riskPromptCopy;
    }

    private void requireGlobalAdmin(ManagementSessionClaims session) {
        if (!session.roles().contains("GLOBAL_ADMIN")) {
            throw new TemplateAccessDeniedException();
        }
    }

    private void auditTemplateOverrideCleared(TemplateEntity template, ManagementSessionClaims session) {
        auditRecorder.recordRiskPromptConfigUpdated(
                TEMPLATE_SCOPE,
                template.getGroupCode(),
                session.username(),
                session.displayName(),
                "templateId=" + template.getId() + ",inheritGlobal=true"
        );
    }

    private RiskPromptConfigView toGlobalView(RiskPromptConfigEntity entity) {
        return new RiskPromptConfigView(
                RiskPromptScope.GLOBAL.name(),
                null,
                readList(entity.getReasonCategoriesJson()),
                readMap(entity.getRiskPromptCopyJson()),
                entity.getUpdatedAt().toString()
        );
    }

    private List<String> readList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Map<String, String> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    public record EffectiveRiskPromptConfig(
            List<String> reasonCategories,
            Map<String, String> riskPromptCopy,
            String updatedAt
    ) {
        public EffectiveRiskPromptConfig {
            reasonCategories = DefensiveCopies.copyStringList(reasonCategories);
            riskPromptCopy = DefensiveCopies.copyStringStringMap(riskPromptCopy);
        }
    }
}
