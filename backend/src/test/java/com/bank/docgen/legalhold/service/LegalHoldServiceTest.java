package com.bank.docgen.legalhold.service;

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
 * BDD-CE-G04-001…006 service behavior.
 */
@ExtendWith(MockitoExtension.class)
class LegalHoldServiceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");

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
    void createTemplateWindow_persistsActiveAndAudits() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID))
                .thenReturn(Optional.of(template(TEMPLATE_ID, "TPL-001")));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.TEMPLATE_WINDOW,
                "reason",
                TEMPLATE_ID,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-06-30T00:00:00Z"),
                null
        );

        LegalHoldView view = service.create(request, admin);

        assertThat(view.status()).isEqualTo(LegalHoldStatus.ACTIVE);
        assertThat(view.scopeType()).isEqualTo(LegalHoldScopeType.TEMPLATE_WINDOW);
        assertThat(view.templateId()).isEqualTo(TEMPLATE_ID);
        verify(auditRecorder).recordLegalHoldCreated(any(), eq("10000001"), eq("Admin (10000001)"));
    }

    @Test
    void createInvocationSet_persistsIds() {
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
                List.of("INV-A", "INV-B")
        );

        LegalHoldView view = service.create(request, admin);

        assertThat(view.invocationExternalIds()).containsExactlyInAnyOrder("INV-A", "INV-B");
        ArgumentCaptor<LegalHoldEntity> captor = ArgumentCaptor.forClass(LegalHoldEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getInvocationExternalIds()).containsExactlyInAnyOrder("INV-A", "INV-B");
    }

    @Test
    void create_rejectsMixedScope() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.TEMPLATE_WINDOW,
                null,
                TEMPLATE_ID,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                List.of("INV-1")
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class);
        verify(repository, never()).save(any());
        verify(auditRecorder, never()).recordLegalHoldCreated(any(), any(), any());
    }

    @Test
    void create_missingTemplate_404() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.empty());

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.TEMPLATE_WINDOW,
                null,
                TEMPLATE_ID,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                null
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    void release_marksReleasedAndAudits() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        UUID holdId = UUID.randomUUID();
        LegalHoldEntity active = activeHold(holdId);
        when(repository.findById(holdId)).thenReturn(Optional.of(active));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LegalHoldView view = service.release(holdId, admin);

        assertThat(view.status()).isEqualTo(LegalHoldStatus.RELEASED);
        assertThat(view.releasedByUsername()).isEqualTo("10000001");
        verify(auditRecorder).recordLegalHoldReleased(any(), eq("10000001"), eq("Admin (10000001)"));
    }

    @Test
    void release_alreadyReleased_409() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        UUID holdId = UUID.randomUUID();
        LegalHoldEntity released = activeHold(holdId);
        released.release(Instant.now(), "10000001");
        when(repository.findById(holdId)).thenReturn(Optional.of(released));

        assertThatThrownBy(() -> service.release(holdId, admin))
                .isInstanceOf(LegalHoldAlreadyReleasedException.class);
        verify(auditRecorder, never()).recordLegalHoldReleased(any(), any(), any());
    }

    @Test
    void nonGlobalAdmin_forbidden() {
        ManagementSessionClaims groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        when(groupAccessService.canManageLegalHold(groupAdmin)).thenReturn(false);

        assertThatThrownBy(() -> service.list(null, 0, 20, groupAdmin))
                .isInstanceOf(LegalHoldAccessDeniedException.class);
    }

    @Test
    void createInvocationSet_rejectsEmptyIds() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.INVOCATION_SET,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createInvocationSet_rejectsMoreThanMaxIds() {
        ManagementSessionClaims admin = globalAdmin();
        when(groupAccessService.canManageLegalHold(admin)).thenReturn(true);
        List<String> tooMany = java.util.stream.IntStream.rangeClosed(1, LegalHoldService.MAX_INVOCATION_IDS + 1)
                .mapToObj(i -> "INV-" + i)
                .toList();

        CreateLegalHoldRequest request = new CreateLegalHoldRequest(
                LegalHoldScopeType.INVOCATION_SET,
                null,
                null,
                null,
                null,
                null,
                tooMany
        );

        assertThatThrownBy(() -> service.create(request, admin))
                .isInstanceOf(LegalHoldValidationException.class);
        verify(repository, never()).save(any());
    }

    private LegalHoldEntity activeHold(UUID id) {
        return new LegalHoldEntity(
                id,
                "HOLD-1",
                LegalHoldScopeType.INVOCATION_SET,
                LegalHoldStatus.ACTIVE,
                "r",
                null,
                null,
                null,
                null,
                Instant.now(),
                "10000001",
                Set.of("INV-1")
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
