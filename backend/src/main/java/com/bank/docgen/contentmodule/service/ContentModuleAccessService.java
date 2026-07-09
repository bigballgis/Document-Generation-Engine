package com.bank.docgen.contentmodule.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.domain.ContentModuleGovernanceActorRole;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContentModuleAccessService {

    private final ContentModuleRepository moduleRepository;
    private final GroupAccessService groupAccessService;
    private final ObjectMapper objectMapper;

    public ContentModuleAccessService(
            ContentModuleRepository moduleRepository,
            GroupAccessService groupAccessService,
            ObjectMapper objectMapper
    ) {
        this.moduleRepository = moduleRepository;
        this.groupAccessService = groupAccessService;
        this.objectMapper = objectMapper;
    }

    public ContentModuleEntity requireReadableModule(String moduleId, ManagementSessionClaims session) {
        ContentModuleEntity module = requireExistingModule(moduleId);
        if (!canAccessModule(session, module)) {
            throw new ContentModuleAccessDeniedException();
        }
        return module;
    }

    public ContentModuleEntity requireAuthoringModule(String moduleId, ManagementSessionClaims session) {
        ContentModuleEntity module = requireReadableModule(moduleId, session);
        if (!groupAccessService.canAuthorContentModules(session)) {
            throw new ContentModuleAccessDeniedException();
        }
        return module;
    }

    public ContentModuleEntity requireExistingModule(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            throw new ContentModuleGovernanceException(
                    "MODULE_ID_REQUIRED",
                    "api.error.contentModule.moduleIdRequired",
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        return resolveModule(moduleId).orElseThrow(ContentModuleNotFoundException::new);
    }

    public java.util.Optional<ContentModuleEntity> resolveModule(String moduleId) {
        try {
            UUID uuid = UUID.fromString(moduleId);
            return moduleRepository.findByIdAndDeletedAtIsNull(uuid);
        } catch (IllegalArgumentException ignored) {
            return moduleRepository.findByModuleCodeAndDeletedAtIsNull(moduleId.trim());
        }
    }

    public boolean canAccessModule(ManagementSessionClaims session, ContentModuleEntity module) {
        if (groupAccessService.canAccessGroup(session, module.getGroupCode())) {
            return true;
        }
        return readSharedGroupCodes(module).stream()
                .anyMatch(code -> groupAccessService.canAccessGroup(session, code));
    }

    public List<String> readSharedGroupCodes(ContentModuleEntity module) {
        try {
            return objectMapper.readValue(module.getSharedGroupCodesJson(), new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    public String writeSharedGroupCodes(List<String> sharedGroupCodes) {
        List<String> normalized = sharedGroupCodes == null ? List.of() : sharedGroupCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    public void assertActorSession(ManagementSessionClaims session, ContentModuleGovernanceActorRole actorRole) {
        if (!sessionHasActorRole(session, actorRole)) {
            throw new ContentModuleGovernanceException(
                    "MODULE_REVIEW_ROLE_DENIED",
                    "api.error.contentModule.reviewRoleDenied",
                    org.springframework.http.HttpStatus.FORBIDDEN
            );
        }
    }

    public void assertLifecycleActorSession(ManagementSessionClaims session, ContentModuleGovernanceActorRole actorRole) {
        if (!sessionHasLifecycleRole(session, actorRole)) {
            throw new ContentModuleGovernanceException(
                    "CONTENT_MODULE_ROLE_DENIED",
                    "api.error.contentModule.lifecycleRoleDenied",
                    org.springframework.http.HttpStatus.FORBIDDEN
            );
        }
    }

    public boolean sessionHasActorRole(ManagementSessionClaims session, ContentModuleGovernanceActorRole actorRole) {
        return switch (actorRole) {
            case GLOBAL_ADMIN -> session.roles().contains("GLOBAL_ADMIN");
            case GROUP_ADMIN -> session.roles().contains("GROUP_ADMIN");
            case APPROVER -> session.roles().contains("TEMPLATE_APPROVER");
            case TEMPLATE_AUTHOR -> session.roles().contains("TEMPLATE_AUTHOR");
            case MASTER_DESIGNER -> session.roles().contains("MASTER_DESIGNER");
        };
    }

    public boolean sessionHasLifecycleRole(ManagementSessionClaims session, ContentModuleGovernanceActorRole actorRole) {
        return switch (actorRole) {
            case GLOBAL_ADMIN -> session.roles().contains("GLOBAL_ADMIN");
            case GROUP_ADMIN -> session.roles().contains("GROUP_ADMIN");
            default -> false;
        };
    }

    public String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }

    public String publicModuleId(ContentModuleEntity module) {
        return module.getModuleCode();
    }
}
