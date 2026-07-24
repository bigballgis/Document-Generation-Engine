# Fix — abandoned STOPPED/DEPRECATED must not block Clone

**Program / slice:** `fix-abandoned-dev-blocks-clone` (ad-hoc **NON-CE** backend lifecycle bug-fix leaf; **not** a formal P-phase; **not** IBL Wave B; **not** CE-O02)  
**Formal plan phase:** **None** — single-active-phase discipline OK (does not occupy a P* slot)  
**Task Master:** **#165** (`fix-abandoned-dev-blocks-clone`) → **Done**  
**Active delivery slice:** none (**sole-active cleared**)  
**Placement:** **ISOLATED** · worktree **REMOVED** · branch `feat/fix-abandoned-dev-blocks-clone` merged  
**Merge SHA:** `c1bb6c77` · feature commit `cfefbb55` · base_sha `79dfb64b`  
**BDD:** [fix-abandoned-dev-blocks-clone.md](../../behavior/fix-abandoned-dev-blocks-clone.md) — **ready**/shipped (`BDD-FIX-ABANDON-CLONE-001…005`); activation `frontend_ui_in_scope=true`; delivery **FE untouched** → E2E/UIUX **N/A** (acceptance via version-lines API on live stack)  
**Evidence:** [fix-abandoned-dev-blocks-clone/](../evidence/fix-abandoned-dev-blocks-clone/README.md)  
**Batch recommendation:** **solo** (`member_task_ids: ["165"]`; `proposed_slice_id: fix-abandoned-dev-blocks-clone`;
`shared_acceptance_surface: Template version-lines + Clone from published release when no real DRAFT/in-flight`;
vetoes_applied: do-not-flip-3b-5a, do-not-mark-53-106-Done, do-not-activate-119, do-not-touch-CE-O02) — **closed**

**Prior (Done, do not reopen):** NON-CE **#164** `demo-catalog-keep-bank-letters` → **Done** (`0e6d0bad`)

---

## Purpose

Close the proven bug that blank/`null` `releaseVersion` alone marks a version as in-flight, so abandoned `STOPPED`/`DEPRECATED` rows fake an in-flight line and block Clone on a published sibling; make version-lines project abandoned terminal rows honestly (own `STOPPED`/`DEPRECATED`, not package `PUBLISHED`/Live overlay).

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** |
| Formal phase | **None** |
| Host sole-active | **cleared** |
| Umbrella #53 / #106 | Registry-only — **not** Done (veto held) |
| #119 | **Blocked**/pending — **not** activated |
| Gate evidence | `mvn verify` **GREEN 2406**; Docker deploy evidence: `DEMO-COVENANT-WAIVER` version-lines `cloneable=true` for PUBLISHED `1.0.0`; abandoned `STOPPED` honest; E2E/UIUX **N/A** (API/resolver-only; FE untouched) |
| Do **not** | Flip **#3b/#5a GO**; mark **#53** / **#106** Done; activate **#119**; invent third `lineKind`; change abandon/publish state-machine; claim go-live / IBL / CE Done |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| FIX-ABANDON-T01 | Plan/TM sole-active activation + detail/ledger/index cross-links | **Done** |
| FIX-ABANDON-T02 | Backend TDD: `isInFlight` + clone guard + version-lines honesty (`BDD-FIX-ABANDON-CLONE-001…005`) | **Done** |
| FIX-ABANDON-T03 | FE honesty / Clone affordance only if required after BE (E2E/UIUX when UI changes) | **Done** (N/A — FE untouched; API acceptance) |
| FIX-ABANDON-T04 | Gates + queued deploy evidence + merge + MAIN doc-sync | **Done** (Stage 12 this closeout; Stage 13 owns commit) |

---

## Scope delivered

| Delivered | Notes |
| --- | --- |
| `TemplateCurrentVersionResolver.isInFlight` | Blank/`null` release **and** active authoring status only (`DRAFT`\|`TESTING`\|`APPROVAL`\|`PENDING_RELEASE`) |
| `findLatestPublishedVersion` | Requires non-blank `releaseVersion` |
| Clone unblocked | When only abandoned STOPPED/DEPRECATED (blank release) siblings remain |
| version-lines honesty | Abandoned row `lifecycleStatus=STOPPED` (not package Live overlay); PUBLISHED `1.0.0` `cloneable=true` |

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
- Evidence: [fix-abandoned-dev-blocks-clone/](../evidence/fix-abandoned-dev-blocks-clone/README.md)
