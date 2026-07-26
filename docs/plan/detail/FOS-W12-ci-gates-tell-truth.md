# FOS-W12 — CI gates tell the truth

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W12 · **Status:** **Done**
**Slice id:** `fos-ci-gates-tell-truth` · worktree `../DGE-fos-ci-gates-tell-truth` · branch `feat/fos-ci-gates-tell-truth`
**Task Master:** **#182** · **delivery_lane:** **full** (test/CI behaviour; may be promoted earlier if CI blocks all delivery)
**Origin:** E1, E5, E14, E15

**Note:** Orchestrator **may** promote this leaf ahead of later FOS UX leaves when
Constitution Gates on `main` stay red — still single-lane serial, still worktree.

---

## Before code

```powershell
git worktree add "..\DGE-fos-ci-gates-tell-truth" -b feat/fos-ci-gates-tell-truth origin/main
```

Inspect latest failed runs with `gh run list --branch main` and `gh run view --log-failed`.

### Tasks

| Id | Sev | Task |
| --- | --- | --- |
| W12-1 | **P0** | Fix the two Surefire failures that abort `verify` on Linux CI |
| W12-2 | **P0** | Stop claiming GREEN in plan docs without a CI run id |
| W12-3 | **P0** | Playwright smoke must not be vacuously green/skip-all |
| W12-4 | **P1** | Retarget/fix smoke specs that depend on purged demo seeds |

---

## W12-1 — Temp-dir races fail Linux CI

**Severity:** P0
**Files (from audit E14):**
- `LibreOfficeDocxNormalizationServiceTest` (counts global temp entries /
  `docxNormalizationFailed` on CI)
- `DockerExecPdfConversionServiceTest.usesDistinctContainerProfilesAcrossParallelConversions`
  (`@DisabledOnOs(WINDOWS)` — fails on Linux CI)

### Implement

Use `@TempDir`-scoped roots instead of scanning `java.io.tmpdir`. Make tests
order/concurrency safe. Remove the Windows-only exclusion by shipping `.cmd` doubles or
dropping script doubles in favour of argv-level assertions (CRCH W0-5 style).

**Done only when** GitHub `Constitution Gates` on the PR is green (or the leaf records a
still-red unrelated failure with evidence — do not claim verify GREEN from a local Windows
run alone).

---

## W12-2 — Plan docs must not invent GREEN

**Files:** any plan/ledger notes written in this leaf

### Implement

When recording gate evidence, cite `gh` run id / URL. If local verify is green but CI is
red, say so. Add a one-line rule to the leaf closeout checklist (no new constitution file
required).

---

## W12-3 — Playwright smoke can skip almost everything

**Files:** `frontend/e2e/helpers/stack-readiness.ts` (`test.skip(!ready, …)`),
`.github/workflows/playwright-smoke.yml`

### Implement

1. Ensure the smoke workflow **starts the stack** (or documents that it requires a
   pre-provisioned environment and fails the job when not ready — **fail**, not skip-all).
2. Add a minimum-executed-tests assertion (custom reporter or final test) so a run with
   0 executed functional tests cannot be green.

Do not invent a second compose project; use existing docker deploy scripts.

---

## W12-4 — Smoke specs target purged demos

**File:** `frontend/e2e/master-replace-docx.spec.ts` (`Demo Retail Letterhead` purged)

### Implement

Retarget at a KEEP-8 master from `demo-catalog-keep-bank-letters`. Commit or CI-generate
the replacement fixture (do not require a manual Maven command as the only path).

---

## Exit

Constitution Gates green on the integration PR; Playwright smoke either meaningful or
honestly failing; ledger cites CI run ids. TM **#182** → done.

**Do not** implement CRCH W5 items (LO pin, default LO smoke, perceptual diff, Word
baseline, soak) here.
