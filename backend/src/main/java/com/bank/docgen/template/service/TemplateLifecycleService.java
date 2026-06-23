package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateLifecycleService {

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final GroupAccessService groupAccessService;

    public TemplateLifecycleService(
            TemplateService templateService,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            GroupAccessService groupAccessService
    ) {
        this.templateService = templateService;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.groupAccessService = groupAccessService;
    }

    @Transactional
    public TemplateDetailView submitForTest(UUID templateId, LifecycleCommentRequest request, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.DRAFT);
        transition(template, TemplateLifecycleStatus.TESTING, LifecycleAction.SUBMIT_FOR_TEST, null, request.commentSummary(), session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView recordTestDecision(
            UUID templateId,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireTestableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.TESTING);
        if (request.decision() == LifecycleDecision.PASSED) {
            transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.RECORD_TEST_DECISION,
                    request.decision(), request.commentSummary(), session);
        } else {
            transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_TEST_DECISION,
                    request.decision(), request.commentSummary(), session);
        }
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView submitForApproval(UUID templateId, LifecycleCommentRequest request, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.APPROVAL);
        transition(template, TemplateLifecycleStatus.APPROVAL, LifecycleAction.SUBMIT_FOR_APPROVAL,
                null, request.commentSummary(), session);
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView recordApprovalDecision(
            UUID templateId,
            LifecycleDecisionRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireApprovableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.APPROVAL);
        if (request.decision() == LifecycleDecision.APPROVED) {
            transition(template, TemplateLifecycleStatus.PENDING_RELEASE, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), request.commentSummary(), session);
        } else {
            transition(template, TemplateLifecycleStatus.DRAFT, LifecycleAction.RECORD_APPROVAL_DECISION,
                    request.decision(), request.commentSummary(), session);
        }
        return templateService.toDetail(template);
    }

    @Transactional
    public TemplateDetailView publish(UUID templateId, PublishTemplateRequest request, ManagementSessionClaims session) {
        TemplateEntity template = requirePublishableTemplate(templateId, session);
        requireStatus(template, TemplateLifecycleStatus.PENDING_RELEASE);
        template.setReleaseVersion(request.releaseVersion());
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        TemplateVersionEntity version = templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1)
                .orElseThrow(TemplateNotFoundException::new);
        version.setReleaseVersion(request.releaseVersion());
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        templateVersionRepository.save(version);
        recordLifecycle(template, LifecycleAction.PUBLISH, TemplateLifecycleStatus.PENDING_RELEASE,
                TemplateLifecycleStatus.PUBLISHED, null, "Published release " + request.releaseVersion(),
                request.releaseVersion(), session);
        return templateService.toDetail(template);
    }

    private void transition(
            TemplateEntity template,
            TemplateLifecycleStatus toStatus,
            LifecycleAction action,
            LifecycleDecision decision,
            String comment,
            ManagementSessionClaims session
    ) {
        TemplateLifecycleStatus from = template.getLifecycleStatus();
        template.setLifecycleStatus(toStatus);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        recordLifecycle(template, action, from, toStatus, decision, comment, null, session);
    }

    private void recordLifecycle(
            TemplateEntity template,
            LifecycleAction action,
            TemplateLifecycleStatus from,
            TemplateLifecycleStatus to,
            LifecycleDecision decision,
            String comment,
            String releaseVersion,
            ManagementSessionClaims session
    ) {
        lifecycleRecordRepository.save(new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                template.getId(),
                action,
                from,
                to,
                decision,
                comment,
                releaseVersion,
                session.username()
        ));
    }

    private void requireStatus(TemplateEntity template, TemplateLifecycleStatus expected) {
        if (template.getLifecycleStatus() != expected) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    private TemplateEntity requireTestableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canDecideTemplateTests(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    private TemplateEntity requireApprovableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canDecideTemplateApprovals(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    private TemplateEntity requirePublishableTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canPublishTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }
}
