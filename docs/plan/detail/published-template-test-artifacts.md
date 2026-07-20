# Published template test artifacts (bug fix)

**Program / slice:** `published-template-test-artifacts` (ad-hoc **NON-CE** bug-fix leaf; **not** a formal P-phase; **not** CE-O02; **not** RTL reopen)  
**Formal plan phase:** **None** — single-active-phase discipline OK (does not occupy a P* slot)  
**Task Master:** **#144** (`published-template-test-artifacts`) → **Done** (2026-07-21; remapped from mistaken **#143** — **#143** remains SYS-NORM Done)  
**Active delivery slice:** *(none — leaf closed)*  
**Placement:** merged to MAIN `ac36ecbc` (feature tip `6bc74ff1`); worktree **REMOVED**  
**BDD:** [published-template-test-artifacts.md](../../behavior/published-template-test-artifacts.md) — **ready** shipped (`BDD-PTA-001…009`); `frontend_ui_in_scope=true`  
**Batch recommendation:** **solo** (`member_task_ids: ["published-template-test-artifacts"]`; `proposed_slice_id: published-template-test-artifacts`; vetoes_applied: **no-merge-neighbors**, **unrelated-CE-deferred**) — **closed**

---

## Purpose

Close the confirmed gap: authors cannot view/download test-generated files for **PUBLISHED** templates (prefer same surface for **STOPPED** / **DEPRECATED** release lines). Root causes:

1. **FE:** Release detail Testing mounts only `BatchTestHistoryPanel`; missing read-only `TemplatePreviewRunHistoryPanel`; `@open-preview` / `@open-data-set` unwired; must **not** flip `showAuthoringSection` for PUBLISHED.
2. **BE:** `AsyncBatchTestExecutionSupport` drops `previewId` / `docxKey` / `pdfKey` when persisting `sampleResults` (writes `null`), breaking Open preview drill-down.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** (MAIN merge `ac36ecbc`; feature `6bc74ff1`; worktree **REMOVED**; 2026-07-21) |
| Formal phase | **None** |
| Host sole-active | **cleared** |
| Prior sole-active | **#143** SYS-NORM Wave 0 → **Done** (`f8e898ad` / `28d4abe1` / `ade18bdb`); prior **#142** Wave B → **Done** (`288ce98f`) |

---

## Task breakdown (implementers)

| ID | Owner | Scope | Status |
| --- | --- | --- | --- |
| **PTA-T01** | **doc-keeper** | OpenAPI / contract-outline / view DTO alignment if `sampleResults` / `SampleResult` expose `previewId` + `docxKey`/`pdfKey` (or documented equivalents); index sync only as needed — **no** permission-matrix widen | **Done** (OpenAPI + contract-outline + API README + CE-U18 U18-D2′) |
| **PTA-T02** | **backend-engineer** | Persist `previewId` + artifact keys on async batch success `sampleResults`; keep fail path keys null-ok; TDD unit/integration; **do not** add PUBLISHED lifecycle gate on download APIs | **Done** |
| **PTA-T03** | **frontend-engineer** | `TemplateReleaseDetailView` Testing: mount read-only `TemplatePreviewRunHistoryPanel`; wire `@open-preview` (select preview row) + `@open-data-set` (non-silent, no authoring reopen per PTA-D4); **forbid** `showAuthoringSection=true` for PUBLISHED/STOPPED/DEPRECATED; English-first i18n reuse | **Done** |
| **PTA-T04** | **e2e-test-engineer** | Playwright journey on release Testing: history visible + SUCCEEDED download + Open preview when `previewId` present | **Done** (TM144 **4/4**) |
| **PTA-T05** | **e2e-uiux-reviewer** | Bank OA UIUX review of release Testing surface (Critical=0) | **Done** (PASS Critical=0) |
| **PTA-T06** | **build-deploy-agent** | Queued `docker-deploy-queue` Stage 5 (E2E prep) and/or Stage 10 evidence | **Done** (Stage 5 + 10 **DEPLOY_OK**) |

**Pipeline order completed:** doc-keeper → backend-engineer + frontend-engineer → Stage 5 → e2e + uiux → architecture → Stage 10 → integration-merger → MAIN doc-sync/commit.

---

## Exit criteria (Done)

| # | Criterion | Evidence |
| --- | --- | --- |
| 1 | BDD-PTA-001…009 acceptance met with durable behavior | Behavior shipped; E2E + unit/integration cover |
| 2 | BE gates: `mvn -B -ntp -f backend/pom.xml verify` GREEN | **GREEN 2344** (`-Xmx1024m`) |
| 3 | FE gates: `pnpm -C frontend lint` / `type-check` / `test` / `build` GREEN | lint/type-check/test **1600**/build **GREEN** |
| 4 | E2E functional + UIUX PASS (Critical=0) | E2E **4/4**; UIUX **PASS** Critical=0 |
| 5 | Queued Docker evidence when required (Stage 5 and/or 10) | Stage 5 + 10 **DEPLOY_OK** |
| 6 | Stage 11 merge to MAIN + worktree removed + MAIN post-task-doc-sync + commit-review | Merge `ac36ecbc`; tip `6bc74ff1`; worktree **REMOVED**; stage 12 this sync |
| 7 | Vetoes held: no `#3b/#5a` GO flip; no RTL; CE-O02 deferred; no go-live claim | Held |

---

## Gate / merge evidence

- **BE:** `mvn verify` **GREEN 2344** (`-Xmx1024m`)
- **FE:** lint / type-check / test **1600** / build **GREEN**
- **E2E:** TM144 **4/4** PASS — `frontend/e2e/evidence/TM144-published-template-test-artifacts-manifest.md`
- **UIUX:** **PASS** Critical=0 — `frontend/e2e/evidence/TM144-published-template-test-artifacts-uiux-manifest.md`
- **Architecture:** **PASS_WITH_NOTES** Critical=0 (after #144 remap)
- **Deploy:** Stage 5 **DEPLOY_OK** — [evidence/published-template-test-artifacts-stage5-deploy/](../evidence/published-template-test-artifacts-stage5-deploy/); Stage 10 **DEPLOY_OK** — [evidence/published-template-test-artifacts-stage10-deploy/](../evidence/published-template-test-artifacts-stage10-deploy/)
- **Merge:** MAIN `ac36ecbc`; feature `6bc74ff1`; worktree **REMOVED**

---

## Vetoes (hard — still held after Done)

- **Do not** flip `showAuthoringSection` for PUBLISHED / STOPPED / DEPRECATED
- **Do not** add download API lifecycle gate for PUBLISHED
- **Do not** widen permission-matrix / new capability bits
- **Do not** flip checklist **#3b / #5a** GO
- **Do not** reopen RTL; **do not** activate CE-O02
- **Do not** claim go-live / IBL program Done

---

## Shared acceptance surface

Release detail **Testing** tab: preview run history + SUCCEEDED DOCX/PDF downloads; batch history **Open preview** after BE persist fix.

---

## Relation to other programs

| Program | Relation |
| --- | --- |
| **CE** (#53 umbrella) | Registry only — do **not** treat as delivery leaf; CE-O02 stays Deferred |
| **IBL** | Unchanged; #119 Blocked; Wave B residual only |
| **#143** SYS-NORM Wave 0 | Remains **Done** — remapped away from this leaf |
| **#142** demo expand | Remains **Done** — unrelated |
| **CE-U18** | Reuse batch history Open preview semantics — do not expand batch run model |
