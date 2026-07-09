package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Validates seal, QR/barcode, image, and attachment-list reference nodes (P18-T05).
 */
@Service
public class ReferenceNodeService {

    public static final String MESSAGE_KEY_SEAL_OUTSIDE_AUTHORIZED_AREA =
            "generation.warning.fidelity.sealOutsideAuthorizedArea";
    public static final String MESSAGE_KEY_SEAL_SCALING_NOT_ALLOWED =
            "generation.warning.fidelity.sealScalingNotAllowed";
    public static final String MESSAGE_KEY_IMAGE_SCALING = "generation.warning.fidelity.imageScalingAdjusted";
    public static final String MESSAGE_KEY_MISSING_REFERENCE = "generation.warning.fidelity.missingReferenceKey";

    private final ObjectMapper objectMapper;

    public ReferenceNodeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ReferenceNodeValidationResult validateStructuredContent(String structuredContentJson) {
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>();
        List<StructuredContentFidelityIssue> warnings = new ArrayList<>();
        List<AttachmentListReferenceModel> attachmentLists = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            if (root.isObject() && root.get("nodes").isArray()) {
                walkNodes(root.get("nodes"), "nodes", blockers, warnings, attachmentLists);
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
                validateSealRef(node, nodeLocation, blockers);
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
                walkNodes(children, nodeLocation + ".children", blockers, warnings, attachmentLists);
            }
        }
    }

    private void validateSealRef(
            JsonNode node,
            String location,
            List<StructuredContentFidelityIssue> blockers
    ) {
        validateReferenceKey(node, location, "referenceKey", blockers);
        JsonNode placement = node.get("placement");
        if (placement != null && placement.isObject() && !placement.path("withinAuthorizedArea").asBoolean(true)) {
            blockers.add(issue(
                    StructuredContentFidelitySeverity.BLOCKER,
                    FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA,
                    MESSAGE_KEY_SEAL_OUTSIDE_AUTHORIZED_AREA,
                    location,
                    "Seal placement at " + location + " is outside the authorized area.",
                    "Reposition the seal within the authorized seal zone."
            ));
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
}
