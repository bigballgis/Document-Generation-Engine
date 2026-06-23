# P18 — Structured Authoring & Rendering-Fidelity Engine (Detailed Plan)

**Phase status:** Not Started | **Depends on:** P3, P4
**Confirmed:** 2026-06-23 (deep-review gap G3 — largest confirmed authoring gap)

> Single-active-phase invariant: P13 completed **Done** (2026-06-23); there is currently no
> single active phase. P18 stays `Not Started` until selected as the next active phase.
> **Scope note:** This is the deepest confirmed domain. It is intentionally decomposed
> into sub-slices (T01…T08); each sub-slice is behavior-spec-first and may be promoted
> to its own active sub-phase when P18 is activated. Do not attempt as one change set.

## Source-of-truth & traceability

- PRD §6.4 (锚点内容类型), §6.5.1 (模板创作与渲染边界 — 节点矩阵 / 编号 / 表格组件 /
  签章 / 渲染配置), §6.4.3 (结构化片段与母版样式映射), §6.4.4 (Word/HTML 粘贴清洗) — confirmed.
- requirements-plan 已确认：模板创作与渲染边界 / 结构化片段与母版样式映射 / Word/HTML 粘贴清洗.
- authoring-rendering-first-principles-review.md.
- Backlog origin: finding X6 / U9 (read-only authoring surfaces).

## Gap evidence

- `anchor_binding.structured_content_json` is an **opaque string** with no controlled
  node-matrix validation, no node typing, no style catalog, no paste cleaning, no
  render-profile lock.
- `AnchorContentType` exists but the structured content tree is not validated against the
  confirmed v1 node matrix; `TemplateRuleValidationService` covers rules, not node fidelity.
- Frontend authoring surfaces are largely read-only (U9); no controlled rich-text editor,
  no master style catalog UI, no paste-cleaning summary.

## Behavior goal

Implement the confirmed controlled structured-authoring model end to end: a conservative
v1 node matrix (block/inline/reference nodes), master style catalog + whitelisted limited
direct format, structured table-component model, seal/QR/attachment/image reference nodes,
deterministic controlled multi-level numbering, Word/HTML paste cleaning with a
transformed/removed/warning/blocked summary, and a publish-locked `renderProfile`. Fidelity
issues are graded into blockers (prevent publish) and warnings (summary + view-confirm),
surfaced in test/approval/publish materials and the runtime success response per confirmed rules.

### v1 node matrix (confirmed)

- **Block:** section/heading, paragraph, list, condition block, loop block, table-component ref.
- **Inline:** text run, variable node, emphasis, underline, line-break.
- **Reference:** clause/content-module ref, image ref, QR/barcode ref, seal/stamp ref,
  attachment-list ref, style ref.
- **Forbidden:** arbitrary HTML/CSS, script, absolute positioning, float, complex columns,
  online header/footer/margin/global-layout edits, unrecognized Word dirty styles.

### Acceptance scenarios

- **Given** an anchor binding content tree, **When** validated, **Then** unsupported nodes,
  missing variable/style/module/table/anchor refs, broken clause-semantic numbering,
  unreliable tables, and abnormal seal placement are flagged as **blockers**; low-risk
  pagination/scaling/style-normalization differences are **warnings** with `warningCode`,
  `messageKey`, location, detection, suggestion.
- **Given** Word/HTML paste, **When** cleaned, **Then** only confirmed controlled nodes +
  limited direct format are produced; scripts/macros/embeds/iframes/external resources/
  unmapped styles/unreliable tables are blocked; a cleaning summary distinguishes
  transformed/removed/warning/blocked; user can cancel or undo to pre-paste state; summary
  stores no sensitive source plaintext.
- **Given** a style ref, **When** validated, **Then** it must come from the accessible
  approved master style catalog and apply to the node type; missing/unapproved/inapplicable
  style or out-of-whitelist limited direct format is a **blocker**.
- **Given** a publish candidate, **When** published, **Then** the `renderProfile` (style,
  numbering, table pagination, image scaling, PDF conversion, fidelity policy) is locked;
  API callers cannot override it.

## Tasks

| ID | Task | Status |
| --- | --- | --- |
| P18-T01 | Controlled node-matrix domain model + JSON schema for the structured content tree | Not Started |
| P18-T02 | Node-matrix validation engine (blockers vs warnings, stable `warningCode`/`messageKey`) | Not Started |
| P18-T03 | Master style catalog (available styles, applicable node types, render purpose) + limited direct-format whitelist + validation | Not Started |
| P18-T04 | Structured table-component model (column schema, repeat header, loop rows, totals, controlled widths) + fidelity rules | Not Started |
| P18-T05 | Seal/stamp, QR/barcode, image, attachment-list reference nodes + placement/visibility blockers | Not Started |
| P18-T06 | Controlled multi-level numbering with deterministic re-sequencing after condition/loop render | Not Started |
| P18-T07 | Word/HTML paste cleaning + cleaning summary (transformed/removed/warning/blocked) + cancel/undo | Not Started |
| P18-T08 | Publish-locked `renderProfile` (+ `renderProfileVersion`) wired through render/preview/runtime | Not Started |
| P18-T09 | Fidelity warnings/blockers surfaced in test/approval/publish materials + runtime success response (`result.fidelityWarnings[]` / stream headers) | Not Started |
| P18-T10 | UI: controlled rich-text editor (familiar toolbar, only confirmed nodes), style picker, paste-cleaning summary, fidelity warning list with filters | Not Started |

## Exit criteria (phase)

- Structured content validated against the confirmed v1 node matrix; blockers prevent
  publish, warnings produce summaries with stable codes; no arbitrary HTML/CSS in contract.
- Master style catalog + limited direct format enforced; paste cleaning with summary +
  cancel/undo; table/seal/QR/attachment nodes + controlled numbering implemented.
- `renderProfile` publish-locked; runtime callers cannot override; fidelity surfaced in
  test/approval/publish materials and runtime success response.
- Green gates: `mvn verify` + frontend lint/type-check/test/build; coverage on core/security
  ≥90% per constitution.
- PRD §6.4–6.5.1 + permission-matrix cross-links verified; ledger evidence recorded.

---

## Task-level behavior specs & test-first plan (2026-06-23)

> **Largest confirmed domain — multi-sub-slice.** Anchored to current code. Activation
> requires P13 Done + plan-orchestrator selection (single-active-phase). When activated,
> each sub-slice (T01…T10) should be treated as its own behavior-spec-first mini-cycle; do
> NOT attempt the whole phase as one change set. Each task: failing test first → smallest
> implementation → refactor → doc sync.

### Current-state anchors (verified)

- `AnchorContentType` — declared content type per anchor (TEXT, RICH_TEXT, TABLE, IMAGE,
  CLAUSE, SEAL, QR_CODE, ATTACHMENT_LIST). Anchor-level only; **no node-level matrix**.
- `DocxAssembler.renderStructuredContent` — handles **only 3 node types**: `text`,
  `variable`, `paragraph`; every other node returns `""`. The confirmed v1 node matrix
  (list, section/heading, condition block, loop block, table-component, emphasis, underline,
  line-break, clause/image/QR/seal/attachment/style refs) is **unimplemented in rendering**.
- `BindingValidationStatus` — has the 4 confirmed statuses (VALID / MISSING_ANCHOR /
  DUPLICATE_BINDING / INCOMPATIBLE_CONTENT_TYPE); binding-level validation exists, but
  **node-matrix / style-catalog / numbering / table / paste / render-profile** validation
  does not.
- `FidelityWarningCode` exists but the warning pipeline emits a single hardcoded warning
  (`PreviewGenerationService` L111–116) — not a real fidelity engine.
- `anchor_binding.structured_content_json` is opaque text — no schema enforcement.

### Sequencing principle

Model → validate → render → surface. Land T01 (model/schema) and T02 (validation) first so
every later node type plugs into a single validated tree and a single blocker/warning grader.

### P18-T01 — Controlled node-matrix model + JSON schema

- **Behavior:** Define the v1 node matrix (block/inline/reference) as a typed model + a JSON
  schema for `structured_content_json`; reject unknown node types at parse time.
- **Failing tests first** (`StructuredContentSchemaTest`):
  1. `parse_supportedNodes_succeeds`
  2. `parse_unknownNodeType_isRejected`
  3. `parse_forbiddenConstruct_isRejected` (script / absolute-pos / arbitrary HTML)
- **Impl:** node model + schema validator; wire into `upsertBinding` save path.

### P18-T02 — Node-matrix validation engine (blockers vs warnings)

- **Behavior:** Validate a content tree into blockers (unsupported node, missing
  variable/style/module/table/anchor ref, broken clause-semantic numbering, unreliable table,
  abnormal seal placement) and warnings (low-risk pagination/scaling/style-normalization), each
  with stable `warningCode` + `messageKey` + location + detection + suggestion.
- **Failing tests first** (`NodeMatrixValidationServiceTest`):
  1. `missingVariableRef_isBlocker`
  2. `unsupportedNode_isBlocker`
  3. `lowRiskScaling_isWarning_withStableCode`
  4. `validation_excludesSensitiveData`
- **Impl:** validation engine returning a graded result; feed into `BindingValidationStatus`
  + publish gate (P19-T06).

### P18-T03 — Master style catalog + limited direct format

- **Behavior:** A style catalog from the approved master (available styles, applicable node
  types, render purpose); template style refs must resolve to it and apply to the node type;
  limited direct format restricted to the confirmed whitelist (font, size, color, line spacing,
  before/after spacing, first-line/left/right indent). Out-of-whitelist / inapplicable / unapproved
  style → blocker. Published templates lock the resolved styles.
- **Failing tests first** (`MasterStyleCatalogServiceTest`):
  1. `styleRef_resolvesToApprovedCatalog`
  2. `styleRef_notInCatalog_isBlocker`
  3. `directFormat_outOfWhitelist_isBlocker`
  4. `directFormat_modifyingGlobalLayout_isBlocker`

### P18-T04 — Structured table-component model

- **Behavior:** Column schema, header, repeat-header across pages, loop rows, simple totals/
  footer, controlled column widths. Nested/floating/absolute tables, unreliable merged-cell
  pagination, or unreadable key amounts/clauses → blocker.
- **Failing tests first** (`TableComponentServiceTest`):
  1. `tableComponent_withColumnSchema_renders`
  2. `nestedTable_isBlocker`
  3. `repeatHeader_acrossPages_preserved`

### P18-T05 — Seal / QR / image / attachment reference nodes

- **Behavior:** Controlled reference nodes for seal/stamp, QR/barcode, image, attachment-list;
  seal must stay within authorized area, not overlap/clip/exceed/be invisible (else blocker);
  QR/barcode/seal not subject to image-scaling warning.
- **Failing tests first** (`ReferenceNodeServiceTest`):
  1. `sealOutsideAuthorizedArea_isBlocker`
  2. `imageScaling_isWarning_butSealScaling_isBlocker`
  3. `attachmentListRef_renders`

### P18-T06 — Controlled multi-level numbering

- **Behavior:** Controlled numbering scheme; deterministic re-sequencing after condition/loop
  render; duplicate/skip/clause-semantic-change/broken cross-ref → blocker.
- **Failing tests first** (`NumberingServiceTest`):
  1. `numbering_afterLoopRender_isDeterministic`
  2. `duplicateNumber_isBlocker`
  3. `brokenCrossReference_isBlocker`

### P18-T07 — Word/HTML paste cleaning + summary + cancel/undo

- **Behavior:** Clean pasted Word/HTML into confirmed nodes + limited direct format only;
  block scripts/macros/embeds/iframes/external resources/unmapped styles/unreliable tables;
  produce a summary (transformed / removed / warning / blocked); allow cancel or undo to
  pre-paste state; summary stores no sensitive source plaintext.
- **Failing tests first** (`PasteCleaningServiceTest`):
  1. `paste_simpleParagraphs_transformed`
  2. `paste_script_isBlocked`
  3. `paste_summary_categorizesTransformedRemovedWarningBlocked`
  4. `paste_summary_excludesSourcePlaintext`

### P18-T08 — Publish-locked `renderProfile`

- **Behavior:** A controlled `renderProfile` (+ `renderProfileVersion`) covering style,
  numbering, table pagination, image scaling, PDF conversion, fidelity policy; locked at
  publish; runtime callers cannot override it.
- **Failing tests first** (`RenderProfileServiceTest`):
  1. `publish_locksRenderProfileVersion`
  2. `runtime_ignoresCallerRenderOverride`
  3. `previewRecord_carriesRenderProfileVersion`

### P18-T09 — Fidelity surfaced everywhere (replace hardcoded warning)

- **Behavior:** Real fidelity warnings/blockers from T02 flow into test/approval/publish
  materials and the runtime success response (`result.fidelityWarnings[]`; stream headers carry
  count + codes). Remove the hardcoded `CONTROLLED_STYLE_FALLBACK` stub.
- **Failing tests first** (extend `TemplatePlatformSliceTest` / preview test):
  1. `preview_emitsRealWarnings_fromValidationEngine`
  2. `runtimeSuccess_includesFidelityWarnings`
  3. `noHardcodedWarning_whenContentClean`

### P18-T10 — UI (controlled editor)

- **Behavior:** Controlled rich-text editor (familiar toolbar, only confirmed nodes; disabled
  capabilities show unavailable reasons), style picker from catalog, paste-cleaning summary
  modal with cancel/undo, fidelity warning list with filters (`warningCode`, location,
  artifact, viewed/unviewed).
- **Failing tests first** (Vitest): only confirmed nodes insertable; paste summary modal flow;
  warning list filters.

### Migration & sequencing

- Flyway: add `master_style_catalog` (+ entries), `render_profile` (+ version), and any node-
  validation result columns; keep `structured_content_json` but enforce the schema on write.
- Suggested order: T01 → T02 → T03 → T04 → T05 → T06 → T07 → T08 → T09 → T10.
- Rendering must stay isolated from lifecycle/authorization/API-governance (module-boundary
  ADR; cf. optimization-plan D1). Core rendering/fidelity → JaCoCo changed-line ≥90%.
