# LRP Wave LR-C — Business Usability Deepening 「业务易用性深化」

**Program:** [launch-readiness-program.md](../launch-readiness-program.md)  
**Wave status:** **In Progress** (partial — LR-C1 + LR-C4 **Done** via CORE-FORTRESS F7 2026-07-09; **LR-C9 Done** 2026-07-10 — slice `lrp-c9-load-error-panel`; **LR-C10 Done** 2026-07-11 — slice `lrp-c10-upload-ux`; **LR-C11 Done** 2026-07-11 — slice `lrp-c11-api-error-i18n`; **LR-C12 Done** 2026-07-11 — slice `lrp-c12-keyboard-a11y`; **LR-C13 Done** 2026-07-11 — slice `lrp-c13-frontend-eng-debt`; **LR-C2 Done** 2026-07-11 — slice `lrp-c2-local-draft-recovery`; **LR-C3 Done** 2026-07-11 — slice `lrp-c3-editor-undo-redo`; **LR-C5 Done** 2026-07-11 — slice `lrp-c5-catalog-pagination`; remaining C6/C7/C8 Not Started)  
**Owner default:** `frontend-engineer` (+ `backend-engineer` where noted); every user-facing slice pairs `e2e-test-engineer` + `e2e-uiux-reviewer`  
**Prerequisites:** none for most tasks; **C6 depends on LR-C5**; **C7 depends on P14 (Done)**; **C10 aligns copy with LR-A3**

> **Completion note (2026-07-11):** **LR-C5 → Done** (slice `lrp-c5-catalog-pagination`; merge `5543a33` / `5543a335c8480f7ca3e6b4d8ef2e57f26f88f003` → `main`; feature `cfab79e` / `cfab79e45f81405e5877c78097ded582ce0d41c2`; worktree removed). Catalog server-side pagination/filter for templates/masters/content-modules; COR-F09 row+group-first default sort; dashboard/picker consumers fixed. BDD **`ready`** (`docs/behavior/lrp-c5-catalog-pagination.md` — `BDD-LRP-C5-CATALOG-001`; scenarios BDD-LRP-C5-001…015). Formal phase remains **None**. **No sole-active LRP slice.** Wave LR-C remains **In Progress** (partial — C1/C4/C9–C13 + **C2** + **C3** + **C5** Done; C6/C7/C8 Not Started). Do **not** mark Wave LR-C Done. Do **not** activate C6/C7/C8/CD-3. **Gates:** `mvn -B -ntp -f backend/pom.xml verify` **GREEN**; `pnpm -C frontend lint && type-check && test && build` **GREEN** (1054+ tests); `docker-deploy-queue.ps1` **DEPLOY_OK** (`:8080` UP, `:4173` 200; seed 500 LOAD-TPL; templates `totalElements=515`); E2E LRP-C5-catalog-pagination **6/6** PASS (re-smoke); UIUX **PASS_WITH_NOTES** (`frontend/e2e/evidence/LRP-C5-uiux-manifest.md`); perf p95 ~75ms (`frontend/e2e/evidence/LRP-C5-list-latency.json`); architecture **PASS_WITH_NOTES** (Critical **0**; Majors dashboard/pickers fixed before merge). **Task Master #31 → done**. OPT-F4 residual closed. Recommend next: **LR-C6** (depends on C5, now unblocked) **OR** **LR-C7** — do not auto-activate.
>
> **Activation note (2026-07-11):** **LR-C5 → In Progress** (slice `lrp-c5-catalog-pagination`; formal phase remains **None**). Sole-active LRP slice — catalog server-side pagination/filter (templates/masters/content-modules); OPT-F4 residual; COR-F09 group-first semantics kept. BDD later **`ready`** (see completion note above). Placement: ISOLATED `D:/working/DGE-lrp-c5-catalog-pagination` · `feat/lrp-c5-catalog-pagination` · base `e860256` (`e86025624bcc11f25ce2228cf1ed69e32d7290ef`). Gate evidence: [] (now **done** — see completion note above). **Task Master #31 → in-progress** (now **done**). Other LR-C rows untouched at activation (C6/C7/C8 Not Started; C1/C2/C3/C4/C9–C13 Done). Do **not** mark Wave LR-C Done. Do **not** activate C6/C7/C8/CD-3 (C6 depends on C5 — leave Not Started).
>
> **Completion note (2026-07-11):** **LR-C3 → Done** (slice `lrp-c3-editor-undo-redo`; merge `0cf553b` / `0cf553bef5dc17cda17bca4a824427b2a1ca2031` → `main`; worktree removed). Structure-level undo/redo (snapshot history cap 50) in `ControlledStructuredContentEditor`; keyboard + toolbar; C2 draft separation (undo stack never in draft blob). BDD **`ready`** (`docs/behavior/lrp-c3-editor-undo-redo.md` — `BDD-LRP-C3-UNDO-001`; scenarios BDD-LRP-C3-001…014). Formal phase remains **None**. Superseded sole-active by LR-C5 (now Done — see completion note above). Wave LR-C remains **In Progress** (partial — C1/C4/C9–C13 + **C2** + **C3** + **C5** Done; C6/C7/C8 Not Started). Do **not** mark Wave LR-C Done. Do **not** activate C6/C7/C8/CD-3. **Gates:** `pnpm -C frontend lint && type-check && test && build` **GREEN** (1036 tests); `docker-deploy-queue.ps1` **DEPLOY_OK** (`:8080` UP, `:4173` 200); E2E LRP-C3-undo-redo **7/7** PASS; UIUX **PASS_WITH_NOTES** (`frontend/e2e/evidence/LRP-C3-uiux-manifest.md`); architecture **PASS_WITH_NOTES** (Critical **0**). **Task Master #30 → done**.
>
> **Activation note (2026-07-11):** **LR-C3 → In Progress** (slice `lrp-c3-editor-undo-redo`; formal phase remains **None**). Sole-active LRP slice — structure-level undo/redo (snapshot history, cap 50) in `ControlledStructuredContentEditor`; keyboard + toolbar; drafts must **NOT** include undo stack (C2 separation). BDD **`ready`** (`docs/behavior/lrp-c3-editor-undo-redo.md` — `BDD-LRP-C3-UNDO-001`; scenarios BDD-LRP-C3-001…014). Placement: ISOLATED `D:/working/DGE-lrp-c3-editor-undo-redo` · `feat/lrp-c3-editor-undo-redo` · base `32ff70a`. Gate evidence: [] (now **done** — see completion note above). **Task Master #30 → in-progress** (now **done**). Other LR-C rows untouched (C5/C7/C8 Not Started; C1/C2/C4/C9–C13 Done). Do **not** mark Wave LR-C Done. Do **not** activate C5/C7/C8/CD-3.
>
> **Completion note (2026-07-11):** **LR-C2 → Done** (slice `lrp-c2-local-draft-recovery`; merge `12a6a7e` / `12a6a7e63f5f873f824335ee3b365e69be3f29db` → `main`; feature `12a6a7e`; worktree removed). Structured editor local draft recovery: debounced localStorage drafts keyed by template id + dev version id + user; recovery banner «Restore draft / Discard» with timestamps; clear on successful save; storage quota guard; BDD-002 soft-refresh after Critical fixes. BDD **`ready`** (`docs/behavior/lrp-c2-structured-editor-local-draft-recovery.md` — `BDD-LRP-C2-DRAFT-001`). Formal phase remains **None**. Superseded sole-active by LR-C3 (now Done — see completion note above). Wave LR-C remains **In Progress** (partial — C1/C4/C9–C13 + **C2** + **C3** Done; C5/C7/C8 Not Started). Do **not** mark Wave LR-C Done. Do **not** activate C5/C7/C8/CD-3. **Gates:** `pnpm -C frontend lint && type-check && test && build` **GREEN** (1016 tests); `docker-deploy-queue.ps1` **DEPLOY_OK** (`:8080` UP, `:4173` 200); E2E LRP-C2-draft-recovery **4/4** PASS (BDD-001/002/003/006); UIUX **PASS_WITH_NOTES** (`frontend/e2e/evidence/LRP-C2-uiux-manifest.md`); architecture **PASS_WITH_NOTES** (Critical **0**; merge_go yes). **Task Master #29 → done**.
>
> **Activation note (2026-07-11):** **LR-C2 → In Progress** (slice `lrp-c2-local-draft-recovery`; formal phase remains **None**). Sole-active LRP slice — structured editor local draft recovery: debounced localStorage drafts keyed by template id + dev version id + user; recovery banner «Restore draft / Discard» with timestamps; clear on successful save; storage quota guard. BDD **`ready`** (`docs/behavior/lrp-c2-structured-editor-local-draft-recovery.md` — `BDD-LRP-C2-DRAFT-001`). Placement: ISOLATED `D:/working/DGE-lrp-c2-local-draft-recovery` · `feat/lrp-c2-local-draft-recovery` · base `24cb253`. Gate evidence: [] (now **done** — see completion note above). **Task Master #29 → in-progress** (now **done**). Other LR-C rows untouched (C3/C5/C7/C8 Not Started; C1/C4/C9–C13 Done). Do **not** mark Wave LR-C Done. Do **not** activate C3/C5/C7/C8/CD-3. User direction: highest UX value; C3 depends on not fighting C2 draft logic.
>
> **Completion note (2026-07-11):** **LR-C13 → Done** (slice `lrp-c13-frontend-eng-debt`; merge `45addd6` / `45addd6d4299c94529d42ad48006a875b9fedefa` → `main`; feature `5c1d23c` / `5c1d23cc55d5239876d897ae4b00da57bf6db585`; worktree removed). OPT-G4 shared unwrap confirmed (already migrated); OPT-G5 fail-closed symmetric route guards + Vitest router tests; `manualChunks` verified (SOR-P06); coverage floors ratcheted **22/32/55/22 → 80/55/80/80**. BDD **not-applicable**. Formal phase remains **None**. Superseded sole-active by LR-C2 activation above. Wave LR-C remains **In Progress** (partial — C1/C4/C9–C13 Done; C2 activated separately; C3/C5/C7/C8 Not Started). Do **not** mark Wave LR-C Done. **Gates:** `pnpm -C frontend lint && type-check && test && build` **GREEN** (992 tests / 172 files); architecture **PASS_WITH_NOTES** (Critical **0**); E2E/deploy skipped (arch GO; BDD not-applicable). **Bundle:** without manualChunks max entry index 535.77 KB; with: index 139.12 / element-plus 756.70 / vue-vendor 108.79 / vendor 158.40 / app-vendor 65.54 KB; Total JS 1567.91 → 1913.13 (cache-friendly split). **Coverage measured:** lines 81.46% / functions 56.58% / branches 82.41% / statements 81.46%. Arch note: Medium legacy home keys (non-blocking). **Task Master #28 → done**. OPT-G4/G5 closed.
>
> **Activation note (2026-07-11):** **LR-C13 → In Progress** (slice `lrp-c13-frontend-eng-debt`; formal phase remains **None**). Sole-active LRP slice — frontend eng debt: shared `unwrap`, route guards, `manualChunks`, coverage ratchet; no intentional behavior change. BDD **not-applicable** (refactor + build config). Placement: ISOLATED `D:/working/DGE-lrp-c13-frontend-eng-debt` · `feat/lrp-c13-frontend-eng-debt` · base `51838da`. Gate evidence: [] (now **done** — see completion note above). **Task Master #28 → in-progress** (now **done**). Other LR-C rows untouched at activation (C2/C3/C5/C7/C8 Not Started; C1/C4/C9/C10/C11/C12 Done). Do **not** mark Wave LR-C Done. Do **not** activate C2/C3/C5/C7/C8/CD-3.
>
> **Completion note (2026-07-11):** **LR-C12 → Done** (slice `lrp-c12-keyboard-a11y`; merge `0357a16` / `0357a166ab2e310aed0ed25f33a5ba8dbfbab63a` → `main`; feature `e207e28`; worktree removed). Keyboard a11y & table activation: Enter/Space on activatable rows (EP store display order), skip-link, focus-ring tokens, extended a11y-smoke, keyboard journey E2E, UIUX manifest. BDD **not-applicable**. Formal phase remains **None**. Superseded sole-active by LR-C13 (now Done — see completion note above). Wave LR-C remains **In Progress** (partial — C1/C4/C9/C10/C11/C12 Done; C13 later Done). Do **not** mark Wave LR-C Done. **Gates:** `pnpm -C frontend lint && type-check && test && build` **GREEN** (983 tests); `docker-deploy-queue.ps1` **DEPLOY_OK** (`:8080` UP, `:4173` 200); E2E a11y-smoke + LRP-C12-keyboard-journey **10/10** PASS; UIUX **PASS_WITH_NOTES** (`frontend/e2e/evidence/LRP-C12-uiux-manifest.md`); architecture **PASS_WITH_NOTES** (Critical **0**). **Task Master #27 → done**. COR-F21 residual closed; OPT-B4 lint baseline absorbed.
>
> **Activation note (2026-07-11):** **LR-C12 → In Progress** (slice `lrp-c12-keyboard-a11y`; formal phase remains **None**). Sole-active LRP slice — keyboard a11y & table activation baseline: extend `useActivatableTableRow` (Enter/Space + focusable rows), shell skip-link, focus-ring audit, extend a11y-smoke axe coverage, Playwright keyboard journey. BDD **not-applicable** (COR-F21 follow-through / a11y hardening of existing interactions). Placement: ISOLATED `D:/working/DGE-lrp-c12-keyboard-a11y` · `feat/lrp-c12-keyboard-a11y` · base `ee1435d`. Gate evidence: []. **Task Master #27 → in-progress** (now **done** — see completion note above). Other LR-C rows untouched (C2/C3/C5/C7/C8/C13 Not Started; C1/C4/C9/C10/C11 Done). Do **not** mark Wave LR-C Done. Do **not** activate C2/C3/CD-3.
>
> **Completion note (2026-07-11):** **LR-C11 → Done** (slice `lrp-c11-api-error-i18n`; merge `44fcf40` / `44fcf4047e0f67a7116703fd60ebf6af29e2bbb4` → `main`; worktree removed). `api.error` catalog parity **159/159**; parity Vitest enforced; `logoSlotLabel` proper-noun exempt; locale formatters verified (`toLocaleString` residual closed). BDD **not-applicable**. Formal phase remains **None**. Superseded sole-active by LR-C12 activation above. Wave LR-C remains **In Progress** (partial — C1/C4/C9/C10/C11 Done; C12 activated separately). Do **not** mark Wave LR-C Done. **Gates:** `pnpm -C frontend lint && type-check && test && build` **GREEN** (973 tests); architecture-reviewer **PASS_WITH_NOTES** (Critical **0**); e2e/uiux/deploy skipped (catalog-only; `e2e_required=no`). **Task Master #26 → done**. OPT-G7 residual closed; OPT-G6 locale/catalog residual closed (aria-label sweep remains).
>
> **Activation note (2026-07-11):** **LR-C11 → In Progress** (slice `lrp-c11-api-error-i18n`; formal phase remains **None**). Sole-active LRP slice — `api.error` frontend catalog parity with backend messageKeys + parity Vitest; replace raw `toLocaleString`; `logoSlotLabel` disposition; OPT-G7/G6 residual updates. BDD **not-applicable** (catalog completion per i18n constitution). Placement: ISOLATED `D:/working/DGE-lrp-c11-api-error-i18n` · `feat/lrp-c11-api-error-i18n`. **Live key baseline (activation verify):** backend `api.error.*` **159** (plan text historically cited **145** — re-baseline on Done); frontend catalog **158** leaves; missing sample `api.error.rendering.ooxmlValidationFailed`. Gate evidence: []. **Task Master #26 → in-progress** (now **done** — see completion note above). Other LR-C rows untouched (C2/C3/C5/C7/C8/C12/C13 Not Started; C1/C4/C9/C10 Done). Do **not** mark Wave LR-C Done. Do **not** activate LR-C12 or CD-3.
>
> **Completion note (2026-07-11):** **LR-C10 → Done** (slice `lrp-c10-upload-ux`; merge `bdaf95d` / `bdaf95d42324a0fbd436d7d5f95eb4822dd9fa4d` → `main`; feature `ddb475e`; worktree removed). Upload UX polish: progress, drag hint, inline errors (LR-A3 messageKeys); list error isolation follow-up. BDD **not-applicable**. Formal phase remains **None**. Wave LR-C remains **In Progress** (partial). **Gates:** `pnpm -C frontend lint && type-check && test && build` **GREEN**; `docker-deploy-queue.ps1` **DEPLOY_OK** (`:8080` UP, `:4173` 200); E2E LRP-C10 **4/4** PASS + LRP-A3 regression **5/5**; UIUX **PASS_WITH_NOTES** (`frontend/e2e/evidence/LRP-C10-uiux-manifest.md`); architecture **PASS_WITH_NOTES** (Critical **0**). **Task Master #25 → done**. Other LR-C rows untouched at C10 closeout (C2/C3/C5/C7/C8/C12/C13 Not Started; C1/C4/C9 Done; C11 later Done — see C11 completion note above).
>
> **Completion note (2026-07-10):** **LR-C9 → Done** (slice `lrp-c9-load-error-panel`; merge `0013615` / `001361599df61c8b2be99cf2ebe5d92b040db508` → `main`; worktree removed). Unified list states: `EmptyStatePanel` `#actions` + `AppDataTable` `#empty` forward; role-aware empty CTAs on six surfaces (`TemplateListView`, `MasterListView`, `ContentModuleListView`, `ApiPolicyHomeView`, `UserManagementListSection`, `GroupManagementPanel`); `LoadErrorPanel` + retry already present. BDD **not-applicable**. Formal phase remains **None**. **Gates:** `pnpm -C frontend lint && type-check && test && build` **GREEN** (951 tests); `docker-deploy-queue.ps1` **DEPLOY_OK** (`:8080` UP, `:4173` 200); E2E `LRP-C9-list-states.spec.ts` **3/3** PASS; UIUX **PASS_WITH_NOTES** (`frontend/e2e/evidence/LRP-C9-uiux-manifest.md`); architecture **PASS_WITH_NOTES** (no Critical). **Task Master #14 → done**. Other LR-C rows untouched (C2/C3/C5/C7/C8/C10–C13 Not Started; C1/C4 Done).
>
> **Session note:** `LR-C*` tasks only. All UI work obeys `.cursor/skills/frontend-oa-design/SKILL.md` (bank OA lock, REDBC/GREENBC dual-brand) and `.cursor/rules/workspace-tab-shell-constitution.mdc`. i18n: English keys first, zh-CN additive — never literals (`.cursor/skills/i18n-english-first/SKILL.md`).
>
> **CORE-FORTRESS F7 mirror (2026-07-09):** LR-C1 + LR-C4 delivered under [CORE-FORTRESS-f7-authoring-ux.md](./CORE-FORTRESS-f7-authoring-ux.md) — Vitest **894**; E2E CORE-FORTRESS-F7 **12/12** PASS.

---

## 1. Environment & gate contract (all LR-C user-facing tasks)

```powershell
# From repo root — implementer MUST run before claiming Done
pnpm -C frontend lint
pnpm -C frontend type-check
pnpm -C frontend test
pnpm -C frontend build
.\scripts\docker-deploy.ps1     # UI http://localhost:4173, health http://localhost:8080/healthz
pnpm -C frontend exec playwright test <spec> --config playwright.docker.config.ts
```

Plus: `e2e-uiux-reviewer` evidence manifest (`frontend/e2e/evidence/<TASK>-uiux-manifest.md`, REDBC + GREENBC) for every task marked **UIUX: yes**. Backend-touching tasks add `mvn -B -ntp -f backend/pom.xml verify`.

**Do NOT (wave-wide):** hardcode user-facing strings; bypass `visibleRoutes` fail-closed gating; regress COR-F09 group-first list semantics; use API helpers to fake UI journeys in E2E.

---

## 2. Task breakdown

### LR-C1 — Dirty-form guard framework `useDirtyGuard`

- **Owner agent:** frontend-engineer
- **BDD:** **required** (guard prompts change navigation behavior).
- **UIUX:** yes
- **Read first:** `frontend/src/composables/` (existing composable conventions); `ControlledStructuredContentEditor.vue`; template/master metadata dialogs; identity forms (`UserManagementPanel.vue`, `GroupManagementPanel.vue`); API policy edit views; `frontend/src/router/`.
- **Do NOT:** Block navigation when the form is pristine; rely on `beforeunload` alone (in-app routing needs `onBeforeRouteLeave`); introduce a global state library change.
- **Steps:**
  1. Wait for BDD spec `ready` (trigger: navigate away / close dialog / close tab with unsaved edits; response: confirm dialog with stay/discard).
  2. Implement `frontend/src/composables/useDirtyGuard.ts`: tracks dirty state; registers `beforeunload` + `onBeforeRouteLeave`; exposes a dialog-close guard helper.
  3. Wire into: structured editor, template/master metadata dialogs, identity user/group forms, API policy editing.
  4. i18n keys for the confirm dialog (en + zh-CN).
  5. Vitest for the composable + one wired component; Playwright journey: edit → attempt route change → prompt → stay/discard both paths.
- **Acceptance (G/W/T):**
  - **G** unsaved edits in the structured editor **W** the user clicks another nav item **T** a confirm prompt appears; «stay» keeps edits, «discard» navigates.
  - **G** a pristine form **W** the user navigates away **T** no prompt (zero friction).
- **Gates:** §1 standard block; spec `frontend/e2e/LRP-C1-dirty-guard.spec.ts`; UIUX manifest.
- **Artifacts:** `useDirtyGuard.ts` + tests; wired views/dialogs; behavior spec; E2E + manifest.
- **Done when:** Scenarios green + gates + UIUX PASS + doc sync + commit review.
- **Status:** **Done** (2026-07-09 — delivered via **CORE-FORTRESS F7**; `useDirtyGuard` + `DirtyGuardConfirmDialog`; E2E `CORE-FORTRESS-F7-dirty-guard.spec.ts`; Vitest **894** gate GREEN)

### LR-C2 — Structured editor local draft recovery

- **Owner agent:** frontend-engineer
- **BDD:** **required** (recovery banner is new user-visible behavior).
- **BDD readiness confirmation (2026-07-11, behavior-spec-author):** **`ready`** — [lrp-c2-structured-editor-local-draft-recovery.md](../../behavior/lrp-c2-structured-editor-local-draft-recovery.md) (`BDD-LRP-C2-DRAFT-001`); decisions C2-C1…C2-C12 (debounce write, Restore/Discard banner, clear-on-save, quota eviction, LR-C1 complementarity, C3 storage separation).
- **UIUX:** yes
- **Read first:** `ControlledStructuredContentEditor.vue` + its store/model; LR-C1 spec (interplay: guard vs draft); `docs/behavior/lrp-c2-structured-editor-local-draft-recovery.md`.
- **Do NOT:** Change explicit-save semantics (server save remains the only authoritative persistence); store drafts server-side; keep drafts after successful save (clear them).
- **Steps:**
  1. Wait for BDD spec `ready`. ✅ (2026-07-11)
  2. Debounced local draft persistence (localStorage) **keyed by template id + dev version id + user**; write on structure change; clear on successful save.
  3. On editor mount with a newer local draft than server state: show a recovery banner — «Restore draft / Discard» with timestamps.
  4. i18n keys; storage quota guard (drop oldest on failure).
  5. Vitest (key scoping, clear-on-save, restore/discard); Playwright: edit → reload page → restore → content present; discard path too.
- **Acceptance (G/W/T):**
  - **G** unsaved editor changes **W** the tab reloads (or session drops) **T** reopening the editor offers the draft with timestamp; restore reproduces the exact structure.
  - **G** a draft restored then saved **W** the editor remounts **T** no banner (draft cleared; server state authoritative).
- **Gates:** §1 standard block; spec `frontend/e2e/LRP-C2-draft-recovery.spec.ts`; UIUX manifest.
- **Artifacts:** draft composable/store logic + banner component; behavior spec; tests + E2E + manifest.
- **Done when:** Scenarios green + gates + UIUX PASS + doc sync + commit review.
- **Maps:** CD-PIT-13 companion (work-loss mitigation); LR-B6.
- **Status:** **Done** (2026-07-11 — slice `lrp-c2-local-draft-recovery`; merge `12a6a7e`; Task Master #29; E2E 4/4; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES)

### LR-C3 — Editor undo/redo

- **Owner agent:** frontend-engineer
- **BDD:** **required**.
- **BDD readiness confirmation (2026-07-11, behavior-spec-author):** **`ready`** — [lrp-c3-editor-undo-redo.md](../../behavior/lrp-c3-editor-undo-redo.md) (`BDD-LRP-C3-UNDO-001`; scenarios BDD-LRP-C3-001…014); structure-level snapshot history cap 50; keyboard + toolbar; C2 draft must not include undo stack.
- **UIUX:** yes
- **Read first:** `ControlledStructuredContentEditor.vue` (structure mutation entry points); LR-C2 draft logic (history and drafts must not fight); `docs/behavior/lrp-c3-editor-undo-redo.md`.
- **Do NOT:** Implement content-level (character) undo inside third-party inputs — scope is **structure-level** operations (add/remove/move/edit node); unbounded history (cap it, e.g. 50); persist undo stack into LR-C2 local drafts.
- **Steps:**
  1. Wait for BDD spec `ready`. ✅ (2026-07-11)
  2. Bounded history stack (snapshots or inverse ops) around structure mutations.
  3. Keyboard: Ctrl+Z / Ctrl+Y (+ Cmd equivalents); toolbar undo/redo buttons with disabled states + i18n tooltips.
  4. Vitest: push/undo/redo/cap/branch-truncation; Playwright: perform 3 edits → undo ×2 → redo ×1 → structure matches expected.
- **Acceptance (G/W/T):**
  - **G** three structure edits **W** Ctrl+Z twice **T** the structure equals the post-first-edit state; toolbar redo enabled.
  - **G** history at cap **W** another edit occurs **T** oldest entry evicted; undo depth stays ≤ cap (no memory growth).
- **Gates:** §1 standard block; spec `frontend/e2e/LRP-C3-undo-redo.spec.ts`; UIUX manifest.
- **Artifacts:** history module + toolbar wiring + tests; behavior spec; E2E + manifest.
- **Done when:** Scenarios green + gates + UIUX PASS + doc sync + commit review.
- **Status:** **Done** (2026-07-11 — slice `lrp-c3-editor-undo-redo`; merge `0cf553b`; Task Master #30; E2E 7/7; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES)

### LR-C4 — Side-by-side authoring preview

- **Owner agent:** frontend-engineer
- **BDD:** **required**.
- **UIUX:** yes
- **Read first:** `TemplatePreviewPanel.vue` (manual `refreshPreview` L50–63); preview generation API (`frontend/src/api/templates.ts`); [CDP-industry-pitfall-registry.md](./CDP-industry-pitfall-registry.md) CD-PIT-08; `.cursor/rules/workspace-tab-shell-constitution.mdc`.
- **Do NOT:** Present the edit-time preview as legal/final evidence — copy MUST keep the «edit preview is not authoritative; final-path artifact governs» boundary (CD-PIT-08); auto-regenerate on every keystroke (explicit refresh CTA + stale badge instead).
- **Steps:**
  1. Wait for BDD spec `ready`.
  2. Split layout: editor left, latest **final-chain preview artifact** right (reuse existing preview records; no new rendering path).
  3. Stale badge when structure changed after the shown preview's timestamp; refresh CTA triggers regeneration.
  4. Boundary copy (i18n) visible on the preview pane; responsive fallback (stacked below breakpoint per OA skill).
  5. Vitest + Playwright: edit → stale badge appears → refresh → badge clears; boundary copy asserted.
- **Acceptance (G/W/T):**
  - **G** an author edits structure after a preview **W** looking at the preview pane **T** a stale badge + refresh CTA are visible; copy states the non-authoritative boundary.
  - **G** refresh is clicked **W** generation completes **T** the right pane shows the new artifact and the badge clears.
- **Gates:** §1 standard block; spec `frontend/e2e/LRP-C4-side-by-side-preview.spec.ts`; UIUX manifest (both brands, incl. narrow viewport).
- **Artifacts:** layout + badge components; behavior spec; tests + E2E + manifest.
- **Done when:** Scenarios green + gates + UIUX PASS + doc sync + commit review.
- **Maps:** CD-PIT-08 (boundary preserved); CD-E2E-T09 (CDP comparison journey — not duplicated).
- **Status:** **Done** (2026-07-09 — delivered via **CORE-FORTRESS F7**; `AuthoringSideBySideLayout` + `AuthoringPreviewPane`; E2E `CORE-FORTRESS-F7-side-by-side-preview.spec.ts`; E2E **12/12** PASS)

### LR-C5 — Catalog server-side pagination/filter

- **Owner agent:** backend-engineer + frontend-engineer
- **BDD:** **required** (list behavior, filters, and counts change contract).
- **BDD readiness confirmation (2026-07-11, behavior-spec-author):** **`ready`** — [lrp-c5-catalog-pagination.md](../../behavior/lrp-c5-catalog-pagination.md) (`BDD-LRP-C5-CATALOG-001`; scenarios BDD-LRP-C5-001…015). Key decisions: default `size=20` (max 100); row-based `PageView`; COR-F09 = default sort `groupCode ASC, updatedAt DESC` (not group-count pages); server-side search/filters/workflow chips; masters + content-modules upgrade to `PageView`; ≥500 seed p95 &lt; 1 s.
- **UIUX:** yes
- **Read first:** `frontend/src/stores/templates.ts` (`fetchTemplates` full fetch); `frontend/src/composables/useCatalogPagination.ts` (client `slice`); COR-F09 group-first pagination decision ([comprehensive-optimization-roadmap.md](../comprehensive-optimization-roadmap.md)); [optimization-plan.md](../optimization-plan.md) OPT-F4; `docs/api/openapi-v1.yaml` + management API conventions; masters/content-modules stores; `docs/behavior/lrp-c5-catalog-pagination.md`.
- **Do NOT:** Break COR-F09 group-first semantics (groups remain the primary organization; pagination within/across groups per the BDD decision); leave OpenAPI/docs unsynced; regress existing Playwright list specs.
- **Steps:**
  1. Wait for BDD spec `ready` (page size defaults, filter fields, group-first interplay). ✅ (2026-07-11)
  2. Backend: make templates/masters/content-modules list endpoints pageable (page/size/filter params; QueryDSL where complex — ADR-0037 opportunistic rule) + total counts. **OpenAPI/contract docs-first sync Done** (2026-07-11 doc-keeper — `listTemplates` / `listMasters` / `listContentModules` PageView + filters; see `docs/api/openapi-v1.yaml` + contract-outline § LR-C5).
  3. Frontend: migrate stores + list views + `useCatalogPagination` consumers to server paging; keep UX (chips/filters) intact.
  4. Seed a ≥500-row dataset (script or seeder flag) and record p95 list latency < 1 s evidence (browser devtools or Playwright timing) in the ledger.
  5. Tests: backend pageable + filter tests; Vitest store tests; Playwright: paginate, filter, group-first ordering preserved.
- **Acceptance (G/W/T):**
  - **G** ≥500 templates across groups **W** the catalog opens **T** first page renders with server-side page metadata; p95 request < 1 s (documented evidence).
  - **G** a filter + page change **W** applied **T** results come from the server (network shows paged calls), group-first semantics intact.
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; §1 standard block; spec `frontend/e2e/LRP-C5-catalog-pagination.spec.ts`; UIUX manifest.
- **Artifacts:** backend pageable endpoints + tests; OpenAPI sync; store/view migration; perf evidence; behavior spec; E2E + manifest.
- **Done when:** Scenarios green + perf evidence + gates + UIUX PASS + doc sync + commit review.
- **Maps:** OPT-F4 residual (absorbed); COR-F09 (semantics preserved).
- **Status:** **Done** (2026-07-11 — slice `lrp-c5-catalog-pagination`; merge `5543a33`; feature `cfab79e`; Task Master #31; BDD ready; E2E 6/6; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES Critical 0; perf p95 ~75ms; OPT-F4 residual closed)

### LR-C6 — Global search / command palette (Ctrl+K)

- **Owner agent:** frontend-engineer (+ backend-engineer for search endpoint)
- **BDD:** **required**.
- **UIUX:** yes
- **Depends on:** LR-C5 server-side search/filter endpoint. If sequenced earlier, ship a **client-side limited version** over already-loaded catalog data and state the limitation in the spec + UI copy.
- **Read first:** `frontend/src/components/` shell (`ManagementShell`); `visibleRoutes` gating in session store/router; LR-C5 endpoints.
- **Do NOT:** Surface entities the session cannot access (search must be authorization-scoped server-side; `visibleRoutes` fail-closed for route entries); add a heavyweight search dependency.
- **Steps:**
  1. Wait for BDD spec `ready`.
  2. Palette component (Ctrl+K / Cmd+K): search authorized templates/masters/content-modules + navigable routes; keyboard navigation (↑↓ Enter Esc).
  3. Server query via LR-C5 search param (scoped by session groups/roles).
  4. i18n keys; recent-items local memory (optional per spec).
  5. Vitest + Playwright: open with keyboard, search seeded template, Enter navigates to its detail; unauthorized entity absent for a restricted role.
- **Acceptance (G/W/T):**
  - **G** any management page **W** Ctrl+K then a template code fragment **T** matching authorized items list within the palette; Enter routes to the item.
  - **G** a role without access to a module **W** searching its name **T** no result leaks (fail-closed).
- **Gates:** §1 standard block (+ backend verify if endpoint changes); spec `frontend/e2e/LRP-C6-command-palette.spec.ts`; UIUX manifest.
- **Artifacts:** palette component + shell wiring; search endpoint usage; behavior spec; tests + E2E + manifest.
- **Done when:** Scenarios green + gates + UIUX PASS + doc sync + commit review.
- **Status:** Not Started

### LR-C7 — In-app notification center

- **Owner agent:** backend-engineer + frontend-engineer
- **BDD:** **required**.
- **UIUX:** yes
- **Depends on:** P14 collaboration domain (**Done**).
- **Read first:** P14 collaboration work items API (`frontend/src/api/collaboration.ts`, backend `CollaborationWorkItem*`); Dashboard task hub partitions (COR-T11); program §0.1 (**in-app only — no email/IM**).
- **Do NOT:** Add email/IM/webhook delivery (v1 confirmed in-app only); create a new notification domain when collaboration items + escalations already carry the data; use SSE/WebSocket here (polling with configurable interval per spec).
- **Steps:**
  1. Wait for BDD spec `ready` (unread definition, read-state persistence, polling interval).
  2. Backend: unread-count + notification-list endpoint derived from open collaboration to-dos + timeout escalations for the session user; read-state persistence (per-user marker).
  3. Frontend: shell bell icon + unread badge; dropdown list; item click deep-links to the task hub partition (existing routes); «mark read» semantics per spec.
  4. Polling with configurable interval (env/config; default per spec); pause when tab hidden.
  5. i18n keys; Vitest; Playwright: seed a collaboration item → bell shows unread → open → deep link lands on the right partition → unread clears per spec.
- **Acceptance (G/W/T):**
  - **G** an open collaboration to-do assigned to the user's role queue **W** the shell polls **T** the bell badge shows ≥1 and the dropdown lists the item with its type.
  - **G** the user opens the item via the dropdown **W** deep link resolves **T** the task hub opens on the correct partition; read state updates per spec.
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; §1 standard block; spec `frontend/e2e/LRP-C7-notification-center.spec.ts`; UIUX manifest (both brands).
- **Artifacts:** backend endpoints + tests; bell/dropdown components; behavior spec; E2E + manifest.
- **Done when:** Scenarios green + gates + UIUX PASS + doc sync + commit review.
- **Maps:** P14 (Done — source data); v1 boundary §0.1.
- **Status:** Not Started

### LR-C8 — Role onboarding tour

- **Owner agent:** frontend-engineer
- **BDD:** **required**.
- **UIUX:** yes
- **Read first:** `frontend/src/components/journey/RoleJourneyTimeline.vue` (per-role journey step definitions — reuse, don't fork); Element Plus `el-tour` docs (built-in ≥2.4 — **no new dependency**; repo has `element-plus@^2.9.1`); shell help menu location.
- **Do NOT:** Add a third-party tour library; force the tour (must be skippable + dismiss-remembered); hardcode step copy.
- **Steps:**
  1. Wait for BDD spec `ready` (per-role step lists, first-login trigger, replay entry).
  2. Build tour steps per role from the RoleJourneyTimeline journey definitions (anchor to stable element ids).
  3. First-login trigger (per-user local marker), skip button, «don't show again»; replay entry in the help menu.
  4. i18n keys (en + zh-CN) for every step.
  5. Vitest (trigger/skip logic); Playwright: fresh user login → tour appears → skip persists; help-menu replay works.
- **Acceptance (G/W/T):**
  - **G** a first-time TEMPLATE_AUTHOR login **W** the shell mounts **T** the tour opens on step 1 of the author journey; skip closes and persists.
  - **G** a returning user **W** opening help menu → tour **T** the tour replays from step 1 regardless of the dismissed marker.
- **Gates:** §1 standard block; spec `frontend/e2e/LRP-C8-onboarding-tour.spec.ts`; UIUX manifest.
- **Artifacts:** tour composable/config + shell wiring; behavior spec; tests + E2E + manifest.
- **Done when:** Scenarios green + gates + UIUX PASS + doc sync + commit review.
- **Maps:** P21 RoleJourneyTimeline (reused).
- **Status:** Not Started

### LR-C9 — Unified list states rollout

- **Owner agent:** frontend-engineer
- **BDD:** not-applicable — rollout of the already-confirmed COR-F05/F14 `LoadErrorPanel` pattern (roadmap §9 non-negotiable patterns); no new behavior contract.
- **BDD readiness confirmation (2026-07-10, behavior-spec-author):** **`not-applicable` confirmed.** No new product/API/permission contract. Implementers treat the Acceptance (G/W/T) below as the TDD/E2E contract. Pattern sources: COR-F05/F14 Done; roadmap §9 (async loading → empty → error+retry; permissions hide controls backend would deny); MGMT P1-2 (`GroupManagementPanel`); MGMT D3/R2 (`ApiPolicyHomeView` Browse-templates empty CTA).
- **UIUX:** yes
- **Read first:** `frontend/src/components/common/LoadErrorPanel.vue`; `EmptyStatePanel.vue` (may need `#actions` slot for in-empty CTAs); the six targets below; [comprehensive-optimization-roadmap.md](../comprehensive-optimization-roadmap.md) §9; [mgmt-ui-defects-behavior-spec.md](../../requirements/mgmt-ui-defects-behavior-spec.md) P1-2 / D3.
- **Do NOT:** Redesign `LoadErrorPanel`; change store error semantics; leave any of the six with a dead-end load-error `el-alert`; invent new empty CTAs beyond create/upload (or Browse templates on API policy home).
- **Surface audit (2026-07-10 — closed on Done):**

  | Surface | LoadErrorPanel + retry | Role-aware empty CTA | Residual for LR-C9 |
  | --- | --- | --- | --- |
  | `TemplateListView` | Done | Done (`#actions` create) | — closed |
  | `MasterListView` | Done | Done (`#actions` upload) | — closed |
  | `ContentModuleListView` | Done | Done (`#actions` create) | — closed |
  | `ApiPolicyHomeView` | Done | Done (Browse-templates via `#actions`) | — closed |
  | `UserManagementPanel` → `UserManagementListSection` | Done | Done (fail-closed create CTA) | — closed |
  | `GroupManagementPanel` | Done (MGMT P1-2) | Done (`canManage` empty CTA) | — closed |

- **Steps:**
  1. Verify each of the six still uses `LoadErrorPanel` + working retry (re-invoke store fetch); close any residual dead-end load-error path (none of the six still use load-error `el-alert` as of audit — do not regress).
  2. Add role-aware empty states: primary CTA in the empty body points to the creating/uploading action the role can perform (API policy home: Browse templates per D3); fail-closed: no empty CTA if not permitted. Extend `EmptyStatePanel` with an `#actions` slot if needed (presentation only — no new behavior contract).
  3. i18n keys for new empty-state CTA copy.
  4. Vitest per view (error → retry → success; empty → CTA visibility by role); Playwright smoke on two representative lists.
- **Acceptance (G/W/T) — contract for implementers:**
  - **LR-C9-A:** **G** a list fetch fails **W** the view renders **T** `LoadErrorPanel` with retry appears; retry after recovery loads data without page reload.
  - **LR-C9-B:** **G** an empty catalog for a role with create/upload permission (API policy: browse-templates) **W** the view renders **T** a CTA to that action is shown in the empty state; for a role without permission, no empty-state CTA.
- **Gates:** §1 standard block; spec `frontend/e2e/LRP-C9-list-states.spec.ts`; UIUX manifest.
- **Artifacts:** six view/panel edits + tests; E2E + manifest.
- **Done when:** All six meet Acceptance A+B + gates + UIUX PASS + doc sync + commit review.
- **Maps:** COR-F05/F14 (pattern source); roadmap §9; MGMT P1-2 / D3.
- **Status:** **Done** (2026-07-10 — slice `lrp-c9-load-error-panel`; merge `0013615`; BDD `not-applicable`; six surfaces closed; gates + E2E 3/3 + UIUX PASS_WITH_NOTES + architecture PASS_WITH_NOTES)

### LR-C10 — Upload UX polish

- **Owner agent:** frontend-engineer
- **BDD:** not-applicable — presentation polish of the existing upload flow; rejection *behavior* is owned by LR-A3's spec.
- **UIUX:** yes
- **Depends on:** LR-A3 (limit values + messageKeys to surface).
- **Read first:** master upload/replace dialogs (`frontend/src/components/masters/`); LR-A3 spec + messageKeys; OA skill upload patterns.
- **Do NOT:** Duplicate validation logic client-side beyond UX hints (server remains authoritative); change upload API calls.
- **Steps:**
  1. Upload progress state (percentage or indeterminate) during transfer.
  2. Drag-and-drop affordance + hint copy; file type/size hint sourced from the LR-A3 configured limits.
  3. Inline error rendering for LR-A3 rejections (magic-byte, oversize) using their messageKeys — no raw envelope leakage.
  4. Vitest; Playwright: upload valid file (progress visible), upload oversized/invalid (inline translated error).
- **Acceptance (G/W/T):**
  - **G** a valid DOCX drag-dropped **W** upload runs **T** progress state visible; success lands as today.
  - **G** an oversized file **W** upload rejected **T** the dialog shows the translated inline error with the configured limit value; no dead-end.
- **Gates:** §1 standard block; spec `frontend/e2e/LRP-C10-upload-ux.spec.ts`; UIUX manifest.
- **Artifacts:** dialog component updates + tests; E2E + manifest.
- **Done when:** Both paths green + gates + UIUX PASS + doc sync + commit review.
- **Maps:** LR-A3 (limits + keys).
- **Status:** **Done** (2026-07-11 — slice `lrp-c10-upload-ux`; merge `bdaf95d`; feature `ddb475e`; BDD `not-applicable`; E2E LRP-C10 **4/4** + LRP-A3 regression **5/5**; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES Critical 0)

### LR-C11 — i18n closure

- **Owner agent:** frontend-engineer
- **BDD:** not-applicable — catalog completion + formatting consistency mandated by the i18n constitution; no behavior contract change.
- **UIUX:** spot-check only (zh-CN screenshots on 2 error surfaces).
- **Read first:** `frontend/src/i18n/catalogs/apiErrorEn.ts` + `apiErrorZhCn.ts` + `apiErrorCatalog.test.ts` (en↔zh + backend parity); `backend/src/main/resources/i18n/messages_en.properties` (**159** `api.error.*` — plan historically cited **145**); `frontend/src/composables/useLocaleFormatters.ts`; `frontend/src/config/brands.ts` (`logoSlotLabel`).
- **Do NOT:** Machine-translate carelessly (zh-CN must be reviewed banking register); change backend keys; encode data into keys.
- **Steps:**
  1. Add any missing `api.error.*` keys to `apiErrorEn.ts` (English base, mirroring backend semantics) + zh-CN counterparts — **Done** (parity **159/159**; closed `api.error.rendering.ooxmlValidationFailed` gap).
  2. Add/strengthen a **backend parity test**: parse `backend/src/main/resources/i18n/messages_en.properties` in Vitest and assert every backend `api.error.*` key resolves in the frontend catalog — **Done** (parity Vitest enforced at live N/N).
  3. Verify locale-aware date formatting via `useLocaleFormatters` (no raw `toLocaleString` residual on scoped surfaces) — **Done** (verify residual closed; already compliant).
  4. **`logoSlotLabel` disposition (docs-first Done):** proper-noun exempt — keep `REDBC`/`GREENBC` literals in `brands.ts`; do **not** key-ify. Recorded in `.cursor/skills/i18n-english-first/SKILL.md` + PRD §6.1. Human-readable names stay on `labelKey`.
  5. Close [optimization-plan.md](../optimization-plan.md) OPT-G7 residual + OPT-G6 locale/catalog residual on slice Done (aria-label sweep remains on OPT-G6).
- **Acceptance (G/W/T):**
  - **G** the parity test **W** `pnpm -C frontend test` runs **T** it fails if any backend `api.error.*` key is missing from the frontend catalog — and passes at **N/N** where N = live backend `api.error.*` count (**159/159** Done; was historically documented as 145/145).
  - **G** locale zh-CN **W** viewing master revision history and release version history **T** timestamps render via locale-aware formatting (no en-US default leakage) — verified.
- **Gates:** §1 standard block (lint/type-check/test/build); zh-CN screenshot spot-check; e2e/uiux/deploy skipped (catalog-only).
- **Artifacts:** catalog additions; parity test; locale formatter verify; `logoSlotLabel` disposition recorded (proper-noun exempt); OPT-G7 closed; OPT-G6 locale/catalog residual closed.
- **Done when:** N/N parity + parity gate active + gates green + doc sync + commit review.
- **Maps:** OPT-G7 (absorbed, residual closed); OPT-G6 residual (locale dates + catalog portion closed; aria-label sweep remains).
- **Status:** **Done** (2026-07-11 — slice `lrp-c11-api-error-i18n`; merge `44fcf40`; BDD `not-applicable`; Task Master #26; parity **159/159**; architecture PASS_WITH_NOTES Critical 0)

### LR-C12 — Keyboard a11y & table activation

- **Owner agent:** frontend-engineer
- **BDD:** not-applicable — a11y baseline hardening of existing interactions (COR-F21 follow-through).
- **UIUX:** yes (a11y evidence)
- **Read first:** `frontend/src/composables/useActivatableTableRow.ts` (click-only today); `frontend/e2e/a11y-smoke.spec.ts`; shell layout (`ManagementShell`); OA skill a11y section.
- **Do NOT:** Break existing row-click; add `tabindex` on non-interactive noise; regress axe smoke.
- **Steps:**
  1. Extend `useActivatableTableRow`: rows focusable + Enter/Space activation (aria `role`/`aria-label` per OA skill).
  2. Add a shell **skip-link** («skip to main content») as first focusable element.
  3. Focus-ring audit across primary journeys (visible `:focus-visible` on nav, tables, dialogs, editors) — fix gaps with tokens, not ad-hoc colors.
  4. Extend `a11y-smoke.spec.ts` (axe) to cover ≥2 more views incl. one table-heavy view.
  5. Vitest for the composable; Playwright keyboard journey: tab from login → skip-link → table → Enter opens detail.
- **Acceptance (G/W/T):**
  - **G** keyboard-only navigation **W** focus reaches a catalog row and Enter is pressed **T** the row's detail opens exactly like click.
  - **G** the extended axe smoke **W** it runs on Docker 4173 **T** zero critical violations on the covered views.
- **Gates:** §1 standard block; `pnpm -C frontend exec playwright test a11y-smoke.spec.ts LRP-C12-keyboard-journey.spec.ts --config playwright.docker.config.ts`; UIUX manifest.
- **Artifacts:** composable + shell changes; extended axe smoke + keyboard spec; manifest.
- **Done when:** Keyboard journey + axe green + gates + doc sync + commit review.
- **Maps:** COR-F21 residual (absorbed / closed); OPT-B4 lint baseline (absorbed).
- **Status:** **Done** (2026-07-11 — slice `lrp-c12-keyboard-a11y`; merge `0357a16`; feature `e207e28`; BDD `not-applicable`; Task Master #27; E2E 10/10; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES Critical 0)

### LR-C13 — Frontend engineering debt

- **Owner agent:** frontend-engineer
- **BDD:** not-applicable — refactor + build configuration; runtime behavior unchanged (proven by unchanged tests).
- **UIUX:** no (non-visual)
- **Read first:** the 10 api modules with private `unwrap` (`templates.ts`, `masters.ts`, `identity.ts`, `audit.ts`, `contract.ts`, `contentModules.ts`, `apiPolicy.ts`, `collaboration.ts`, `riskPromptConfig.ts`, `templateRiskPromptConfig.ts`); [optimization-plan.md](../optimization-plan.md) OPT-G4/G5; `frontend/src/stores/session.ts` + `frontend/src/router/` (client role checks asymmetry); `frontend/vite.config.ts` (no `manualChunks`); `frontend/vitest.config.ts` (floors lines 22 / functions 32 / branches 55).
- **Do NOT:** Change API response handling semantics while deduplicating; enable route guards that contradict backend `visibleRoutes` (client checks mirror, backend stays authoritative); set coverage floors above measured reality (ratchet = current actual − small margin).
- **Steps:**
  1. Extract a shared `unwrap`/envelope helper (e.g. `frontend/src/api/envelope.ts`); migrate all 10 modules; delete duplicates (OPT-G4).
  2. Add symmetric client-side role checks for routes relying only on backend `visibleRoutes`; add router integration tests (OPT-G5) — fail-closed on unknown routes.
  3. Add `build.rollupOptions.output.manualChunks` in `vite.config.ts` (vendor: element-plus, vue stack); record before/after bundle sizes in the ledger.
  4. Measure current coverage; raise `vitest.config.ts` floors to the measured baseline (ratchet), documenting old→new values.
  5. Full regression: no store/view test changes needed (behavior unchanged).
- **Acceptance (G/W/T):**
  - **G** the shared helper migration **W** `pnpm -C frontend test` runs **T** all suites pass with zero remaining private `unwrap` definitions (grep-verified).
  - **G** an unauthorized role deep-links to a guarded route **W** the router resolves **T** redirect per guard test; backend behavior unchanged.
  - **G** the production build **W** `pnpm -C frontend build` completes **T** vendor chunks split per `manualChunks`; sizes recorded.
- **Gates:** §1 standard block (lint/type-check/test/build).
- **Artifacts:** shared helper + 10 module migrations; router guards + integration tests; vite/vitest config changes; bundle + coverage notes in ledger; OPT-G4/G5 row updates.
- **Done when:** Zero duplicate unwraps + guards tested + ratchet raised + gates green + doc sync + commit review.
- **Maps:** OPT-G4/G5 (absorbed); program §1 finding 10.
- **Status:** **Done** (2026-07-11 — slice `lrp-c13-frontend-eng-debt`; merge `45addd6`; feature `5c1d23c`; BDD `not-applicable`; Task Master #28; OPT-G4/G5 closed; coverage floors 80/55/80/80; architecture PASS_WITH_NOTES Critical 0)

---

## 3. Exit gate (Wave LR-C)

- [x] LR-C1 + LR-C4 shipped via CORE-FORTRESS F7 (2026-07-09; Vitest **894**; E2E **12/12**; BDD + UIUX evidence)
- [ ] LR-C2/C3/C5…C8 shipped with BDD specs, functional Playwright journeys, and UIUX manifests (both brands) *(LR-C2 Done 2026-07-11 merge `12a6a7e`; **LR-C3 Done** 2026-07-11 merge `0cf553b`; **LR-C5 Done** 2026-07-11 merge `5543a33`; C6/C7/C8 Not Started)*
- [x] LR-C9…C13 shipped with green gates; OPT-G4/G5/G6-residual/G7 + F4-residual rows updated *(LR-C9 Done 2026-07-10; LR-C10 Done 2026-07-11 merge `bdaf95d`; **LR-C11 Done** 2026-07-11 merge `44fcf40` — OPT-G7 + OPT-G6 locale/catalog residual closed; **LR-C12 Done** 2026-07-11 merge `0357a16` — COR-F21 residual + OPT-B4 absorbed; **LR-C13 Done** 2026-07-11 merge `45addd6` — OPT-G4/G5 closed; **OPT-F4 residual closed by LR-C5** 2026-07-11 merge `5543a33`; OPT-G6 aria-label sweep remains)*
- [x] `api.error` parity test enforcing live backend N/N (**159/159**; historically 145) active in `pnpm -C frontend test` (LR-C11 Done 2026-07-11)
- [x] No dead-end error list remains among the six §2/LR-C9 targets (LR-C9 Done 2026-07-10; merge `0013615`)
- [x] LR-C10 upload UX polish shipped (progress / drag hint / inline errors; merge `bdaf95d`; E2E 4/4)
- [x] LR-C12 keyboard a11y & table activation shipped (Enter/Space, skip-link, focus-ring; merge `0357a16`; E2E 10/10; COR-F21 residual closed)
- [x] LR-C13 frontend eng debt shipped (shared unwrap confirmed; route guards + Vitest; manualChunks; coverage 80/55/80/80; merge `45addd6`; OPT-G4/G5 closed)
- [x] LR-C5 catalog server-side pagination/filter shipped (merge `5543a33`; E2E 6/6; UIUX PASS_WITH_NOTES; perf p95 ~75ms; OPT-F4 residual closed)
- [ ] Ledger § LRP wave row updated with per-task evidence *(LR-C5 Done recorded 2026-07-11; C1/C4/C9–C13 + C2 + C3 + C5 Done; **no sole-active**; wave still partial — C6/C7/C8 Not Started)*
