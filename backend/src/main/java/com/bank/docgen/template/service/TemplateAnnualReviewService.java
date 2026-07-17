package com.bank.docgen.template.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnnualReviewDueAuthorTaskView;
import com.bank.docgen.template.api.CompleteTemplateAnnualReviewRequest;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CE-G05 — annual-review due-task projection and complete API.
 */
@Service
public class TemplateAnnualReviewService {

    private final TemplateRepository templateRepository;
    private final TemplateService templateService;
    private final TemplateViewMapper templateViewMapper;
    private final GroupAccessService groupAccessService;
    private final TemplateAnnualReviewSupport annualReviewSupport;
    private final ManagementAuditRecorder auditRecorder;

    public TemplateAnnualReviewService(
            TemplateRepository templateRepository,
            TemplateService templateService,
            TemplateViewMapper templateViewMapper,
            GroupAccessService groupAccessService,
            TemplateAnnualReviewSupport annualReviewSupport,
            ManagementAuditRecorder auditRecorder
    ) {
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.templateViewMapper = templateViewMapper;
        this.groupAccessService = groupAccessService;
        this.annualReviewSupport = annualReviewSupport;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<AnnualReviewDueAuthorTaskView> listDueTasks(ManagementSessionClaims session) {
        if (!groupAccessService.canAuthorTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        if (groupCodes.isEmpty()) {
            return List.of();
        }
        LocalDate todayUtc = annualReviewSupport.todayUtc();
        List<TemplateEntity> due;
        if (groupCodes.contains("*")) {
            due = templateRepository.findByDeletedAtIsNullAndNextReviewDueLessThanEqualOrderByNextReviewDueAscUpdatedAtDesc(
                    todayUtc
            );
        } else {
            due = templateRepository
                    .findByDeletedAtIsNullAndGroupCodeInAndNextReviewDueLessThanEqualOrderByNextReviewDueAscUpdatedAtDesc(
                            groupCodes,
                            todayUtc
                    );
        }
        return due.stream()
                .sorted(Comparator
                        .comparing(TemplateEntity::getNextReviewDue)
                        .thenComparing(TemplateEntity::getUpdatedAt, Comparator.reverseOrder()))
                .map(template -> new AnnualReviewDueAuthorTaskView(
                        template.getId().toString(),
                        template.getExternalId(),
                        template.getGroupCode(),
                        template.getName(),
                        template.getNextReviewDue(),
                        template.getLifecycleStatus(),
                        template.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional
    public TemplateSummaryView complete(
            UUID templateId,
            CompleteTemplateAnnualReviewRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        LocalDate previous = template.getNextReviewDue();
        LocalDate next = request != null && request.nextReviewDue() != null
                ? request.nextReviewDue()
                : annualReviewSupport.defaultNextReviewDueAfterComplete();
        template.setNextReviewDue(next);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        auditRecorder.recordTemplateAnnualReviewCompleted(
                template,
                previous,
                next,
                session.username(),
                session.displayName()
        );
        return templateViewMapper.toSummary(template);
    }
}
