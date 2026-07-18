package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses root-level {@code authorizedSealAreas[]} for IBL-B5 geometry validation.
 */
final class AuthorizedSealAreaCatalog {

    private final Map<String, SealGeometryRules.SealAxisAlignedBox> byId;
    private final Set<String> duplicateIds;
    private final Set<String> invalidIds;
    private final List<StructuredContentFidelityIssue> catalogBlockers;

    private AuthorizedSealAreaCatalog(
            Map<String, SealGeometryRules.SealAxisAlignedBox> byId,
            Set<String> duplicateIds,
            Set<String> invalidIds,
            List<StructuredContentFidelityIssue> catalogBlockers
    ) {
        this.byId = Map.copyOf(byId);
        this.duplicateIds = Set.copyOf(duplicateIds);
        this.invalidIds = Set.copyOf(invalidIds);
        this.catalogBlockers = List.copyOf(catalogBlockers);
    }

    static AuthorizedSealAreaCatalog parse(
            JsonNode root,
            ReferenceNodeService.IssueFactory issues
    ) {
        Map<String, SealGeometryRules.SealAxisAlignedBox> byId = new HashMap<>();
        Set<String> duplicateIds = new HashSet<>();
        Set<String> invalidIds = new HashSet<>();
        List<StructuredContentFidelityIssue> catalogBlockers = new ArrayList<>();

        JsonNode areas = root.get("authorizedSealAreas");
        if (areas == null || areas.isNull()) {
            return new AuthorizedSealAreaCatalog(byId, duplicateIds, invalidIds, catalogBlockers);
        }
        if (!areas.isArray()) {
            catalogBlockers.add(issues.create(
                    StructuredContentFidelitySeverity.BLOCKER,
                    FidelityWarningCode.SEAL_AUTHORIZED_AREA_INVALID,
                    ReferenceNodeService.MESSAGE_KEY_SEAL_AUTHORIZED_AREA_INVALID,
                    "authorizedSealAreas",
                    "authorizedSealAreas must be an array of axis-aligned seal zones.",
                    "Provide authorizedSealAreas as an array of valid area rectangles."
            ));
            return new AuthorizedSealAreaCatalog(byId, duplicateIds, invalidIds, catalogBlockers);
        }

        Set<String> seen = new HashSet<>();
        for (int index = 0; index < areas.size(); index++) {
            JsonNode areaNode = areas.get(index);
            String location = "authorizedSealAreas[" + index + "]";
            if (areaNode == null || !areaNode.isObject()) {
                catalogBlockers.add(issues.create(
                        StructuredContentFidelitySeverity.BLOCKER,
                        FidelityWarningCode.SEAL_AUTHORIZED_AREA_INVALID,
                        ReferenceNodeService.MESSAGE_KEY_SEAL_AUTHORIZED_AREA_INVALID,
                        location,
                        "Authorized seal area at " + location + " is not a valid object.",
                        "Correct the authorized seal area definition."
                ));
                continue;
            }
            String id = areaNode.path("id").asText("").trim();
            if (id.isEmpty()) {
                catalogBlockers.add(issues.create(
                        StructuredContentFidelitySeverity.BLOCKER,
                        FidelityWarningCode.SEAL_AUTHORIZED_AREA_INVALID,
                        ReferenceNodeService.MESSAGE_KEY_SEAL_AUTHORIZED_AREA_INVALID,
                        location,
                        "Authorized seal area at " + location + " is missing a non-blank id.",
                        "Provide a unique authorized seal area id."
                ));
                continue;
            }
            if (!seen.add(id)) {
                duplicateIds.add(id);
                catalogBlockers.add(issues.create(
                        StructuredContentFidelitySeverity.BLOCKER,
                        FidelityWarningCode.SEAL_AUTHORIZED_AREA_INVALID,
                        ReferenceNodeService.MESSAGE_KEY_SEAL_AUTHORIZED_AREA_INVALID,
                        location,
                        "Duplicate authorized seal area id '" + id + "' at " + location + ".",
                        "Use unique authorizedSealAreas[].id values."
                ));
                byId.remove(id);
                continue;
            }
            Optional<SealGeometryRules.SealAxisAlignedBox> box = parseAreaBox(areaNode);
            if (box.isEmpty()) {
                invalidIds.add(id);
                catalogBlockers.add(issues.create(
                        StructuredContentFidelitySeverity.BLOCKER,
                        FidelityWarningCode.SEAL_AUTHORIZED_AREA_INVALID,
                        ReferenceNodeService.MESSAGE_KEY_SEAL_AUTHORIZED_AREA_INVALID,
                        location,
                        "Authorized seal area geometry is invalid at " + location + ".",
                        "Use pageIndex>=0 and xPt/yPt>=0 with widthPt/heightPt>0 in pt."
                ));
                continue;
            }
            byId.put(id, box.get());
        }
        return new AuthorizedSealAreaCatalog(byId, duplicateIds, invalidIds, catalogBlockers);
    }

    List<StructuredContentFidelityIssue> catalogBlockers() {
        return catalogBlockers;
    }

    AreaLookup lookup(String authorizedAreaId) {
        String id = authorizedAreaId == null ? "" : authorizedAreaId.trim();
        if (id.isEmpty()) {
            return AreaLookup.unknown();
        }
        if (duplicateIds.contains(id) || invalidIds.contains(id)) {
            return AreaLookup.invalid();
        }
        SealGeometryRules.SealAxisAlignedBox area = byId.get(id);
        if (area == null) {
            return AreaLookup.unknown();
        }
        return AreaLookup.found(area);
    }

    private static Optional<SealGeometryRules.SealAxisAlignedBox> parseAreaBox(JsonNode node) {
        OptionalIntValue pageIndex = readNonNegativeInt(node.get("pageIndex"), true);
        OptionalDoubleValue xPt = readNonNegativeFinite(node.get("xPt"), true);
        OptionalDoubleValue yPt = readNonNegativeFinite(node.get("yPt"), true);
        OptionalDoubleValue widthPt = readPositiveFinite(node.get("widthPt"));
        OptionalDoubleValue heightPt = readPositiveFinite(node.get("heightPt"));
        if (!pageIndex.present() || !xPt.present() || !yPt.present()
                || !widthPt.present() || !heightPt.present()) {
            return Optional.empty();
        }
        return Optional.of(new SealGeometryRules.SealAxisAlignedBox(
                pageIndex.value(),
                xPt.value(),
                yPt.value(),
                widthPt.value(),
                heightPt.value()
        ));
    }

    static Optional<SealGeometryRules.SealAxisAlignedBox> parseSealBox(JsonNode sealBox) {
        if (sealBox == null || !sealBox.isObject()) {
            return Optional.empty();
        }
        OptionalIntValue pageIndex = readNonNegativeInt(sealBox.get("pageIndex"), false);
        OptionalDoubleValue xPt = readNonNegativeFinite(sealBox.get("xPt"), true);
        OptionalDoubleValue yPt = readNonNegativeFinite(sealBox.get("yPt"), true);
        if (!pageIndex.present() || !xPt.present() || !yPt.present()) {
            return Optional.empty();
        }
        OptionalDoubleValue widthPt = readPositiveFinite(sealBox.get("widthPt"));
        OptionalDoubleValue heightPt = readPositiveFinite(sealBox.get("heightPt"));
        if (sealBox.has("widthPt") && !widthPt.present()) {
            return Optional.empty();
        }
        if (sealBox.has("heightPt") && !heightPt.present()) {
            return Optional.empty();
        }
        double width = widthPt.present() ? widthPt.value() : SealGeometryRules.DEFAULT_SEAL_WIDTH_PT;
        double height = heightPt.present() ? heightPt.value() : SealGeometryRules.DEFAULT_SEAL_HEIGHT_PT;
        return Optional.of(new SealGeometryRules.SealAxisAlignedBox(
                pageIndex.value(),
                xPt.value(),
                yPt.value(),
                width,
                height
        ));
    }

    private static OptionalIntValue readNonNegativeInt(JsonNode node, boolean required) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return required ? OptionalIntValue.absent() : OptionalIntValue.of(0);
        }
        if (!node.isNumber()) {
            return OptionalIntValue.absent();
        }
        double raw = node.asDouble();
        if (!Double.isFinite(raw) || raw < 0.0d || Math.rint(raw) != raw) {
            return OptionalIntValue.absent();
        }
        return OptionalIntValue.of((int) raw);
    }

    private static OptionalDoubleValue readNonNegativeFinite(JsonNode node, boolean required) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return required ? OptionalDoubleValue.absent() : OptionalDoubleValue.of(0.0d);
        }
        if (!node.isNumber()) {
            return OptionalDoubleValue.absent();
        }
        double value = node.asDouble();
        if (!Double.isFinite(value) || value < 0.0d) {
            return OptionalDoubleValue.absent();
        }
        return OptionalDoubleValue.of(value);
    }

    private static OptionalDoubleValue readPositiveFinite(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode() || !node.isNumber()) {
            return OptionalDoubleValue.absent();
        }
        double value = node.asDouble();
        if (!Double.isFinite(value) || value <= 0.0d) {
            return OptionalDoubleValue.absent();
        }
        return OptionalDoubleValue.of(value);
    }

    enum AreaStatus {
        FOUND,
        UNKNOWN,
        INVALID
    }

    record AreaLookup(AreaStatus status, SealGeometryRules.SealAxisAlignedBox area) {
        static AreaLookup found(SealGeometryRules.SealAxisAlignedBox area) {
            return new AreaLookup(AreaStatus.FOUND, area);
        }

        static AreaLookup unknown() {
            return new AreaLookup(AreaStatus.UNKNOWN, null);
        }

        static AreaLookup invalid() {
            return new AreaLookup(AreaStatus.INVALID, null);
        }
    }

    private record OptionalIntValue(boolean present, int value) {
        static OptionalIntValue absent() {
            return new OptionalIntValue(false, 0);
        }

        static OptionalIntValue of(int value) {
            return new OptionalIntValue(true, value);
        }
    }

    private record OptionalDoubleValue(boolean present, double value) {
        static OptionalDoubleValue absent() {
            return new OptionalDoubleValue(false, 0.0d);
        }

        static OptionalDoubleValue of(double value) {
            return new OptionalDoubleValue(true, value);
        }
    }
}
