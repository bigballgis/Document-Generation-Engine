package com.bank.docgen.template.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateMasterPinMapperTest {

    @Mock
    private MasterRevisionLineRepository masterRevisionLineRepository;

    private TemplateMasterPinMapper mapper;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        mapper = new TemplateMasterPinMapper(masterRevisionLineRepository, new ObjectMapper());
        templateId = UUID.randomUUID();
    }

    @Test
    void toView_returnsNullWhenUnpinned() {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000003");

        assertThat(mapper.toView(version)).isNull();
    }

    @Test
    void toView_returnsNullWhenHashBlank() {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000003");
        version.setMasterRevisionId(UUID.randomUUID());
        version.setMasterFileHash("   ");

        assertThat(mapper.toView(version)).isNull();
    }

    @Test
    void toView_mapsPinnedFieldsWithSequenceAndOrigin() {
        UUID revisionId = UUID.randomUUID();
        String hash = "a".repeat(64);
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000003");
        version.setMasterRevisionId(revisionId);
        version.setMasterFileHash(hash.toUpperCase());
        version.setPinMetadataJson("{\"pinOrigin\":\"PINNED_RETROACTIVELY\"}");

        MasterRevisionLineEntity revision = new MasterRevisionLineEntity(
                revisionId,
                UUID.randomUUID(),
                "masters/key.docx",
                "master.docx",
                1,
                MasterDocumentStatus.APPROVED,
                3,
                true,
                "pin",
                "10000003"
        );
        when(masterRevisionLineRepository.findById(revisionId)).thenReturn(Optional.of(revision));

        TemplateExportMasterPinView pin = mapper.toView(version);

        assertThat(pin).isNotNull();
        assertThat(pin.masterRevisionId()).isEqualTo(revisionId.toString());
        assertThat(pin.masterFileHash()).isEqualTo(hash);
        assertThat(pin.revisionSequence()).isEqualTo(3);
        assertThat(pin.pinOrigin()).isEqualTo("PINNED_RETROACTIVELY");
    }

    @Test
    void toView_defaultsPinOriginToPublishedWhenMetadataAbsent() {
        UUID revisionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000003");
        version.setMasterRevisionId(revisionId);
        version.setMasterFileHash("b".repeat(64));
        when(masterRevisionLineRepository.findById(revisionId)).thenReturn(Optional.empty());

        TemplateExportMasterPinView pin = mapper.toView(version);

        assertThat(pin.revisionSequence()).isNull();
        assertThat(pin.pinOrigin()).isEqualTo("PUBLISHED");
    }
}
