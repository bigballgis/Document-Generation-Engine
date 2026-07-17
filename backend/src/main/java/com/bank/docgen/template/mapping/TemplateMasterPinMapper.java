package com.bank.docgen.template.mapping;

import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Maps CE-K01 release-bundle pin fields on {@link TemplateVersionEntity} into the
 * management-read {@link TemplateExportMasterPinView} shape (CE-U19 / CE-E01).
 */
@Component
public class TemplateMasterPinMapper {

    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectMapper objectMapper;

    public TemplateMasterPinMapper(
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectMapper objectMapper
    ) {
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * @return pin view when revision id and file hash are present; otherwise {@code null}
     */
    public TemplateExportMasterPinView toView(TemplateVersionEntity version) {
        if (version == null) {
            return null;
        }
        UUID revisionId = version.getMasterRevisionId();
        String fileHash = version.getMasterFileHash();
        if (revisionId == null || fileHash == null || fileHash.isBlank()) {
            return null;
        }
        Integer sequence = masterRevisionLineRepository.findById(revisionId)
                .filter(line -> line.getDeletedAt() == null)
                .map(MasterRevisionLineEntity::getRevisionSequence)
                .orElse(null);
        return new TemplateExportMasterPinView(
                revisionId.toString(),
                fileHash.toLowerCase(Locale.ROOT),
                sequence,
                parsePinOrigin(version.getPinMetadataJson())
        );
    }

    private String parsePinOrigin(String pinMetadataJson) {
        if (pinMetadataJson == null || pinMetadataJson.isBlank()) {
            return "PUBLISHED";
        }
        try {
            JsonNode node = objectMapper.readTree(pinMetadataJson);
            String origin = node.path("pinOrigin").asText(null);
            return origin == null || origin.isBlank() ? "PUBLISHED" : origin;
        } catch (IOException ex) {
            return "PUBLISHED";
        }
    }
}
