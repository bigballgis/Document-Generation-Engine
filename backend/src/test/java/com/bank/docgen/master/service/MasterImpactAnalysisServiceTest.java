package com.bank.docgen.master.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.api.MasterImpactAnalysisView;
import com.bank.docgen.master.api.MasterRevisionDiffView;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineAnchorEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MasterImpactAnalysisServiceTest {

    private static final ManagementSessionClaims RETAIL_ADMIN = session(
            "10000010",
            List.of("GROUP_ADMIN"),
            List.of("RETAIL")
    );

    @Mock
    private MasterDocumentRepository masterDocumentRepository;

    @Mock
    private MasterRevisionLineRepository masterRevisionLineRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private ObjectStoragePort objectStoragePort;

    private MasterImpactAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new MasterImpactAnalysisService(
                masterDocumentRepository,
                masterRevisionLineRepository,
                templateRepository,
                objectStoragePort,
                new GroupAccessService()
        );
    }

    @Test
    void impactAnalysisReturnsReferencedTemplatesWithNames() {
        UUID masterId = UUID.randomUUID();
        stubReadableMaster(masterId);
        when(masterRevisionLineRepository.findWithAnchorsByMasterIdAndCurrentTrueAndDeletedAtIsNull(masterId))
                .thenReturn(Optional.of(revision(masterId, 2, true, List.of("HEADER", "BODY"))));
        when(masterRevisionLineRepository.findWithAnchorsByMasterIdAndRevisionSequenceAndDeletedAtIsNull(masterId, 1))
                .thenReturn(Optional.of(revision(masterId, 1, false, List.of("HEADER", "BODY"))));

        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        when(templateRepository.findByMasterIdAndDeletedAtIsNull(masterId)).thenReturn(List.of(
                template(t1, masterId, "Loan Contract", TemplateLifecycleStatus.DRAFT),
                template(t2, masterId, "Credit Notice", TemplateLifecycleStatus.PUBLISHED)
        ));

        MasterImpactAnalysisView view = service.impactAnalysis(masterId, RETAIL_ADMIN);

        assertThat(view.referencedTemplateIds()).containsExactlyInAnyOrder(t1.toString(), t2.toString());
        assertThat(view.referencedTemplates()).extracting(t -> t.name())
                .containsExactlyInAnyOrder("Loan Contract", "Credit Notice");
        assertThat(view.retestRequired()).isFalse();
    }

    @Test
    void impactAnalysisTrueEmptyWhenNoTemplates() {
        UUID masterId = UUID.randomUUID();
        stubReadableMaster(masterId);
        when(masterRevisionLineRepository.findWithAnchorsByMasterIdAndCurrentTrueAndDeletedAtIsNull(masterId))
                .thenReturn(Optional.of(revision(masterId, 2, true, List.of("HEADER", "FOOTER"))));
        when(masterRevisionLineRepository.findWithAnchorsByMasterIdAndRevisionSequenceAndDeletedAtIsNull(masterId, 1))
                .thenReturn(Optional.of(revision(masterId, 1, false, List.of("HEADER"))));
        when(templateRepository.findByMasterIdAndDeletedAtIsNull(masterId)).thenReturn(List.of());

        MasterImpactAnalysisView view = service.impactAnalysis(masterId, RETAIL_ADMIN);

        assertThat(view.referencedTemplates()).isEmpty();
        assertThat(view.referencedTemplateIds()).isEmpty();
        assertThat(view.retestRequired()).isFalse();
        assertThat(view.anchorDelta().addedAnchors()).containsExactly("FOOTER");
    }

    @Test
    void retestRequiredWhenAnchorDeltaAndReferencesExist() {
        UUID masterId = UUID.randomUUID();
        stubReadableMaster(masterId);
        when(masterRevisionLineRepository.findWithAnchorsByMasterIdAndCurrentTrueAndDeletedAtIsNull(masterId))
                .thenReturn(Optional.of(revision(masterId, 2, true, List.of("HEADER"))));
        when(masterRevisionLineRepository.findWithAnchorsByMasterIdAndRevisionSequenceAndDeletedAtIsNull(masterId, 1))
                .thenReturn(Optional.of(revision(masterId, 1, false, List.of("HEADER", "FOOTER"))));
        when(templateRepository.findByMasterIdAndDeletedAtIsNull(masterId)).thenReturn(List.of(
                template(UUID.randomUUID(), masterId, "Loan Contract", TemplateLifecycleStatus.TESTING)
        ));

        MasterImpactAnalysisView view = service.impactAnalysis(masterId, RETAIL_ADMIN);

        assertThat(view.retestRequired()).isTrue();
        assertThat(view.anchorDelta().removedAnchors()).containsExactly("FOOTER");
    }

    @Test
    void retestNotRequiredWhenAnchorSetsMatchDespiteReferences() {
        UUID masterId = UUID.randomUUID();
        stubReadableMaster(masterId);
        when(masterRevisionLineRepository.findWithAnchorsByMasterIdAndCurrentTrueAndDeletedAtIsNull(masterId))
                .thenReturn(Optional.of(revision(masterId, 2, true, List.of("HEADER", "BODY"))));
        when(masterRevisionLineRepository.findWithAnchorsByMasterIdAndRevisionSequenceAndDeletedAtIsNull(masterId, 1))
                .thenReturn(Optional.of(revision(masterId, 1, false, List.of("HEADER", "BODY"))));
        when(templateRepository.findByMasterIdAndDeletedAtIsNull(masterId)).thenReturn(List.of(
                template(UUID.randomUUID(), masterId, "Loan Contract", TemplateLifecycleStatus.DRAFT)
        ));

        assertThat(service.impactAnalysis(masterId, RETAIL_ADMIN).retestRequired()).isFalse();
    }

    @Test
    void retestNotRequiredWhenNoPreviousRevision() {
        UUID masterId = UUID.randomUUID();
        stubReadableMaster(masterId);
        when(masterRevisionLineRepository.findWithAnchorsByMasterIdAndCurrentTrueAndDeletedAtIsNull(masterId))
                .thenReturn(Optional.of(revision(masterId, 1, true, List.of("HEADER"))));
        when(templateRepository.findByMasterIdAndDeletedAtIsNull(masterId)).thenReturn(List.of(
                template(UUID.randomUUID(), masterId, "Loan Contract", TemplateLifecycleStatus.DRAFT)
        ));

        assertThat(service.impactAnalysis(masterId, RETAIL_ADMIN).retestRequired()).isFalse();
    }

    @Test
    void revisionDiffReturnsAnchorDeltaAndDistinctHashes() throws Exception {
        UUID masterId = UUID.randomUUID();
        stubReadableMaster(masterId);
        MasterRevisionLineEntity baseline = revision(masterId, 1, false, List.of("HEADER", "FOOTER", "KEEP"));
        MasterRevisionLineEntity candidate = revision(masterId, 2, true, List.of("HEADER", "KEEP", "BODY"));
        // Force BODY onto a distinct sequence so FOOTER is removed (not renamed).
        candidate.replaceAnchors(List.of(
                new MasterRevisionLineAnchorEntity(candidate.getId(), "HEADER", "HEADER", 0),
                new MasterRevisionLineAnchorEntity(candidate.getId(), "KEEP", "KEEP", 2),
                new MasterRevisionLineAnchorEntity(candidate.getId(), "BODY", "BODY", 3)
        ));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(baseline.getId(), masterId))
                .thenReturn(Optional.of(baseline));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(candidate.getId(), masterId))
                .thenReturn(Optional.of(candidate));

        byte[] baseBytes = "baseline-docx".getBytes(StandardCharsets.UTF_8);
        byte[] candBytes = "candidate-docx".getBytes(StandardCharsets.UTF_8);
        when(objectStoragePort.get(baseline.getStorageKey())).thenReturn(new ByteArrayInputStream(baseBytes));
        when(objectStoragePort.get(candidate.getStorageKey())).thenReturn(new ByteArrayInputStream(candBytes));

        MasterRevisionDiffView diff = service.revisionDiff(
                masterId,
                baseline.getId(),
                candidate.getId(),
                RETAIL_ADMIN
        );

        assertThat(diff.addedAnchors()).containsExactly("BODY");
        assertThat(diff.removedAnchors()).containsExactly("FOOTER");
        assertThat(diff.baselineFileHash()).isEqualTo(sha256Hex(baseBytes));
        assertThat(diff.candidateFileHash()).isEqualTo(sha256Hex(candBytes));
        assertThat(diff.baselineFileHash()).isNotEqualTo(diff.candidateFileHash());
    }

    @Test
    void revisionDiffDeniesCrossGroupAccess() {
        UUID masterId = UUID.randomUUID();
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId,
                "CORP",
                "Corp Master",
                null,
                "key",
                "m.docx",
                "10000001"
        );
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));

        assertThatThrownBy(() -> service.revisionDiff(masterId, UUID.randomUUID(), UUID.randomUUID(), RETAIL_ADMIN))
                .isInstanceOf(MasterAccessDeniedException.class);
    }

    private void stubReadableMaster(UUID masterId) {
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId,
                "RETAIL",
                "Retail Master",
                null,
                "masters/" + masterId + "/current.docx",
                "master.docx",
                "10000010"
        );
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
    }

    private static MasterRevisionLineEntity revision(
            UUID masterId,
            int sequence,
            boolean current,
            List<String> anchorKeys
    ) {
        UUID lineId = UUID.randomUUID();
        MasterRevisionLineEntity line = new MasterRevisionLineEntity(
                lineId,
                masterId,
                "masters/" + masterId + "/r" + sequence + ".docx",
                "r" + sequence + ".docx",
                anchorKeys.size(),
                MasterDocumentStatus.DRAFT,
                sequence,
                current,
                null,
                "10000010"
        );
        List<MasterRevisionLineAnchorEntity> anchors = new java.util.ArrayList<>();
        for (int i = 0; i < anchorKeys.size(); i++) {
            String key = anchorKeys.get(i);
            MasterRevisionLineAnchorEntity anchor =
                    new MasterRevisionLineAnchorEntity(lineId, key, key, i);
            anchor.setRevisionLine(line);
            anchors.add(anchor);
        }
        line.replaceAnchors(anchors);
        return line;
    }

    private static TemplateEntity template(
            UUID id,
            UUID masterId,
            String name,
            TemplateLifecycleStatus status
    ) {
        TemplateEntity entity = new TemplateEntity(
                id,
                "ext-" + id,
                "RETAIL",
                name,
                null,
                masterId,
                "10000010"
        );
        entity.setLifecycleStatus(status);
        return entity;
    }

    private static ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.template-authoring-home",
                List.of(),
                Instant.now().plusSeconds(3600)
        );
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        byte[] hashed = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder(hashed.length * 2);
        for (byte b : hashed) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
