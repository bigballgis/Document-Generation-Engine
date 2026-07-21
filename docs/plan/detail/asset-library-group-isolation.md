# Asset library group isolation

**Program / slice:** `asset-library-group-isolation` (post-SYS-NORM parked UX §4a; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#154** → **In Progress**  
**Active delivery slice:** `asset-library-group-isolation` (**sole-active**)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-asset-library-group-isolation` · branch `feat/asset-library-group-isolation` · base `2fcd3f74897a55749f18dea979c92d391806ef6b`  
**BDD:** [asset-library-group-isolation.md](../../behavior/asset-library-group-isolation.md) — **ready** (`BDD-ALGI-001…018`); CE-E02 §15 amendment; `frontend_ui_in_scope=true`; `backend_api_contract_change=true`  
**Upstream:** SYS-NORM Waves **0–8 Done** (program **Done**); §4a Reminder timing **#153** → **Done**; this leaf was parked #2  
**Migration:** **ALGI-M1** quarantine-disable + admin rehome (locked)  
**Batch recommendation:** **solo** (`member_task_ids: ["154"]`; `proposed_slice_id: asset-library-group-isolation`;
`shared_acceptance_surface: Asset library + resolve within group`;
`evidence_amortization: one verify + FE + E2E + deploy`;
vetoes: Binding editor, Auto `referenceKey`, checklist-#3b/#5a, CE-O02, #53;
`on_red_split_hint: N/A solo`) — **open**

---

## Purpose

Deliver **group-scoped** Asset library hard isolation, superseding CE-E02 platform-shared catalog:

- Every managed asset owned by exactly one business `groupCode` — identity `(groupCode, assetKey)`
- List / upload / disable scoped by authorized groups; GLOBAL may see all or filter
- Template `imageRef` / `sealRef` resolve only against template owning group's ACTIVE assets
- MinIO physical key namespaced `{groupCode}/{assetKey}` (bindings stay bare `assetKey`)
- Migration **ALGI-M1**: fail-closed quarantine (DISABLED) of legacy unscoped rows + admin rehome — **not** silent assign-to-`PLATFORM`/`CORP` as ACTIVE
- Demo/验收 seed (when enabled) writes group-scoped ACTIVE keys per seeded business group

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** |
| Formal phase | **None** |
| Host sole-active | **#154** `asset-library-group-isolation` (**In Progress**) — only host delivery leaf |
| Program | SYS-NORM Waves **0–8 Done** — program **Done**; §4a Asset library → **In Progress**; siblings Binding editor / Auto `referenceKey` stay **Parked** |
| Gate evidence | `[]` (plan activation only — product implementation not started) |
| Do **not** | Activate Binding editor / Auto `referenceKey`; flip **#3b/#5a**; mark **#53** Done; invent `PLATFORM` group; claim Done without gates |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| ALGI-T01 | Register TM **#154** + plan/ledger sole-active mirror | **In Progress** (this stage) |
| ALGI-T02 | Doc-keeper: permission-matrix / domain / OpenAPI / PRD CE-E02 group-scope sync | **Not Started** |
| ALGI-T03 | Backend: Flyway `groupCode` + composite uniqueness + ALGI-M1 quarantine | **Not Started** |
| ALGI-T04 | Backend: list/upload/disable API group-scoped + namespaced MinIO | **Not Started** |
| ALGI-T05 | Backend: template resolve gated by template `groupCode` (ALGI-C5) | **Not Started** |
| ALGI-T06 | Backend: demo seeder group-scopes keys when enabled (ALGI-C13) | **Not Started** |
| ALGI-T07 | Frontend: ScopedGroupSelect filter + upload requires group | **Not Started** |
| ALGI-T08 | FE/BE unit tests (TDD) for ALGI-001…014/017/018 | **Not Started** |
| ALGI-T09 | E2E + UIUX (BDD-ALGI-015…016 + regressions) | **Not Started** |
| ALGI-T10 | Queued docker deploy evidence + merge + MAIN doc-sync | **Not Started** |

---

## Exit criteria (from BDD-ALGI-001…018)

| # | Criterion | BDD | Status |
| --- | --- | --- | --- |
| 1 | Upload requires `groupCode` and scopes ownership | ALGI-001 | **Not Started** |
| 2 | Upload missing `groupCode` rejected | ALGI-002 | **Not Started** |
| 3 | Upload to unauthorized group fail-closed | ALGI-003 | **Not Started** |
| 4 | Uniqueness is per `(groupCode, assetKey)` | ALGI-004 | **Not Started** |
| 5 | List scoped to authorized groups | ALGI-005 | **Not Started** |
| 6 | GLOBAL_ADMIN lists all and can filter | ALGI-006 | **Not Started** |
| 7 | Unauthorized `groupCode` filter empty (no leak) | ALGI-007 | **Not Started** |
| 8 | Disable scoped + removes namespaced object | ALGI-008 | **Not Started** |
| 9 | Disable outside authorized group forbidden | ALGI-009 | **Not Started** |
| 10 | Resolve only within template group ACTIVE | ALGI-010 | **Not Started** |
| 11 | Resolve ignores foreign MinIO bare key | ALGI-011 | **Not Started** |
| 12 | Migration quarantine disables legacy rows | ALGI-012 | **Not Started** |
| 13 | Admin rehome after quarantine | ALGI-013 | **Not Started** |
| 14 | SEAL upload still admin-gated and group-scoped | ALGI-014 | **Not Started** |
| 15 | FE group filter + upload requires group (E2E) | ALGI-015 | **Not Started** |
| 16 | FE GLOBAL can clear/omit filter to see all (E2E) | ALGI-016 | **Not Started** |
| 17 | Demo seeder group-scopes keys when enabled | ALGI-017 | **Not Started** |
| 18 | TEMPLATE_TESTER read-only ACTIVE in scope | ALGI-018 | **Not Started** |

---

## Gate evidence

| Gate | Result |
| --- | --- |
| Backend `mvn verify` | **Pending** |
| Frontend lint / type-check / test / build | **Pending** |
| Stage 5 + 10 queued deploy | **Pending** |
| E2E functional | **Pending** |
| UIUX | **Pending** |
| Architecture / CQ | **Pending** |
| Stage 11 merge | **Pending** |

---

## Out of scope this leaf

- Parked §4a siblings: Binding editor re-layout; Auto `referenceKey`  
- Flipping checklist **#3b** / **#5a**  
- Marking umbrella **#53** Done  
- Activating CE-O02 / RTL  
- Inventing a durable `PLATFORM` asset-owner business group  
- Cross-group share / copy-on-read / global asset pool (ALGI-C11)  
- Silent ACTIVE assign-all-to-`CORP` / `PLATFORM` (rejected by ALGI-M1)

## Related docs

| Doc | Role |
| --- | --- |
| [asset-library-group-isolation.md](../../behavior/asset-library-group-isolation.md) | BDD SoT (**ready**) |
| [ce-e02-asset-library.md](../../behavior/ce-e02-asset-library.md) §15 | Historical CE-E02 + ALGI amendment |
| [demo-acceptance-asset-seed.md](../../operations/demo-acceptance-asset-seed.md) | Seed contract amend (ALGI-C13) |
| [permission-matrix.md](../../security/permission-matrix.md) | Group-scope CE-E02 actions (doc-keeper) |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | §4a parked queue — Asset library **In Progress** |
| [openapi-v1.yaml](../../api/openapi-v1.yaml) | List/upload/disable + `groupCode` (doc-keeper) |
