---
id: DOC-BEHAVIOR-SYS-NORM-DEMO-SEED-TERMS
type: Behavior Spec
status: Confirmed
readiness: ready
program: SYS-NORM
wave: 8
slice: sys-norm-demo-seed-terms
taskMaster: "152"
frontend_ui_in_scope: true
backend_seed_in_scope: true
related:
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/product/business-terminology-guide.md
  - docs/behavior/ce-e02-asset-library.md
  - docs/behavior/ce-g04-legal-hold.md
  - docs/behavior/core-fortress-f1-rendering-correctness.md
  - docs/behavior/prod-true-prod-contract.md
  - docs/behavior/sys-norm-shell-fluid-nav.md
  - docs/behavior/sys-norm-hub-ia.md
  - docs/behavior/sys-norm-roles.md
---

# SYS-NORM Wave 8 — Demo/验收 seed · honest empty · L1 Letterhead/母版

> **Slice:** `sys-norm-demo-seed-terms` · TM **#152** → **Done** (MAIN `8aca145b` / feature
> `7df6c563`; worktree **REMOVED**; SYS-NORM Waves **0–8 Done** — **program Done**).  
> **Placement:** merged · sole-active **cleared**.  
> **Locks:** charter [system-normalization-program.md](./system-normalization-program.md)
> §2.8 / §2.9; plan [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md)
> Wave 8 + N13 / N15 / N16–N17 / N21 / N23; terminology
> [business-terminology-guide.md](../product/business-terminology-guide.md).  
> **Formal phase:** **None**.  
> **Still do not:** flip checklist **#3b** / **#5a**; mark **#53** Done; claim parked UX Done
> (Reminder timing / Asset group isolation / Binding re-layout / Auto `referenceKey`);
> claim **N18** Done; activate CE-O02.

```
bdd_readiness: ready
frontend_ui_in_scope: true
backend_seed_in_scope: true
open_questions: []
pending_non_blocking:
  - DOCUMENT_AUTHOR L1 EN/ZH display label finalize (charter P-Q1) — interim OK; capacity residual
  - N18 Legal hold actor EntityLink — EXPLICITLY DEFERRED (does not block Wave 8 Done)
  - N19–N20 / N22 residuals — capacity; defer with evidence if not in leaf
owning_doc: docs/behavior/sys-norm-demo-seed-terms.md
task_ids: ["152"]
queue_slice_id: sys-norm-demo-seed-terms
scenario_ids:
  - BDD-SYS-NORM-W8-001 … BDD-SYS-NORM-W8-018
scenario_count: 18
batch_recommendation:
  decision: solo
  member_task_ids: ["152"]
  proposed_slice_id: sys-norm-demo-seed-terms
  shared_acceptance_surface: asset seed/honest empty + L1 Letterhead/母版 + N13/N15/N16/N17/N21/N23
  vetoes_applied:
    - checklist-#3b/#5a
    - CE-O02
    - "#53"
    - parked-UX-not-in-W8
  evidence_amortization: one FE/BE evidence run for seed+terms
  on_red_split_hint: If seed BE fails, peel terminology FE to follow-up leaf
```

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  rationale: >
    Wave 8 is sole remaining program wave; parked UX is a different risk domain —
    do not mega-merge
  member_task_ids: ["152"]
  proposed_slice_id: sys-norm-demo-seed-terms
  shared_acceptance_surface: >
    asset seed/honest empty + L1 Letterhead/母版 sweep + N13/N15/N16/N17/N21/N23
  evidence_amortization: one FE/BE evidence run for seed+terms
  on_red_split_hint: If seed BE fails, peel terminology FE to follow-up leaf
```

| IN（本叶） | OUT（明确禁止 / 后续） |
| --- | --- |
| Asset library **honest empty** as product default when zero managed `library_asset` | Parked: Reminder timing |
| Optional **demo/验收 seed** path (profile/Flyway) for managed assets — documented | Parked: Asset library group isolation |
| **N23** docs clarity: `demo-images` classpath ≠ Asset Library | Parked: Binding editor re-layout |
| **N13** Legal hold empty catalog honest empty | Parked: Auto `referenceKey` |
| **N16–N17** L1 EN **Letterhead** / ZH **母版**; purge L1 Master mix | **N18** Legal hold actor EntityLink (**deferred**) |
| **N15** Master revision empty design summary → honest empty | Flip **#3b** / **#5a**; mark **#53** Done |
| **N21** Role journey timeline silent empty → honest empty | Claim SYS-NORM program Done before Wave 8 impl Done |
| Residuals N19–N20 / N22 / P-Q1 labels **as capacity allows** | Change F1 resolver signature / production demo-tier default |

---

## 1. Actor / role

| Actor | Role / scope |
| --- | --- |
| `GLOBAL_ADMIN` / `GROUP_ADMIN` | Open Asset Library + Legal holds; create holds / upload assets in scope |
| `DOCUMENT_AUTHOR` | Letterhead (master) revision design surfaces; role journey empty states |
| `TEMPLATE_TESTER` / `LEGAL_REVIEWER` / `AUDIT_ADMIN` | Role journey empty guidance when queues empty |
| Demo / 验收 operator | Uses profile-gated seed so catalogs are non-empty for walkthroughs |
| Platform / docs reader | Understands `demo-images` bypass vs managed asset (N23) |
| Unauthorized / out-of-group | Fail-closed; no fabricated rows |

---

## 2. User goal

1. When Asset Library or Legal holds have **zero** in-scope rows, see an **honest empty**
   (title + next-step copy + CTA when permitted) — never a silent blank table or fake sample data.
2. On **demo/验收** stacks, optionally bootstrap minimal **managed** library assets so operators
   can walk image/seal bindings without relying on classpath bypass alone.
3. Understand that `rendering/demo-images/` classpath resolution is a **LAB/test/rendering**
   fallback tier — **not** a substitute for Asset Library catalog rows (N23).
4. See L1 English **Letterhead** and Chinese **母版** consistently; no mixed “Master” primary
   labels on L1 (API/L3 may keep `masterId`).
5. Master revision **design** tab and role **journey timeline** show honest empty copy when
   there is nothing to summarize / no current step (N15 / N21).

---

## 3. Trigger

- Operator opens Asset Library (`/library/assets`) or Legal holds catalog with zero rows.
- Demo/验收 bootstrap enables optional seed profile / Flyway seed for managed assets.
- Operator opens Letterhead (master) revision **design** tab with empty change/design summary.
- Dashboard / hub role journey timeline has no current step or empty work set.
- Reader consults terminology guide / ops docs for `demo-images` vs managed asset.

---

## 4. Preconditions

- Waves **0–7** Done; sole-active cleared before this leaf activates.
- CE-E02 Asset Library and CE-G04 Legal Hold product surfaces exist.
- F1 image resolver order (MinIO → optional demo classpath → fail-closed) unchanged in signature.
- ADR-0070 / ADR-0071 Accepted; D1 brand/entity surfaces retired.
- Formal phase **None**; checklist **#3b** / **#5a** untouched; **#53** not Done.
- Implementation only in isolated worktree (not MAIN).

---

## 5. Confirmed decisions (Wave 8 locks)

| ID | Decision | Source |
| --- | --- | --- |
| **W8-C1** | **Coherent empty story:** Product default for zero managed assets / zero legal holds = **honest empty** (EmptyStatePanel or equivalent: title + description + optional CTA). **Not** fabricated sample metrics/rows. | charter §2.9; handoff |
| **W8-C2** | **Optional demo/验收 seed path (documented):** A profile-gated or Flyway seed **may** insert minimal managed `library_asset` rows (e.g. keys aligned with demo bindings such as `IMG-1` / `SEAL-1`) for acceptance walkthroughs. Seed is **not** required for production empty correctness. If seed is absent, W8-C1 still holds. | handoff prefer honest empty + seed path documented |
| **W8-C3** | **N23 — demo-images vs managed asset:** Classpath `rendering/demo-images/` (and `DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED`) is a **rendering fallback** for LAB/test when object storage miss — **must not** be documented or UX-implied as “Asset Library content.” Management Asset Library lists only **managed** `library_asset` rows. Production/true-prod default keeps demo tier **off** ([prod-true-prod-contract.md](./prod-true-prod-contract.md) TPC-C7). Do **not** change F1 resolver signature (CE-E02 E02-C13). | N23; F1; CE-E02; TPC |
| **W8-C4** | **N13 — Legal hold empty catalog:** Zero holds in scope → honest empty + Create CTA when `manageLegalHold`; without manage capability → honest empty **without** create CTA (not silent blank). | N13 |
| **W8-C5** | **N16–N17 — L1 terminology:** EN primary object label **Letterhead**; ZH **母版**. Purge user-facing mixed “Master” / “Master documents” as **L1 primary** labels (nav titles, page H1, primary buttons, journey step object nouns where the object is the DOCX letterhead). API / L3 / L2 technical fields may keep `masterId`, `MasterDocument`, route segments, audit codes. | charter §2.8; terminology guide |
| **W8-C6** | **N15 — Master revision empty design:** Design workspace summary with no change summary / empty design evidence shows **honest empty** copy (reason + next step), not a silent empty card body. | N15 |
| **W8-C7** | **N21 — Role journey silent empty:** When there is no current journey step / empty work set, timeline surfaces **honest empty guidance** (existing `*.empty.guidance` keys or equivalent). **Forbidden:** render a blank timeline region with neither steps nor guidance copy. | N21 |
| **W8-C8** | **N18 remains explicitly deferred** — Wave 8 Done **must not** claim Legal hold actor EntityLink. | Wave 1 deferral; handoff |
| **W8-C9** | **Parked UX OUT OF SCOPE:** Reminder timing; Asset library group isolation; Binding editor re-layout; Auto `referenceKey`. Post-program parked queue only. | plan §4a |
| **W8-C10** | **P-Q1 non-blocking:** `DOCUMENT_AUTHOR` L1 EN/ZH display finalize remains Pending if capacity insufficient; interim labels OK (Wave 5). Defer with evidence — does not block Wave 8 Done. | charter §3 P-Q1 |
| **W8-C11** | **Permissions:** No new capability codes. Seed/admin paths remain fail-closed by existing matrix. | matrix |
| **W8-C12** | **Governance vetoes:** Do not flip #3b/#5a; do not mark #53 Done; do not claim program Done until Wave 8 **implementation** Done + N* closed or deferred with evidence. | plan §5–§6 |

---

## 6. Primary journey

### 6.1 Asset Library empty / seed

1. Authorized operator opens Asset Library with zero managed assets → honest empty + Upload CTA when permitted.
2. On demo/验收 stack with seed enabled → catalog shows seeded managed assets (not classpath-only ghosts).
3. Docs/ops note states demo-images tier is rendering fallback only (N23).

### 6.2 Legal hold empty (N13)

1. Authorized operator opens Legal holds with zero rows → honest empty + Create when permitted.
2. Read-only / no-manage session → honest empty without create action.

### 6.3 L1 terminology sweep (N16–N17)

1. Operator switches `en` / `zh-CN` on Letterhead (master) nav, catalogs, hubs, primary CTAs.
2. EN shows **Letterhead**; ZH shows **母版**; L1 primary no longer mixes bare “Master” as the object noun.
3. Technical `masterId` fields remain on L2/L3.

### 6.4 Empty design + journey (N15 / N21)

1. Open Letterhead revision design with empty summary → honest empty copy.
2. Open dashboard/hub journey with no current step → guidance paragraph visible (not silent blank).

---

## 7. System responses

| Situation | Response |
| --- | --- |
| Zero managed assets | Honest empty UI; optional Upload CTA |
| Demo/验收 seed on | Managed rows visible in Asset Library list API + UI |
| Demo tier off + missing object | Rendering fail-closed (existing F1 codes) |
| Zero legal holds | Honest empty; Create CTA iff manage capability |
| L1 EN master surfaces | **Letterhead** primary labels |
| L1 ZH master surfaces | **母版** primary labels |
| Empty design summary | Honest empty (not silent card) |
| Journey no current step | `*.empty.guidance` (or equivalent) visible |
| Unauthorized seed/admin | `401` / `403` fail-closed |
| N18 actor column | Unchanged plain text / deferred — **not** claimed fixed |

---

## 8. Acceptance scenarios (Given / When / Then)

### BDD-SYS-NORM-W8-001 — Asset library honest empty (default)

**Given** an authorized session whose Asset Library list returns zero managed `library_asset` rows in scope  
**When** the operator opens Asset Library  
**Then** the UI shows an **honest empty** state (title + description; English-first i18n)  
**And** does **not** fabricate sample assets or silent blank chrome  
**And** when the operator may upload/manage assets, a primary Upload/Create CTA is available  
**And** when not permitted, the CTA is absent (fail-closed presentation)

### BDD-SYS-NORM-W8-002 — Demo/验收 optional managed-asset seed path

**Given** a demo/验收 environment with the Wave 8 seed path **enabled** (profile and/or Flyway — implementer picks one durable mechanism)  
**When** bootstrap completes and Asset Library is listed  
**Then** at least the minimal documented managed keys (aligned with demo bindings, e.g. `IMG-1` / `SEAL-1` if retained) appear as **managed** library assets  
**And** the seed path is documented in this leaf’s ops/plan notes  
**And** production/default profiles without the seed still satisfy **W8-001**

### BDD-SYS-NORM-W8-003 — N23 docs: demo-images bypass ≠ Asset Library

**Given** F1 resolver may use classpath `rendering/demo-images/` when the demo tier flag/profile is on  
**When** Wave 8 documentation (behavior + terminology/ops pointer as needed) is read  
**Then** it states clearly that classpath demo images are a **rendering fallback for LAB/test**, not Asset Library catalog content  
**And** management Asset Library must not imply classpath-only keys are “in the library”  
**And** CE-E02 / F1 resolver **signatures** remain unchanged (E02-C13)

### BDD-SYS-NORM-W8-004 — Production / true-prod demo tier default off

**Given** a production-claiming or true-prod contract profile  
**When** configuration defaults are inspected  
**Then** demo classpath image tier is **off** by default (TPC-C7)  
**And** missing managed object bytes fail closed at render — not silently filled from undocumented paths

### BDD-SYS-NORM-W8-005 — N13 Legal hold empty catalog (manage)

**Given** an authorized session with legal-hold manage capability and zero holds in scope  
**When** the operator opens the Legal holds catalog  
**Then** an honest empty state is shown (title + description)  
**And** a Create Legal hold CTA is available from the empty state

### BDD-SYS-NORM-W8-006 — N13 Legal hold empty catalog (no manage)

**Given** a session that can view Legal holds but **cannot** manage/create  
**And** zero holds are in scope  
**When** the catalog opens  
**Then** honest empty is shown  
**And** Create CTA is **absent**  
**And** the page is not a silent blank table

### BDD-SYS-NORM-W8-007 — N16 L1 English Letterhead

**Given** English locale (`en`)  
**When** an operator views L1 primary Letterhead (master) surfaces in scope of this sweep (nav item label, catalog/page titles, primary CTAs that name the object)  
**Then** the primary business label is **Letterhead** (or established compound such as “Letterhead package” where already SSOT)  
**And** bare “Master” / “Master documents” is not used as the L1 primary object noun on those surfaces

### BDD-SYS-NORM-W8-008 — N17 L1 Chinese 母版

**Given** Chinese locale (`zh-CN`)  
**When** the same L1 primary surfaces render  
**Then** the primary business label is **母版** (or established compound such as “母版包” per terminology SSOT)  
**And** L1 does not use 主文档 as the primary object noun

### BDD-SYS-NORM-W8-009 — Purge L1 Master mix; keep L3 identifiers

**Given** the Wave 8 terminology sweep  
**When** L1 copy and L2/L3 technical fields are compared  
**Then** residual user-facing Master mix on L1 primary surfaces in scope is removed or remapped per W8-C5  
**And** API paths, `masterId` fields, OpenAPI schema names, and audit codes may still say `master`  
**And** L2 field labels that intentionally expose “Master ID” remain allowed per terminology guide

### BDD-SYS-NORM-W8-010 — Terminology guide SSOT pointer

**Given** Wave 8 L1 sweep lands  
**When** [business-terminology-guide.md](../product/business-terminology-guide.md) SYS-NORM section is read  
**Then** Letterhead / 母版 remain Confirmed  
**And** the guide no longer claims Wave 8 L1 sweep as merely “pending” once implementation Done syncs (implementation leaf updates status; this BDD locks the acceptance)

### BDD-SYS-NORM-W8-011 — English-first i18n for changed strings

**Given** any new or changed user-facing strings in this leaf  
**When** locales are checked  
**Then** English values exist in `en.ts` first  
**And** zh-CN mirrors business meaning  
**And** no hardcoded user-facing strings appear in changed Vue surfaces

### BDD-SYS-NORM-W8-012 — N15 Master revision empty design summary

**Given** a Letterhead (master) revision whose design/change summary is empty  
**When** the operator opens the revision **design** workspace tab  
**Then** the summary area shows an **honest empty** state (localized reason + next-step guidance)  
**And** does not leave a silent empty card body with no explanation

### BDD-SYS-NORM-W8-013 — N21 Role journey timeline honest empty

**Given** a role journey timeline with steps defined but **no** current step / empty work set (guidance key resolves to `*.empty.guidance` or equivalent)  
**When** the timeline renders on dashboard or hub surfaces in scope  
**Then** empty guidance copy is visible to the operator  
**And** the timeline region is not a silent blank (no copy, no steps, no guidance)

### BDD-SYS-NORM-W8-014 — N21 Forbidden silent empty when steps array empty

**Given** a misconfigured or empty `steps` array for a journey surface that still mounts the timeline chrome  
**When** the component renders  
**Then** the UI provides an honest empty / fallback guidance (or hides the chrome entirely with an equivalent empty panel)  
**And** does **not** claim a working journey while showing nothing

### BDD-SYS-NORM-W8-015 — N18 remains deferred (explicit)

**Given** Wave 8 delivery scope  
**When** Legal hold catalog actor/username columns are inspected  
**Then** EntityLink for Legal hold actor (**N18**) is **not** required for Wave 8 Done  
**And** Wave 8 Done evidence must **not** claim N18 closed  
**And** N18 remains queued for a later leaf with dedicated BDD before code

### BDD-SYS-NORM-W8-016 — Parked UX out of scope

**Given** Wave 8 leaf  
**When** implementers assess scope  
**Then** Reminder timing, Asset library group isolation, Binding editor re-layout, and Auto `referenceKey` are **not** implemented  
**And** they remain on the post-program parked queue (plan §4a)

### BDD-SYS-NORM-W8-017 — Residuals capacity / defer with evidence

**Given** capacity constraints during Wave 8 implementation  
**When** residuals N19–N20 (EntityLink where-used / MasterImpact), N22 (catalog-wide Actions pattern), or P-Q1 `DOCUMENT_AUTHOR` L1 label finalize are not completed  
**Then** each unfinished item is listed as **deferred with evidence** in post-task doc-sync  
**And** does not silently flip to Done  
**And** does not block Wave 8 Done if W8-C1…C9 acceptance scenarios for in-scope N* are green

### BDD-SYS-NORM-W8-018 — Governance boundaries (non-goals)

**Given** Wave 8 delivery scope  
**When** implementers or reviewers assess Done  
**Then** the leaf does **not** flip go-live checklist **#3b** / **#5a**  
**And** does **not** mark CE umbrella **#53** Done  
**And** does **not** claim SYS-NORM program Done until Wave 8 implementation + N* close/defer evidence land  
**And** formal phase remains **None**  
**And** CE-O02 remains Deferred

---

## 9. Boundary / exception behavior

| Case | Behavior |
| --- | --- |
| Zero assets, seed off | Honest empty (W8-001) |
| Seed on, wrong profile | Seed must not activate on true-prod defaults |
| Classpath demo hit with empty library | Allowed for LAB render only; catalog still empty (N23) |
| Cross-group asset/hold | Fail-closed group isolation (existing CE-E02 / CE-G04) |
| Unauthorized create/upload | `403` / UI CTA hidden |
| L1 vs L3 master wording | L1 Letterhead/母版; L3 `masterId` OK |
| N18 actor cell | Deferred — no Wave 8 requirement to EntityLink |
| Parked UX request | Refuse / queue outside Wave 8 |

---

## 10. Observable evidence

| Evidence | Proves |
| --- | --- |
| Asset Library empty E2E / Vitest | W8-001 |
| Seed migration or profile + list API/UI | W8-002 (`backend_seed_in_scope`) |
| Docs/N23 pointer + F1 signature unchanged | W8-003 / W8-004 |
| Legal hold empty E2E / Vitest | W8-005 / W8-006 |
| i18n snapshot / nav contract for Letterhead/母版 | W8-007…W8-011 |
| Master revision design empty state test | W8-012 |
| RoleJourneyTimeline empty guidance test | W8-013 / W8-014 |
| Deferred table in doc-sync (N18 + residuals) | W8-015…W8-017 |
| Checklist / #53 / program status untouched | W8-018 |
| FE gates + BE verify (if seed) + E2E + queued deploy | Wave 8 Done criteria |

---

## 11. Traceability

| Source | Link / note |
| --- | --- |
| User / program lock | 2026-07-21 SYS-NORM; Wave 8 close-out |
| Charter | [system-normalization-program.md](./system-normalization-program.md) §2.8–§2.9, §8 Wave 8 |
| Program plan | [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) §3 Wave 8, §4 N13/N15/N16–N17/N21/N23, §4a parked |
| Terminology SSOT | [business-terminology-guide.md](../product/business-terminology-guide.md) |
| Seed / N23 ops | [demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md) |
| Asset library | [ce-e02-asset-library.md](./ce-e02-asset-library.md) |
| Legal hold | [ce-g04-legal-hold.md](./ce-g04-legal-hold.md) |
| Demo classpath | [core-fortress-f1-rendering-correctness.md](./core-fortress-f1-rendering-correctness.md); [prod-true-prod-contract.md](./prod-true-prod-contract.md) |
| Catalog / empty UX | [catalog-navigation-ux.md](../product/catalog-navigation-ux.md) |
| N18 deferral evidence | [sys-norm-shell-fluid-nav.md](./sys-norm-shell-fluid-nav.md) Explicitly deferred |
| Task | TM **#152** · `sys-norm-demo-seed-terms` |

---

## 12. Handoff notes (plan-orchestrator / implementers)

1. Activate TM **#152** / sole-active; keep Batch **solo**.
2. Prefer TDD: empty-state + i18n contract tests Red first; seed Flyway/profile behind demo/验收 only.
3. If seed BE fails gates → peel terminology FE per `on_red_split_hint`.
4. Do **not** fold parked UX; do **not** claim N18.
5. After gates: stage 11 merge → MAIN doc-sync (Wave 8 Done criteria) — **not** claimed by this BDD-authoring stage.
6. Terminology guide docs-locked Letterhead/母版 (stage 3); flip “sweep Done” banner only at
   implementation Done sync (post-task) — do not claim i18n Done from docs-first alone.

---

## 13. Stage-1 authoring metadata

```text
bdd_readiness: ready
behavior_doc: docs/behavior/sys-norm-demo-seed-terms.md
scenario_ids: BDD-SYS-NORM-W8-001 … BDD-SYS-NORM-W8-018
frontend_ui_in_scope: true
backend_seed_in_scope: true
deferred:
  - N18 Legal hold actor EntityLink (explicit; does not block Wave 8 Done)
  - Parked UX queue (Reminder timing; Asset group isolation; Binding re-layout; Auto referenceKey)
  - P-Q1 DOCUMENT_AUTHOR L1 label finalize (capacity residual)
  - N19–N20 / N22 residuals (capacity; defer with evidence if not in leaf)
open_questions: []
task_ids: ["152"]
```
