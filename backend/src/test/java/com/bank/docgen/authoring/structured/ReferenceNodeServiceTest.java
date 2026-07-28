package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * BDD: docs/behavior/ibl-b5-seal-geometry.md (BDD-IBL-B5-001…010).
 */
class ReferenceNodeServiceTest {

    private static final String IN_AREA_FIXTURE = """
            {
              "authorizedSealAreas": [
                {
                  "id": "SEAL_ZONE_A",
                  "pageIndex": 0,
                  "xPt": 400,
                  "yPt": 600,
                  "widthPt": 120,
                  "heightPt": 120
                }
              ],
              "nodes": [
                {
                  "type": "sealRef",
                  "referenceKey": "SEAL-1",
                  "placement": {
                    "authorizedAreaId": "SEAL_ZONE_A",
                    "sealBox": { "xPt": 420, "yPt": 620, "widthPt": 48, "heightPt": 48 }
                  }
                }
              ]
            }
            """;

    private static final String OUT_OF_AREA_FIXTURE = """
            {
              "authorizedSealAreas": [
                {
                  "id": "SEAL_ZONE_A",
                  "pageIndex": 0,
                  "xPt": 400,
                  "yPt": 600,
                  "widthPt": 120,
                  "heightPt": 120
                }
              ],
              "nodes": [
                {
                  "type": "sealRef",
                  "referenceKey": "SEAL-1",
                  "placement": {
                    "authorizedAreaId": "SEAL_ZONE_A",
                    "sealBox": { "xPt": 500, "yPt": 620, "widthPt": 48, "heightPt": 48 }
                  }
                }
              ]
            }
            """;

    private final ReferenceNodeService service = new ReferenceNodeService(new ObjectMapper());

    @Test
    void bddIblB5_001_geometryModelIsDocumentedAndPinned() throws Exception {
        Path bdd = resolveRepoFile("docs/behavior/ibl-b5-seal-geometry.md");
        String text = Files.readString(bdd);
        assertThat(text).contains("geometry_units: pt");
        assertThat(text).contains("geometry_shape: axis_aligned_rect_aabb");
        assertThat(text).contains("coordinate_origin: page_media_top_left_x_right_y_down");
        assertThat(text).contains("containment: closed_full_footprint");
        assertThat(text).contains("placement_boolean_authority: deprecated_non_authoritative");
        assertThat(text).contains("authorizedSealAreas");
        assertThat(SealGeometryRules.DEFAULT_SEAL_WIDTH_PT).isEqualTo(48.0d);
        assertThat(SealGeometryRules.DEFAULT_SEAL_HEIGHT_PT).isEqualTo(48.0d);
        assertThat(SealGeometryRules.UNITS).isEqualTo("pt");
    }

    @Test
    void bddIblB5_002_inAreaFixture_passesGeometryGate() {
        ReferenceNodeValidationResult result = service.validateStructuredContent(IN_AREA_FIXTURE);

        assertThat(blockerCodes(result)).doesNotContain(
                FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA,
                FidelityWarningCode.SEAL_AUTHORIZED_AREA_UNKNOWN,
                FidelityWarningCode.SEAL_AUTHORIZED_AREA_INVALID,
                FidelityWarningCode.SEAL_PLACEMENT_GEOMETRY_INVALID
        );
        assertThat(result.fidelity().blockers()).isEmpty();
    }

    @Test
    void bddIblB5_003_outOfAreaFixture_isWarningOnly_crchW07() {
        ReferenceNodeValidationResult result = service.validateStructuredContent(OUT_OF_AREA_FIXTURE);

        assertThat(result.fidelity().blockers()).isEmpty();
        assertThat(warningCodes(result)).contains(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
        assertThat(result.fidelity().warnings().stream()
                .filter(issue -> issue.code() == FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA)
                .findFirst().orElseThrow().messageKey())
                .isEqualTo(ReferenceNodeService.MESSAGE_KEY_SEAL_OUTSIDE_AUTHORIZED_AREA);
    }

    @Test
    void bddIblB5_004_booleanTrue_cannotMaskOutOfArea() {
        String json = """
                {
                  "authorizedSealAreas": [
                    {
                      "id": "SEAL_ZONE_A",
                      "pageIndex": 0,
                      "xPt": 400,
                      "yPt": 600,
                      "widthPt": 120,
                      "heightPt": 120
                    }
                  ],
                  "nodes": [
                    {
                      "type": "sealRef",
                      "referenceKey": "SEAL-1",
                      "placement": {
                        "authorizedAreaId": "SEAL_ZONE_A",
                        "withinAuthorizedArea": true,
                        "sealBox": { "xPt": 500, "yPt": 620, "widthPt": 48, "heightPt": 48 }
                      }
                    }
                  ]
                }
                """;

        ReferenceNodeValidationResult result = service.validateStructuredContent(json);

        assertThat(blockerCodes(result)).doesNotContain(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
        assertThat(warningCodes(result)).contains(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
    }

    @Test
    void bddIblB5_005_booleanFalse_isNotAuthoritativeWhenInArea() {
        String json = """
                {
                  "authorizedSealAreas": [
                    {
                      "id": "SEAL_ZONE_A",
                      "pageIndex": 0,
                      "xPt": 400,
                      "yPt": 600,
                      "widthPt": 120,
                      "heightPt": 120
                    }
                  ],
                  "nodes": [
                    {
                      "type": "sealRef",
                      "referenceKey": "SEAL-1",
                      "placement": {
                        "authorizedAreaId": "SEAL_ZONE_A",
                        "withinAuthorizedArea": false,
                        "sealBox": { "xPt": 420, "yPt": 620, "widthPt": 48, "heightPt": 48 }
                      }
                    }
                  ]
                }
                """;

        ReferenceNodeValidationResult result = service.validateStructuredContent(json);

        assertThat(blockerCodes(result)).doesNotContain(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
        assertThat(result.fidelity().blockers()).isEmpty();
    }

    @Test
    void bddIblB5_006_unknownAuthorizedAreaId_isFailClosed() {
        String json = """
                {
                  "authorizedSealAreas": [
                    {
                      "id": "SEAL_ZONE_A",
                      "pageIndex": 0,
                      "xPt": 400,
                      "yPt": 600,
                      "widthPt": 120,
                      "heightPt": 120
                    }
                  ],
                  "nodes": [
                    {
                      "type": "sealRef",
                      "referenceKey": "SEAL-1",
                      "placement": {
                        "authorizedAreaId": "MISSING_ZONE",
                        "sealBox": { "xPt": 420, "yPt": 620, "widthPt": 48, "heightPt": 48 }
                      }
                    }
                  ]
                }
                """;

        ReferenceNodeValidationResult result = service.validateStructuredContent(json);

        assertThat(blockerCodes(result)).doesNotContain(FidelityWarningCode.SEAL_AUTHORIZED_AREA_UNKNOWN);
        assertThat(warningCodes(result)).contains(FidelityWarningCode.SEAL_AUTHORIZED_AREA_UNKNOWN);
        assertThat(warningCodes(result)).doesNotContain(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
    }

    @Test
    void bddIblB5_007_invalidGeometry_isFailClosed() {
        String missingXy = """
                {
                  "authorizedSealAreas": [
                    {
                      "id": "SEAL_ZONE_A",
                      "pageIndex": 0,
                      "xPt": 400,
                      "yPt": 600,
                      "widthPt": 120,
                      "heightPt": 120
                    }
                  ],
                  "nodes": [
                    {
                      "type": "sealRef",
                      "referenceKey": "SEAL-1",
                      "placement": {
                        "authorizedAreaId": "SEAL_ZONE_A",
                        "withinAuthorizedArea": false,
                        "sealBox": { "widthPt": 48, "heightPt": 48 }
                      }
                    }
                  ]
                }
                """;
        String nonPositiveAreaHeight = """
                {
                  "authorizedSealAreas": [
                    {
                      "id": "SEAL_ZONE_A",
                      "pageIndex": 0,
                      "xPt": 400,
                      "yPt": 600,
                      "widthPt": 120,
                      "heightPt": 0
                    }
                  ],
                  "nodes": [
                    {
                      "type": "sealRef",
                      "referenceKey": "SEAL-1",
                      "placement": {
                        "authorizedAreaId": "SEAL_ZONE_A",
                        "sealBox": { "xPt": 420, "yPt": 620, "widthPt": 48, "heightPt": 48 }
                      }
                    }
                  ]
                }
                """;
        String legacyBooleanOnlyPlacement = """
                {
                  "nodes": [
                    {
                      "type": "sealRef",
                      "referenceKey": "SEAL-1",
                      "placement": {
                        "authorizedAreaId": "SEAL_ZONE_A",
                        "withinAuthorizedArea": false
                      }
                    }
                  ]
                }
                """;

        assertThat(warningCodes(service.validateStructuredContent(missingXy)))
                .contains(FidelityWarningCode.SEAL_PLACEMENT_GEOMETRY_INVALID);
        assertThat(blockerCodes(service.validateStructuredContent(missingXy)))
                .doesNotContain(FidelityWarningCode.SEAL_PLACEMENT_GEOMETRY_INVALID);
        // Catalog-invalid area entry remains a catalog integrity blocker.
        assertThat(blockerCodes(service.validateStructuredContent(nonPositiveAreaHeight)))
                .contains(FidelityWarningCode.SEAL_AUTHORIZED_AREA_INVALID);
        assertThat(warningCodes(service.validateStructuredContent(legacyBooleanOnlyPlacement)))
                .contains(FidelityWarningCode.SEAL_PLACEMENT_GEOMETRY_INVALID);
        assertThat(blockerCodes(service.validateStructuredContent(legacyBooleanOnlyPlacement)))
                .doesNotContain(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
    }

    @Test
    void bddIblB5_008_missingPlacement_skipsGeometryGate() {
        String json = """
                {
                  "nodes": [
                    { "type": "sealRef", "referenceKey": "SEAL-1" }
                  ]
                }
                """;

        ReferenceNodeValidationResult result = service.validateStructuredContent(json);

        assertThat(blockerCodes(result)).doesNotContain(
                FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA,
                FidelityWarningCode.SEAL_AUTHORIZED_AREA_UNKNOWN,
                FidelityWarningCode.SEAL_AUTHORIZED_AREA_INVALID,
                FidelityWarningCode.SEAL_PLACEMENT_GEOMETRY_INVALID
        );
        assertThat(result.fidelity().blockers()).isEmpty();
    }

    @Test
    void bddIblB5_009_crossPageIndex_isOutsideAuthorizedArea() {
        String json = """
                {
                  "authorizedSealAreas": [
                    {
                      "id": "SEAL_ZONE_A",
                      "pageIndex": 0,
                      "xPt": 400,
                      "yPt": 600,
                      "widthPt": 120,
                      "heightPt": 120
                    }
                  ],
                  "nodes": [
                    {
                      "type": "sealRef",
                      "referenceKey": "SEAL-1",
                      "placement": {
                        "authorizedAreaId": "SEAL_ZONE_A",
                        "sealBox": {
                          "pageIndex": 1,
                          "xPt": 420,
                          "yPt": 620,
                          "widthPt": 48,
                          "heightPt": 48
                        }
                      }
                    }
                  ]
                }
                """;

        ReferenceNodeValidationResult result = service.validateStructuredContent(json);

        assertThat(blockerCodes(result)).doesNotContain(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
        assertThat(warningCodes(result)).contains(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
    }

    @Test
    void bddIblB5_010_outOfArea_isWarningNotPublishBlocker_crchW07() {
        ReferenceNodeValidationResult result = service.validateStructuredContent(OUT_OF_AREA_FIXTURE);

        assertThat(result.fidelity().hasBlockers()).isFalse();
        assertThat(result.fidelity().warnings())
                .filteredOn(issue -> issue.code() == FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA)
                .allMatch(issue -> issue.severity() == StructuredContentFidelitySeverity.WARNING);
        assertThat(warningCodes(result)).contains(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
    }

    @Test
    void edgeTouch_isAllowedAsFullyContained() {
        String json = """
                {
                  "authorizedSealAreas": [
                    {
                      "id": "SEAL_ZONE_A",
                      "pageIndex": 0,
                      "xPt": 400,
                      "yPt": 600,
                      "widthPt": 120,
                      "heightPt": 120
                    }
                  ],
                  "nodes": [
                    {
                      "type": "sealRef",
                      "referenceKey": "SEAL-1",
                      "placement": {
                        "authorizedAreaId": "SEAL_ZONE_A",
                        "sealBox": { "xPt": 472, "yPt": 672, "widthPt": 48, "heightPt": 48 }
                      }
                    }
                  ]
                }
                """;

        assertThat(service.validateStructuredContent(json).fidelity().blockers()).isEmpty();
    }

    @Test
    void defaultSealSize_appliesWhenWidthHeightOmitted() {
        String json = """
                {
                  "authorizedSealAreas": [
                    {
                      "id": "SEAL_ZONE_A",
                      "pageIndex": 0,
                      "xPt": 400,
                      "yPt": 600,
                      "widthPt": 120,
                      "heightPt": 120
                    }
                  ],
                  "nodes": [
                    {
                      "type": "sealRef",
                      "referenceKey": "SEAL-1",
                      "placement": {
                        "authorizedAreaId": "SEAL_ZONE_A",
                        "sealBox": { "xPt": 420, "yPt": 620 }
                      }
                    }
                  ]
                }
                """;

        assertThat(service.validateStructuredContent(json).fidelity().blockers()).isEmpty();
    }

    @Test
    void imageScaling_isWarning_butSealScaling_isBlocker() {
        String json = """
                {
                  "nodes": [
                    { "type": "imageRef", "imageRef": "IMG-1", "applyScaling": true },
                    { "type": "sealRef", "referenceKey": "SEAL-1", "applyScaling": true },
                    { "type": "qrBarcodeRef", "referenceKey": "QR-1", "applyScaling": true }
                  ]
                }
                """;

        ReferenceNodeValidationResult result = service.validateStructuredContent(json);

        assertThat(result.fidelity().warnings()).hasSize(1);
        assertThat(result.fidelity().warnings().getFirst().code()).isEqualTo(FidelityWarningCode.IMAGE_SCALING_ADJUSTED);
        assertThat(result.fidelity().blockers()).hasSize(1);
        assertThat(result.fidelity().blockers().getFirst().code()).isEqualTo(FidelityWarningCode.SEAL_SCALING_NOT_ALLOWED);
    }

    @Test
    void attachmentListRef_renders() {
        String json = """
                {
                  "nodes": [
                    { "type": "attachmentListRef", "referenceKey": "ATT-1" }
                  ]
                }
                """;

        ReferenceNodeValidationResult result = service.validateStructuredContent(json);

        assertThat(result.fidelity().blockers()).isEmpty();
        assertThat(result.fidelity().warnings()).isEmpty();
        assertThat(result.attachmentLists()).hasSize(1);
        assertThat(result.attachmentLists().getFirst().referenceKey()).isEqualTo("ATT-1");
        assertThat(result.attachmentLists().getFirst().location()).isEqualTo("nodes[0]");
    }

    private static Set<FidelityWarningCode> warningCodes(ReferenceNodeValidationResult result) {
        return result.fidelity().warnings().stream()
                .map(StructuredContentFidelityIssue::code)
                .collect(Collectors.toSet());
    }

    private static Set<FidelityWarningCode> blockerCodes(ReferenceNodeValidationResult result) {
        return result.fidelity().blockers().stream()
                .map(StructuredContentFidelityIssue::code)
                .collect(Collectors.toSet());
    }

    private static Path resolveRepoFile(String relativeFromRepoRoot) {
        Path fromModule = Path.of("..").resolve(relativeFromRepoRoot).normalize().toAbsolutePath();
        if (Files.exists(fromModule)) {
            return fromModule;
        }
        Path fromRepoRoot = Path.of(relativeFromRepoRoot).normalize().toAbsolutePath();
        if (Files.exists(fromRepoRoot)) {
            return fromRepoRoot;
        }
        throw new IllegalStateException("Missing repo file: " + relativeFromRepoRoot);
    }
}
