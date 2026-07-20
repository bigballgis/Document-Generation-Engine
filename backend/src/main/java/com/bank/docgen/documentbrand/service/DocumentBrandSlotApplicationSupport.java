package com.bank.docgen.documentbrand.service;

import com.bank.docgen.documentbrand.domain.ResolvedDocumentBrand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies resolved DocumentBrand assets to structured-content brand slots (ADR-0065 E4-C9).
 *
 * <p>Slot markers use {@code documentBrandSlot}: {@code LOGO}, {@code DEFAULT_SEAL},
 * {@code LETTERHEAD_LEGAL_NAME}. Explicit seal {@code referenceKey} wins over brand default.
 */
public final class DocumentBrandSlotApplicationSupport {

    public static final String WARNING_SLOTS_ABSENT = "DOCUMENT_BRAND_SLOTS_ABSENT";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SLOT_LOGO = "LOGO";
    private static final String SLOT_DEFAULT_SEAL = "DEFAULT_SEAL";
    private static final String SLOT_LETTERHEAD = "LETTERHEAD_LEGAL_NAME";

    private DocumentBrandSlotApplicationSupport() {
    }

    public static Applied apply(Map<String, String> bindingJson, ResolvedDocumentBrand brand) {
        if (brand == null) {
            return new Applied(bindingJson == null ? Map.of() : Map.copyOf(bindingJson), List.of(), false);
        }
        Map<String, String> source = bindingJson == null ? Map.of() : bindingJson;
        Map<String, String> out = new LinkedHashMap<>();
        boolean anySlot = false;
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String json = entry.getValue();
            if (json == null || json.isBlank()) {
                out.put(entry.getKey(), json);
                continue;
            }
            try {
                JsonNode root = MAPPER.readTree(json);
                ApplyWalk walk = new ApplyWalk(brand);
                walk.walk(root);
                anySlot = anySlot || walk.slotsSeen;
                out.put(entry.getKey(), MAPPER.writeValueAsString(root));
            } catch (JsonProcessingException ex) {
                out.put(entry.getKey(), json);
            }
        }
        List<String> warnings = new ArrayList<>();
        // E4-C9: missing slots → non-blocking fidelity warning. Skip for PLATFORM_DEFAULT
        // omit/fallback path so every pre-E4 template without brand slots does not suddenly
        // block submit-for-approval until authors acknowledge a seed-brand placeholder.
        if (!anySlot
                && hasApplyableAssets(brand)
                && !DocumentBrandCodes.PLATFORM_DEFAULT.equals(brand.documentBrandCode())) {
            warnings.add(WARNING_SLOTS_ABSENT);
        }
        return new Applied(Map.copyOf(out), List.copyOf(warnings), anySlot);
    }

    private static boolean hasApplyableAssets(ResolvedDocumentBrand brand) {
        return isPresent(brand.logoObjectRef())
                || isPresent(brand.defaultSealObjectRef())
                || isPresent(brand.letterheadLegalName());
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    public record Applied(
            Map<String, String> bindingJson,
            List<String> fidelityWarningCodes,
            boolean slotsApplied
    ) {
    }

    private static final class ApplyWalk {
        private final ResolvedDocumentBrand brand;
        private boolean slotsSeen;

        private ApplyWalk(ResolvedDocumentBrand brand) {
            this.brand = brand;
        }

        private void walk(JsonNode node) {
            if (node == null || node.isNull()) {
                return;
            }
            if (node.isObject()) {
                applyObject((ObjectNode) node);
                var fields = node.fields();
                while (fields.hasNext()) {
                    walk(fields.next().getValue());
                }
                return;
            }
            if (node.isArray()) {
                for (JsonNode child : node) {
                    walk(child);
                }
            }
        }

        private void applyObject(ObjectNode node) {
            JsonNode slotNode = node.get("documentBrandSlot");
            if (slotNode == null || !slotNode.isTextual()) {
                return;
            }
            String slot = slotNode.asText("").trim();
            if (slot.isEmpty()) {
                return;
            }
            slotsSeen = true;
            if (SLOT_LOGO.equals(slot) && brand.logoObjectRef() != null && !brand.logoObjectRef().isBlank()) {
                node.put("imageRef", brand.logoObjectRef());
                return;
            }
            if (SLOT_DEFAULT_SEAL.equals(slot)) {
                String existing = firstText(node, "referenceKey", "sealRef");
                if ((existing == null || existing.isBlank())
                        && brand.defaultSealObjectRef() != null
                        && !brand.defaultSealObjectRef().isBlank()) {
                    node.put("referenceKey", brand.defaultSealObjectRef());
                }
                return;
            }
            if (SLOT_LETTERHEAD.equals(slot)
                    && brand.letterheadLegalName() != null
                    && !brand.letterheadLegalName().isBlank()) {
                node.put("text", brand.letterheadLegalName());
            }
        }

        private static String firstText(ObjectNode node, String... fields) {
            for (String field : fields) {
                JsonNode value = node.get(field);
                if (value != null && value.isTextual()) {
                    return value.asText();
                }
            }
            return null;
        }
    }
}
