# Bank letter demo refresh — Wave A (ops / demo content)

**Program / slice family:** `bank-letter-demo-refresh` (ad-hoc **NON-CE** ops-demo content leaf; **not** a formal P-phase; **not** IBL Wave B; **not** CE-O02)  
**Formal plan phase:** **None** — single-active-phase discipline OK (does not occupy a P* slot)  
**Task Master:** **#141** Wave A (`demo-refresh-wave-a`) · **#142** Wave B expand (`bank-letter-demo-expand`, queued)  
**Active delivery slice:** `bank-letter-demo-refresh` — Wave A → **In Progress** (sole-active, 2026-07-20)  
**Queued next (not activated):** `bank-letter-demo-expand` — Wave B new letter families → **Not Started** / TM **pending**  
**Placement:** **ISOLATED** — worktree `D:/working/DGE-bank-letter-demo-refresh` · `feat/bank-letter-demo-refresh` (base `origin/main`)  
**BDD:** [bank-letter-demo-refresh.md](../../behavior/bank-letter-demo-refresh.md) — **ready** (`BDD-DEMO-REFRESH-001…014`); `frontend_ui_in_scope=false`  
**Batch recommendation:** **split** (`member_task_ids: ["demo-refresh-wave-a"]`; `proposed_slice_id: bank-letter-demo-refresh`; on_red_split_hint: Peel CORP vs RETAIL vs TRADE)

---

## Purpose

After deep-system remediations, clean shallow/padding/test-flavored demo content and refresh the **eight existing** `deploy/demo-*` packages plus **DEMO-FULL-FLOW-LETTER** to 100% realistic international/corporate bank-letter quality; prove via cleanup → import-all → publish-all → generate-all evidence.

Historical TM **#4–#8** (P23-era foreign-bank-letter rewrite + publish + generate + evidence) remain **Done**. This leaf is a **new post-remediation refresh wave** — do **not** reopen P22/P23 phase status.

---

## Wave status

| Wave | Slice id | TM | Scope | Status |
| --- | --- | --- | --- | --- |
| **A** | `bank-letter-demo-refresh` | **#141** (`demo-refresh-wave-a`) | Clean stack + uplift existing 8 packages + DEMO-FULL-FLOW-LETTER; import/publish/generate evidence | **In Progress** (sole-active) |
| **B** | `bank-letter-demo-expand` | **#142** | New letter families / catalogue expand | **Not Started** (queued; do **not** activate) |

---

## Exit criteria (Wave A)

| # | Criterion | Evidence (when Done) |
| --- | --- | --- |
| 1 | BDD-DEMO-REFRESH-001…014 acceptance met | Behavior + generated DOCX / manifests |
| 2 | Existing eight `deploy/demo-*` + DEMO-FULL-FLOW-LETTER read as realistic bank letters (mock parties/amounts only) | Package configs + generated `.tmp/generated_*.docx` |
| 3 | Ops path: cleanup (as needed) → `import-all-demos.ps1` → `publish-all-demos.ps1` → `generate-all-demos.ps1` | Script exit + evidence bundle |
| 4 | Gates green for touched surfaces; queued Docker when acceptance surface requires | `mvn verify` / FE N/A if UI untouched; `docker-deploy-queue.ps1` as required |
| 5 | Stage 11 merge + MAIN doc-sync + commit-review | Worktree removed; sole-active cleared |

---

## Vetoes (hard)

- **CE-O02** deferred (D5 / 2026-07-20「暂时不做」) — do **not** activate
- Checklist **#3b / #5a** — do **not** flip **GO**
- **RTL** — do **not** reopen (ADR-0068 DESCOPE)
- **Wave B** expand — **OUT OF SCOPE** this leaf (queued as #142 only)
- Do **not** invent Word-host evidence; do **not** claim go-live / IBL program Done

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
| **P23** Done | Typography excellence + historical #4–#8 — do **not** reopen; this leaf is post-remediation **content refresh** |
| **IBL / CE / CDP / LRP** | Sibling programs — Wave A does **not** change their wave Done/Blocked status |

---

## Ops-safe cleanup / reimport (docs SoT pointer)

Prefer overwrite; **no** reckless DB `DROP`. Canonical notes: [deploy/demo-shared/README.md](../../../deploy/demo-shared/README.md) § Ops-safe cleanup.

1. Optional: `deploy/demo-fol/cleanup-fol-test-data-sets.ps1` (`-WhatIf` first)
2. Optional narrow: `cleanup-catalog-except-fol.ps1` (not default full refresh)
3. Primary: `deploy/import-all-demos.ps1` (DRAFT reset via `demo-import-shared` when needed)
4. `publish-all-demos.ps1` → `generate-all-demos.ps1`
5. Archive under [plan/evidence/bank-letter-demo-refresh/](../evidence/bank-letter-demo-refresh/README.md) when available

## Changelog

| Date | Note |
| --- | --- |
| 2026-07-20 | Stage 3 doc-keeper: SoT sync (PRD/requirements/EXP/TYP/fundraising/demo-shared); evidence stub; ops-safe cleanup path; **not** claiming Done |
| 2026-07-20 | Stage 2 plan-orchestrator: registered Wave A sole-active (#141); queued Wave B (#142 pending); formal phase remains **None** |
