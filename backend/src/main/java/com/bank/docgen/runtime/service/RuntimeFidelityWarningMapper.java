package com.bank.docgen.runtime.service;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.FidelityWarning;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps engine warning codes to OpenAPI-shaped {@link FidelityWarning} objects for JSON
 * batch/task responses. SYNC_STREAM continues to expose codes only via response headers.
 */
@Component
public class RuntimeFidelityWarningMapper {

    private static final String MESSAGE_KEY_PREFIX = "generation.warning.fidelity.";
    private static final Map<String, CatalogEntry> CATALOG = buildCatalog();

    private final MessageResolver messageResolver;

    public RuntimeFidelityWarningMapper(MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public List<FidelityWarning> toWarnings(List<String> warningCodes) {
        if (warningCodes == null || warningCodes.isEmpty()) {
            return List.of();
        }
        List<FidelityWarning> warnings = new ArrayList<>(warningCodes.size());
        for (String code : warningCodes) {
            warnings.add(toWarning(code));
        }
        return List.copyOf(warnings);
    }

    public List<String> toWarningCodes(List<FidelityWarning> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return List.of();
        }
        return warnings.stream().map(FidelityWarning::warningCode).toList();
    }

    private FidelityWarning toWarning(String warningCode) {
        String normalized = warningCode == null ? "" : warningCode.trim();
        CatalogEntry entry = CATALOG.getOrDefault(normalized, defaultEntry(normalized));
        String message = messageResolver.resolveOrDefault(entry.messageKey(), entry.defaultMessage());
        return new FidelityWarning(
                normalized,
                entry.messageKey(),
                message,
                entry.locationSummary(),
                entry.detectedSummary(),
                entry.recommendation(),
                true
        );
    }

    private static CatalogEntry defaultEntry(String warningCode) {
        String messageKey = MESSAGE_KEY_PREFIX + toCamelCase(warningCode);
        return new CatalogEntry(
                messageKey,
                "A low-risk fidelity warning was detected during generation.",
                "artifact=generated-document",
                "warningCode=" + warningCode,
                "Review the affected section if exact layout is business critical."
        );
    }

    private static String toCamelCase(String snake) {
        if (snake == null || snake.isBlank()) {
            return "unknown";
        }
        String[] parts = snake.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }

    private static Map<String, CatalogEntry> buildCatalog() {
        Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
        put(catalog, "OPTIONAL_CONTENT_EMPTY",
                "optionalContentEmpty",
                "Optional content was empty and omitted from the generated document.",
                "region=optional-content",
                "An optional content block had no resolvable value.",
                "Confirm optional content rules if the section should appear.");
        put(catalog, "LOW_RISK_PAGINATION_DIFFERENCE",
                "lowRiskPaginationDifference",
                "A low-risk pagination difference was detected.",
                "region=pagination",
                "Page breaks differ slightly from the preview baseline without changing meaning.",
                "Review pagination only if exact page boundaries are business critical.");
        put(catalog, "LOW_RISK_TABLE_PAGE_BREAK",
                "lowRiskTablePageBreak",
                "A low-risk table page break was applied during rendering.",
                "region=table",
                "A table crossed a page boundary using an approved break strategy.",
                "Review the table layout if split rows are business critical.");
        put(catalog, "CONTROLLED_STYLE_FALLBACK",
                "controlledStyleFallback",
                "Generated output used an approved controlled style fallback.",
                "region=style-catalog",
                "The fallback style is approved and does not change document meaning.",
                "Review the affected section if exact typography is business critical.");
        put(catalog, "IMAGE_SCALING_ADJUSTED",
                "imageScalingAdjusted",
                "Image scaling may be adjusted during rendering.",
                "region=image",
                "Image dimensions were adjusted within approved bounds.",
                "Review image placement if exact size is business critical.");
        put(catalog, "MASTER_STYLE_FALLBACK",
                "masterStyleFallback",
                "Master document defaults were missing; system baseline typography was applied.",
                "region=master-styles",
                "Master style defaults were unavailable; baseline typography was applied.",
                "Restore master style defaults before republishing if branding must match.");
        put(catalog, "PDF_PAGE_NUMBER_STAMP_FAILED",
                "pdfPageNumberStampFailed",
                "PDF page-number stamping could not be completed; the document was still generated.",
                "region=pdf-page-number",
                "Page-number stamp post-processing failed without blocking generation.",
                "Retry generation or inspect the PDF page-number profile if page numbers are required.");
        put(catalog, "PDF_PAGE_NUMBER_STAMP_SKIPPED_FOR_PDFA",
                "pdfPageNumberStampSkippedForPdfa",
                "PDF page-number stamping was skipped to preserve PDF/A-2b archival conformance.",
                "region=pdf-page-number",
                "Post-conversion PDFBox stamp was skipped because pdfArchivalProfile is PDF_A_2B.",
                "Rely on DOCX page-number fields before LibreOffice conversion when archival PDF is required.");
        put(catalog, "DOCX_PERMISSIONS_NOT_APPLIED",
                "docxPermissionsNotApplied",
                "Requested encryption permissions were not applied because permissions map only to PDF output.",
                "region=encryption-permissions",
                "Non-empty encryption.permissions were requested for DOCX output.",
                "Omit permissions for DOCX, or use PDF output when access-permission bits are required.");
        put(catalog, "PARTIAL_TABLE_LAYOUT_ADJUSTMENT",
                "partialTableLayoutAdjustment",
                "A partial table layout adjustment was applied during rendering.",
                "region=table",
                "Table layout was adjusted within approved low-risk bounds.",
                "Review the table if exact column widths are business critical.");
        put(catalog, "UNRESOLVED_VARIABLE",
                "unresolvedVariable",
                "A variable reference is not declared in the template schema.",
                "region=variables",
                "An undeclared variable reference was detected during generation.",
                "Declare the variable in the template schema before republishing.");
        put(catalog, "INVALID_CONDITION_EXPRESSION",
                "invalidConditionExpression",
                "The condition expression syntax is invalid.",
                "region=condition",
                "A condition expression failed syntax validation.",
                "Correct the condition expression before republishing.");
        put(catalog, "MISSING_ANCHOR_CONTENT",
                "missingAnchorContent",
                "Required anchor content was missing during generation.",
                "region=anchor",
                "An expected content anchor had no bound content.",
                "Bind content to the anchor before republishing.");
        put(catalog, "UNSUPPORTED_NODE",
                "unsupportedNode",
                "An unsupported structured content node was detected.",
                "region=structured-content",
                "A node type outside the v1 matrix was encountered.",
                "Replace the unsupported node with an approved node type.");
        put(catalog, "MISSING_STYLE_REFERENCE",
                "missingStyleReference",
                "A style reference is missing or not in the approved master style catalog.",
                "region=style-catalog",
                "A style reference could not be resolved from the master catalog.",
                "Select an approved master style before republishing.");
        put(catalog, "INAPPLICABLE_STYLE",
                "inapplicableStyle",
                "The selected style does not apply to this node type.",
                "region=style-catalog",
                "A style was applied to an incompatible node type.",
                "Choose a style applicable to the node type.");
        put(catalog, "DIRECT_FORMAT_OUT_OF_WHITELIST",
                "directFormatOutOfWhitelist",
                "Direct formatting uses a field outside the v1 whitelist.",
                "region=direct-format",
                "Direct formatting included a non-whitelisted field.",
                "Remove non-whitelisted direct formatting before republishing.");
        put(catalog, "DIRECT_FORMAT_GLOBAL_LAYOUT",
                "directFormatGlobalLayout",
                "Direct formatting must not modify global document layout.",
                "region=direct-format",
                "Direct formatting attempted a global layout change.",
                "Remove global layout direct formatting before republishing.");
        put(catalog, "NESTED_TABLE",
                "nestedTable",
                "Nested tables are not supported in v1 table components.",
                "region=table",
                "A nested table structure was detected.",
                "Flatten nested tables before republishing.");
        put(catalog, "UNRELIABLE_TABLE_LAYOUT",
                "unreliableTableLayout",
                "The table layout cannot render reliably across pages.",
                "region=table",
                "Table layout may not render consistently across pages.",
                "Simplify the table layout before republishing.");
        put(catalog, "INVALID_TABLE_COMPONENT",
                "invalidTableComponent",
                "The table component definition is invalid.",
                "region=table",
                "A table component failed schema validation.",
                "Correct the table component definition before republishing.");
        put(catalog, "SEAL_OUTSIDE_AUTHORIZED_AREA",
                "sealOutsideAuthorizedArea",
                "Seal placement is outside the authorized area.",
                "region=seal",
                "A seal reference was placed outside the authorized region.",
                "Move the seal into the authorized area before republishing.");
        put(catalog, "SEAL_SCALING_NOT_ALLOWED",
                "sealScalingNotAllowed",
                "Seal references must not use image scaling.",
                "region=seal",
                "Seal scaling was requested but is not allowed.",
                "Remove seal scaling before republishing.");
        put(catalog, "MISSING_REFERENCE_KEY",
                "missingReferenceKey",
                "A reference node is missing its reference key.",
                "region=reference",
                "A reference node lacked a required key.",
                "Provide the reference key before republishing.");
        put(catalog, "DUPLICATE_NUMBER",
                "duplicateNumber",
                "A duplicate clause or section number was detected.",
                "region=numbering",
                "Duplicate numbering was detected in structured content.",
                "Resolve duplicate numbers before republishing.");
        put(catalog, "BROKEN_NUMBER_CROSS_REFERENCE",
                "brokenNumberCrossReference",
                "A numbering cross-reference target does not exist.",
                "region=numbering",
                "A cross-reference pointed to a missing number target.",
                "Repair the cross-reference before republishing.");
        return Map.copyOf(catalog);
    }

    private static void put(
            Map<String, CatalogEntry> catalog,
            String warningCode,
            String camelSuffix,
            String defaultMessage,
            String locationSummary,
            String detectedSummary,
            String recommendation
    ) {
        catalog.put(warningCode, new CatalogEntry(
                MESSAGE_KEY_PREFIX + camelSuffix,
                defaultMessage,
                locationSummary,
                detectedSummary,
                recommendation
        ));
    }

    private record CatalogEntry(
            String messageKey,
            String defaultMessage,
            String locationSummary,
            String detectedSummary,
            String recommendation
    ) {
    }
}
