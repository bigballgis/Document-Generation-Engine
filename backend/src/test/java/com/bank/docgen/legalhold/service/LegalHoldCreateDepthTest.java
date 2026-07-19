package com.bank.docgen.legalhold.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.legalhold.api.CreateLegalHoldRequest;
import com.bank.docgen.legalhold.api.LegalHoldView;
import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import com.bank.docgen.legalhold.persistence.LegalHoldEntity;
import com.bank.docgen.legalhold.persistence.LegalHoldRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * IBL-D5 / F23 — create-path depth beyond thin LegalHoldServiceTest (CE-G04 create/list/get).
 */
@ExtendWith(MockitoExtension.class)
class LegalHoldCreateDepthTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-30T00:00:00Z");

    @Mock
    private LegalHoldRepository repository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private LegalHoldService service;

    @BeforeEach
    void setUp() {
        service = new LegalHoldService(repository, templateRepository, groupAccessService, auditRecorder);
    }

    @Test
    void create_nullRequest_rejectsWithoutPersist() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        assertThatThrownBy(() -> service.create(null, admin))
                .isInstanceOf(LegalHoldValidationException.class)
                .extracting(ex -> ((LegalHoldValidationException) ex).messageKey())
                .isEqualTo("api.error.validation.requestBodyInvalid");
        verify(repository, never()).save(any());
        verify(auditRecorder, never()).recordLegalHoldCreated(any(), any(), any());
    }

    @Test
    void create_nullScopeType_rejectsWithoutPersist() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                null, "reason", TEMPLATE_ID, null, FROM, TO, null);

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_reasonOver512_rejectsWithoutAudit() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.INVOCATION_SET,
                "x".repeat(513),
                null,
                null,
                null,
                null,
                List.of("INV-1")
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class)
                .extracting(ex -> ((LegalHoldValidationException) ex).messageKey())
                .isEqualTo("api.error.validation.fieldSizeInvalid");
        verify(repository, never()).save(any());
        verify(auditRecorder, never()).recordLegalHoldCreated(any(), any(), any());
    }

    @Test
    void createTemplateWindow_missingEffectiveFrom_rejects() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.TEMPLATE_WINDOW,
                null,
                TEMPLATE_ID,
                null,
                null,
                TO,
                null
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class)
                .extracting(ex -> ((LegalHoldValidationException) ex).messageKey())
                .isEqualTo("api.error.validation.fieldRequired");
        verify(repository, never()).save(any());
    }

    @Test
    void createTemplateWindow_effectiveToBeforeFrom_rejects() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.TEMPLATE_WINDOW,
                null,
                TEMPLATE_ID,
                null,
                TO,
                FROM,
                null
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class)
                .extracting(ex -> ((LegalHoldValidationException) ex).messageKey())
                .isEqualTo("api.error.validation.fieldInvalid");
        verify(repository, never()).save(any());
    }

    @Test
    void createTemplateWindow_byExternalId_persistsResolvedTemplate() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        when(templateRepository.findByExternalIdAndDeletedAtIsNull("TPL-001"))
                .thenReturn(Optional.of(template(TEMPLATE_ID, "TPL-001")));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.TEMPLATE_WINDOW,
                "  litigation  ",
                null,
                "TPL-001",
                FROM,
                null,
                null
        );

        LegalHoldView view = service.create(request, admin);

        assertThat(view.templateId()).isEqualTo(TEMPLATE_ID);
        assertThat(view.templateExternalId()).isEqualTo("TPL-001");
        assertThat(view.reason()).isEqualTo("litigation");
        assertThat(view.effectiveTo()).isNull();
        ArgumentCaptor<LegalHoldEntity> captor = ArgumentCaptor.forClass(LegalHoldEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getHoldExternalId()).startsWith("HOLD-");
    }

    @Test
    void createTemplateWindow_idAndExternalIdMismatch_rejects() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID))
                .thenReturn(Optional.of(template(TEMPLATE_ID, "TPL-001")));

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.TEMPLATE_WINDOW,
                null,
                TEMPLATE_ID,
                "TPL-OTHER",
                FROM,
                TO,
                null
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class)
                .extracting(ex -> ((LegalHoldValidationException) ex).messageKey())
                .isEqualTo("api.error.validation.fieldInvalid");
        verify(repository, never()).save(any());
    }

    @Test
    void createTemplateWindow_missingTemplateIdentity_rejects() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.TEMPLATE_WINDOW,
                null,
                null,
                "  ",
                FROM,
                TO,
                null
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class)
                .extracting(ex -> ((LegalHoldValidationException) ex).messageKey())
                .isEqualTo("api.error.validation.fieldRequired");
    }

    @Test
    void createTemplateWindow_unknownExternalId_404() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        when(templateRepository.findByExternalIdAndDeletedAtIsNull("MISSING"))
                .thenReturn(Optional.empty());

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.TEMPLATE_WINDOW,
                null,
                null,
                "MISSING",
                FROM,
                TO,
                null
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(TemplateNotFoundException.class);
        verify(auditRecorder, never()).recordLegalHoldCreated(any(), any(), any());
    }

    @Test
    void createInvocationSet_rejectsTemplateWindowFields() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.INVOCATION_SET,
                null,
                null,
                null,
                FROM,
                null,
                List.of("INV-1")
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createInvocationSet_blankId_rejects() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.INVOCATION_SET,
                null,
                null,
                null,
                null,
                null,
                List.of("INV-1", "  ")
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class)
                .extracting(ex -> ((LegalHoldValidationException) ex).messageKey())
                .isEqualTo("api.error.validation.fieldInvalid");
        verify(repository, never()).save(any());
    }

    @Test
    void createInvocationSet_trimsAndDedupesIds() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.INVOCATION_SET,
                null,
                null,
                null,
                null,
                null,
                List.of(" INV-A ", "INV-A", "INV-B")
        );

        LegalHoldView view = service.create(request, admin);

        assertThat(view.invocationExternalIds()).containsExactlyInAnyOrder("INV-A", "INV-B");
        assertThat(view.invocationCount()).isEqualTo(2);
    }

    @Test
    void create_nonGlobalAdmin_forbiddenWithoutPersist() {
        ManagementSessionClaims groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        when(groupAccessService.canManageLegalHold(groupAdmin)).thenReturn(false);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.INVOCATION_SET,
                null,
                null,
                null,
                null,
                null,
                List.of("INV-1")
        );

        assertThatThrownBy(() -> service.create(request, groupAdmin))
                .isInstanceOf(LegalHoldAccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(auditRecorder, never()).recordLegalHoldCreated(any(), any(), any());
    }

    @Test
    void get_unknownHold_404() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(missing, admin))
                .isInstanceOf(LegalHoldNotFoundException.class);
    }

    @Test
    void list_filtersByStatusAndPages() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        LegalHoldEntity active = activeInvocationHold(UUID.randomUUID(), Set.of("INV-1"));
        LegalHoldEntity released = activeInvocationHold(UUID.randomUUID(), Set.of("INV-2"));
        released.release(Instant.parse("2026-07-01T00:00:00Z"), "10000001");
        when(repository.findByStatusOrderByCreatedAtDesc(LegalHoldStatus.ACTIVE))
                .thenReturn(List.of(active));
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(active, released));

        PageView<LegalHoldView> activeOnly = service.list(LegalHoldStatus.ACTIVE, 0, 10, admin);
        PageView<LegalHoldView> all = service.list(null, 0, 1, admin);

        assertThat(activeOnly.content()).hasSize(1);
        assertThat(activeOnly.content().getFirst().status()).isEqualTo(LegalHoldStatus.ACTIVE);
        assertThat(all.content()).hasSize(1);
        assertThat(all.totalElements()).isEqualTo(2);
        assertThat(all.totalPages()).isEqualTo(2);
    }

    @Test
    void release_unknownHold_404WithoutAudit() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.release(missing, admin))
                .isInstanceOf(LegalHoldNotFoundException.class);
        verify(auditRecorder, never()).recordLegalHoldReleased(any(), any(), any());
    }

    private LegalHoldEntity activeInvocationHold(UUID id, Set<String> ids) {
        return new LegalHoldEntity(
                id,
                "HOLD-DEPTH-1",
                LegalHoldScopeType.INVOCATION_SET,
                LegalHoldStatus.ACTIVE,
                "r",
                null,
                null,
                null,
                null,
                Instant.parse("2026-01-02T00:00:00Z"),
                "10000001",
                ids
        );
    }

    private TemplateEntity template(UUID id, String externalId) {
        return new TemplateEntity(
                id,
                externalId,
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
    }

    private ManagementSessionClaims globalAdmin() {
        return session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
    }

    private ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                "Admin",
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "",
                List.of(),
                Instant.EPOCH
        );
    }
}
