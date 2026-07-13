package com.bank.docgen.template.service;

import com.bank.docgen.template.api.RiskPromptConfigView;
import com.bank.docgen.template.api.TemplateRiskPromptConfigView;
import com.bank.docgen.template.domain.RiskPromptScope;
import com.bank.docgen.template.persistence.RiskPromptConfigEntity;
import com.bank.docgen.template.persistence.RiskPromptConfigRepository;
import com.bank.docgen.template.persistence.TemplateRiskPromptOverrideRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Package-private JSON / view mapping for risk-prompt configuration.
 */
final class RiskPromptConfigMappingSupport {

    private final RiskPromptConfigRepository riskPromptConfigRepository;
    private final TemplateRiskPromptOverrideRepository templateRiskPromptOverrideRepository;
    private final ObjectMapper objectMapper;

    RiskPromptConfigMappingSupport(
            RiskPromptConfigRepository riskPromptConfigRepository,
            TemplateRiskPromptOverrideRepository templateRiskPromptOverrideRepository,
            ObjectMapper objectMapper
    ) {
        this.riskPromptConfigRepository = riskPromptConfigRepository;
        this.templateRiskPromptOverrideRepository = templateRiskPromptOverrideRepository;
        this.objectMapper = objectMapper;
    }

    RiskPromptConfigEntity loadGlobalEntity() {
        return riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null)
                .orElseThrow(() -> new TemplateValidationException("api.error.validation.requestBodyInvalid"));
    }

    RiskPromptConfigView toGlobalView(RiskPromptConfigEntity entity) {
        return new RiskPromptConfigView(
                RiskPromptScope.GLOBAL.name(),
                null,
                readList(entity.getReasonCategoriesJson()),
                readMap(entity.getRiskPromptCopyJson()),
                entity.getUpdatedAt().toString()
        );
    }

    TemplateRiskPromptConfigView buildTemplateView(UUID templateId) {
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

    RiskPromptConfigService.EffectiveRiskPromptConfig resolveEffective(UUID templateId) {
        return templateRiskPromptOverrideRepository.findById(templateId)
                .map(override -> new RiskPromptConfigService.EffectiveRiskPromptConfig(
                        readList(override.getReasonCategoriesJson()),
                        readMap(override.getRiskPromptCopyJson()),
                        override.getUpdatedAt().toString()
                ))
                .orElseGet(() -> {
                    RiskPromptConfigEntity global = loadGlobalEntity();
                    return new RiskPromptConfigService.EffectiveRiskPromptConfig(
                            readList(global.getReasonCategoriesJson()),
                            readMap(global.getRiskPromptCopyJson()),
                            global.getUpdatedAt().toString()
                    );
                });
    }

    List<String> readList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    Map<String, String> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }
}
