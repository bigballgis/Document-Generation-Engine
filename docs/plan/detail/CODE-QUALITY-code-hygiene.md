# CODE-QUALITY — Code Hygiene & Structural Consistency (Detailed Plan)

**Program ID:** `CODE-QUALITY`  
**Phase ID:** `CODE-QUALITY-CODE-HYGIENE`  
**Program status:** **Done** (2026-07-09 — CQ-01A/B…CQ-08 all slices complete; ArchUnit **11/11 GREEN**)  
**Depends on:** CORE-FORTRESS F1–F8 (**Done** 2026-07-09) — stable rendering kernel + frontend composable baseline  
**BDD:** **`not-applicable`** (behavior-unchanging refactor; regression via existing gates + slice tests)

> **Single-active-phase invariant:** **CODE-QUALITY** is the **sole** formal program `In Progress` (2026-07-09). CORE-FORTRESS remains **Done** — do not reopen F1–F8.

> **Audit baseline (2026-07-09):** code-quality-reviewer + explore consensus — module boundary violations, god classes, DRY debt in demo tests and PDF conversion, `*AccessSupport` naming sprawl, E2E readiness copy-paste.

---

## 1. North star

**Zero-waste, boundary-clean codebase** — rendering isolated from lifecycle/template orchestration; files under soft size budgets; duplicated construction/test patterns collapsed; naming consistent — **with zero user-visible behavior change**.

---

## 2. Scope (in) / out (out)

| In scope | Out of scope |
| --- | --- |
| CQ-01A/B: module boundary decoupling + ArchUnit enforcement | New features, API contract changes, permission matrix changes |
| CQ-02: `StructuredContentDocxWriter` `WriteSession` extraction (~640 LOC class → session object) | SpEL/scripting or new node types |
| CQ-03: `ManagementAuditRecorder` entity construction dedup (11× patterns) | New audit event types or retention policy |
| CQ-04: `ApiPolicyDomainEditor.vue` decompose (~829 LOC audit figure; ~760 LOC measured) | API policy behavior / new domains |
| CQ-05: Demo `*MasterDocxAssetGeneratorTest` shared fixture base | Demo content/typography rewrites (P23 Done) |
| CQ-06: `*AccessSupport` naming normalization | Authorization rule changes |
| CQ-07: LibreOffice + DockerExec PDF conversion DRY | New conversion modes or LO pool sizing |
| CQ-08: E2E stack-readiness helper | New Playwright journeys (unless parity smoke required) |

### Already done — reuse, do NOT re-implement

| Asset | Evidence | CQ usage |
| --- | --- | --- |
| CORE-FORTRESS F6 lifecycle/navigation composables | [CORE-FORTRESS-f6-frontend-kernel-refactor.md](./CORE-FORTRESS-f6-frontend-kernel-refactor.md) — **Done** 2026-07-09 | **CQ-04** may extract policy-domain sub-composables; **do not** re-split `useTemplateLifecycleGates` / `useTemplateDetailTabs` |
| SOR-F03 `ApiPolicyDomainEditor` + `stores/apiPolicy.ts` | [system-optimization-review-2026-07.md](../system-optimization-review-2026-07.md) SOR-F03 **Done** | **CQ-04** further decomposes editor SFC only |
| CORE-FORTRESS F1 unified `StructuredContentDocxWriter` | [CORE-FORTRESS-f1-rendering-correctness.md](./CORE-FORTRESS-f1-rendering-correctness.md) | **CQ-02** extracts session state; does not fork render paths |

---

## 3. Exit criteria (program)

1. **Boundaries:** No `rendering` → `template.service.*` imports for orchestration; authoring shared types in agreed shared packages; ArchUnit rules green in CI.
2. **Size:** `StructuredContentDocxWriter` orchestrator ≤ **350** LOC; `WriteSession` owns mutable write state; `ManagementAuditRecorder` ≤ **400** LOC; `ApiPolicyDomainEditor.vue` ≤ **400** LOC (template + script split).
3. **DRY:** Demo master generator tests share one fixture base; PDF conversion shared helper between LO and DockerExec paths; E2E specs use one stack-readiness helper.
4. **Naming:** `*AccessSupport` classes follow one documented convention (stateless helper vs service delegate).
5. **Gates:** `mvn -B -ntp -f backend/pom.xml verify` **GREEN**; `pnpm -C frontend lint && type-check && test && build` **GREEN**; no new SpotBugs/Checkstyle/PMD violations.
6. **Doc sync:** program roadmap, master-plan, ledger — post closeout per slice.

---

## 4. Architecture design gate (CQ-01A + CQ-01B)

**Gate date:** 2026-07-09 · **Verdict:** **PASS** (architecture-reviewer CQ-01 design review)

**Locked package anchor (merge prerequisite):** `com.bank.docgen.sharedkernel.document` + `com.bank.docgen.sharedkernel.document.expression` — CQ-01A/B must agree before `integration-merger`.

### Wave 1 merge hard gates (Critical)

1. **`RenderProfileService`** — `applyPreviewRenderProfileVersion(PreviewRecordEntity, …)` moves to `rendering.service`; authoring must not depend on `rendering.persistence`.
2. **`template` → `rendering.persistence`** — `PublishGateService`, `CoverageComputationService` use **`PreviewEvidencePort`** only.
3. **Package name** — single `sharedkernel.document` anchor; no duplicate contract packages.

### Port contracts (CQ-01B)

| Port | Purpose |
| --- | --- |
| `TemplatePreviewAuthorizationPort` | Readable template + permission check |
| `RenderableTemplateSnapshot` | Version/bindings/modules/master key + `RenderProfile` DTO |
| `StructuredFidelityWarningPort` | `collectWarnings` → `FidelityWarningSummary` (sharedkernel) |
| `PreviewEvidencePort` | Template-side preview/batch evidence (replaces repo reach-in) |
| `TestDataSetEvidencePort` | `lockForEvidence` (peel from rendering orchestrators) |

### ArchUnit (`ModuleBoundaryArchTest`)

Rules R1–R8: rendering ↛ template.service/persistence; rendering kernel ↛ authoring; authoring ↛ rendering; template ↛ rendering.persistence; `sharedkernel.document` spring-free; rendering ↛ `TemplateValidationException`. Enable R3+R5+R7 first (type migration), then R1–R2/R4/R6/R8 after port wiring.

### Deferred WIP (do not merge ahead of Wave 1)

Main-tree premature slices — **park until Wave 1 Done:** CQ-03 (`ManagementAuditRecorder`), CQ-05 (demo `*MasterDocxAssetGeneratorTest` / `DemoMasterDocxTestAssertions`).

---

## 5. Task breakdown

| ID | Owner | Task | Wave | Depends on | Status |
| --- | --- | --- | --- | --- | --- |
| **CQ-01A** | rendering-engineer | **Rendering↔authoring boundary decoupling** — move shared style/catalog contracts to neutral packages; eliminate rendering imports of authoring orchestration types; introduce rendering-local adapters where needed | 1 | — | **Done** (2026-07-09 — `MasterStyleCatalog`/`RenderProfile`/`ConditionExpressionEvaluator` → `sharedkernel.document`; `renderingKernelMustNotDependOnAuthoring` **GREEN**) |
| **CQ-01B** | backend-engineer | **Template↔rendering boundary + ArchUnit rules** — stop `rendering` depending on `template.service.*` for generation orchestration; add `ModuleBoundaryArchTest` (or extend existing) with fail-on-violation rules for rendering/template/authoring edges | 1 | — | **Done** (2026-07-09 — 5 ports + 4 adapters; 7 rendering services refactored; `renderingMustNotDependOnTemplateService` **GREEN**; 32 targeted tests) |
| **CQ-02** | rendering-engineer | **`StructuredContentDocxWriter` `WriteSession` extract** — extract ~mutable write state/session (~640 LOC class today) into `WriteSession` (or equivalent); writer becomes orchestrator; preserve all existing writer tests green | 2 | CQ-01A, CQ-01B | **Done** (2026-07-09 — `StructuredContentDocxWriteSession` 553 LOC; writer 132 LOC; 51 rendering tests **GREEN**) |
| **CQ-03** | backend-engineer | **`ManagementAuditRecorder` entity construction dedup** — collapse 11× repeated entity builder patterns into private helpers or small value objects; no audit payload shape change | 2 | — | **Done** (2026-07-09 — `persistAuditEvent()`; 23 targeted tests + full verify **GREEN**) |
| **CQ-04** | frontend-engineer | **`ApiPolicyDomainEditor.vue` decompose** — split ~829 LOC monolith into domain sub-components + composables (policy domains, credentials section, impact preview wiring); **F6 Done** — reuse existing lifecycle/navigation composables from template detail; do not re-split F6 kernel | 3 | — | **Done** (2026-07-09 — editor 183 LOC + extracted panels/composable/scss; gates **GREEN**) |
| **CQ-05** | backend-engineer | **Demo `*MasterDocxAssetGeneratorTest` DRY base** — shared abstract base or `DemoMasterDocxTestSupport` for 10+ `*MasterDocxAssetGeneratorTest` classes; keep per-demo POI assertions | 3 | — | **Done** (2026-07-09 — `DemoMasterDocxTestAssertions`; 8 generator tests deduped) |
| **CQ-06** | backend-engineer | **`*AccessSupport` naming normalization** — align `TemplateExportAccessSupport`, `ContentModuleAccessSupport`, `CollaborationWorkItemAccessSupport` naming/placement; document intentional splits vs merge candidates | 3 | CQ-01B | **Done** (2026-07-09 — renamed to `*AccessService`; tests renamed) |
| **CQ-07** | rendering-engineer | **PDF conversion LibreOffice/DockerExec DRY** — extract shared command/build/cleanup helper used by `LibreOfficePdfConversionService` and `DockerExecPdfConversionService`; no conversion semantics change | 2 | CQ-01A | **Done** (2026-07-09 — `ResilientPdfConversionSupport`; LO + DockerExec share resilience wrapper) |
| **CQ-08** | e2e-test-engineer | **E2E stack-readiness helper** — single Playwright fixture/helper for Docker stack health + seed readiness; replace duplicated `test.skip` + curl/copy patterns across specs | 3 | — | **Done** (2026-07-09 — `helpers/stack-readiness.ts`; 49 specs + `core-fortress-f7.ts` migrated) |

**Task count:** **8** slices (CQ-01A … CQ-08)

---

## 6. Recommended wave order

```text
Wave 1 — Boundary decoupling (IN PROGRESS — parent parallel)
  CQ-01A (rendering↔authoring)  ∥  CQ-01B (template↔rendering + ArchUnit)

Wave 2 — God-class + backend DRY (after Wave 1 green)
  CQ-02 (WriteSession) — after CQ-01A+B
  CQ-03 (ManagementAuditRecorder) — parallel OK with CQ-02
  CQ-07 (PDF DRY) — after CQ-01A; parallel OK with CQ-02/CQ-03

Wave 3 — Frontend + test infrastructure
  CQ-04 (ApiPolicyDomainEditor) — parallel OK
  CQ-05 (demo test base) — parallel OK
  CQ-06 (AccessSupport naming) — after CQ-01B
  CQ-08 (E2E helper) — parallel OK; run after touched specs identified
```

**Parallel note:** Wave 1 **CQ-01A** and **CQ-01B** are intentionally parallel under parent orchestration — merge conflicts likely in `rendering/*` and `DocxAssembler` wiring; coordinate via `integration-merger` if isolated worktrees used.

---

## 7. Hotspot evidence (audit)

| Hotspot | LOC (2026-07-09) | Finding | Slice |
| --- | --- | --- | --- |
| `rendering/StructuredContentDocxWriter.java` | **~615** | God class; mutable session state inline | CQ-02 (after CQ-01) |
| `rendering/*` → `authoring.structured.*` imports | — | Boundary coupling per `module-boundaries.md` | CQ-01A |
| `rendering/service/PreviewGenerationService.java` → `template.service.*` | — | Rendering orchestration depends on template services | CQ-01B |
| `audit/service/ManagementAuditRecorder.java` | **~651** | 11× entity construction duplication | CQ-03 |
| `frontend/.../ApiPolicyDomainEditor.vue` | **~759** (~829 audit) | Oversize SFC; SOR-F03 partial split only | CQ-04 |
| `demo/*MasterDocxAssetGeneratorTest.java` | 10+ classes | Copy-paste fixture setup | CQ-05 |
| `*AccessSupport` (template, contentmodule, collaboration) | 3+ classes | Naming/placement sprawl | CQ-06 |
| `LibreOfficePdfConversionService` + `DockerExecPdfConversionService` | — | Parallel conversion paths | CQ-07 |
| Playwright `test.skip` + stack checks | multiple specs | Duplicated readiness boilerplate | CQ-08 |

---

## 8. Gate commands (per slice)

| Layer | Command |
| --- | --- |
| Backend TDD inner loop | `mvn -B -ntp -f backend/pom.xml -Pdev-fast test -Dtest=<SliceTestClasses>` |
| Backend full gate | `mvn -B -ntp -f backend/pom.xml verify` |
| ArchUnit / boundary | Include in `mvn verify` via new `ModuleBoundaryArchTest` (CQ-01B) |
| Frontend gate | `pnpm -C frontend lint && pnpm -C frontend type-check && pnpm -C frontend test && pnpm -C frontend build` |
| E2E (when UI touched) | `pnpm -C frontend test:e2e:docker` — subset per CQ-04/CQ-08 |

---

## 9. Traceability

| Document | Link |
| --- | --- |
| Program entry | [code-quality-program.md](../code-quality-program.md) |
| Master plan pointer | [master-plan.md](../master-plan.md) |
| Module boundaries | [module-boundaries.md](../../architecture/module-boundaries.md) |
| Code quality skill | `.cursor/skills/code-quality-review/SKILL.md` |
| CORE-FORTRESS F6 composables (CQ-04 prerequisite) | [CORE-FORTRESS-f6-frontend-kernel-refactor.md](./CORE-FORTRESS-f6-frontend-kernel-refactor.md) |
| Ledger evidence | [execution-sync-ledger.md](../execution-sync-ledger.md) |
