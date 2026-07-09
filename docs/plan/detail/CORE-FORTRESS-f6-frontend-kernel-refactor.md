# CORE-FORTRESS F6 — Frontend Kernel Refactor (Detailed Plan)

**Program ID:** `CORE-FORTRESS`  
**Phase ID:** `CORE-FORTRESS-F6-FRONTEND-KERNEL-REFACTOR`  
**Phase status:** **Done** (2026-07-09)  
**Depends on:** CORE-FORTRESS F5 (**Done** 2026-07-09)  
**Parallel with:** CORE-FORTRESS F7 (authoring UX — coordinate merge on `ControlledStructuredContentEditor` / dev-workspace)  
**BDD:** `docs/behavior/core-fortress-f6-frontend-kernel-refactor.md` — **ready** (`BDD-CORE-FORTRESS-F6-001`)

> **Single-active-phase invariant:** **F6 Done** (2026-07-09). **F7 Done** (2026-07-09). **F8** sole formal `In Progress`. F1–F7 **Done**.

> **Baseline (SOR-F01 Done 2026-07-04):** `useTemplateDetailController` **408** lines; siblings: `useTemplateLifecycleActions` **658**, `useTemplateDetailNavigation` **542**, `useTemplatePolicyCredentials` **154**; **46** Vitest tests across composables.

---

## 1. North star

**Template detail frontend kernel is modular, tested, and under file-size budget** — domain composables for preview, lifecycle gates/actions, and navigation/journey; thin controller facade; **zero user-visible behavior change**; Bank OA style lock preserved.

---

## 2. Scope (in) / out (out)

| In scope (F6) | Out of scope (F7 / Done elsewhere) |
| --- | --- |
| F6-A1: Extract `useTemplatePreviewActions` (test generate, batch, preview select) | **F7** dirty guard (`useDirtyGuard`) |
| F6-A2: Split `useTemplateLifecycleActions` → gate / decision / governance composables | **F7** side-by-side authoring preview |
| F6-A3: Split `useTemplateDetailNavigation` → tabs / journey / load composables | New API endpoints or OpenAPI changes |
| F6-A4: Slim controller facade; return-shape stability test | Visual redesign / new OA tokens |
| F6-A5: Vitest migration + parity gates | Backend `mvn verify` (no backend changes) |
| Line-count budget enforcement (≤350 per composable) | Splitting `TemplateDetailView.vue` unless required for wiring |
| | **SOR-F01** already extracted policy composable — regression only |

### Already done — do NOT re-implement

| Asset | Evidence |
| --- | --- |
| `useTemplatePolicyCredentials.ts` | SOR-F01 slice 2; 15 Vitest tests |
| `useTemplateLifecycleActions.ts` (monolith) | SOR-F01 slice 1; 15 Vitest tests — **F6 splits, not deletes** |
| `useTemplateDetailNavigation.ts` (monolith) | SOR-F01 slice 3; 16 Vitest tests — **F6 splits** |
| WorkspaceTabShell + journey read-only | P12-UIUX-DEEP-REFACTOR Done |

---

## 3. Exit criteria

1. **A1–A3:** New composables landed; no composable > **350** lines; controller ≤ **250** lines.
2. **A4:** Return-shape stability — `TemplateDetailView` needs **no** prop/handler renames.
3. **A5:** `pnpm -C frontend lint && type-check && test && build` — **GREEN**; composable tests ≥ **54** (46 baseline + ≥8 new).
4. **No visual diff:** No template `.vue` layout/class changes except import paths.
5. **Doc sync:** roadmap, ledger, behavior index — post closeout (F6-T09).

---

## 4. Task breakdown

| ID | Owner | Task | Depends on | Status |
| --- | --- | --- | --- | --- |
| **F6-T01** | behavior-spec-author | **BDD behavior spec** — `core-fortress-f6-frontend-kernel-refactor.md` + this plan | — | **Done** (2026-07-09; readiness `ready`) |
| **F6-T02** | frontend-engineer | **A1 preview extract** — `useTemplatePreviewActions.ts` + tests; wire controller; BDD-F6-A1-* | F6-T01, F5 Done | **Done** (2026-07-09) |
| **F6-T03** | frontend-engineer | **A2 lifecycle split** — e.g. `useTemplateLifecycleGates.ts`, `useTemplateLifecycleDecisions.ts`; migrate tests from monolith | F6-T01, F5 Done | **Done** (2026-07-09) |
| **F6-T04** | frontend-engineer | **A3 navigation split** — e.g. `useTemplateDetailTabs.ts`, `useTemplateJourneyContext.ts`, `useTemplateDetailLoad.ts`; migrate tests | F6-T01, F5 Done | **Done** (2026-07-09) |
| **F6-T05** | frontend-engineer | **A4 facade slim** — controller ≤250 lines; add return-shape stability test (`useTemplateDetailController.test.ts`) | F6-T02–T04 | **Done** (2026-07-09; controller **243** lines) |
| **F6-T06** | code-quality-reviewer | **Hygiene pass** — dead exports, DRY duplicates across split files, naming consistency | F6-T05 | **Done** (2026-07-09) |
| **F6-T07** | build-deploy-agent | **Frontend gates** — lint, type-check, test, build evidence in ledger | F6-T05 | **Done** (2026-07-09; **874** Vitest total; composable **73** = 46 baseline + 27 new) |
| **F6-T08** | e2e-test-engineer | **Parity smoke** — rerun existing template detail / dev-workspace Playwright specs (no new journeys required) | F6-T07 | **Done** (2026-07-09; Playwright docker **9/9** — `P21-T06b`, `template-dev-workspace`, `fol-corporate-catalog`; FOL import fix `orderedList`→`list`+`ordered`) |
| **F6-T09** | post-task-doc-sync | **Plan + ledger closeout** — mark F6 Done; update roadmap + behavior index | F6-T07 green; F6-T08 env blocker documented | **Done** (2026-07-09) |

**Task count:** **9** (F6-T01 … F6-T09)

---

## 5. Recommended wave order

```text
Wave 0 — BDD + plan (Done)
  F6-T01

Wave 1 — Extractions (parallel after F5 Done)
  F6-T02 (preview)
  F6-T03 (lifecycle split) — parallel OK
  F6-T04 (navigation split) — parallel OK

Wave 2 — Facade + quality
  F6-T05 → F6-T06

Wave 3 — Gates + closeout
  F6-T07 → F6-T08 → F6-T09
```

**Parallel note:** F6-T02–T04 may run concurrently with F7-T02–T05 if merge conflicts are resolved at `TemplateDetailDevWorkspace.vue` / controller wiring — prefer **F6 first** on shared files, then F7 UX layers on top.

---

## 6. Target composable map (post-F6)

| Composable | Responsibility | Est. lines |
| --- | --- | --- |
| `useTemplateDetailController` | Facade wiring + re-exports | ≤250 |
| `useTemplatePreviewActions` | Test generate, batch, preview select | ~120 |
| `useTemplateLifecycleGates` | Publish/submit/binding gate load + state | ~200 |
| `useTemplateLifecycleDecisions` | Decision dialogs, submit/publish/governance handlers | ~250 |
| `useTemplateDetailTabs` | Tab lists, route query sync | ~180 |
| `useTemplateJourneyContext` | Journey visibility + context objects | ~200 |
| `useTemplateDetailLoad` | loadTemplate shell, skeleton, backToList | ~120 |
| `useTemplatePolicyCredentials` | *(unchanged)* | ~154 |

---

## 7. Gate commands

| Context | Command |
| --- | --- |
| TDD inner loop | `pnpm -C frontend test --run useTemplate` |
| Full frontend gate | `pnpm -C frontend lint && pnpm -C frontend type-check && pnpm -C frontend test && pnpm -C frontend build` |
| E2E parity (optional) | `pnpm -C frontend exec playwright test --config playwright.docker.config.ts -g "template detail"` |

---

## 8. Acceptance scenarios → tests (TDD map)

| BDD ID | Target test / artifact |
| --- | --- |
| BDD-F6-A1-001 | `useTemplatePreviewActions.test.ts` |
| BDD-F6-A1-002 | Same — batch path |
| BDD-F6-A1-003 | Same — preview select |
| BDD-F6-A2-001 | `useTemplateLifecycleGates.test.ts` |
| BDD-F6-A2-002 | `useTemplateLifecycleDecisions.test.ts` |
| BDD-F6-A2-003 | Same — governance |
| BDD-F6-A3-001 | `useTemplateDetailTabs.test.ts` |
| BDD-F6-A3-002 | `useTemplateJourneyContext.test.ts` |
| BDD-F6-A3-003 | `useTemplateDetailLoad.test.ts` |
| BDD-F6-A4-001 | `useTemplateDetailController.test.ts` return-shape snapshot |
| BDD-F6-A4-002 | CI script or manual line-count check in review |
| BDD-F6-A5-001/002 | Full frontend gate |

---

## 9. LRP / SOR cross-reference

| Source | F6 relationship |
| --- | --- |
| **SOR-F01** | Partial Done — F6 completes kernel decomposition |
| **LR-C1 / LR-C4** | Owned by **F7** — not F6 |
| **OPT-G3 / P12 D1–D3** | Historical — controller extraction Done |

---

## 10. Traceability

| Document | Purpose |
| --- | --- |
| [Behavior spec F6](../../behavior/core-fortress-f6-frontend-kernel-refactor.md) | BDD source |
| [Program roadmap](./CORE-FORTRESS-program-roadmap.md) | F6 status |
| [System optimization review](../system-optimization-review-2026-07.md) | SOR-F01 baseline |
| [Execution ledger](../execution-sync-ledger.md) | Gate evidence |
