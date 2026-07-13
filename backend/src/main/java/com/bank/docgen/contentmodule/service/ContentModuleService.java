package com.bank.docgen.contentmodule.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleDetailView;
import com.bank.docgen.contentmodule.api.ContentModuleSummaryView;
import com.bank.docgen.contentmodule.api.CreateContentModuleRequest;
import com.bank.docgen.contentmodule.api.CreateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.api.UpdateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.List;
import java.util.Locale;
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
    private final ContentModuleCatalogSupport catalog;

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
        this.catalog = new ContentModuleCatalogSupport(
                moduleRepository, versionRepository, groupAccessService, accessSupport);
    }

    @Transactional(readOnly = true)
    public PageView<ContentModuleSummaryView> list(
            ManagementSessionClaims session,
            Integer page,
            Integer size,
            String search,
            String groupCode,
            String sort
    ) {
        return catalog.list(session, page, size, search, groupCode, sort);
    }

    @Transactional(readOnly = true)
    public List<ContentModuleSummaryView> listAccessible(ManagementSessionClaims session) {
        return list(session, 0, CatalogPageSupport.MAX_SIZE, null, null, null).content();
    }

    @Transactional(readOnly = true)
    public List<ContentModuleSummaryView> list(String groupCode, ManagementSessionClaims session) {
        return catalog.listByGroup(groupCode, session);
    }

    @Transactional(readOnly = true)
    public ContentModuleDetailView get(String moduleId, ManagementSessionClaims session) {
        catalog.assertCatalogBrowseAllowed(session);
        ContentModuleEntity module = accessSupport.requireReadableModule(moduleId, session);
        return catalog.toDetail(module, session);
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
        return catalog.toDetail(module, session);
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
        return catalog.toDetail(module, session);
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
        return catalog.toDetail(module, session);
    }

    private void validateContentStructureJson(String contentStructureJson) {
        if (contentStructureJson == null || contentStructureJson.isBlank()) {
            throw new ContentModuleValidationException("api.error.contentModule.contentStructureRequired");
        }
    }
}
