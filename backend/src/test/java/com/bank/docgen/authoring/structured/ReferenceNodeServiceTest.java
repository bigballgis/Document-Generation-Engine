package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ReferenceNodeServiceTest {

    private final ReferenceNodeService service = new ReferenceNodeService(new ObjectMapper());

    @Test
    void sealOutsideAuthorizedArea_isBlocker() {
        String json = """
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

        ReferenceNodeValidationResult result = service.validateStructuredContent(json);

        assertThat(result.fidelity().blockers()).hasSize(1);
        assertThat(result.fidelity().blockers().getFirst().code())
                .isEqualTo(FidelityWarningCode.SEAL_OUTSIDE_AUTHORIZED_AREA);
        assertThat(result.fidelity().blockers().getFirst().messageKey())
                .isEqualTo(ReferenceNodeService.MESSAGE_KEY_SEAL_OUTSIDE_AUTHORIZED_AREA);
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
}
