package com.bank.docgen.authoring.structured;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.master.service.MasterValidationException;
import com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParseException;
import com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParser;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogJsonCodec;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogMergeSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the approved master style catalog and validates style refs + limited direct format (P18-T03).
 *
 * <p>CE-K02: {@link #loadForMaster(UUID)} returns the persisted (or lazily hydrated) catalog for the
 * master's current revision — never a hardcoded classpath typography catalog.
 */
@Service
public class MasterStyleCatalogService {

    public static final String MESSAGE_KEY_MISSING_STYLE = "generation.warning.fidelity.missingStyleReference";
    public static final String MESSAGE_KEY_INAPPLICABLE_STYLE = "generation.warning.fidelity.inapplicableStyle";
    public static final String MESSAGE_KEY_DIRECT_FORMAT_OUT_OF_WHITELIST =
            "generation.warning.fidelity.directFormatOutOfWhitelist";
    public static final String MESSAGE_KEY_DIRECT_FORMAT_GLOBAL_LAYOUT =
            "generation.warning.fidelity.directFormatGlobalLayout";
    public static final String MESSAGE_KEY_MASTER_STYLE_FALLBACK =
            "generation.warning.fidelity.masterStyleFallback";

    private static final String DEFAULT_CATALOG_RESOURCE = "authoring/default-master-style-catalog-v1.json";

    private final ObjectMapper objectMapper;
    private final MasterStyleCatalog platformMetadataCatalog;
    private final MasterStyleCatalogValidationSupport validation;
    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;

    /**
     * Test-only constructor: validation against platform metadata catalog (no persistence).
     */
    public MasterStyleCatalogService(ObjectMapper objectMapper) {
        this(objectMapper, null, null, null);
    }

    @Autowired
    public MasterStyleCatalogService(
            ObjectMapper objectMapper,
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort
    ) {
        this.objectMapper = objectMapper;
        this.validation = new MasterStyleCatalogValidationSupport(objectMapper);
        this.platformMetadataCatalog = validation.loadCatalogResource(DEFAULT_CATALOG_RESOURCE);
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
    }

    /**
     * Platform metadata catalog (applicableNodeTypes / renderPurpose only). Not a typography source.
     */
    public MasterStyleCatalog platformMetadataCatalog() {
        return platformMetadataCatalog;
    }

    @Transactional
    public MasterStyleCatalog loadForMaster(UUID masterId) {
        if (masterDocumentRepository == null || masterRevisionLineRepository == null) {
            // Unit-test constructor: return platform metadata so existing validate() tests keep working.
            return platformMetadataCatalog;
        }
        if (masterId == null) {
            throw new MasterNotFoundException();
        }
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        UUID revisionId = master.getCurrentRevisionLineId();
        if (revisionId == null) {
            throw new MasterValidationException("api.error.master.currentRevisionUnavailable");
        }
        return loadForRevision(masterId, revisionId);
    }

    @Transactional
    public MasterStyleCatalog loadForRevision(UUID masterId, UUID revisionLineId) {
        if (masterRevisionLineRepository == null) {
            return platformMetadataCatalog;
        }
        MasterRevisionLineEntity revision = masterRevisionLineRepository
                .findByIdAndMasterIdAndDeletedAtIsNull(revisionLineId, masterId)
                .orElseThrow(MasterNotFoundException::new);
        MasterStyleCatalog persisted = MasterStyleCatalogJsonCodec.read(
                objectMapper,
                revision.getStyleCatalogJson()
        );
        if (persisted != null) {
            return MasterStyleCatalogMergeSupport.mergeWithPlatformMetadata(persisted, platformMetadataCatalog);
        }
        return hydrateAndPersist(revision);
    }

    private MasterStyleCatalog hydrateAndPersist(MasterRevisionLineEntity revision) {
        if (objectStoragePort == null) {
            throw new MasterValidationException("api.error.master.styleCatalogParseFailed");
        }
        try (InputStream inputStream = objectStoragePort.get(revision.getStorageKey())) {
            MasterStyleCatalog parsed = MasterDocxStyleCatalogParser.parse(inputStream.readAllBytes());
            MasterStyleCatalog merged = MasterStyleCatalogMergeSupport.mergeWithPlatformMetadata(
                    parsed,
                    platformMetadataCatalog
            );
            revision.setStyleCatalogJson(MasterStyleCatalogJsonCodec.write(objectMapper, merged));
            masterRevisionLineRepository.save(revision);
            return merged;
        } catch (MasterDocxStyleCatalogParseException | MasterValidationException ex) {
            throw new MasterValidationException("api.error.master.styleCatalogParseFailed");
        } catch (IOException | RuntimeException ex) {
            throw new MasterValidationException("api.error.master.styleCatalogParseFailed");
        }
    }

    public StructuredContentValidationResult validate(String structuredContentJson, MasterStyleCatalog catalog) {
        MasterStyleCatalog resolvedCatalog = catalog == null ? platformMetadataCatalog : catalog;
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>();
        List<StructuredContentFidelityIssue> warnings = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            if (!root.isObject() || !root.get("nodes").isArray()) {
                return StructuredContentValidationResult.of(blockers, warnings);
            }
            JsonNode nodes = root.get("nodes");
            for (int index = 0; index < nodes.size(); index++) {
                walkNode(nodes.get(index), "nodes[" + index + "]", null, resolvedCatalog, blockers, warnings);
            }
        } catch (IOException ex) {
            return StructuredContentValidationResult.of(blockers, warnings);
        }
        return StructuredContentValidationResult.of(blockers, warnings);
    }

    private void walkNode(
            JsonNode node,
            String location,
            String parentNodeType,
            MasterStyleCatalog catalog,
            List<StructuredContentFidelityIssue> blockers,
            List<StructuredContentFidelityIssue> warnings
    ) {
        if (!node.isObject()) {
            return;
        }
        String rawType = node.path("type").asText("");
        StructuredContentNodeType nodeType = StructuredContentNodeType.fromJsonType(rawType).orElse(null);
        String contextNodeType = nodeType == StructuredContentNodeType.STYLE_REF ? parentNodeType : rawType;

        validation.validateDirectFormat(node.get("directFormat"), location, blockers);
        if (node.has("styleRef") && !node.get("styleRef").isNull()) {
            validation.validateStyleReference(
                    node.get("styleRef").asText(""),
                    contextNodeType,
                    location,
                    catalog,
                    blockers
            );
        }
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (int index = 0; index < children.size(); index++) {
                walkNode(children.get(index), location + ".children[" + index + "]", rawType, catalog, blockers, warnings);
            }
        }
    }
}
