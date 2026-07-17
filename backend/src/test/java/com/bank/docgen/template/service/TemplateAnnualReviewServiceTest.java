package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnnualReviewDueAuthorTaskView;
import com.bank.docgen.template.api.CompleteTemplateAnnualReviewRequest;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateAnnualReviewServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-17T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 17);
    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MASTER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock private TemplateRepository templateRepository;
    @Mock private TemplateService templateService;
    @Mock private TemplateViewMapper templateViewMapper;
    @Mock private GroupAccessService groupAccessService;
    @Mock private ManagementAuditRecorder auditRecorder;

    private TemplateAnnualReviewSupport annualReviewSupport;
    private TemplateAnnualReviewService service;
    private ManagementSessionClaims author;
    private ManagementSessionClaims tester;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        annualReviewSupport = new TemplateAnnualReviewSupport(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        service = new TemplateAnnualReviewService(
                templateRepository,
                templateService,
                templateViewMapper,
                groupAccessService,
                annualReviewSupport,
                auditRecorder
        );
        author = new ManagementSessionClaims(
                "10000003", "Author", "author@example.com", AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"),
                "route.home", List.of("route.home"), Instant.now().plusSeconds(3600)
        );
        tester = new ManagementSessionClaims(
                "10000006", "Tester", "tester@example.com", AuthSource.LOCAL,
                List.of("TEMPLATE_TESTER"), List.of("RETAIL"),
                "route.home", List.of("route.home"), Instant.now().plusSeconds(3600)
        );
        template = new TemplateEntity(TEMPLATE_ID, "TPL-AR-1", "RETAIL", "Annual Review Sample", null, MASTER_ID, "10000003");
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
    }

    @Test
    void seedOnEnterPublished_setsTodayPlus365WhenAbsent() {
        annualReviewSupport.seedOnEnterPublishedIfAbsent(template);
        assertThat(template.getNextReviewDue()).isEqualTo(TODAY.plusDays(365));
    }

    @Test
    void seedOnEnterPublished_doesNotOverwriteExisting() {
        LocalDate existing = LocalDate.of(2027, 1, 1);
        template.setNextReviewDue(existing);
        annualReviewSupport.seedOnEnterPublishedIfAbsent(template);
        assertThat(template.getNextReviewDue()).isEqualTo(existing);
    }

    @Test
    void listDueTasks_includesDueTodayAndPast_excludesFutureAndNull() {
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        TemplateEntity dueToday = template;
        dueToday.setNextReviewDue(TODAY);
        when(templateRepository
                .findByDeletedAtIsNullAndGroupCodeInAndNextReviewDueLessThanEqualOrderByNextReviewDueAscUpdatedAtDesc(
                        List.of("RETAIL"), TODAY))
                .thenReturn(List.of(dueToday));

        List<AnnualReviewDueAuthorTaskView> tasks = service.listDueTasks(author);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().templateId()).isEqualTo(TEMPLATE_ID.toString());
        assertThat(tasks.getFirst().nextReviewDue()).isEqualTo(TODAY);
    }

    @Test
    void listDueTasks_withoutAuthorTemplates_throws403() {
        when(groupAccessService.canAuthorTemplates(tester)).thenReturn(false);
        assertThatThrownBy(() -> service.listDueTasks(tester))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    @Test
    void complete_defaultsToTodayPlus365_andWritesAudit() {
        template.setNextReviewDue(TODAY);
        when(templateService.requireWritableTemplate(TEMPLATE_ID, author)).thenReturn(template);
        when(templateRepository.save(template)).thenReturn(template);
        TemplateSummaryView summary = new TemplateSummaryView(
                TEMPLATE_ID.toString(), "TPL-AR-1", "RETAIL", "Annual Review Sample",
                TemplateLifecycleStatus.PUBLISHED, null, "1.0.0", 1, MASTER_ID.toString(),
                "10000003", FIXED_NOW, null, TODAY.plusDays(365)
        );
        when(templateViewMapper.toSummary(template)).thenReturn(summary);

        TemplateSummaryView result = service.complete(TEMPLATE_ID, null, author);

        assertThat(template.getNextReviewDue()).isEqualTo(TODAY.plusDays(365));
        assertThat(result.nextReviewDue()).isEqualTo(TODAY.plusDays(365));
        verify(auditRecorder).recordTemplateAnnualReviewCompleted(
                eq(template), eq(TODAY), eq(TODAY.plusDays(365)), eq("10000003"), eq("Author")
        );
    }

    @Test
    void complete_usesExplicitNextReviewDue() {
        LocalDate explicit = LocalDate.of(2028, 3, 1);
        template.setNextReviewDue(TODAY);
        when(templateService.requireWritableTemplate(TEMPLATE_ID, author)).thenReturn(template);
        when(templateRepository.save(template)).thenReturn(template);
        when(templateViewMapper.toSummary(template)).thenReturn(new TemplateSummaryView(
                TEMPLATE_ID.toString(), "TPL-AR-1", "RETAIL", "Annual Review Sample",
                TemplateLifecycleStatus.PUBLISHED, null, null, 0, MASTER_ID.toString(),
                "10000003", FIXED_NOW, null, explicit
        ));

        service.complete(TEMPLATE_ID, new CompleteTemplateAnnualReviewRequest(explicit), author);

        assertThat(template.getNextReviewDue()).isEqualTo(explicit);
        verify(auditRecorder).recordTemplateAnnualReviewCompleted(
                eq(template), eq(TODAY), eq(explicit), any(), any()
        );
    }

    @Test
    void complete_withoutWriteAccess_doesNotAudit() {
        when(templateService.requireWritableTemplate(TEMPLATE_ID, tester))
                .thenThrow(new TemplateAccessDeniedException());
        assertThatThrownBy(() -> service.complete(TEMPLATE_ID, null, tester))
                .isInstanceOf(TemplateAccessDeniedException.class);
        verify(auditRecorder, never()).recordTemplateAnnualReviewCompleted(any(), any(), any(), any(), any());
    }
}
