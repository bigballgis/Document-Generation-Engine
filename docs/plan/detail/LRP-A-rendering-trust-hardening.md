# LRP Wave LR-A — Rendering Trust Chain & File Safety 「渲染信任链与文件安全」

**Program:** [launch-readiness-program.md](../launch-readiness-program.md)  
**Wave status:** **Done** (activated 2026-07-04 after LR-B closure; A1–A7 **Done** 2026-07-10 — see exit gate; Word-vs-LO / ADR-0042 Accepted + ADR-0043 slice B **explicitly deferred out of wave exit**; ADR-0041 Accepted residual **closed** 2026-07-10 via architecture-reviewer)  
**Owner default:** `backend-engineer` (+ `deploy-engineer` for images, `doc-keeper` for ADRs)  
**Prerequisites:** none for A1/A2/A3/A5; **A4/A6 depend on P22-T01/T02 Done**; **A7 depends on P23 demo packages (T04+ letter-grade)** — track via [P23 detail](./P23-demo-typography-layout-excellence.md)

> **Sign-off note (2026-07-10):** **ADR-0041 → Accepted** (architecture-reviewer **PASS_WITH_NOTES**; slice `lrp-a5-adr-closeout`). 0041 Accepted residual **closed**. 0042/0043 remain **Proposed**. Wave LR-A Done honesty **PASS**. Remaining deferred out of wave: Word-vs-LO + ADR-0042 Accepted; ADR-0043 slice B (XSD/LO24). Do **not** treat those residuals as LR-C9 scope (LR-C9 separately scheduled 2026-07-10 as usability quick win).

> **Completion note (2026-07-10):** **LR-A5 → Done** (slice `lrp-a5-adr-closeout`; docs-only; **merge `cc9e5f6`** → `main`; worktree removed). ADR triad on disk + indexed: [ADR-0041](../../adr/rendering-authoring/0041-rendering-font-baseline.md) **Accepted** (architecture-reviewer PASS_WITH_NOTES 2026-07-10; LR-A2 evidence solid); [ADR-0042](../../adr/rendering-authoring/0042-pagination-delta-budget.md) **Proposed** (Word n/a residual strengthened — no invented Word numbers); [ADR-0043](../../adr/rendering-authoring/0043-ooxml-output-validation-gate.md) **Proposed** (A6 slice A Done; XSD/LO24 residual). BDD **not-applicable**. Formal phase remains **None**. **Wave LR-A → Done** with explicit deferrals out of wave exit: (1) Word-vs-LO page delta + ADR-0042 Accepted on Word-equipped host; (2) ADR-0043 slice B (ECMA-376 XSD + LO24 headless). ADR-0041 Accepted residual **closed** by this review. Do **not** start LR-C9 *for these residuals* (LR-C9 later scheduled separately as usability quick win — not residual work). Gate evidence: docs-only + architecture-reviewer PASS_WITH_NOTES. **Task Master #13 → done**.

> **Activation note (2026-07-10):** **LR-A5 → In Progress** (slice `lrp-a5-adr-closeout`; ISOLATED `D:/working/DGE-lrp-a5-adr-closeout` · `feat/lrp-a5-adr-closeout`; base `b4e4632`). Superseded by LR-A5 completion note above.

> **Completion note (2026-07-10):** **LR-A6 → Done** (slice `lrp-a6-ooxml-gate`; merge `122d6d1` / `122d6d1f385bb28214373c63ef29740b0d447cb3`; worktree removed). OOXML output validation **fail-closed** gate delivered: `OoxmlOutputValidator` + `DocxAssembler` wiring + runtime error propagation (`OOXML_VALIDATION_FAILED`). Acceptance: structured well-formed accept; corrupt reject; corpus; runtime surfaces validation failure. **CD-HARD-T03 → Done** (executed-by-LR-A6). BDD **not-applicable**. Formal phase remains **None**. Wave LR-A remains **In Progress** (**A5** sole remaining — see activation note above). **Residuals (deferred):** ECMA-376 XSD; LO24 headless; ADR-0043 remains **Proposed**. **Gates:** `mvn verify` GREEN (1208+ tests); architecture-reviewer **PASS**; `docker-deploy-queue` DEPLOY_OK (healthz UP; UI 4173 200; image contains `OoxmlOutputValidator`); E2E skipped (not user-facing). **Task Master #12 → done**.

> **Activation note (2026-07-10):** **LR-A6 → In Progress** (slice `lrp-a6-ooxml-gate`; ISOLATED `D:/working/DGE-lrp-a6-ooxml-gate` · `feat/lrp-a6-ooxml-gate`; base `a806b4c`). Superseded by LR-A6 completion note above.

> **Completion note (2026-07-10):** **LR-A7 → Done** (slice `lrp-a7-pagination-measure`; merge `abf2048` / base `9a40b48`; worktree removed) **with documented exception**. Docker PDF corpus measurement gap **closed** (≥5 P23 letters + optional FOL; durable evidence). MS Word authoring baseline **unavailable** on measurement host → Word pages / delta columns remain **n/a** (`method=ms-word-unavailable-on-host`; not fabricated). True Word-vs-LO delta validation + ADR-0042 **Accepted** remain a **residual follow-up** (record under LR-A5/ADR-0042 — do **not** expand A5 Word residual or start LR-C9 for this residual; not a new In Progress task). **CD-HARD-T04 → Done** (executed-by-LR-A7) with same honesty note. BDD **not-applicable**. Formal phase remains **None**. Wave LR-A remains **In Progress** (**A5 Partial**; **LR-A6 Done** — see completion note above). Evidence: [pagination-delta-corpus.md](../pagination-delta-corpus.md); [docs/evidence/lrp-a7-pagination/](../../evidence/lrp-a7-pagination/); NFR §production rendering.

> **Activation note (2026-07-10):** **LR-A7 → In Progress** (slice `lrp-a7-pagination-measure`; ISOLATED `D:/working/DGE-lrp-a7-pagination-measure` · `feat/lrp-a7-pagination-measure`; base `9a40b48`). Superseded by LR-A7 completion note above.

> **Completion note (2026-07-10):** **LR-A4 → Done** (slice `lrp-a4-fail-closed-nodes`; merge `a523a09` / feature `4bccf9d`). Fail-closed publish/render for writer-unsupported nodes (`qrBarcodeRef`, `attachmentListRef`); no silent omit; full writers deferred. BDD **ready** (`BDD-LRP-A4-FAIL-CLOSED-001` A1–A9). Formal phase remains **None**. Optional follow-up (arch PASS_WITH_NOTES): pinned-module deep scan.

---

## 0. Problem statement

2026-07-03 inventory (evidence verified in program §1):

- `LibreOfficePdfConversionService` L63–71 launches `soffice` with **no `-env:UserInstallation`**; the conversion pool (`PdfConversionExecutorConfig`, default size 2) makes concurrent conversions share one LibreOffice profile — the industry's top intermittent-failure class for headless conversion (**CD-PIT-11**, added 2026-07-03).
- `backend/Dockerfile` / `backend/Dockerfile.packaged` ship only `ttf-dejavu` — no CJK, no Calibri/Cambria metric-compatible fonts (**CD-PIT-01**).
- Upload validation is filename-suffix only (`MasterDocumentService` L342–349); no multipart size caps anywhere.
- `StructuredContentNodeType` declares `qrBarcodeRef`/`attachmentListRef` with **no writer branch**; `StructuredContentDocxWriter` L225–226 silently drops `contentModuleRef` without pinned structure.

---

## 1. Task breakdown

### LR-A1 — LibreOffice per-invocation profile isolation + CLI hardening

- **Owner agent:** backend-engineer
- **BDD:** not-applicable — infrastructure fidelity fix; conversion output contract unchanged.
- **Read first:**
  1. `backend/src/main/java/com/bank/docgen/rendering/LibreOfficePdfConversionService.java`
  2. `backend/src/main/java/com/bank/docgen/rendering/DockerExecPdfConversionService.java`
  3. `backend/src/main/java/com/bank/docgen/rendering/LibreOfficeDocxNormalizationService.java`
  4. `backend/src/main/java/com/bank/docgen/infrastructure/config/PdfConversionExecutorConfig.java` + `DocgenRenderingProperties.java`
  5. [CDP-industry-pitfall-registry.md](./CDP-industry-pitfall-registry.md) — CD-PIT-11 (added 2026-07-03)
- **Do NOT:** Change conversion output semantics or the `pdfConversionExecutor` pool contract (COR-P02 Done); remove the Resilience4j wrapper; introduce a different conversion engine; touch `StructuredContentDocxWriter` (P22 territory).
- **Steps:**
  1. In `LibreOfficePdfConversionService.convertInternal`, create a unique temp profile dir per invocation (e.g. under the existing temp dir: `profile/`).
  2. Pass `-env:UserInstallation=file:///<abs-profile-path>` (URL-encoded, forward slashes) to the `soffice` command.
  3. Append hardening flags: `--norestore --nolockcheck --nodefault --nologo`.
  4. Delete the profile dir recursively in the existing `finally` block (best-effort, like current temp cleanup).
  5. Apply the same isolation to every other `soffice` CLI launch site found in step Read-first (docker-exec variant + DOCX normalization service) where a shared profile is possible.
  6. Add a regression test: submit **≥4 parallel conversions** through the pooled path and assert all succeed (integration test with a real `soffice` when available, otherwise a Docker smoke script documented in the evidence).
- **Acceptance (G/W/T):**
  - **G** conversion pool size ≥2 **W** ≥4 conversions run in parallel **T** all succeed with valid PDFs and zero shared-profile errors (no `dconf`/lock warnings causing failure).
  - **G** any single conversion completes or fails **W** the finally block runs **T** its profile dir and temp files are removed (no accumulation across 10 sequential runs).
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`
- **Artifacts:** modified `LibreOfficePdfConversionService.java` (+ sibling CLI launch sites); new parallel-conversion regression test under `backend/src/test/java/com/bank/docgen/rendering/`; Docker smoke note in ledger if used.
- **Done when:** Parallel regression green + `mvn verify` green + post-task-doc-sync (this row + ledger) + post-task-commit-review.
- **Maps:** CD-PIT-11; COR-P02 / OPT-F6 (pool already Done — this task hardens it).
- **Status:** **Done** (2026-07-09 — closed by **CORE-FORTRESS F4**; `LibreOfficeParallelConversionIntegrationTest` + profile isolation tests)

### LR-A2 — Font baseline (CJK + metric-compatible) in images + smoke test

> **P23 coordination:** **P23-T02** executes this task for demo typography acceptance. Record Done **once** in both LRP-A and P23 ledger rows.

- **Owner agent:** deploy-engineer + backend-engineer
- **BDD:** not-applicable — packaging/runtime asset baseline; rendering behavior contract unchanged (output becomes correct, not different).
- **Read first:**
  1. `backend/Dockerfile` and `backend/Dockerfile.packaged`
  2. [CDP-industry-pitfall-registry.md](./CDP-industry-pitfall-registry.md) — CD-PIT-01 + §4 ADR-0041 outline
  3. [CDP program](../competitiveness-deepening-program.md) §6 — CD-HARD-T01 row (this task executes it)
  4. `.cursor/rules/tech-stack-guardrails.mdc` — dependency policy
- **Do NOT:** Bake licensed Microsoft fonts into images; switch base image; claim pixel-identical Word parity (that is ADR-0042 / LR-A7 territory).
- **Steps:**
  1. Verify exact **Debian jammy** apt package names against the company-approved repository per dependency policy (shipped: `fonts-noto-cjk`, `fonts-crosextra-carlito`, `fonts-crosextra-caladea`). Early drafts mentioned Alpine names (`font-noto-cjk`, `font-carlito`) — **not** the production baseline; see ADR-0041 drift table. Record verification in ADR-0041 (LR-A5).
  2. Add the verified font packages + `fc-cache -f` to **both** `backend/Dockerfile` and `backend/Dockerfile.packaged`.
  3. Add a build-stage assertion: `fc-list :lang=zh` non-empty (fail the image build if CJK fonts are missing).
  4. Add `RenderingFontSmokeTest` (backend): render a sample DOCX containing Chinese text + Calibri-styled runs → PDF; assert expected page count and that extracted PDF text contains the Chinese sample (no tofu/`#`/empty extraction).
  5. Rebuild and redeploy: `.\scripts\docker-deploy.ps1`; generate one Chinese demo letter and archive the PDF as evidence.
  6. Mark CDP **CD-HARD-T01** as executed-by-LR-A2 in the CDP program doc (reference, not duplicate).
- **Acceptance (G/W/T):**
  - **G** the packaged backend image **W** `fc-list :lang=zh` runs inside it **T** ≥1 CJK font family is listed.
  - **G** a template with Chinese content **W** rendered to PDF in the Docker stack **T** extracted text contains the Chinese sample and page count matches the smoke baseline.
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; image build succeeds with font assertion; `.\scripts\docker-deploy.ps1` + manual generation smoke on `http://localhost:4173`.
- **Artifacts:** modified `backend/Dockerfile`, `backend/Dockerfile.packaged`; new `RenderingFontSmokeTest`; evidence PDF/screenshot reference in ledger.
- **Done when:** Smoke test green + Docker evidence archived + CD-HARD-T01 cross-referenced + doc sync + commit review.
- **Maps:** CD-PIT-01; CD-HARD-T01; ADR-0041 (drafted by LR-A5).
- **Status:** **Done** (2026-07-08; P23-T02 / CD-HARD-T01 executed — `fonts-noto-cjk`, `fonts-crosextra-carlito`, `fonts-crosextra-caladea` in both Dockerfiles; `fc-list :lang=zh` build assertion; `RenderingFontSmokeTest` green in `mvn verify`; gates BUILD SUCCESS)

### LR-A3 — Upload deep validation + size limits (**gap-close**, not greenfield)

- **Owner agent:** backend-engineer (+ deploy-engineer for nginx)
- **Status:** **Done** (2026-07-10 — slice `lrp-a3-upload-validation`; merge `e62c210` / feature `9d5a270`; formal phase **None**)
- **BDD:** **ready** — [docs/behavior/lrp-a3-master-docx-upload-validation.md](../../behavior/lrp-a3-master-docx-upload-validation.md) (`BDD-LRP-A3-UPLOAD-001` v1.0.0); scenarios A1–A7 green via unit + E2E
- **Nature:** **Gap-close** against partial implementation already in tree. Confirmed defaults: **50MB** file / **60MB** request + nginx. MessageKey for corrupt package: `api.error.master.docxCorrupt` (no `invalidDocxContent`). Virus scan = **non-goal** (pending Q — remains open).
- **Delivered (gap-close):**
  - `validateDocxFile`: suffix, Content-Type whitelist, service-level size (`docgen.master.max-docx-upload-bytes` 50MB), ZIP magic + OPC required entries → `docxCorrupt`
  - Spring multipart `max-file-size: 50MB` / `max-request-size: 60MB` + oversize → unified envelope (`docxTooLarge`)
  - nginx `client_max_body_size 60m` + 413 → readable UI mapping
  - i18n keys `docxRequired` / `docxTooLarge` / `docxCorrupt`
  - create + replace dialog client precheck; dedicated magic-byte unit test
- **Gates (GREEN):** `mvn -B -ntp -f backend/pom.xml verify`; `pnpm -C frontend lint` / `type-check` / `test` / `build`; E2E `frontend/e2e/LRP-A3-master-docx-upload-validation.spec.ts` **5/5**; UIUX PASS; architecture PASS; `docker-deploy-queue.ps1` DEPLOY_OK (:8080/:4173)
- **Maps:** program §1 finding 6; launch-readiness-gate LR-A3 checkbox.

### LR-A4 — Unsupported-node fail-closed closure

- **Owner agent:** backend-engineer
- **BDD:** **ready** (2026-07-10) — [docs/behavior/lrp-a4-fail-closed-unsupported-nodes.md](../../behavior/lrp-a4-fail-closed-unsupported-nodes.md) (`BDD-LRP-A4-FAIL-CLOSED-001` v1.0.0)
- **Locked decision (user 2026-07-10):** **(b) hard-block at publish gate** for writer-unsupported declared types (`qrBarcodeRef`, `attachmentListRef`); **do not** implement full writers in this slice; **no silent omit** at publish/render (incl. nested paths). Empty `contentModuleRef` pinned remains F1 fail-closed (`CONTENT_MODULE_STRUCTURE_MISSING`) — not “warning-only”.
- **Depends on:** **P22-T01/T02 Done** (writer + style catalog stabilized) — verify in [P22 detail](./P22-demo-expansion-rendering-fidelity.md) before starting.
- **Read first:**
  1. `backend/src/main/java/com/bank/docgen/authoring/structured/StructuredContentNodeType.java` (`qrBarcodeRef`, `attachmentListRef`)
  2. `backend/src/main/java/com/bank/docgen/rendering/StructuredContentDocxWriteSession.java` (top-level unsupported throw; nested silent-omit gap)
  3. `PublishGateService` + `NodeMatrixValidationService` + `ReferenceNodeService`
  4. P18 node matrix + fidelity warning model ([P18 detail](./P18-structured-authoring-fidelity-engine.md))
  5. [CDP-industry-pitfall-registry.md](./CDP-industry-pitfall-registry.md) — CD-PIT-07
  6. Behavior spec above (A1–A9)
- **Do NOT:** Invent new node types; reopen P18/P22/F1 phase status; weaken existing UNSUPPORTED_NODE validation; implement QR/attachment writers here; allow warning-only publish for writer-unsupported nodes.
- **Steps (implementation — after this BDD):**
  1. Single authoritative writer-unsupported set shared by validation + writer.
  2. Binding / node-matrix blockers for that set; extend `PublishGateService` (dedicated check **or** equivalent ANCHOR_INTEGRITY/BLOCKER_STATUS surfacing — see LR-A4-C5).
  3. Close nested silent-omit in `StructuredContentDocxWriteSession` (condition/loop/module/inline paths).
  4. Keep top-level render throw + F1 empty-pinned fail-closed; add A1–A9 tests.
- **Acceptance (G/W/T):** see behavior spec §10 (A1–A9).
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; Docker redeploy + manual checklist evidence when implementing.
- **Artifacts:** behavior spec (**Done**); modified writer/publish gate + tests; PRD/domain note (**Done** in BDD persist).
- **Done when:** Scenarios green + gates green + doc sync + commit review (implementation slice).
- **Maps:** CD-PIT-07; P18/P22; F1-A2 regression; ledger seams «Structured content DOCX write».
- **Status:** **Done** (2026-07-10 — slice `lrp-a4-fail-closed-nodes`; merge `a523a09` / feature `4bccf9d`; Task Master #10)
- **Delivered:** `WriterUnsupportedStructuredNodeTypes` + `DocxWriterHandledStructuredNodeTypes`; publish-gate hard-block; nested silent-omit closed in `StructuredContentDocxWriteSession`; A1–A9 tests green. Full QR/attachment writers **deferred**.
- **Gates (GREEN):** `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS; architecture-reviewer **PASS_WITH_NOTES**; `docker-deploy-queue.ps1` exit 0 (healthz 8080 UP; UI 4173 200; compose `documentgenerationengine`).
- **Deferred / optional:** virus scan (non-goal); full QR/attachment writers; pinned-module deep scan (arch note); CD-E2E T02–T12; Word-vs-LO delta on Word-equipped host (LR-A7 residual under ADR-0042); DGE-audit-governance.

### LR-A5 — ADR-0041/0042/0043 drafting

- **Status:** **Done** (2026-07-10 — slice `lrp-a5-adr-closeout`; docs-only; merge `cc9e5f6`)
- **Owner agent:** doc-keeper (+ architecture-reviewer for any Accepted promotion)
- **BDD:** not-applicable — documentation/decision records only.
- **Delivered:**
  1. Authored `docs/adr/rendering-authoring/0041-rendering-font-baseline.md` — **Accepted** (architecture-reviewer PASS_WITH_NOTES 2026-07-10; cites LR-A2 / P23-T02 / CD-HARD-T01 Debian jammy `fonts-noto-cjk`, `fonts-crosextra-carlito`, `fonts-crosextra-caladea`; `RenderingFontSmokeTest`; Alpine vs jammy package-name drift recorded).
  2. Reconciled ADR-0042 — remains **Proposed**; Word-vs-LO residual strengthened (`ms-word-unavailable-on-host`; evidence paths; no invented Word numbers).
  3. ADR-0043 — remains **Proposed** (A6 slice A Done; ECMA-376 XSD / LO24 deferred); triad cross-refs added.
  4. ADR index + LRP-A / program / ledger synced; triad no longer Missing.
- **Statuses decided (doc-keeper + architecture-reviewer):** 0041 **Accepted** (2026-07-10 sign-off). 0042/0043 remain **Proposed**. Do **not** Accept 0042 without Word-equipped host. Do **not** Accept 0043 on well-formedness alone.
- **Residual (deferred out of Wave LR-A exit — not blocking A5/Wave Done):** Word page baselines + Word-vs-LO deltas on a Word-equipped host → then consider ADR-0042 Accepted / enforcement; ADR-0043 slice B (XSD/LO24). ADR-0041 Accepted residual **closed** by architecture-reviewer 2026-07-10. Do not invent numbers; do not treat these residuals as LR-C9 scope (LR-C9 is a separate usability slice).
- **Maps:** CD-PIT-01/02/03; LR-A2/A6/A7 consume these decisions.

### LR-A6 — OOXML output validation gate

- **Owner agent:** backend-engineer
- **BDD:** not-applicable — test/CI gate only; no user-facing behavior change.
- **Depends on:** P22-T01 Done (writer output stabilized).
- **Read first:**
  1. [CDP program](../competitiveness-deepening-program.md) §6 — **CD-HARD-T03** row (this task executes it)
  2. [CDP-industry-pitfall-registry.md](./CDP-industry-pitfall-registry.md) — CD-PIT-03
  3. `backend/src/main/java/com/bank/docgen/rendering/StructuredContentDocxWriter.java` + existing rendering tests
- **Do NOT:** Change writer behavior (only add validation); introduce non-POI validation dependencies without dependency-policy verification.
- **Steps:**
  1. Add a regression test class that runs generated DOCX artifacts (structured content incl. special characters `& < > "` and CJK) through `OPCPackage.open` + POI schema/content-type validation.
  2. Include a corpus case per structured node family (paragraph/list/table/module/image) once P22-T01 output is green.
  3. Wire into `mvn verify` (normal Surefire/Failsafe run — no separate profile).
  4. Mark CDP **CD-HARD-T03** as executed-by-LR-A6 (reference, not duplicate).
- **Acceptance (G/W/T):**
  - **G** a generated DOCX containing `&`, `<`, CJK and nested nodes **W** the validation test opens it via `OPCPackage.open` **T** it opens cleanly with valid package parts (no malformed XML).
  - **G** a deliberately corrupted fixture (unescaped `&` injected) **W** the same validation runs **T** the test fails — proving the gate detects the CD-PIT-03 class.
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`
- **Artifacts:** new validation test class + fixtures under `backend/src/test/`.
- **Done when:** Gate active in verify + CD-HARD-T03 cross-referenced + doc sync + commit review.
- **Maps:** CD-PIT-03; ADR-0043 (LR-A5).
- **Status:** **Done** (2026-07-10 — slice `lrp-a6-ooxml-gate`; merge `122d6d1`; CD-HARD-T03 Done executed-by-LR-A6; Task Master #12)
- **Delivered:** `OoxmlOutputValidator` + `DocxAssembler` fail-closed gate; runtime `OOXML_VALIDATION_FAILED` propagation; well-formed accept / corrupt reject / corpus coverage.
- **Gates (GREEN):** `mvn -B -ntp -f backend/pom.xml verify` (1208+ tests); architecture-reviewer **PASS**; `docker-deploy-queue.ps1` DEPLOY_OK (healthz `:8080` UP; UI `:4173` 200; backend image contains `OoxmlOutputValidator`); E2E skipped (not user-facing).
- **Residuals (deferred — not blocking Done):** ECMA-376 XSD validation; LO24 headless open proof; ADR-0043 remains **Proposed** (do not mark Accepted without architecture-reviewer).

### LR-A7 — Pagination delta budget + corpus

- **Owner agent:** doc-keeper (+ backend-engineer for extraction tooling if needed)
- **BDD:** not-applicable — measurement + documentation baseline.
- **Depends on:** **P23** demo packages available (**P23-T04+** Done for ≥5 letter-grade types) — see [P23 detail](./P23-demo-typography-layout-excellence.md) §7
- **Read first:**
  1. [CDP program](../competitiveness-deepening-program.md) §6 — **CD-HARD-T04** row (this task executes it)
  2. [CDP-industry-pitfall-registry.md](./CDP-industry-pitfall-registry.md) — CD-PIT-02
  3. ADR-0042 draft (LR-A5)
- **Do NOT:** Promise Word-identical pagination; block launch on deltas within the recorded budget.
- **Steps:**
  1. Assemble a corpus of ≥5 demo letters (from **P23** packages) with a manual Word page-count baseline per letter.
  2. Generate PDFs through the Docker stack; record page counts + notable layout drift.
  3. Compute deltas; propose the acceptance budget in ADR-0042 (finalize its Proposed → review cycle).
  4. Document the corpus + rerun procedure in NFR §production rendering; wire budget breach → fidelity-warning follow-up as a recorded future task (not implemented here).
  5. Mark CDP **CD-HARD-T04** as executed-by-LR-A7.
- **Acceptance (G/W/T):**
  - **G** the ≥5-letter corpus **W** deltas are measured **T** every letter has baseline, actual, delta recorded in NFR with generation date + stack version.
  - **G** ADR-0042 review completes **W** the budget is accepted **T** LR-E2 checklist can reference a concrete numeric budget (no «TBD»).
- **Gates:** Doc-only + Docker generation evidence (`.\scripts\docker-deploy.ps1`).
- **Artifacts:** NFR §production rendering corpus table; ADR-0042 finalized budget; evidence PDFs referenced in ledger.
- **Done when:** Corpus + budget merged + CD-HARD-T04 cross-referenced + doc sync + commit review.
- **Maps:** CD-PIT-02; ADR-0042.
- **Status:** **Done** (2026-07-10 — documented exception: Docker PDF corpus closed; Word pages/delta n/a until Word-equipped host; ADR-0042 remains Proposed; CD-HARD-T04 Done executed-by-LR-A7)
- **Residual (not a new In Progress task):** Re-measure Word baselines + Word-vs-LO deltas on a Word-equipped host; only then consider ADR-0042 Accepted / enforcement. Track under LR-A5 / ADR-0042 notes — do not expand LR-A6 or LR-C9 for this residual.

---

## 2. Exit gate (Wave LR-A)

- [x] LR-A1 Done (2026-07-09 — F4); LR-A2 Done (2026-07-08 — P23); **LR-A3 Done** (2026-07-10 gap-close; merge `e62c210`); **LR-A4 Done** (2026-07-10; merge `a523a09`); **LR-A5 Done** (2026-07-10 — `lrp-a5-adr-closeout`; triad on disk; **ADR-0041 Accepted** via architecture-reviewer); **LR-A6 Done** (2026-07-10 — merge `122d6d1`; CD-HARD-T03 executed-by-LR-A6); **LR-A7 Done** (2026-07-10 — Docker PDF corpus; Word delta residual deferred out of wave exit)
- [x] LR-A4 Done (writer-unsupported fail-closed; full writers deferred)
- [x] LR-A6 Done (2026-07-10 — `lrp-a6-ooxml-gate`; merge `122d6d1`)
- [x] Writer-unsupported declared nodes (`qrBarcodeRef`/`attachmentListRef`) cannot silently disappear (publish hard-block + render fail-closed) — full writers still deferred
- [x] Ledger § LRP wave row updated with A5 gate evidence (docs-only; 2026-07-10)
- [x] **Wave LR-A Done** — Word-vs-LO / ADR-0042 Accepted and ADR-0043 slice B (XSD/LO24) are **explicitly deferred out of wave exit**; ADR-0041 Accepted residual **closed** (architecture-reviewer PASS_WITH_NOTES 2026-07-10). Post-wave follow-ups are not LR-C9; not a new In Progress task unless scheduled
