package com.bank.docgen.master.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.api.PinnedReleaseReference;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CE-K01 BDD-CE-K01-010..013: master revision delete protection (physical + soft)
 * fail-closed when referenced by any non-deleted published-lifecycle template version.
 */
@ExtendWith(MockitoExtension.class)
class MasterRevisionLineDeleteProtectionTest {

    private static final UUID MASTER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID REVISION_R1 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID REVISION_R3 = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @Mock private MasterDocumentRepository masterDocumentRepository;
    @Mock private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock private MasterReviewRecordRepository masterReviewRecordRepository;
    @Mock private ObjectStoragePort objectStoragePort;
    @Mock private GroupAccessService groupAccessService;
    @Mock private ManagementUserDisplayService managementUserDisplayService;
    @Mock private TemplateVersionRepository templateVersionRepository;

    private MasterRevisionLineService service;
    private ManagementSessionClaims masterAdmin;

    @BeforeEach
    void setUp() {
        service = new MasterRevisionLineService(
                masterDocumentRepository,
                masterRevisionLineRepository,
                masterReviewRecordRepository,
                objectStoragePort,
                groupAccessService,
                managementUserDisplayService,
                templateVersionRepository
        );
        masterAdmin = new ManagementSessionClaims(
                "10000010", "Master Admin", "master@example.com",
                com.bank.docgen.authorization.management.domain.AuthSource.LOCAL,
                List.of("GLOBAL_ADMIN"), List.of("*"),
                "route.master-home", List.of("route.master-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void deleteRevision_referencedByPublishedRelease_failClosed409_bddK01_010() {
        MasterDocumentEntity master = readableMaster();
        MasterRevisionLineEntity r1 = revision(REVISION_R1);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_R1, MASTER_ID))
                .thenReturn(Optional.of(r1));
        when(templateVersionRepository.findByMasterRevisionIdAndDeletedAtIsNull(REVISION_R1))
                .thenReturn(List.of(publishedVersion(TemplateLifecycleStatus.PUBLISHED, "1.0.0")));

        assertThatThrownBy(() -> service.deleteRevisionLine(MASTER_ID, REVISION_R1, masterAdmin))
                .isInstanceOf(MasterRevisionInUseException.class)
                .satisfies(ex -> {
                    List<PinnedReleaseReference> refs = ((MasterRevisionInUseException) ex).references();
                    assertThat(refs).hasSize(1);
                    assertThat(refs.get(0).templateId()).isEqualTo(TEMPLATE_ID.toString());
                    assertThat(refs.get(0).releaseVersion()).isEqualTo("1.0.0");
                    assertThat(refs.get(0).lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PUBLISHED.name());
                });
        verify(masterRevisionLineRepository, never()).save(any(MasterRevisionLineEntity.class));
        verify(objectStoragePort, never()).delete(any());
    }

    @Test
    void deleteRevision_referencedByStoppedRelease_stillBlocked_bddK01_013() {
        MasterDocumentEntity master = readableMaster();
        MasterRevisionLineEntity r1 = revision(REVISION_R1);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_R1, MASTER_ID))
                .thenReturn(Optional.of(r1));
        when(templateVersionRepository.findByMasterRevisionIdAndDeletedAtIsNull(REVISION_R1))
                .thenReturn(List.of(publishedVersion(TemplateLifecycleStatus.STOPPED, "1.0.0")));

        assertThatThrownBy(() -> service.deleteRevisionLine(MASTER_ID, REVISION_R1, masterAdmin))
                .isInstanceOf(MasterRevisionInUseException.class);
        verify(masterRevisionLineRepository, never()).save(any(MasterRevisionLineEntity.class));
    }

    @Test
    void deleteRevision_notReferenced_succeeds_bddK01_012() {
        MasterDocumentEntity master = readableMaster();
        master.setCurrentRevisionLineId(REVISION_R1);
        MasterRevisionLineEntity r3 = revision(REVISION_R3);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_R3, MASTER_ID))
                .thenReturn(Optional.of(r3));
        when(templateVersionRepository.findByMasterRevisionIdAndDeletedAtIsNull(REVISION_R3))
                .thenReturn(List.of());
        when(objectStoragePort.exists(r3.getStorageKey())).thenReturn(true);

        service.deleteRevisionLine(MASTER_ID, REVISION_R3, masterAdmin);

        assertThat(r3.getDeletedAt()).isNotNull();
        verify(masterRevisionLineRepository).save(r3);
        verify(objectStoragePort).delete(r3.getStorageKey());
    }

    @Test
    void deleteRevision_currentLine_failClosed_fosW6_2() {
        MasterDocumentEntity master = readableMaster();
        master.setCurrentRevisionLineId(REVISION_R1);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(MASTER_ID)).thenReturn(Optional.of(master));

        assertThatThrownBy(() -> service.deleteRevisionLine(MASTER_ID, REVISION_R1, masterAdmin))
                .isInstanceOf(MasterValidationException.class)
                .extracting(ex -> ((MasterValidationException) ex).messageKey())
                .isEqualTo("api.error.master.cannotDeleteCurrentRevision");
        verify(masterRevisionLineRepository, never()).save(any(MasterRevisionLineEntity.class));
        verify(objectStoragePort, never()).delete(any());
    }

    private MasterDocumentEntity readableMaster() {
        MasterDocumentEntity master = new MasterDocumentEntity(
                MASTER_ID, "RETAIL", "Retail Master", "desc",
                "masters/live.docx", "master.docx", "10000010"
        );
        when(groupAccessService.canAccessGroup(masterAdmin, master.getGroupCode())).thenReturn(true);
        return master;
    }

    private MasterRevisionLineEntity revision(UUID id) {
        return new MasterRevisionLineEntity(
                id, MASTER_ID, "masters/" + id + ".docx", "master.docx",
                1, MasterDocumentStatus.APPROVED, 1, false, "change", "10000010"
        );
    }

    private TemplateVersionEntity publishedVersion(TemplateLifecycleStatus status, String releaseVersion) {
        TemplateVersionEntity v = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000009");
        v.setReleaseVersion(releaseVersion);
        v.setLifecycleStatus(status);
        v.setMasterRevisionId(REVISION_R1);
        return v;
    }
}
