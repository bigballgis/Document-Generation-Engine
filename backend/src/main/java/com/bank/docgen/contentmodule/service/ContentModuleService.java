package com.bank.docgen.contentmodule.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleDetailView;
import com.bank.docgen.contentmodule.api.ContentModuleSummaryView;
import com.bank.docgen.contentmodule.api.ContentModuleVersionView;
import com.bank.docgen.contentmodule.api.CreateContentModuleRequest;
import com.bank.docgen.contentmodule.api.CreateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.api.UpdateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentModuleService {

    private final ContentModuleRepository moduleRepository;
    private final ContentModuleVersionRepository versionRepository;
    private final GroupAccessService groupAccessService;
    private final ContentModuleAccessService accessSupport;
    private final ManagementAuditRecorder auditRecorder;

    public ContentModuleService(
            ContentModuleRepository moduleRepository,
            ContentModuleVersionRepository versionRepository,
            GroupAccessService groupAccessService,
            ContentModuleAccessService accessSupport,
            ManagementAuditRecorder auditRecorder
    ) {
        this.moduleRepository = moduleRepository;
        this.versionRepository = versionRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<ContentModuleSummaryView> listAccessible(ManagementSessionClaims session) {
        assertCatalogBrowseAllowed(session);
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        Map<UUID, ContentModuleEntity> modules = new LinkedHashMap<>();
        if (groupCodes.contains("*")) {
            moduleRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc()
                    .forEach(module -> modules.put(module.getId(), module));
        } else if (groupCodes.isEmpty()) {
            return List.of();
        } else {
            List<String> normalizedGroups = groupCodes.stream()
                    .map(code -> code.trim().toUpperCase(Locale.ROOT))
                    .toList();
            moduleRepository.findByGroupCodeInAndDeletedAtIsNullOrderByUpdatedAtDesc(normalizedGroups)
                    .forEach(module -> modules.put(module.getId(), module));
            moduleRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc().stream()
                    .filter(module -> !modules.containsKey(module.getId()))
                    .filter(module -> accessSupport.readSharedGroupCodes(module).stream()
                            .anyMatch(normalizedGroups::contains))
                    .forEach(module -> modules.put(module.getId(), module));
        }
        return modules.values().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<ContentModuleSummaryView> list(String groupCode, ManagementSessionClaims session) {
        assertCatalogBrowseAllowed(session);
        if (groupCode == null || groupCode.isBlank()) {
            throw new ContentModuleValidationException("api.error.contentModule.groupCodeRequired");
        }
        if (!groupAccessService.canAccessGroup(session, groupCode)) {
            throw new ContentModuleAccessDeniedException();
        }
        String normalizedGroup = groupCode.trim().toUpperCase(Locale.ROOT);
        Map<UUID, ContentModuleEntity> modules = new LinkedHashMap<>();
        moduleRepository.findByGroupCodeAndDeletedAtIsNull(normalizedGroup)
                .forEach(module -> modules.put(module.getId(), module));
        moduleRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc().stream()
                .filter(module -> !modules.containsKey(module.getId()))
                .filter(module -> accessSupport.readSharedGroupCodes(module).contains(normalizedGroup))
                .forEach(module -> modules.put(module.getId(), module));
        return modules.values().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ContentModuleDetailView get(String moduleId, ManagementSessionClaims session) {
        assertCatalogBrowseAllowed(session);
        ContentModuleEntity module = accessSupport.requireReadableModule(moduleId, session);
        return toDetail(module, session);
    }

    @Transactional
    public ContentModuleDetailView create(CreateContentModuleRequest request, ManagementSessionClaims session) {
        if (!groupAccessService.canAuthorContentModules(session)) {
            throw new ContentModuleAccessDeniedException();
        }
        String groupCode = request.groupCode().trim().toUpperCase(Locale.ROOT);
        if (!groupAccessService.canAccessGroup(session, groupCode)) {
            throw new ContentModuleAccessDeniedException();
        }
        String moduleCode = request.moduleCode().trim().toUpperCase(Locale.ROOT);
        if (moduleRepository.existsByModuleCodeAndDeletedAtIsNull(moduleCode)) {
            throw new ContentModuleValidationException("api.error.contentModule.moduleCodeExists");
        }
        validateContentStructureJson(request.contentStructureJson());

        UUID moduleId = UUID.randomUUID();
        ContentModuleEntity module = new ContentModuleEntity(
                moduleId,
                moduleCode,
                groupCode,
                request.name().trim(),
                request.description(),
                accessSupport.writeSharedGroupCodes(request.sharedGroupCodes()),
                session.username()
        );
        moduleRepository.save(module);

        ContentModuleVersionEntity version = new ContentModuleVersionEntity(
                UUID.randomUUID(),
                moduleId,
                request.semanticVersion().trim(),
                request.contentStructureJson(),
                request.changeDescription(),
                session.username()
        );
        versionRepository.save(version);

        auditRecorder.recordContentModuleCreated(
                moduleId,
                groupCode,
                moduleCode,
                session.username(),
                accessSupport.actorSummary(session)
        );
        return toDetail(module, session);
    }

    @Transactional
    public ContentModuleDetailView createVersion(
            String moduleId,
            CreateContentModuleVersionRequest request,
            ManagementSessionClaims session
    ) {
        ContentModuleEntity module = accessSupport.requireAuthoringModule(moduleId, session);
        if (versionRepository.existsByModuleIdAndSemanticVersion(module.getId(), request.semanticVersion().trim())) {
            throw new ContentModuleValidationException("api.error.contentModule.versionExists");
        }
        validateContentStructureJson(request.contentStructureJson());

        versionRepository.save(new ContentModuleVersionEntity(
                UUID.randomUUID(),
                module.getId(),
                request.semanticVersion().trim(),
                request.contentStructureJson(),
                request.changeDescription(),
                session.username()
        ));
        module.setUpdatedBy(session.username());
        moduleRepository.save(module);

        auditRecorder.recordContentModuleVersionCreated(
                module.getId(),
                module.getGroupCode(),
                module.getModuleCode(),
                request.semanticVersion().trim(),
                session.username(),
                accessSupport.actorSummary(session)
        );
        return toDetail(module, session);
    }

    @Transactional
    public ContentModuleDetailView updateDraftVersion(
            String moduleId,
            String semanticVersion,
            UpdateContentModuleVersionRequest request,
            ManagementSessionClaims session
    ) {
        ContentModuleEntity module = accessSupport.requireAuthoringModule(moduleId, session);
        ContentModuleVersionEntity version = versionRepository
                .findByModuleIdAndSemanticVersion(module.getId(), semanticVersion.trim())
                .orElseThrow(ContentModuleNotFoundException::new);
        if (version.getReviewState() != ContentModuleReviewState.DRAFT) {
            throw new ContentModuleValidationException("api.error.contentModule.draftOnlyEditable");
        }
        validateContentStructureJson(request.contentStructureJson());
        version.setContentStructureJson(request.contentStructureJson());
        if (request.changeDescription() != null) {
            version.setChangeDescription(request.changeDescription());
        }
        version.setUpdatedBy(session.username());
        versionRepository.save(version);
        module.setUpdatedBy(session.username());
        moduleRepository.save(module);

        auditRecorder.recordContentModuleVersionUpdated(
                module.getId(),
                module.getGroupCode(),
                module.getModuleCode(),
                semanticVersion.trim(),
                session.username(),
                accessSupport.actorSummary(session)
        );
        return toDetail(module, session);
    }

    private void assertCatalogBrowseAllowed(ManagementSessionClaims session) {
        if (!groupAccessService.canBrowseContentModuleCatalog(session)) {
            throw new ContentModuleAccessDeniedException();
        }
    }

    private void validateContentStructureJson(String contentStructureJson) {
        if (contentStructureJson == null || contentStructureJson.isBlank()) {
            throw new ContentModuleValidationException("api.error.contentModule.contentStructureRequired");
        }
    }

    private ContentModuleSummaryView toSummary(ContentModuleEntity module) {
        return new ContentModuleSummaryView(
                accessSupport.publicModuleId(module),
                module.getModuleCode(),
                module.getGroupCode(),
                module.getName(),
                module.getDescription(),
                accessSupport.readSharedGroupCodes(module),
                module.getCreatedAt(),
                module.getUpdatedAt()
        );
    }

    private ContentModuleDetailView toDetail(ContentModuleEntity module, ManagementSessionClaims session) {
        List<ContentModuleVersionView> versions = versionRepository
                .findByModuleIdOrderBySemanticVersionDesc(module.getId()).stream()
                .map(version -> toVersionView(version, session))
                .toList();
        return new ContentModuleDetailView(
                accessSupport.publicModuleId(module),
                module.getModuleCode(),
                module.getGroupCode(),
                module.getName(),
                module.getDescription(),
                accessSupport.readSharedGroupCodes(module),
                versions
        );
    }

    private ContentModuleVersionView toVersionView(
            ContentModuleVersionEntity version,
            ManagementSessionClaims session
    ) {
        String contentStructureJson = groupAccessService.canViewContentModuleStructure(session)
                ? version.getContentStructureJson()
                : null;
        return new ContentModuleVersionView(
                version.getId().toString(),
                version.getSemanticVersion(),
                version.getReviewState(),
                version.getLifecycleState(),
                version.getChangeDescription(),
                contentStructureJson,
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }
}
