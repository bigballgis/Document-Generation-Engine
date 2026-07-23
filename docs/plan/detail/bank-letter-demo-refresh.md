# Bank letter demo refresh — Wave A + Wave B (ops / demo content)

**Program / slice family:** `bank-letter-demo-refresh` / `bank-letter-demo-expand` (ad-hoc **NON-CE** ops-demo content program; **not** a formal P-phase; **not** IBL Wave B; **not** CE-O02)  
**Formal plan phase:** **None** — single-active-phase discipline OK (does not occupy a P* slot)  
**Task Master:** **#141** Wave A (`demo-refresh-wave-a`) → **Done** · **#142** Wave B expand (`bank-letter-demo-expand`) → **Done**  
**Active delivery slice (this family):** **none** — Wave A/B sole-active **cleared** (2026-07-20). **Successor host sole-active (2026-07-24):** TM **#164** / `demo-catalog-keep-bank-letters` → **In Progress** — see [demo-catalog-keep-bank-letters.md](./demo-catalog-keep-bank-letters.md)  
**Placement (Wave B historical):** worktree `D:/working/DGE-bank-letter-demo-expand` · `feat/bank-letter-demo-expand` → MAIN merge `288ce98f`; worktree **REMOVED**  
**Placement (Wave A historical):** worktree `D:/working/DGE-bank-letter-demo-refresh` · `feat/bank-letter-demo-refresh` → MAIN merge `aa88170f` (feature `5ae9575a`); worktree **REMOVED**  
**BDD Wave A:** [bank-letter-demo-refresh.md](../../behavior/bank-letter-demo-refresh.md) — **ready** (`BDD-DEMO-REFRESH-001…014`); `frontend_ui_in_scope=false`  
**BDD Wave B:** [bank-letter-demo-expand.md](../../behavior/bank-letter-demo-expand.md) — **ready** (16 scenarios; 7 new families); `frontend_ui_in_scope=false`  
**Batch recommendation (Wave A):** **split** (`member_task_ids: ["demo-refresh-wave-a"]`; `proposed_slice_id: bank-letter-demo-refresh`) — **closed**  
**Batch recommendation (Wave B):** **solo** (`member_task_ids: ["142"]`; `proposed_slice_id: bank-letter-demo-expand`) — **closed**

---

## Purpose

After deep-system remediations, clean shallow/padding/test-flavored demo content and refresh the **eight existing** `deploy/demo-*` packages plus **DEMO-FULL-FLOW-LETTER** to 100% realistic international/corporate bank-letter quality; then expand the demo catalogue with **seven** new Meridian bank-letter families; prove via cleanup → import-all → publish-all → generate-all evidence (**20/20** registry).

Historical TM **#4–#8** (P23-era foreign-bank-letter rewrite + publish + generate + evidence) remain **Done**. These leaves are a **new post-remediation refresh/expand wave** — do **not** reopen P22/P23 phase status.

---

## Wave status

| Wave | Slice id | TM | Scope | Status |
| --- | --- | --- | --- | --- |
| **A** | `bank-letter-demo-refresh` | **#141** (`demo-refresh-wave-a`) | Clean stack + uplift existing 8 packages + DEMO-FULL-FLOW-LETTER; import/publish/generate evidence | **Done** |
| **B** | `bank-letter-demo-expand` | **#142** | New letter families / catalogue expand (7 confirmed `DEMO-*` families) | **Done** |

---

## Exit criteria (Wave A)

| # | Criterion | Evidence (when Done) |
| --- | --- | --- |
| 1 | BDD-DEMO-REFRESH-001…014 acceptance met | Behavior + generated DOCX / manifests |
| 2 | Existing eight `deploy/demo-*` + DEMO-FULL-FLOW-LETTER read as realistic bank letters (mock parties/amounts only) | Package configs + generated `.tmp/generated_*.docx` |
| 3 | Ops path: cleanup (as needed) → `import-all-demos.ps1` → `publish-all-demos.ps1` → `generate-all-demos.ps1` | Script exit + evidence bundle **13/13** |
| 4 | Gates green for touched surfaces; queued Docker when acceptance surface requires | `mvn verify` **GREEN 2312**; FE/E2E **N/A**; `docker-deploy-queue` **DEPLOY_OK** |
| 5 | Stage 11 merge + MAIN doc-sync + commit-review | Worktree removed; sole-active cleared; merge `aa88170f` |

---

## Exit criteria (Wave B)

| # | Criterion | Evidence (when Done) |
| --- | --- | --- |
| 1 | BDD-DEMO-EXPAND-001…016 acceptance met | Behavior + generated DOCX / manifests |
| 2 | Seven new `DEMO-*` families authored (Meridian; CORP/RETAIL); commitment ≠ FOL alias | Package configs + registry |
| 3 | Ops path: import → publish → generate covers Wave A **13** + Wave B **7** = **20** | Evidence bundle **20/20** |
| 4 | Gates for touched surfaces; queued Docker | `mvn verify` **GREEN 2340**; FE E2E **N/A**; vitest baseline RED unrelated; `docker-deploy-queue` **DEPLOY_OK**; arch **merge_go** |
| 5 | Stage 11 merge + MAIN doc-sync + commit-review | Worktree removed; sole-active cleared; merge `288ce98f` |

---

## Vetoes (hard) — still in force

- **CE-O02** deferred (D5 / 2026-07-20「暂时不做」) — do **not** activate
- Checklist **#3b / #5a** — do **not** flip **GO**
- **RTL** — do **not** reopen (ADR-0068 DESCOPE)
- Do **not** invent Word-host evidence; do **not** claim go-live / IBL program Done
- Do **not** treat `DEMO-COMMITMENT-LETTER` as FOL / `CORP-FOL-OFFER` alias; do **not** rename PRD §6.7 eight product rows

---

## Owners (pipeline)

1. **doc-keeper** (stage 3) — package/README/index sync if content contracts change  
2. **rendering-engineer** (stage 4) — master assets / structured content / demo package quality uplift  
3. **build-deploy-agent** — queued Docker when import/generate evidence needs the acceptance stack

---

## Relation to closed phases

| Closed | Relation |
| --- | --- |
| **P22** Done | Engine + scaffolds — do **not** reopen |
| **P23** Done | Typography excellence + historical #4–#8 — do **not** reopen; this leaf is post-remediation **content refresh/expand** |
| **IBL / CE / CDP / LRP** | Sibling programs — Wave A/B do **not** change their wave Done/Blocked status |

---

## Ops-safe cleanup / reimport (docs SoT pointer)

Prefer overwrite; **no** reckless DB `DROP`. Canonical notes: [deploy/demo-shared/README.md](../../../deploy/demo-shared/README.md) § Ops-safe cleanup.

1. Optional: `deploy/demo-fol/cleanup-fol-test-data-sets.ps1` (`-WhatIf` first)
2. Optional narrow: `cleanup-catalog-except-fol.ps1` (not default full refresh)
3. Primary: `deploy/import-all-demos.ps1` (DRAFT reset via `demo-import-shared` when needed)
4. `publish-all-demos.ps1` → `generate-all-demos.ps1`
5. Archive under [plan/evidence/bank-letter-demo-refresh/](../evidence/bank-letter-demo-refresh/README.md) (Wave A) and [plan/evidence/bank-letter-demo-expand/](../evidence/bank-letter-demo-expand/README.md) (Wave B)

## Wave B confirmed families (#142 Done)

| # | Runtime externalId | Group | Notes |
| --- | --- | --- | --- |
| 1 | `DEMO-FACILITY-AMENDMENT` | CORP | Facility amendment / variation |
| 2 | `DEMO-KYC-CDD-NOTICE` | RETAIL | KYC / CDD notice |
| 3 | `DEMO-ACCOUNT-CLOSURE` | RETAIL | Account closure |
| 4 | `DEMO-COMMITMENT-LETTER` | CORP | Commitment — **not** FOL alias |
| 5 | `DEMO-FORMAL-DEMAND` | CORP | Formal demand |
| 6 | `DEMO-COVENANT-WAIVER` | CORP | Covenant waiver / consent |
| 7 | `DEMO-INSURANCE-ENDORSEMENT` | RETAIL | Insurance endorsement / security notice |

Full matrix + acceptance: [bank-letter-demo-expand.md](../../behavior/bank-letter-demo-expand.md) (§7; BDD-DEMO-EXPAND-001…016). Target registry: Wave A **13** + Wave B **7** = **20** — **achieved**.

## Changelog

| Date | Note |
| --- | --- |
| 2026-07-20 | Stage 12 MAIN doc-sync: Wave B **#142 → Done**; sole-active cleared; merge `288ce98f`; evidence **20/20**; gates `mvn verify` **GREEN 2340** / **DEPLOY_OK** / arch **merge_go**; do **not** flip **#3b/#5a GO** / CE-O02 / RTL |
| 2026-07-20 | Stage 2 plan-orchestrator: Wave B **#142 → In Progress** (**sole-active**); worktree `DGE-bank-letter-demo-expand` / `feat/bank-letter-demo-expand`; BDD **ready** (16 scenarios; 7 families) |
| 2026-07-20 | Stage 12 MAIN doc-sync: Wave A **#141 → Done**; sole-active cleared; Wave B **#142** pending then activated; evidence 13/13; merge `aa88170f` / feature `5ae9575a` |
| 2026-07-20 | Stage 3 doc-keeper: SoT sync (PRD/requirements/EXP/TYP/fundraising/demo-shared); evidence stub; ops-safe cleanup path; **not** claiming Done |
| 2026-07-20 | Stage 2 plan-orchestrator: registered Wave A sole-active (#141); queued Wave B (#142 pending); formal phase remains **None** |
