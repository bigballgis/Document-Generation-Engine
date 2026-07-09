# CORE-FORTRESS F7 — Authoring UX (Detailed Plan)

**Program ID:** `CORE-FORTRESS`  
**Phase ID:** `CORE-FORTRESS-F7-AUTHORING-UX`  
**Phase status:** **Done** (2026-07-09)  
**Depends on:** CORE-FORTRESS F5 (**Done** 2026-07-09); F4 **Done** (LO pool for preview reliability); F6 **Done** (2026-07-09; kernel composables landed)  
**Parallel with:** — (F6 closed; coordinate on dev-workspace / controller wiring complete)  
**BDD:** `docs/behavior/core-fortress-f7-authoring-ux.md` — **ready** (`BDD-CORE-FORTRESS-F7-001`)

> **Single-active-phase invariant:** **F7 Done** (2026-07-09). **F8** sole formal `In Progress`. F1–F7 **Done**.

> **LRP mapping:** F7 delivered **LR-C1** (dirty guard) + **LR-C4** (side-by-side preview) under the CORE-FORTRESS program umbrella. LR-C2/C3 remain Not Started in LRP-C.

---

## 1. North star

**Authors never lose unsaved structured edits silently, and see final-chain preview beside the editor** — confirm on navigate-away; stale badge + explicit refresh; CD-PIT-08 boundary copy visible; Bank OA dual-brand; full E2E + UIUX evidence.

---

## 2. Scope (in) / out (out)

| In scope (F7) | Out of scope (later LRP / other) |
| --- | --- |
| F7-B1: `useDirtyGuard` composable + wiring (structured editor, metadata dialog) | LR-C2 local draft recovery |
| F7-B1: `beforeunload` + `onBeforeRouteLeave` + dialog-close helper | LR-C3 undo/redo |
| F7-B2: Side-by-side layout in dev-editor authoring | ADR-0051 debounced auto-refresh (v1) |
| F7-B2: Stale badge + Refresh now CTA | New backend preview API (reuse existing) |
| F7-B2: CD-PIT-08 boundary i18n copy | HTML approximation preview |
| i18n en + zh-CN for all new strings | LR-C5+ catalog pagination |
| Vitest + Playwright + UIUX manifest | Backend `mvn verify` (unless preview contract change — **none planned**) |

### Reuse — do NOT re-implement

| Asset | Evidence |
| --- | --- |
| `TemplatePreviewPanel.vue` refresh/download | Manual refresh L57–68; comparison display |
| Test-generate / preview API | `templatesStore.testGenerate`, `panelDataStore.fetchPreview` |
| P19 preview comparison backend | `PreviewComparisonService` Done |
| WorkspaceTabShell / dev-workspace tabs | P12 Done |

---

## 3. Exit criteria

1. **B1:** Dirty guard wired to structured editor + metadata dialog; BDD-F7-B1-001…007 green in Vitest + E2E. ✅
2. **B2:** Side-by-side layout + stale/refresh flow; BDD-F7-B2-001…007 green. ✅
3. **i18n:** All new strings in `en.ts` + `zh-CN.ts`; no literals. ✅
4. **Gates:** `pnpm -C frontend lint && type-check && test && build` — **GREEN** (**894** Vitest). ✅
5. **E2E:** Docker Playwright CORE-FORTRESS-F7 specs **12/12** PASS. ✅
6. **Doc sync:** roadmap, LRP-C1/C4 status mirror, ledger — F7-T11. ✅

---

## 4. Task breakdown

| ID | Owner | Task | Depends on | Status |
| --- | --- | --- | --- | --- |
| **F7-T01** | behavior-spec-author | **BDD behavior spec** — `core-fortress-f7-authoring-ux.md` + this plan | — | **Done** (2026-07-09; readiness `ready`) |
| **F7-T02** | frontend-engineer | **B1 composable** — `useDirtyGuard.ts`: dirty ref API, `beforeunload`, `onBeforeRouteLeave`, dialog-close helper; Vitest | F7-T01, F5 Done | **Done** (2026-07-09) |
| **F7-T03** | frontend-engineer | **B1 wiring wave 1** — `ControlledStructuredContentEditor.vue` + save/discard integration; `DirtyGuardConfirmDialog.vue` | F7-T02 | **Done** (2026-07-09) |
| **F7-T03b** | frontend-engineer | **B1 wiring wave 2 (optional)** — template metadata dialog; defer if scope pressure | F7-T03 | **Done** (2026-07-09; in scope delivered) |
| **F7-T04** | frontend-engineer | **B2 layout shell** — `AuthoringSideBySideLayout.vue` (editor slot + preview slot); responsive collapse; integrate `TemplateDetailDevWorkspace` / authoring sub tab | F7-T01, F5 Done | **Done** (2026-07-09) |
| **F7-T05** | frontend-engineer | **B2 stale + refresh** — track last-preview vs structure revision; stale badge; Refresh CTA → existing preview generate/refresh; in-flight guard | F7-T04 | **Done** (2026-07-09; `AuthoringPreviewPane.vue`) |
| **F7-T06** | frontend-engineer | **i18n** — dirty guard + stale/boundary keys (en + zh-CN) | F7-T03, F7-T05 | **Done** (2026-07-09) |
| **F7-T07** | frontend-engineer | **Vitest** — composable + layout/stale component tests | F7-T03, F7-T05 | **Done** (2026-07-09; frontend **894** Vitest) |
| **F7-T08** | e2e-test-engineer | **E2E dirty guard** — `frontend/e2e/CORE-FORTRESS-F7-dirty-guard.spec.ts` (stay/discard/save/pristine) | F7-T03, docker stack | **Done** (2026-07-09) |
| **F7-T09** | e2e-test-engineer | **E2E side-by-side** — `frontend/e2e/CORE-FORTRESS-F7-side-by-side-preview.spec.ts` | F7-T05, docker stack | **Done** (2026-07-09) |
| **F7-T10** | e2e-uiux-reviewer | **UIUX manifest** — REDBC/GREENBC, md + narrow viewport; a11y spot check | F7-T08, F7-T09 | **Done** (2026-07-09; E2E **12/12** PASS) |
| **F7-T11** | post-task-doc-sync | **Plan + ledger + LRP mirror** — mark F7 Done; LR-C1/C4 Done in LRP-C row | F7-T10 + green gates | **Done** (2026-07-09) |

**Task count:** **12** (F7-T01 … F7-T11 incl. T03b) — **all Done**.

---

## 5. Recommended wave order

```text
Wave 0 — BDD + plan (Done)
  F7-T01

Wave 1 — Foundations (parallel after F5 Done)
  F7-T02 (useDirtyGuard)
  F7-T04 (layout shell) — parallel OK if different files

Wave 2 — Feature wiring
  F7-T03 (dirty guard wire)
  F7-T05 (stale + refresh) — after T04
  F7-T03b (optional metadata) — after T03

Wave 3 — i18n + unit tests
  F7-T06 → F7-T07

Wave 4 — E2E + UIUX + closeout
  F7-T08 → F7-T09 → F7-T10 → F7-T11
```

**F6 coordination:** Prefer merge F6 preview extract (F6-T02) before F7-T05 to avoid dual-editing preview state. ✅ completed before F7 close.

---

## 6. Component / file map (delivered)

| File | Purpose |
| --- | --- |
| `frontend/src/composables/useDirtyGuard.ts` | Dirty guard core |
| `frontend/src/composables/useDirtyGuard.test.ts` | Unit tests |
| `frontend/src/components/common/DirtyGuardConfirmDialog.vue` | Shared confirm UI (OA tokens) |
| `frontend/src/components/templates/AuthoringSideBySideLayout.vue` | Two-column / stacked layout |
| `frontend/src/components/templates/AuthoringPreviewPane.vue` | Stale badge, boundary copy, refresh |
| `ControlledStructuredContentEditor.vue` | Emit dirty / accept guard |
| `frontend/e2e/CORE-FORTRESS-F7-*.spec.ts` | E2E specs (**12/12** PASS) |
| `frontend/e2e/helpers/core-fortress-f7.ts` | E2E helpers |

---

## 7. Gate commands

| Context | Command | Result |
| --- | --- | --- |
| Full frontend gate | `pnpm -C frontend lint && pnpm -C frontend type-check && pnpm -C frontend test && pnpm -C frontend build` | **GREEN** (**894** Vitest) |
| E2E | `pnpm -C frontend exec playwright test --config playwright.docker.config.ts CORE-FORTRESS-F7` | **12/12** PASS |

---

## 8. Acceptance scenarios → tests (TDD map)

| BDD ID | Target test / artifact |
| --- | --- |
| BDD-F7-B1-001…005 | `useDirtyGuard.test.ts` + E2E dirty-guard spec |
| BDD-F7-B1-006 | Vitest `beforeunload` registration mock |
| BDD-F7-B1-007 | Metadata dialog component test + E2E (if T03b) |
| BDD-F7-B2-001…002 | `AuthoringSideBySideLayout` tests + E2E |
| BDD-F7-B2-003…004 | `AuthoringPreviewPane` tests + E2E side-by-side |
| BDD-F7-B2-005 | E2E assert boundary copy visible |
| BDD-F7-B2-006 | Vitest in-flight guard |
| BDD-F7-B2-007 | Empty state component test |

---

## 9. LRP cross-reference

| LRP task | F7 task | Notes |
| --- | --- | --- |
| **LR-C1** | F7-T02, T03, T08 | **Done** (mirrored via F7 closeout) |
| **LR-C4** | F7-T04, T05, T09 | **Done** (mirrored via F7 closeout) |
| **LR-C2** | — | Not in F7 |
| **LR-C3** | — | Not in F7 |

---

## 10. Traceability

| Document | Purpose |
| --- | --- |
| [Behavior spec F7](../../behavior/core-fortress-f7-authoring-ux.md) | BDD source |
| [Program roadmap](./CORE-FORTRESS-program-roadmap.md) | F7 status |
| [LRP-C detail](./LRP-C-usability-deepening.md) | Origin tasks |
| [ADR-0051](../../adr/rendering-authoring/0051-side-by-side-authoring-preview.md) | Architecture (v1 subset) |
| [Execution ledger](../execution-sync-ledger.md) | Gate evidence |
