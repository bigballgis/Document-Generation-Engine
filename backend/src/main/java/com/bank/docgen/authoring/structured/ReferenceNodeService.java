package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Validates seal, QR/barcode, image, and attachment-list reference nodes (P18-T05 + IBL-B5).
 */
@Service
public class ReferenceNodeService {

    public static final String MESSAGE_KEY_SEAL_OUTSIDE_AUTHORIZED_AREA =
            "generation.warning.fidelity.sealOutsideAuthorizedArea";
    public static final String MESSAGE_KEY_SEAL_AUTHORIZED_AREA_UNKNOWN =
            "generation.warning.fidelity.sealAuthorizedAreaUnknown";
    public static final String MESSAGE_KEY_SEAL_AUTHORIZED_AREA_INVALID =
            "generation.warning.fidelity.sealAuthorizedAreaInvalid";
    public static final String MESSAGE_KEY_SEAL_PLACEMENT_GEOMETRY_INVALID =
            "generation.warning.fidelity.sealPlacementGeometryInvalid";
    public static final String MESSAGE_KEY_SEAL_SCALING_NOT_ALLOWED =
            "generation.warning.fidelity.sealScalingNotAllowed";
    public static final String MESSAGE_KEY_IMAGE_SCALING = "generation.warning.fidelity.imageScalingAdjusted";
    public static final String MESSAGE_KEY_MISSING_REFERENCE = "generation.warning.fidelity.missingReferenceKey";

    private final ObjectMapper objectMapper;
    private final IssueFactory issueFactory = this::issue;

    public ReferenceNodeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ReferenceNodeValidationResult validateStructuredContent(String structuredContentJson) {
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>();
        List<StructuredContentFidelityIssue> warnings = new ArrayList<>();
        List<AttachmentListReferenceModel> attachmentLists = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            if (root.isObject() && root.get("nodes") != null && root.get("nodes").isArray()) {
                AuthorizedSealAreaCatalog catalog = AuthorizedSealAreaCatalog.parse(root, issueFactory);
                blockers.addAll(catalog.catalogBlockers());
                walkNodes(root.get("nodes"), "nodes", catalog, blockers, warnings, attachmentLists);
            }
        } catch (IOException ex) {
            return ReferenceNodeValidationResult.of(
                    StructuredContentValidationResult.of(blockers, warnings),
                    attachmentLists
            );
        }
        return ReferenceNodeValidationResult.of(
                StructuredContentValidationResult.of(blockers, warnings),
                attachmentLists
        );
    }

    private void walkNodes(
            JsonNode nodes,
            String location,
            AuthorizedSealAreaCatalog catalog,
            List<StructuredContentFidelityIssue> blockers,
            List<StructuredContentFidelityIssue> warnings,
            List<AttachmentListReferenceModel> attachmentLists
    ) {
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);
            String nodeLocation = location + "[" + index + "]";
            if (!node.isObject()) {
                continue;
            }
            StructuredContentNodeType nodeType =
                    StructuredContentNodeType.fromJsonType(node.path("type").asText("")).orElse(null);
            if (nodeType == StructuredContentNodeType.SEAL_REF) {
                validateSealRef(node, nodeLocation, catalog, blockers, warnings);
            } else if (nodeType == StructuredContentNodeType.IMAGE_REF) {
                validateImageRef(node, nodeLocation, warnings);
            } else if (nodeType == StructuredContentNodeType.QR_BARCODE_REF) {
                validateReferenceKey(node, nodeLocation, "referenceKey", blockers);
            } else if (nodeType == StructuredContentNodeType.ATTACHMENT_LIST_REF) {
                String referenceKey = validateReferenceKey(node, nodeLocation, "referenceKey", blockers);
                if (referenceKey != null) {
                    attachmentLists.add(new AttachmentListReferenceModel(referenceKey, nodeLocation));
                }
            }
            JsonNode children = node.get("children");
            if (children != null && children.isArray()) {
                walkNodes(children, nodeLocation + ".children", catalog, blockers, warnings, attachmentLists);
            }
        }
    }

    private void validateSealRef(
            JsonNode node,
            String location,
            AuthorizedSealAreaCatalog catalog,
            List<StructuredContentFidelityIssue> blockers,
            List<StructuredContentFidelityIssue> warnings
    ) {
        validateReferenceKey(node, location, "referenceKey", blockers);
        JsonNode placement = node.get("placement");
        if (placement != null && placement.isObject()) {
            // CRCH-W0-7 / D4: seals render inline — authorized-area geometry is advisory only.
            validateSealPlacementGeometry(placement, location, catalog, warnings);
        }
        if (node.path("applyScaling").asBoolean(false)) {
            blockers.add(issue(
                    StructuredContentFidelitySeverity.BLOCKER,
                    FidelityWarningCode.SEAL_SCALING_NOT_ALLOWED,
                    MESSAGE_KEY_SEAL_SCALING_NOT_ALLOWED,
                    location,
                    "Seal scaling is not allowed at " + location + ".",
                    "Remove applyScaling from seal references; seals must render at authorized size."
            ));
        }
    }

    private void validateSealPlacementGeometry(
            JsonNode placement,
            String location,
            AuthorizedSealAreaCatalog catalog,
            List<StructuredContentFidelityIssue> warnings
    ) {
        String authorizedAreaId = placement.path("authorizedAreaId").asText("").trim();
        JsonNode sealBoxNode = placement.get("sealBox");
        Optional<SealGeometryRules.SealAxisAlignedBox> sealBox =
                AuthorizedSealAreaCatalog.parseSealBox(sealBoxNode);
        if (sealBox.isEmpty()) {
            warnings.add(issue(
                    StructuredContentFidelitySeverity.WARNING,
                    FidelityWarningCode.SEAL_PLACEMENT_GEOMETRY_INVALID,
                    MESSAGE_KEY_SEAL_PLACEMENT_GEOMETRY_INVALID,
                    location + ".placement",
                    "Seal placement geometry is invalid at " + location
                            + "; seals are placed inline at the anchor (absolute page box is not applied).",
                    "Provide placement.sealBox width/height for inline seal size, or remove placement if unused."
            ));
            return;
        }

        AuthorizedSealAreaCatalog.AreaLookup lookup = catalog.lookup(authorizedAreaId);
        if (lookup.status() == AuthorizedSealAreaCatalog.AreaStatus.UNKNOWN) {
            warnings.add(issue(
                    StructuredContentFidelitySeverity.WARNING,
                    FidelityWarningCode.SEAL_AUTHORIZED_AREA_UNKNOWN,
                    MESSAGE_KEY_SEAL_AUTHORIZED_AREA_UNKNOWN,
                    location + ".placement.authorizedAreaId",
                    "Authorized seal area id is unknown at " + location
                            + "; seals remain inline at the anchor.",
                    "Declare the area in authorizedSealAreas for documentation, or remove authorizedAreaId."
            ));
            return;
        }
        if (lookup.status() == AuthorizedSealAreaCatalog.AreaStatus.INVALID) {
            warnings.add(issue(
                    StructuredContentFidelitySeverity.WARNING,
                    FidelityWarningCode.SEAL_AUTHORIZED_AREA_INVALID,
                    MESSAGE_KEY_SEAL_AUTHORIZED_AREA_INVALID,
                    location + ".placement.authorizedAreaId",
                    "Authorized seal area referenced at " + location
                            + " is invalid; seals remain inline at the anchor.",
                    "Correct the authorizedSealAreas entry geometry or remove duplicate ids."
            ));
            return;
        }

        if (!SealGeometryRules.fullyContains(lookup.area(), sealBox.get())) {
            warnings.add(issue(
                    StructuredContentFidelitySeverity.WARNING,
                    FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA,
                    MESSAGE_KEY_SEAL_OUTSIDE_AUTHORIZED_AREA,
                    location,
                    "Seal placement coordinates at " + location
                            + " fall outside the declared authorized area; "
                            + "the product places seals inline at the anchor and does not honour absolute page boxes.",
                    "Treat authorized areas as authoring guidance, or remove placement coordinates."
            ));
        }
    }

    private void validateImageRef(
            JsonNode node,
            String location,
            List<StructuredContentFidelityIssue> warnings
    ) {
        if (node.path("applyScaling").asBoolean(false)) {
            warnings.add(issue(
                    StructuredContentFidelitySeverity.WARNING,
                    FidelityWarningCode.IMAGE_SCALING_ADJUSTED,
                    MESSAGE_KEY_IMAGE_SCALING,
                    location,
                    "Image scaling adjustment may apply at " + location + ".",
                    "Review the rendered image size in preview before publish."
            ));
        }
    }

    private String validateReferenceKey(
            JsonNode node,
            String location,
            String fieldName,
            List<StructuredContentFidelityIssue> blockers
    ) {
        String referenceKey = node.path(fieldName).asText("").trim();
        if (referenceKey.isBlank()) {
            blockers.add(issue(
                    StructuredContentFidelitySeverity.BLOCKER,
                    FidelityWarningCode.MISSING_REFERENCE_KEY,
                    MESSAGE_KEY_MISSING_REFERENCE,
                    location,
                    "Reference key is missing at " + location + ".",
                    "Provide a referenceKey for the reference node."
            ));
            return null;
        }
        return referenceKey;
    }

    private StructuredContentFidelityIssue issue(
            StructuredContentFidelitySeverity severity,
            FidelityWarningCode code,
            String messageKey,
            String location,
            String detectionSummary,
            String suggestion
    ) {
        return new StructuredContentFidelityIssue(
                severity,
                code,
                messageKey,
                location,
                detectionSummary,
                suggestion
        );
    }

    @FunctionalInterface
    interface IssueFactory {
        StructuredContentFidelityIssue create(
                StructuredContentFidelitySeverity severity,
                FidelityWarningCode code,
                String messageKey,
                String location,
                String detectionSummary,
                String suggestion
        );
    }
}
