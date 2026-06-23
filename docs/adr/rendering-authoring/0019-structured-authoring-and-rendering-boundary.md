---
id: ADR-0019
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - template-authoring
  - rendering
adrNumber: "0019"
topic: rendering-authoring
related:
  - docs/product/authoring-rendering-first-principles-review.md
  - docs/domain/domain-model.md
  - docs/architecture/module-boundaries.md
---

# ADR 0019: Structured Authoring and Rendering Boundary

## Status

Accepted

## Context

The platform is intended to let business users maintain banking financial letter templates without development. The confirmed requirements already include DOCX master documents, anchors, rich text paragraphs, tables, clause fragments, variables, conditions, loops, DOCX/PDF output, preview, release checks, and dynamic APIs.

Further design discussion identified a core product risk: if complex 100-page letters are maintained as one large DOCX master with many fine-grained anchors, template maintenance becomes fragile and costly. Another core risk is DOCX fidelity. If the platform relies on arbitrary HTML/CSS conversion to DOCX, rich text, numbering, tables, pagination, and inline variables may not render reliably enough for banking documents.

The documentation currently has strong API governance, lifecycle, security, and audit decisions. Template authoring, structured rich text, reusable content modules, and DOCX fidelity need a clear decision boundary before implementation design begins.

## Decision

The platform treats the DOCX master document as a layout container, not as the primary carrier of long-form business content.

The master document should primarily contain page layout, headers, footers, global styles, signature/seal areas, appendix areas, and coarse section-level anchors. It should not be the place where large volumes of product clauses, customer-specific content, or fine-grained business rules are maintained.

Template authoring is based on structured composition. A template composes document structure, section order, anchor bindings, template variables, rules, reusable content fragments, table components, and release evidence.

Rich text uses a controlled structured content model rather than arbitrary HTML/CSS as the source of truth. HTML may be used for editing or display, but the persisted contract preserves document semantics such as paragraphs, runs, variables, marks, numbering, conditions, loops, styles, clause references, and table component references.

The platform provides a reusable structured document fragment editing capability. The confirmed v1 boundary covers document-generation content only: template sections, structured document fragments, clause or content modules, and confirmed controlled nodes such as tables, images, QR/barcodes, signatures/seals, and attachment lists. v1 does not extend this editor capability to notification templates, approval comment templates, test comment templates, or other non-document-generation business text scenarios. Future reuse outside document generation requires separate confirmation of the object model, permissions, lifecycle, approval, audit, and output boundary.

DOCX rendering maps the structured content model to supported DOCX constructs, including styles, paragraphs, runs, numbering, tables, images, signatures/seals, appendices, and generated document metadata. Unsupported rich text constructs are detected before publication rather than silently degraded.

DOCX/PDF fidelity is an explicit product validation concern. Preview, test generation, difference summary, and release checklist use blocking and warning levels: severe fidelity issues block publication, while lower-risk fidelity issues are surfaced as warnings.

v1 fidelity warnings do not use a silent-ignore threshold. Every confirmed lower-risk fidelity issue produces a warning. Warnings do not block publication solely by count. If an issue affects document meaning, compliance outcome, signature or seal placement, QR/barcode usability, attachment completeness, variable or reference validity, critical table readability, or clause numbering meaning, it is a blocker instead of a warning.

Confirmed v1 warning codes are `OPTIONAL_CONTENT_EMPTY`, `LOW_RISK_PAGINATION_DIFFERENCE`, `LOW_RISK_TABLE_PAGE_BREAK`, `CONTROLLED_STYLE_FALLBACK`, and `IMAGE_SCALING_ADJUSTED`. Warning presentation includes a stable `warningCode`, `messageKey`, location summary, detected-result summary, and recommendation. It must not expose variable raw values, pasted source text, customer data, complete request bodies, or sensitive generated document content.

Fidelity warnings are visible in test generation records, approval materials, release checklists, release summaries, audit summaries, and successful generation API responses. JSON success responses expose single-document warnings at `result.fidelityWarnings[]`; batch item warnings are exposed at `result.batch.items[].fidelityWarnings[]`. Synchronous file-stream responses expose only warning count and warning-code summary in response headers; full warning detail is retained in the audit summary.

Complex numbering in v1 is limited to controlled multi-level numbering schemes confirmed by the platform or the approved master style catalog. Numbering after condition and loop rendering must be deterministic. Duplicate numbers, skipped numbers, clause-number meaning changes, or unresolved cross references block publication.

Table components in v1 use a structured table-component model. Confirmed support includes column schema, header rows, repeated header rows across pages, loop-generated rows, simple totals or footer rows, and controlled widths. Nested tables, floating tables, absolute-positioned tables, merged cells that cannot render stably across pages, or tables that cannot keep key amounts or clauses readable block publication.

Signature and seal support in v1 is limited to controlled placeholders and signature or seal image assets. Cryptographic electronic signature, electronic seal, and signature verification capabilities are not confirmed for v1. Signature or seal output must stay inside an authorized area, must not overlap or be clipped, and must remain visible in DOCX and PDF output.

PDF conversion acceptance uses a semantic-fidelity baseline. Missing pages, abnormal blank pages, clipped content, lost critical content, invisible signatures or seals, unreadable QR/barcodes, unreadable critical tables, or lost attachment references block publication. Lower-risk visual differences follow the confirmed warning model.

Runtime generation for released templates exposes only successful generation warnings or generation failure errors. API contracts and responses do not expose internal rendering diagnostic plaintext, raw template variable values, customer data, complete request bodies, or complete generated content.

Templates bind master anchors by stable `anchorId` values from an approved master anchor catalog. A released template locks the master document version and anchor catalog version used at publication time. Word bookmark names, document position, or fuzzy matching are not the release contract.

Each `anchorId` can have at most one binding in a template release candidate. The binding owns one ordered structured content tree, and the bound content type must be compatible with the anchor's allowed content types. Binding validation states include at least `VALID`, `MISSING_ANCHOR`, `DUPLICATE_BINDING`, and `INCOMPATIBLE_CONTENT_TYPE`; the latter three are publication blockers.

Rendering configuration uses a release-locked controlled `renderProfile`. The profile covers style mapping, numbering behavior, table pagination, image scaling, PDF conversion, and fidelity policy. API callers cannot supply arbitrary rendering parameters that override the released template configuration; output format, output mode, and dynamic encryption remain constrained by the API contract and API management policy.

Preview output is represented as a traceable preview artifact from the final DOCX/PDF rendering path. The v1 artifact includes at least `previewId`, `testDataSetId`, `variablesHash`, `outputFormat`, `renderProfileVersion`, `status`, `documentId` or preview reference, final generated artifact reference, warning summary, blocker summary, comparison summary, creation time, update time, and non-sensitive summary. Preview artifact statuses are `ACCEPTED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, and `EXPIRED`.

Editor-state fast preview may provide immediate feedback, but it is not authoritative test, approval, or release evidence. Test, approval, and release decisions rely on artifacts produced by the final DOCX/PDF rendering path.

The platform provides a side-by-side comparison view between preview and final DOCX/PDF artifacts. Differences are located by page, anchor, section, or component and become either fidelity warnings or publication blockers according to risk.

Test records, approval summaries, and release summaries lock final generated artifact references according to output policy, preview artifact references, comparison summaries, and non-sensitive summaries. Preview files, final artifacts, and side-by-side comparison views are viewable only through authorized access. Audit summaries must not store raw template variable values, customer data, complete request bodies, or complete generated content.

The first v1 delivery slice uses a conservative structured rich-text node matrix. Supported block-level nodes are section or heading, paragraph, list, condition block, loop block, and table component reference. Supported inline nodes are text run, variable node, emphasis, underline, and line break. Supported reference nodes are clause or content module reference, image reference, QR/barcode reference, signature/seal reference, attachment-list reference, and style reference.

v1 explicitly excludes arbitrary HTML/CSS, scripts, absolute positioning, floating layout, complex columns, online arbitrary editing of headers, footers, page margins, or global layout, and unrecognized Word dirty styles entering a release version.

Word or HTML pasted content must be cleaned and converted into controlled structured nodes before it can enter a template. Unrecognized content must not be persisted as raw source in the release contract; it is handled as a blocking issue or warning depending on risk.

The v1 paste-cleanup conversion target is limited to confirmed controlled structured nodes and controlled structured attributes. Automatically converted paste content can include paragraphs, sections or headings, lists, text runs, emphasis, underline, line breaks, style references, and whitelisted limited direct formatting.

Simple pasted tables can become draft table components. Local pasted images can become draft image references. Complex tables, tables whose structure cannot be recognized reliably, external images, signatures or seals, QR/barcode content, and attachment lists are not silently imported; they must use the corresponding controlled component or node flow.

Scripts, macros, embedded objects, iframes, external resources, absolute positioning, floating layout, complex columns, unrecognized Word styles, unmapped variables, unmapped styles, and unreliably recognized table structures block the paste from entering the template. Unrecognized content is not persisted as raw Word, HTML, CSS, or binary fragments in the release contract.

After paste cleanup, users see a cleanup summary that distinguishes converted content, removed content, warnings, and blockers. Users can cancel the paste or undo back to the pre-paste state. Paste cleanup summaries and blockers are traceable as editing or release-check evidence, but they must not store sensitive plaintext or raw source content from the pasted material.

Confirmed publication blockers include unsupported nodes, missing variable references, missing style references, missing clause or content module references, missing table component references, missing master anchor references, numbering breaks that affect clause meaning, tables that cannot render reliably, abnormal signature or seal placement, and PDF conversion failure.

Style references in v1 come from the approved DOCX master style catalog. Template authors can reference styles from the currently accessible, approved master catalog, and the catalog identifies usable style names, applicable node types, and rendering purposes.

Template authors may also apply limited direct formatting as controlled structured attributes. The v1 direct-formatting whitelist is font family, font size, text color, line spacing, paragraph spacing before and after, first-line indent, left indent, and right indent. Direct formatting must not become arbitrary CSS, arbitrary Word dirty styles, scripts, headers, footers, page margins, paper setup, global layout, complex columns, floating layout, or absolute positioning.

Missing style references, styles outside the accessible approved master style catalog, styles that do not apply to the node type, direct-formatting values outside the whitelist, or style/direct-formatting choices that cannot render reliably are publication blockers.

Approved master style catalog changes do not automatically affect already released templates. Released templates lock the style references and limited direct-formatting values used at publication time. Upgrading a template to a changed master style catalog or adjusted style mapping requires impact analysis and a new test, approval, and release flow.

Reusable clause or content modules have an independent version approval lifecycle in v1. Each module version moves through draft, pending approval, approved, disabled, and deprecated states; rejected versions return to draft. New template release candidates can reference only approved, non-disabled, non-deprecated module versions in the same group or an authorized sharing scope.

Templates lock the exact approved clause or content module versions they reference at release time. A newly approved module version does not automatically affect already released templates. Upgrading a template to a newer module version requires impact analysis and a new test, approval, and release flow.

Clause or content modules reuse the existing role model rather than introducing a dedicated module-maintainer role. Administrators, master designers, and template authors can create, edit drafts, create new versions, and submit modules for approval within their scope. Approvers perform normal approval decisions. Module disable, restore, and deprecation actions are administrator-only governance actions that require impact analysis, secondary confirmation, and audit records.

Disabling or deprecating a clause or content module version does not change runtime generation for already released templates that lock that module version. Released templates continue to generate from the content locked at release time. Module disablement or deprecation only prevents future template release candidates from referencing that module version.

If the business needs to immediately stop generation that includes a problematic clause or content module version, administrators must disable the affected template or release version through the template lifecycle. Module disablement or deprecation impact analysis must cover referencing templates, referencing release versions, default-route impact, recent usage, whether template or release-version disablement is required, and suggested replacement module versions.

This ADR does not choose a specific editor framework, rendering library, storage format, or document conversion engine.

## Consequences

- Complex financial letters can be maintained as structured document products instead of fragile large Word templates.
- The master document remains simpler and more reusable because it focuses on layout and coarse insertion areas.
- Template authoring becomes more important than DOCX master editing for long-form banking letters.
- Rich text behavior becomes testable and auditable because the platform controls the persisted content model.
- The structured fragment editor remains scoped to document-generation content in v1; broader reuse is deliberately deferred.
- Anchor bindings, render profiles, and preview artifacts become stable release evidence rather than transient UI state or renderer-specific options.
- Release checks may need new validation items for unsupported rich text nodes, missing styles, invalid variables, numbering risks, and rendering fidelity issues.
- Implementation teams must avoid treating arbitrary HTML-to-DOCX conversion as the core rendering contract.

## Alternatives Considered

- Maintain complex letters as one large DOCX master with many anchors: not recommended because anchor count, Word editing complexity, diff review, reuse, testing, and collaboration all become high risk for 100-page documents.
- Store arbitrary HTML from a standard rich text editor and convert it to DOCX at generation time: not recommended because HTML/CSS and DOCX have different layout and numbering models, creating fidelity and audit risks.
- Build a complete online Word replacement: not recommended for the current product direction because the platform needs controlled document generation, not unconstrained document design.
- Keep rich text as a simple paragraph capability only: not sufficient for complex banking letters that require variables, inline emphasis, numbering, conditional content, reusable clauses, and reliable DOCX/PDF output.

## Remaining Open Questions

No remaining open questions are tracked in this ADR.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Template Authoring and Rendering First-Principles Review](../../product/authoring-rendering-first-principles-review.md)
- [Usability Review](../../product/usability-review.md)
- [Template Verifiability ADR](../template-lifecycle/0015-template-release-verifiability.md)