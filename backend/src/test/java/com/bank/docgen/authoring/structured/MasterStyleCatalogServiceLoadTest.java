package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParserTest;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogJsonCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-CE-K02-002 / 005 — loadForMaster honors masterId; lazy hydrate.
 */
@ExtendWith(MockitoExtension.class)
class MasterStyleCatalogServiceLoadTest {

    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MasterStyleCatalogService service;

    @BeforeEach
    void setUp() {
        service = new MasterStyleCatalogService(
                objectMapper,
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort
        );
    }

    @Test
    void loadForMasterReturnsDifferentCatalogsPerMaster() throws Exception {
        UUID masterA = UUID.randomUUID();
        UUID masterB = UUID.randomUUID();
        UUID revA = UUID.randomUUID();
        UUID revB = UUID.randomUUID();

        MasterStyleCatalog catalogA = parseAndTagSongti();
        MasterStyleCatalog catalogB = parseAndTagFangsong();

        stubMaster(masterA, revA, catalogA);
        stubMaster(masterB, revB, catalogB);

        MasterStyleCatalog loadedA = service.loadForMaster(masterA);
        MasterStyleCatalog loadedB = service.loadForMaster(masterB);

        assertThat(loadedA.find("ClauseBody").typography().eastAsia()).isEqualTo("宋体");
        assertThat(loadedB.find("ClauseBody").typography().eastAsia()).isEqualTo("仿宋");
        assertThat(loadedA.catalogVersion()).isNotEqualTo(loadedB.catalogVersion());
    }

    @Test
    void lazyHydratesMissingCatalogFromStorage() throws Exception {
        UUID masterId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        byte[] docx = MasterDocxStyleCatalogParserTest.dualFontMasterBytes();

        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId, "RETAIL", "M", null, "key", "m.docx", "10000001");
        master.setCurrentRevisionLineId(revisionId);
        MasterRevisionLineEntity revision = new MasterRevisionLineEntity(
                revisionId,
                masterId,
                "key",
                "m.docx",
                1,
                MasterDocumentStatus.DRAFT,
                1,
                true,
                null,
                "10000001"
        );

        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(revisionId, masterId))
                .thenReturn(Optional.of(revision));
        when(objectStoragePort.get("key")).thenReturn(new ByteArrayInputStream(docx));
        when(masterRevisionLineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MasterStyleCatalog loaded = service.loadForMaster(masterId);

        assertThat(loaded.hasDocDefaults()).isTrue();
        assertThat(loaded.find("ClauseBody").typography().eastAsia()).isEqualTo("仿宋");
        ArgumentCaptor<MasterRevisionLineEntity> captor = ArgumentCaptor.forClass(MasterRevisionLineEntity.class);
        verify(masterRevisionLineRepository).save(captor.capture());
        assertThat(captor.getValue().getStyleCatalogJson()).contains("仿宋");
    }

    private void stubMaster(UUID masterId, UUID revisionId, MasterStyleCatalog catalog) {
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId, "RETAIL", "M", null, "key-" + masterId, "m.docx", "10000001");
        master.setCurrentRevisionLineId(revisionId);
        MasterRevisionLineEntity revision = new MasterRevisionLineEntity(
                revisionId,
                masterId,
                "key-" + masterId,
                "m.docx",
                1,
                MasterDocumentStatus.DRAFT,
                1,
                true,
                null,
                "10000001"
        );
        revision.setStyleCatalogJson(MasterStyleCatalogJsonCodec.write(objectMapper, catalog));
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(revisionId, masterId))
                .thenReturn(Optional.of(revision));
    }

    private MasterStyleCatalog parseAndTagSongti() throws Exception {
        MasterStyleCatalog base = com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParser
                .parse(MasterDocxStyleCatalogParserTest.dualFontMasterBytes());
        // Override ClauseBody eastAsia to 宋体 for master A differentiation
        var entry = base.find("ClauseBody");
        var typography = new com.bank.docgen.sharedkernel.document.style.MasterStyleTypography(
                entry.typography().ascii(),
                entry.typography().hAnsi(),
                "宋体",
                entry.typography().cs(),
                entry.typography().fontSizeHalfPoints(),
                entry.typography().bold(),
                entry.typography().italic(),
                entry.typography().color()
        );
        return new MasterStyleCatalog(
                "master-a",
                java.util.Map.of(
                        "ClauseBody",
                        new com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogEntry(
                                "ClauseBody",
                                entry.applicableNodeTypes(),
                                entry.renderPurpose(),
                                entry.styleType(),
                                typography
                        )
                ),
                base.docDefaults(),
                base.themeFonts()
        );
    }

    private MasterStyleCatalog parseAndTagFangsong() throws Exception {
        MasterStyleCatalog base = com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParser
                .parse(MasterDocxStyleCatalogParserTest.dualFontMasterBytes());
        return new MasterStyleCatalog(
                "master-b",
                base.stylesByKey(),
                base.docDefaults(),
                base.themeFonts()
        );
    }
}
