# LRP Wave LR-A — Rendering Trust Chain & File Safety 「渲染信任链与文件安全」

**Program:** [launch-readiness-program.md](../launch-readiness-program.md)  
**Wave status:** **In Progress** (activated 2026-07-04 after LR-B closure; core **A1/A2/A3** first — all independent of P22)  
**Owner default:** `backend-engineer` (+ `deploy-engineer` for images, `doc-keeper` for ADRs)  
**Prerequisites:** none for A1/A2/A3/A5; **A4/A6 depend on P22-T01/T02 Done**; **A7 depends on P23 demo packages (T04+ letter-grade)** — track via [P23 detail](./P23-demo-typography-layout-excellence.md)

> **Session note:** `LR-A*` tasks only. Do **not** pick up `P22-*` (rendering write path — other session) or `CD-*` (CDP session). Where a task executes a CD-HARD task (A2→CD-HARD-T01, A6→CD-HARD-T03, A7→CD-HARD-T04), update the CDP row by reference — do not fork status.

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
- **Status:** Not Started

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
- **Status:** Not Started

### LR-A3 — Upload deep validation + size limits

- **Owner agent:** backend-engineer (+ deploy-engineer for nginx)
- **BDD:** **required** — rejection behavior (422 + messageKey) is user-visible; `behavior-spec-author` must publish a `ready` spec in `docs/behavior/` first.
- **Read first:**
  1. `backend/src/main/java/com/bank/docgen/master/service/MasterDocumentService.java` L342–349 (`validateDocxFile`)
  2. All `MultipartFile` entry points (inventory via search) — master upload/replace first
  3. `backend/src/main/resources/application.yml` (no `spring.servlet.multipart` block today)
  4. `frontend/nginx.conf` (no `client_max_body_size` today)
  5. `.cursor/skills/i18n-english-first/SKILL.md` — messageKey workflow
- **Do NOT:** Implement virus scanning (record as **pending question** in `docs/requirements/requirements-plan.md` open questions — do not implement); block valid `.docx` produced by Word/LibreOffice; change the upload API shape.
- **Steps:**
  1. Wait for BDD spec `ready` (actor: master designer; trigger: upload/replace DOCX; boundary: wrong magic bytes, oversized file, corrupt package).
  2. Add content probing to `validateDocxFile`: ZIP magic bytes (`PK\x03\x04`) + `OPCPackage.open` probe (reject with a **new** messageKey e.g. `api.error.master.invalidDocxContent` on failure); keep existing suffix check.
  3. Configure `spring.servlet.multipart.max-file-size` / `max-request-size` in `application.yml` (externalized via env with sane defaults, e.g. 20MB/25MB — confirm defaults in BDD spec).
  4. Add `client_max_body_size` to `frontend/nginx.conf` `/api/` location, aligned with backend limit.
  5. Add the new messageKey to `backend/src/main/resources/i18n/messages_en.properties` (English base) and frontend `apiErrorEn.ts` + `apiErrorZhCn.ts`.
  6. Tests: magic-byte reject, corrupt-zip reject, oversize reject (413/422 per spec), happy-path unchanged.
  7. Record the virus-scanning pending question with owner + date.
- **Acceptance (G/W/T):**
  - **G** a file renamed to `.docx` that is not a ZIP/OPC package **W** uploaded as master **T** 422 with the new messageKey; nothing stored.
  - **G** a file larger than the configured limit **W** uploaded through the 4173 proxy **T** rejected at nginx or backend with a translated, user-readable error (no raw 413 HTML).
  - **G** a genuine Word-produced `.docx` **W** uploaded **T** accepted exactly as before.
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; `pnpm -C frontend lint` / `type-check` / `test` / `build`; Docker redeploy + manual upload smoke.
- **Artifacts:** modified `MasterDocumentService.java`, `application.yml`, `frontend/nginx.conf`, i18n catalogs; new behavior spec in `docs/behavior/`; tests.
- **Done when:** BDD scenarios green + gates green + pending question recorded + doc sync + commit review.
- **Maps:** program §1 finding 6.
- **Status:** Not Started

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

- **Owner agent:** doc-keeper (+ architecture-reviewer sign-off)
- **BDD:** not-applicable — documentation/decision records only.
- **Read first:**
  1. [CDP-industry-pitfall-registry.md](./CDP-industry-pitfall-registry.md) §4 (ADR outlines + target paths)
  2. `docs/adr/0000-template.md` + `docs/adr/README.md` (metadata taxonomy)
  3. [CDP program](../competitiveness-deepening-program.md) §8 (CD-0 exit gate marks these drafts «optional»)
- **Do NOT:** Mark ADRs Accepted without architecture-reviewer review; edit accepted ADR decisions; renumber existing ADRs (0040 is the latest accepted; 0044 is reserved by LR-B1).
- **Steps:**
  1. Draft `docs/adr/rendering-authoring/0041-rendering-font-baseline.md` (required font bundle in deploy images; cite LR-A2 package verification).
  2. Draft `docs/adr/rendering-authoring/0042-pagination-delta-budget.md` (acceptable Word-vs-LO page-count delta for v1; consumed by LR-A7).
  3. Draft `docs/adr/rendering-authoring/0043-ooxml-output-validation.md` (OOXML validation gate in CI; consumed by LR-A6).
  4. Status **Proposed**; request architecture-reviewer review; record outcome.
  5. Update ADR index + [CDP program](../competitiveness-deepening-program.md) §8: the «optional before CD-0 close» line now reads **required for LRP launch readiness** (LR-E2 checklist input).
- **Acceptance (G/W/T):**
  - **G** the three drafts exist **W** ADR index is rebuilt **T** each is reachable from `docs/adr/README.md` with status Proposed and correct topic directory.
  - **G** LR-E2 builds the launch checklist **W** it references font/pagination/OOXML gates **T** each resolves to one of these ADRs (no dangling anchor).
- **Gates:** Doc-only — relative links resolve; architecture-reviewer review recorded.
- **Artifacts:** three ADR drafts; ADR index row updates; CDP §8 one-line amendment.
- **Done when:** Drafts merged + reviewed + indexed + doc sync + commit review.
- **Maps:** CD-PIT-01/02/03; LR-A2/A6/A7 consume these decisions.
- **Status:** Not Started

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
- **Status:** Not Started

---

## 2. Exit gate (Wave LR-A)

- [ ] LR-A1/A2/A3 Done (schedulable now); LR-A5 ADR drafts reviewed
- [ ] LR-A4/A6 Done once P22-T01/T02 Done; LR-A7 Done once **P23** letter-grade demo corpus exists (≥5 types)
- [ ] No structured node can silently disappear from generated DOCX (blocked or warned)
- [ ] Ledger § LRP wave row updated with gate evidence per task
