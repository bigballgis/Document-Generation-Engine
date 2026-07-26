# FOS-W14 — Demo literacy path

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W14 · **Status:** **Not Started**
**Slice id:** `fos-demo-literacy-path` · worktree `../DGE-fos-demo-literacy-path` · branch `feat/fos-demo-literacy-path`
**Task Master:** **#184** · **delivery_lane:** **full** if scripts/UI touched; **light** only if pure docs (BDD must prove E1–E5)
**Origin:** E13, E19, E20  
**Related:** [FOS-W15](./FOS-W15-word-foundation-honesty.md) extends literacy for letterhead / FOL volume≠layout / money-formatter honesty (WF-4, WF-7, WF-8).

---

## Before code

```powershell
git worktree add "..\DGE-fos-demo-literacy-path" -b feat/fos-demo-literacy-path origin/main
```

**No new demo products.** Work only with KEEP-8 bank letters from
[demo-catalog-keep-bank-letters.md](./demo-catalog-keep-bank-letters.md).

### Tasks

| Id | Sev | Task |
| --- | --- | --- |
| W14-1 | **P1** | `demo-runtime-generate` must fail (not skip) when KEEP-8 missing |
| W14-2 | **P2** | Linux/macOS entry point for import/publish demos |
| W14-3 | **P2** | Learner walkthrough page: import → open → understand |
| W14-4 | **P2** | Migrate demo SQL from legacy `blocks` to `nodes` shape (optional if large — may split) |

---

## W14-1 — Demo runtime generate skips when catalog incomplete

**File:** `frontend/e2e/demo-runtime-generate.spec.ts`

### Implement

For KEEP-8 templates, **fail** when missing/not PUBLISHED (skip only for explicitly
optional extras). Ensure the suite is invoked from an acceptance path (workflow or
documented deploy evidence step) — do not leave it as a never-run script.

---

## W14-2 — Demo load path is PowerShell-only

**Files:** `deploy/import-all-demos.ps1`, `publish-all-demos.ps1`, `generate-all-demos.ps1`

### Implement

Add a bash or `pwsh`-oriented Linux entry (thin wrapper calling the same scripts via
PowerShell Core, which the repo already documents for docker-deploy-queue). Do not
reintroduce Java `ApplicationRunner` auto-seed (retired).

---

## W14-3 — No learner-facing walkthrough

### Implement

Add a short page under `docs/` (e.g. `docs/operations/demo-learner-walkthrough.md`) linked
from `docs/README.md` and `deploy/README.md`: import → publish → open one KEEP-8 template
→ where variables/bindings/clauses live → copy contract. English-first.

---

## W14-4 — Demo SQL still uses legacy `blocks` shape

Migrate at least one KEEP-8 package’s content modules to the `nodes` shape the editor
emits, so learners see the same schema. If all eight are too large for one leaf, migrate
**one** flagship letter and record the residual.

---

## Exit

KEEP-8 generate proof fails closed; walkthrough linked; Linux entry works on acceptance
host. TM **#184** → done. After Leaf 14, FOS program may move to Done per program §9.
