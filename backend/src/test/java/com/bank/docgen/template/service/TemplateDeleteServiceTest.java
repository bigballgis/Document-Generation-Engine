package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateDeleteServiceTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private GroupAccessService groupAccessService;

    private TemplateDeleteService service;

    @BeforeEach
    void setUp() {
        service = new TemplateDeleteService(templateRepository, lifecycleRecordRepository, groupAccessService);
    }

    @Test
    void globalAdminDelete_setsDeletedAtAndRecordsLifecycleAudit() {
        UUID templateId = UUID.randomUUID();
        ManagementSessionClaims globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"));
        TemplateEntity template = template(templateId);
        when(groupAccessService.canDeleteTemplate(globalAdmin)).thenReturn(true);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        service.deleteTemplate(templateId, new LifecycleGovernanceRequest("retire template", true), globalAdmin);

        assertThat(template.getDeletedAt()).isNotNull();
        assertThat(template.getUpdatedBy()).isEqualTo("10000001");
        verify(templateRepository).save(template);
        ArgumentCaptor<TemplateLifecycleRecordEntity> captor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(LifecycleAction.DELETE);
    }

    @Test
    void groupAdminDelete_throwsAccessDenied() {
        UUID templateId = UUID.randomUUID();
        ManagementSessionClaims groupAdmin = session("10000002", List.of("GROUP_ADMIN"));
        when(groupAccessService.canDeleteTemplate(groupAdmin)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteTemplate(
                templateId,
                new LifecycleGovernanceRequest("retire template", true),
                groupAdmin
        )).isInstanceOf(TemplateAccessDeniedException.class);

        verifyNoInteractions(templateRepository, lifecycleRecordRepository);
    }

    @Test
    void deletingAlreadyDeletedTemplate_throwsValidationError() {
        UUID templateId = UUID.randomUUID();
        ManagementSessionClaims globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"));
        TemplateEntity template = template(templateId);
        template.setDeletedAt(Instant.now().minusSeconds(60));
        when(groupAccessService.canDeleteTemplate(globalAdmin)).thenReturn(true);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> service.deleteTemplate(
                templateId,
                new LifecycleGovernanceRequest("retire template", true),
                globalAdmin
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.template.alreadyDeleted");
    }

    private ManagementSessionClaims session(String username, List<String> roles) {
        return new ManagementSessionClaims(
                username,
                "Operator",
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    private TemplateEntity template(UUID templateId) {
        return new TemplateEntity(
                templateId,
                "TPL-DELETE-001",
                "RETAIL",
                "Delete Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
    }
}
