# Fix — abandoned STOPPED/DEPRECATED must not block Clone

**Program / slice:** `fix-abandoned-dev-blocks-clone` (ad-hoc **NON-CE** backend lifecycle bug-fix leaf; **not** a formal P-phase; **not** IBL Wave B; **not** CE-O02)  
**Formal plan phase:** **None** — single-active-phase discipline OK (does not occupy a P* slot)  
**Task Master:** **#165** (`fix-abandoned-dev-blocks-clone`) → **In Progress** (sole-active)  
**Active delivery slice:** `fix-abandoned-dev-blocks-clone`  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-fix-abandoned-dev-blocks-clone` · branch `feat/fix-abandoned-dev-blocks-clone` · base_sha `79dfb64b`  
**BDD:** [fix-abandoned-dev-blocks-clone.md](../../behavior/fix-abandoned-dev-blocks-clone.md) — **ready** (`BDD-FIX-ABANDON-CLONE-001…005`); `frontend_ui_in_scope=true`  
**Batch recommendation:** **solo** (`member_task_ids: ["165"]`; `proposed_slice_id: fix-abandoned-dev-blocks-clone`;
`shared_acceptance_surface: Template version-lines + Clone from published release when no real DRAFT/in-flight`;
vetoes_applied: do-not-flip-3b-5a, do-not-mark-53-106-Done, do-not-activate-119, do-not-touch-CE-O02)

**Prior (Done, do not reopen):** NON-CE **#164** `demo-catalog-keep-bank-letters` → **Done** (`0e6d0bad`)

---

## Purpose

Close the proven bug that blank/`null` `releaseVersion` alone marks a version as in-flight, so abandoned `STOPPED`/`DEPRECATED` rows fake an in-flight line and block Clone on a published sibling; make version-lines project abandoned terminal rows honestly (own `STOPPED`/`DEPRECATED`, not package `PUBLISHED`/Live overlay).

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** |
| Formal phase | **None** |
| Host sole-active | **#165** / `fix-abandoned-dev-blocks-clone` |
| Umbrella #53 / #106 | Registry-only — **not** Done (veto held) |
| #119 | **Blocked**/pending — **not** activated |
| Gate evidence | Pending implementation (Stage 4+) |
| Do **not** | Flip **#3b/#5a GO**; mark **#53** / **#106** Done; activate **#119**; invent third `lineKind`; change abandon/publish state-machine; claim go-live / IBL / CE Done |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| FIX-ABANDON-T01 | Plan/TM sole-active activation + detail/ledger/index cross-links | **In Progress** (this Stage 2) |
| FIX-ABANDON-T02 | Backend TDD: `isInFlight` + clone guard + version-lines honesty (`BDD-FIX-ABANDON-CLONE-001…005`) | **Not Started** |
| FIX-ABANDON-T03 | FE honesty / Clone affordance only if required after BE (E2E/UIUX when UI changes) | **Not Started** |
| FIX-ABANDON-T04 | Gates + queued deploy evidence + merge + MAIN doc-sync | **Not Started** |

---

## Scope

| IN | OUT |
| --- | --- |
| Correct `isInFlight` (blank release **and** active authoring status) | Abandon / publish / restore transition changes |
| Clone succeeds when only abandoned STOPPED/DEPRECATED (blank release) siblings exist | New Clone API / new error codes |
| version-lines show abandoned STOPPED as **STOPPED** | New `TemplateVersionLineKind` value |
| Real in-flight still blocks Clone (`409 TEMPLATE_DEV_LINE_IN_FLIGHT`) | Checklist **#3b** / **#5a** GO flips |

---

## Traceability

- Behavior: [fix-abandoned-dev-blocks-clone.md](../../behavior/fix-abandoned-dev-blocks-clone.md)
- Catalog nav: [catalog-navigation-ux.md](../../product/catalog-navigation-ux.md) **BDD-TEMPLATE-PACKAGE-NAV-001** S4/S7
- Domain: [domain-model.md](../../domain/domain-model.md) §2.10–2.11
