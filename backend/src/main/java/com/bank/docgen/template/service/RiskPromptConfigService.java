package com.bank.docgen.template.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.RiskPromptConfigView;
import com.bank.docgen.template.api.UpsertRiskPromptConfigRequest;
import com.bank.docgen.template.domain.RiskPromptScope;
import com.bank.docgen.template.persistence.RiskPromptConfigEntity;
import com.bank.docgen.template.persistence.RiskPromptConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskPromptConfigService {

    public static final String AUDIT_EVENT = "RISK_PROMPT_CONFIG_UPDATED";

    private final RiskPromptConfigRepository riskPromptConfigRepository;
    private final GroupAccessService groupAccessService;
    private final ManagementAuditRecorder auditRecorder;
    private final ObjectMapper objectMapper;

    public RiskPromptConfigService(
            RiskPromptConfigRepository riskPromptConfigRepository,
            GroupAccessService groupAccessService,
            ManagementAuditRecorder auditRecorder,
            ObjectMapper objectMapper
    ) {
        this.riskPromptConfigRepository = riskPromptConfigRepository;
        this.groupAccessService = groupAccessService;
        this.auditRecorder = auditRecorder;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public RiskPromptConfigView resolve(String groupCode, ManagementSessionClaims session) {
        if (groupCode != null && !groupCode.isBlank()) {
            requireReadableGroup(groupCode, session);
            return riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GROUP, groupCode)
                    .map(this::toView)
                    .orElseGet(() -> resolveGlobal());
        }
        return resolveGlobal();
    }

    @Transactional
    public RiskPromptConfigView upsert(UpsertRiskPromptConfigRequest request, ManagementSessionClaims session) {
        RiskPromptScope scope = parseScope(request.scopeType());
        requireMaintainPermission(scope, session);
        if (scope == RiskPromptScope.GROUP) {
            requireReadableGroup(request.groupCode(), session);
        }

        String categoriesJson = writeJson(request.reasonCategories());
        String copyJson = writeJson(request.riskPromptCopy());
        RiskPromptConfigEntity entity = riskPromptConfigRepository
                .findByScopeTypeAndGroupCode(scope, scope == RiskPromptScope.GLOBAL ? null : request.groupCode())
                .orElseGet(() -> new RiskPromptConfigEntity(
                        UUID.randomUUID(),
                        scope,
                        scope == RiskPromptScope.GLOBAL ? null : request.groupCode(),
                        categoriesJson,
                        copyJson
                ));
        entity.update(categoriesJson, copyJson);
        riskPromptConfigRepository.save(entity);

        auditRecorder.recordRiskPromptConfigUpdated(
                scope.name(),
                entity.getGroupCode(),
                session.username(),
                session.displayName(),
                "reasonCategories=" + request.reasonCategories().size()
        );
        return toView(entity);
    }

    private RiskPromptConfigView resolveGlobal() {
        return riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null)
                .map(this::toView)
                .orElseThrow(() -> new TemplateValidationException("api.error.validation.requestBodyInvalid"));
    }

    private void requireMaintainPermission(RiskPromptScope scope, ManagementSessionClaims session) {
        if (scope == RiskPromptScope.GLOBAL) {
            if (!session.roles().contains("GLOBAL_ADMIN")) {
                throw new TemplateAccessDeniedException();
            }
            return;
        }
        if (!session.roles().contains("GLOBAL_ADMIN") && !session.roles().contains("GROUP_ADMIN")) {
            throw new TemplateAccessDeniedException();
        }
    }

    private void requireReadableGroup(String groupCode, ManagementSessionClaims session) {
        if (!groupAccessService.canAccessGroup(session, groupCode)) {
            throw new TemplateAccessDeniedException();
        }
    }

    private RiskPromptScope parseScope(String scopeType) {
        try {
            return RiskPromptScope.valueOf(scopeType);
        } catch (IllegalArgumentException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private RiskPromptConfigView toView(RiskPromptConfigEntity entity) {
        return new RiskPromptConfigView(
                entity.getScopeType().name(),
                entity.getGroupCode(),
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
}
