package com.bank.docgen.contentmodule.service;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleWhereUsedTemplateView;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CE-G05 — read-only where-used projection for content modules.
 */
@Service
public class ContentModuleWhereUsedService {

    private final ContentModuleAccessService accessSupport;
    private final GroupAccessService groupAccessService;
    private final ContentModuleVersionRepository versionRepository;
    private final TemplateContentModuleReferenceRepository referenceRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateRepository templateRepository;

    public ContentModuleWhereUsedService(
            ContentModuleAccessService accessSupport,
            GroupAccessService groupAccessService,
            ContentModuleVersionRepository versionRepository,
            TemplateContentModuleReferenceRepository referenceRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateRepository templateRepository
    ) {
        this.accessSupport = accessSupport;
        this.groupAccessService = groupAccessService;
        this.versionRepository = versionRepository;
        this.referenceRepository = referenceRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public PageView<ContentModuleWhereUsedTemplateView> listWhereUsed(
            String moduleId,
            Integer page,
            Integer size,
            ManagementSessionClaims session
    ) {
        if (!groupAccessService.canBrowseContentModuleCatalog(session)) {
            throw new ContentModuleAccessDeniedException();
        }
        ContentModuleEntity module = accessSupport.requireReadableModule(moduleId, session);
        int safePage = CatalogPageSupport.normalizePage(page);
        int safeSize = CatalogPageSupport.normalizeSize(size);

        List<ContentModuleVersionEntity> versions =
                versionRepository.findByModuleIdOrderBySemanticVersionDesc(module.getId());
        if (versions.isEmpty()) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }
        Map<UUID, String> versionIdToSemver = new LinkedHashMap<>();
        for (ContentModuleVersionEntity version : versions) {
            versionIdToSemver.put(version.getId(), version.getSemanticVersion());
        }

        List<TemplateContentModuleReferenceEntity> references =
                referenceRepository.findByContentModuleVersionIdIn(versionIdToSemver.keySet());

        Map<UUID, ContentModuleWhereUsedTemplateView> byTemplate = new LinkedHashMap<>();
        for (TemplateContentModuleReferenceEntity reference : references) {
            TemplateVersionEntity templateVersion = templateVersionRepository
                    .findById(reference.getTemplateVersionId())
                    .orElse(null);
            if (templateVersion == null || templateVersion.getDeletedAt() != null) {
                continue;
            }
            TemplateEntity template = templateRepository
                    .findByIdAndDeletedAtIsNull(templateVersion.getTemplateId())
                    .orElse(null);
            if (template == null) {
                continue;
            }
            if (!groupAccessService.canAccessGroup(session, template.getGroupCode())) {
                continue;
            }
            String pinned = versionIdToSemver.get(reference.getContentModuleVersionId());
            byTemplate.putIfAbsent(
                    template.getId(),
                    new ContentModuleWhereUsedTemplateView(
                            template.getId().toString(),
                            template.getExternalId(),
                            template.getName(),
                            template.getGroupCode(),
                            template.getLifecycleStatus(),
                            pinned
                    )
            );
        }

        List<ContentModuleWhereUsedTemplateView> all = new ArrayList<>(byTemplate.values());
        all.sort(Comparator
                .comparing(ContentModuleWhereUsedTemplateView::groupCode)
                .thenComparing(ContentModuleWhereUsedTemplateView::name));
        long total = all.size();
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        List<ContentModuleWhereUsedTemplateView> pageContent = all.subList(from, to);
        return new PageView<>(
                pageContent,
                safePage,
                safeSize,
                total,
                CatalogPageSupport.totalPages(total, safeSize)
        );
    }
}
