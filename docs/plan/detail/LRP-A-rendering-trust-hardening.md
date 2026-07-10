# LRP Wave LR-A — Rendering Trust Chain & File Safety 「渲染信任链与文件安全」

**Program:** [launch-readiness-program.md](../launch-readiness-program.md)  
**Wave status:** **In Progress** (activated 2026-07-04 after LR-B closure; core **A1/A2 Done**; **A3 In Progress** 2026-07-10 — gap-close)  
**Owner default:** `backend-engineer` (+ `deploy-engineer` for images, `doc-keeper` for ADRs)  
**Prerequisites:** none for A1/A2/A3/A5; **A4/A6 depend on P22-T01/T02 Done**; **A7 depends on P23 demo packages (T04+ letter-grade)** — track via [P23 detail](./P23-demo-typography-layout-excellence.md)

> **Session note (2026-07-10):** Active delivery slice = **`lrp-a3-upload-validation` / LR-A3** only. Formal phase remains **None**. Do **not** pick up `P22-*`, `CD-*`, or **LR-A4**. Where a task executes a CD-HARD task (A2→CD-HARD-T01, A6→CD-HARD-T03, A7→CD-HARD-T04), update the CDP row by reference — do not fork status.

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
  1. Verify exact Alpine package names against the company-approved repository per dependency policy (candidates: `font-noto-cjk` for CJK; `font-carlito` Calibri-metric-compatible; Caladea/Cambria-metric equivalent if available). Record the verification result in the ADR-0041 draft (LR-A5).
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
- **Status:** **In Progress** (2026-07-10 — slice `lrp-a3-upload-validation`; formal phase **None**)
- **BDD:** **ready** — [docs/behavior/lrp-a3-master-docx-upload-validation.md](../../behavior/lrp-a3-master-docx-upload-validation.md) (`BDD-LRP-A3-UPLOAD-001` v1.0.0)
- **Nature:** **Gap-close** against partial implementation already in tree — do **not** rewrite from zero. Confirmed defaults: **50MB** file / **60MB** request + nginx. MessageKey for corrupt package: existing `api.error.master.docxCorrupt` (do **not** add `invalidDocxContent`). Virus scan = **non-goal** (pending Q).
- **Partial Done already (keep):**
  - `validateDocxFile`: suffix, Content-Type whitelist, service-level size (`docgen.master.max-docx-upload-bytes` 50MB), ZIP magic + OPC required entries → `docxCorrupt`
  - Spring multipart `max-file-size: 50MB` / `max-request-size: 60MB`
  - nginx `client_max_body_size 60m`
  - i18n keys `docxRequired` / `docxTooLarge` / `docxCorrupt`
  - create-dialog client precheck (`MasterUploadDialog`)
- **Open gaps (this slice):**
  1. Dedicated **magic-byte unit test** (corrupt fixtures today cover missing OPC entries more than signature alone)
  2. **Replace-dialog** client precheck (`MasterReplaceFileDialog` — align with create dialog / shared composable)
  3. Spring **multipart oversize → unified JSON envelope** handler (`MaxUploadSizeExceededException` / `MultipartException` → `docxTooLarge`, 413 or 422)
  4. nginx **413 → readable UI** mapping (no raw HTML as primary error surface)
- **Read first:**
  1. BDD spec above (§2 current-vs-target matrix)
  2. `MasterDocumentService.validateDocxFile` / `assertDocxPackageStructure`
  3. `application.yml` multipart + `docgen.master.max-docx-upload-bytes`
  4. `frontend/nginx.conf` `client_max_body_size`
  5. `.cursor/skills/i18n-english-first/SKILL.md`
- **Do NOT:** Implement virus scanning; expand to **LR-A4**; change upload API shape; reject valid Word/LibreOffice `.docx`; mark Done before gap scenarios green.
- **Acceptance:** BDD scenarios in the ready spec (G/W/T) + gates below.
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; `pnpm -C frontend lint` / `type-check` / `test` / `build`; Docker redeploy + upload smoke when required by pipeline.
- **Done when:** All open gaps closed + BDD scenarios green + gates green + pending virus-scan Q recorded + doc sync + commit review.
- **Maps:** program §1 finding 6; launch-readiness-gate LR-A3 checkbox.

### LR-A4 — Unsupported-node fail-closed closure

- **Owner agent:** backend-engineer
- **BDD:** **required** — publish-gate blocking and fidelity warnings are user-visible behavior.
- **Depends on:** **P22-T01/T02 Done** (writer + style catalog stabilized) — verify in [P22 detail](./P22-demo-expansion-rendering-fidelity.md) before starting.
- **Read first:**
  1. `backend/src/main/java/com/bank/docgen/authoring/structured/StructuredContentNodeType.java` (`qrBarcodeRef`, `attachmentListRef`)
  2. `backend/src/main/java/com/bank/docgen/rendering/StructuredContentDocxWriter.java` (esp. `expandContentModule` L222–234 silent return)
  3. P18 node matrix + fidelity warning model ([P18 detail](./P18-structured-authoring-fidelity-engine.md))
  4. [CDP-industry-pitfall-registry.md](./CDP-industry-pitfall-registry.md) — CD-PIT-07
- **Do NOT:** Invent new node types; reopen P18/P22 phase status; weaken existing UNSUPPORTED_NODE validation to make rendering pass.
- **Steps:**
  1. Wait for BDD spec `ready` covering both decisions (render vs hard-block; warning vs silence).
  2. Decide per node with the user via spec: `qrBarcodeRef`/`attachmentListRef` either (a) get a writer implementation, or (b) are **hard-blocked at the publish gate** when present (fail-closed) — record the decision in the spec + PRD note.
  3. Implement the chosen path; if (b), extend `PublishGateService` checks + gate UI copy (existing pattern, COR-T01).
  4. Change `StructuredContentDocxWriter.expandContentModule`: missing/blank pinned structure emits a **fidelity warning** (existing warning channel) instead of silent return.
  5. Tests: publish blocked (or node rendered), fidelity warning emitted, regression on existing module expansion.
- **Acceptance (G/W/T):**
  - **G** a template whose structure contains a node with no writer support **W** publish is attempted **T** publish is blocked with a visible checklist reason (or the node renders correctly, per recorded decision) — never silent loss.
  - **G** a `contentModuleRef` whose pinned structure is absent **W** DOCX is generated **T** output carries a fidelity warning naming the reference key; generation does not silently omit content without trace.
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; Docker redeploy + one manual generation showing the warning surface.
- **Artifacts:** behavior spec; modified writer/publish gate + tests; PRD/domain note for the decision.
- **Done when:** Decision recorded + scenarios green + gates green + doc sync + commit review.
- **Maps:** CD-PIT-07; P18/P22; ledger seams «Structured content DOCX write» adjacency (closure itself stays with P22).
- **Status:** Not Started

### LR-A5 — ADR-0041/0042/0043 drafting

- **Status:** **Partial** (2026-07-10 plan pass) — `0042-pagination-delta-budget.md` and `0043-ooxml-output-validation-gate.md` exist as **Proposed**; **`0041-rendering-font-baseline.md` is missing** (still referenced by ADR-0042). Closing the full LR-A5 triad (draft 0041 + index + architecture-reviewer) is **deferred** — not expanded in the LR-A3 delivery slice.
- **Owner agent:** doc-keeper (+ architecture-reviewer sign-off)
- **BDD:** not-applicable — documentation/decision records only.
- **Read first:**
  1. [CDP-industry-pitfall-registry.md](./CDP-industry-pitfall-registry.md) §4 (ADR outlines + target paths)
  2. `docs/adr/0000-template.md` + `docs/adr/README.md` (metadata taxonomy)
  3. Existing drafts: `docs/adr/rendering-authoring/0042-pagination-delta-budget.md`, `0043-ooxml-output-validation-gate.md`
- **Do NOT:** Mark ADRs Accepted without architecture-reviewer review; edit accepted ADR decisions; expand into LR-A3 implementation work.
- **Remaining steps (deferred):**
  1. Draft missing `docs/adr/rendering-authoring/0041-rendering-font-baseline.md` (cite LR-A2 package verification).
  2. Confirm ADR index reachability for 0041–0043.
  3. Request architecture-reviewer review; record outcome.
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
- **Status:** Not Started

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
- **Status:** **In Progress** (2026-07-09 — **F4 subset Done**: NFR §production rendering corpus schema + rerun procedure; row measurements _待测_ until Docker redeploy)

---

## 2. Exit gate (Wave LR-A)

- [x] LR-A1 Done (2026-07-09 — F4); LR-A2 Done (2026-07-08 — P23); **LR-A3 In Progress** (2026-07-10 gap-close); LR-A5 **Partial** (0042/0043 on disk; 0041 deferred)
- [ ] LR-A4/A6 Done once P22-T01/T02 Done; LR-A7 Done once **P23** letter-grade demo corpus measurements land
- [ ] No structured node can silently disappear from generated DOCX (blocked or warned)
- [ ] Ledger § LRP wave row updated with gate evidence per task
