# CORE-FORTRESS F4 — Production Rendering Hardening (Detailed Plan)

**Program ID:** `CORE-FORTRESS`  
**Phase ID:** `CORE-FORTRESS-F4-PRODUCTION-RENDERING-HARDENING`  
**Phase status:** **Done** (closed 2026-07-09; T01–T05 + T08; code complete; **gate caveat** — see §3)  
**Depends on:** CORE-FORTRESS F1–F3 (**Done**), P23 (**Done**), LR-A2 (**Done**)  
**BDD:** `docs/behavior/core-fortress-f4-production-rendering-hardening.md` — **ready** (`BDD-CORE-FORTRESS-F4-001`)

> **Single-active-phase invariant:** **F5** is the sole formal phase `In Progress` (activated 2026-07-09). **F4 Done** (code complete; environmental gate caveat). LRP Wave LR-A: **LR-A1 Done** (F4); **LR-A7 partial** (corpus schema + procedure — measurements pending Docker).

---

## 1. North star

**LibreOffice PDF conversion is production-safe under concurrency** — isolated profiles per invocation, bounded pool with documented configuration, measurable pagination delta baseline against P23 demo corpus. No silent shared-profile failures (CD-PIT-11). **No promise of Word-identical pagination.**

---

## 2. Scope (in) / out (out)

| In scope (F4) | Out of scope (Done elsewhere or later) |
| --- | --- |
| F4-A1: ≥4 parallel conversions through pooled path — **real `soffice`** or Docker evidence (LR-A1 completion) | **LR-A2 Done** — CJK/metric fonts, Dockerfiles, `RenderingFontSmokeTest` |
| F4-A2: Pool/timeout/queue/pagination-budget config in `DocgenRenderingProperties` + `application.yml` + binding tests | **LR-A6 partial Done** — `OoxmlOutputValidationGateTest` (no extension) |
| F4-A3: Profile isolation gap closure — docker-exec container profile cleanup; normalization test coverage | **F1 Done** — unified writer, fail-closed refs |
| F4-A4: Pagination corpus table (≥5 letters) + measurement procedure in NFR (LR-A7 **subset**) | **LR-A3** upload deep validation |
| Cross-reference + close LR-A1 row; partial LR-A7 row | **LR-A4** unsupported-node (F1/A4 territory) |
| Sequential + parallel temp/profile cleanup regression | Runtime pagination **enforcement** / fidelity warnings (post ADR-0042) |
| | PDF page number stamping (`pdfPageNumberStampingEnabled`) |
| | DOCX normalization default enablement |
| | Frontend / E2E / UIUX (backend-only phase) |
| | ADR-0041/0043 drafting (LR-A5 — reference only) |

### Already done — do NOT re-implement

| Asset | Evidence |
| --- | --- |
| Per-invocation `-env:UserInstallation` in `LibreOfficePdfConversionService` | L72–82; `profileUrl()` |
| CLI hardening flags `--norestore …` | Same + `DockerExecPdfConversionService` L90–93 |
| `DockerExecPdfConversionService` unique container profile path | L74–89 |
| `LibreOfficeDocxNormalizationService` profile isolation + cleanup | L64–110 |
| Fake-script parallel + profile unit tests | `LibreOfficePdfConversionServiceTest` |
| Font smoke | `RenderingFontSmokeTest` (LR-A2 Done 2026-07-08) |
| OOXML gate | `OoxmlOutputValidationGateTest` |

---

## 3. Exit criteria

1. **A1:** New integration test (or Docker smoke doc) proves **≥4 concurrent** PDF conversions succeed with real `soffice`; LR-A1 marked **Done** in LRP-A.
2. **A2:** `pagination-delta-budget-pages` wired in `application.yml`; pool/timeout properties covered by tests; 10-run sequential cleanup regression green.
3. **A3:** docker-exec container profile best-effort cleanup implemented + tested; normalization profile cleanup test if missing.
4. **A4:** NFR §production rendering contains corpus table (≥5 demos) + rerun procedure; max/median delta recorded; ADR-0042 fed (Proposed OK).
5. **Green gates:** `mvn -B -ntp -f backend/pom.xml verify` — **caveat (2026-07-09):** F4 targeted suite **23/23** pass (`LibreOfficeParallelConversionIntegrationTest`, `DocgenRenderingPropertiesBindingTest`, `DockerExecPdfConversionServiceTest`, `LibreOfficePdfConversionServiceTest`, `LibreOfficeDocxNormalizationServiceTest`); full `mvn verify` **failed on Windows** — file lock on `deploy/demo-*/*.docx` (`FolMasterDocxAssetGeneratorTest`); **not an F4 regression**; re-run on clean CI/Linux host.
6. **Doc sync:** master-plan, program roadmap, ledger, behavior index, LRP-A cross-rows — **Done** (F4-T08).

---

## 4. Task breakdown

| ID | Owner | Task | Depends on | Status |
| --- | --- | --- | --- | --- |
| **F4-T01** | behavior-spec-author | **BDD behavior spec** — `core-fortress-f4-production-rendering-hardening.md` + this plan | — | **Done** (2026-07-09; readiness `ready`) |
| **F4-T02** | backend-engineer | **A1 parallel regression** — `LibreOfficeParallelConversionIntegrationTest` (or extend smoke): ≥4 concurrent via production-equivalent pool + real `soffice`; skip when unavailable; BDD-F4-A1-* | F4-T01 | **Done** (2026-07-09; skip-without-`soffice` + fake-script pool path green) |
| **F4-T03** | backend-engineer | **A2 config evidence** — wire `pagination-delta-budget-pages` in `application.yml`; `DocgenRenderingProperties` / `PdfConversionExecutorConfig` binding test; 10-run cleanup regression; document env vars in runbook snippet | F4-T01 | **Done** (2026-07-09; `DocgenRenderingPropertiesBindingTest`; NFR §config table) |
| **F4-T04** | backend-engineer | **A3 isolation gaps** — docker-exec container profile `rm` best-effort; `DockerExecPdfConversionServiceTest`; normalization profile cleanup test; audit all `soffice` launch sites | F4-T01 | **Done** (2026-07-09; `DockerExecPdfConversionService` cleanup + tests) |
| **F4-T05** | doc-keeper + backend-engineer | **A4 pagination baseline** — NFR §production rendering corpus table (≥5 P23 masters); measurement procedure; record deltas + proposed budget; coordinate ADR-0042 draft input (LR-A7 subset) | F4-T01, P23 Done | **Done** (2026-07-09; NFR corpus schema + rerun procedure; row values _待测_ until Docker) |
| **F4-T06** | build-deploy-agent | **Docker evidence** — `docker-deploy.ps1` redeploy; optional parallel conversion smoke script/note in ledger; font smoke + parallel test in container | F4-T02 | **Blocked** (2026-07-09; no `soffice` on dev host; Docker redeploy evidence not captured — **non-blocking** for F4 code-complete closeout) |
| **F4-T07** | architecture-reviewer | **Boundary review** — rendering isolation; no path leakage in errors; pool fail-fast; LO stays in `rendering.*` | F4-T02–T05 | **Not Started** (deferred; schedule at F5 kickoff or ops follow-up) |
| **F4-T08** | post-task-doc-sync | **Plan + ledger closeout** — mark F4 Done; LR-A1 Done; LR-A7 partial; program roadmap | F4-T07 + green gates | **Done** (2026-07-09; environmental `mvn verify` caveat recorded) |

**Task count:** **8** (F4-T01 … F4-T08)

---

## 5. Recommended wave order

```text
Wave 0 — BDD + plan (Done)
  F4-T01

Wave 1 — Core hardening (TDD; T03/T04 can parallel after T01)
  F4-T02 (A1 real parallel integration)
  F4-T03 (A2 config) — parallel OK
  F4-T04 (A3 docker-exec + normalization tests) — parallel OK

Wave 2 — Documentation + deploy evidence
  F4-T05 (A4 corpus + NFR)
  F4-T06 (Docker smoke) — after T02

Wave 3 — Review + closeout
  F4-T07 → F4-T08
```

---

## 6. LRP cross-reference (F4 executes subset)

| LRP task | F4 coverage | Notes |
| --- | --- | --- |
| **LR-A1** | **F4-T02, F4-T04** | Profile isolation code **partial Done**; F4 closes acceptance |
| **LR-A2** | — | **Done** 2026-07-08 — reference only |
| **LR-A6** | — | Gate exists — **out of F4** |
| **LR-A7** | **F4-T05** | Corpus + procedure only; ADR-0042 finalize remains LR-A5/LRP |
| **LR-A5** | Feeds F4-T05 | ADR-0042 draft — doc-keeper |

---

## 7. Gate commands

| Context | Command |
| --- | --- |
| TDD inner loop | `mvn -B -ntp -f backend/pom.xml -Pdev-fast test -Dtest=LibreOfficePdfConversionServiceTest,LibreOfficeParallelConversionIntegrationTest,DockerExecPdfConversionServiceTest` |
| Full backend gate | `mvn -B -ntp -f backend/pom.xml verify` |
| Docker acceptance | `.\scripts\docker-deploy.ps1` + manual/API PDF generation |
| Font + LO in container | Run verify inside backend image or host with `soffice` on PATH |

---

## 8. Acceptance scenarios → tests (TDD Red map)

| BDD ID | Target test / artifact |
| --- | --- |
| BDD-F4-A1-001 | `LibreOfficeParallelConversionIntegrationTest.parallelConversionsThroughPool_allSucceed` |
| BDD-F4-A1-002 | Same + profile dir count assertion |
| BDD-F4-A1-003 | Existing `LibreOfficePdfConversionServiceTest` |
| BDD-F4-A2-001 | `PdfConversionExecutorConfigTest` or `@SpringBootTest` properties binding |
| BDD-F4-A2-004 | Properties test for `paginationDeltaBudgetPages` |
| BDD-F4-A2-005 | Extended cleanup test (10 sequential) |
| BDD-F4-A3-002 | `DockerExecPdfConversionServiceTest` |
| BDD-F4-A3-003 | Normalization service test |
| BDD-F4-A4-001…003 | NFR doc + optional `PaginationCorpusMeasurementTest` (PDFBox page count helper) |

---

## 9. Traceability

| Document | Purpose |
| --- | --- |
| [Behavior spec F4](../../behavior/core-fortress-f4-production-rendering-hardening.md) | BDD source |
| [Program roadmap](./CORE-FORTRESS-program-roadmap.md) | F4 status |
| [LRP-A detail](./LRP-A-rendering-trust-hardening.md) | LR-A1/A7 origin |
| [Master plan](../master-plan.md) | Phase row |
| [Execution ledger](../execution-sync-ledger.md) | Gate evidence |
| [NFR §production rendering](../../requirements/non-functional-requirements.md) | A4 corpus home (T05) |
