# Intranet SCA submission runbook (M9-T02 Step 2)

> **Status (2026-07-02):** Procedure + bundle script **ready in-repo**. Actual org submission **pending owner** — cannot be marked Done without ticket/scan evidence.

## Purpose

Prepare and submit CycloneDX SBOM artifacts to the bank-approved **intranet SCA** tool. Triage findings per [quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md) (block critical/high in available internal evidence).

This runbook satisfies **in-repo** Step 2 preparation. **M9-T02 Done** requires recorded submission + triage (Steps 2–4).

## Roles

| Role | Responsibility |
| --- | --- |
| **Platform engineer** | Generate SBOM, prepare bundle, attach to ticket |
| **Security reviewer** | Accept scan results, approve exceptions |
| **Dev owner** | Remediate or exception-track findings (Step 3) |

Replace `_TBD_` in checklists with named owners when assigned.

## Prerequisites

1. Green local gates on the commit being scanned (recommended):
   - `mvn -B -ntp -f backend/pom.xml verify`
   - `pnpm -C frontend lint` / `type-check` / `test` / `build`
2. SBOM tooling installed via repo lockfiles (no ad-hoc dependency switches).

## Step 1 — Generate SBOM (Done in-repo)

```powershell
.\scripts\generate-sbom.ps1
```

Outputs: `artifacts/sbom/frontend-cyclonedx.json`, `artifacts/sbom/backend-cyclonedx.json`

See [README.md](./README.md).

## Step 2 — Prepare submission bundle

```powershell
.\scripts\prepare-sca-submission-bundle.ps1
# optional: -RegenerateSbom
```

Creates `artifacts/sca-submission/<timestamp>/` with:

- `frontend-cyclonedx.json`, `backend-cyclonedx.json`
- `manifest.json` (commit SHA, SHA256, component counts)
- `SUBMISSION-CHECKLIST.md`

## Step 3 — Submit to intranet SCA (org gate)

1. Open the approved intranet SCA portal (URL and project code — **record in execution log when known**).
2. Create a new scan / component import for **Document Generation Engine**.
3. Upload **both** JSON files from the bundle directory.
4. Record in the bundle checklist:
   - Scan job ID
   - Submission timestamp
   - Submitter
5. Copy ticket reference to [sca-execution-log.md](./sca-execution-log.md).

**Do not** mark M9-T02 Done until this step has a real ticket ID.

## Step 4 — Triage findings (Step 3 in closure plan)

Per quality gate baseline ([quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md)):

| Severity | Action |
| --- | --- |
| **Critical / High** | Block release until remediated **or** documented exception (owner + expiry) |
| **Medium** | Risk note + remediation ticket with due date |
| **Low** | Track; fix in normal backlog |

Remediation options (prefer in order):

1. Bump dependency to patched version (company-approved repo).
2. Remove unused dependency.
3. Security exception with expiry and compensating control (M9-T03).

**Related in-repo hygiene:** Task Master **#49** / [deps-security-refresh.md](../../behavior/deps-security-refresh.md) → **Done** (merge `08c7d56`) — baseline-safe bumps + Vitest Critical exception (**#50**, expires 2026-10-13; Boot **3.3.x**, ShedLock **6.x**, no major Vue/Vite without ADR). That work does **not** replace org upload (Step 3 above) and does **not** mark M9-T02 Done.

## Step 5 — Re-verify gates (Step 4 in closure plan)

After any dependency change:

```powershell
mvn -B -ntp -f backend/pom.xml verify
pnpm -C frontend lint
pnpm -C frontend type-check
pnpm -C frontend test
pnpm -C frontend build
.\scripts\generate-sbom.ps1
```

Attach gate logs to the SCA ticket or [execution-sync-ledger.md](../../plan/execution-sync-ledger.md).

## Evidence recording

| Document | Use |
| --- | --- |
| [sca-execution-log.md](./sca-execution-log.md) | Per-submission cycle (ticket, result, reviewer) |
| [m9-t02-closure-plan.md](../../architecture/m9-t02-closure-plan.md) | Step status |
| [execution-sync-ledger.md](../../plan/execution-sync-ledger.md) | Milestone mirror |

## Out of scope

- Substituting local `npm audit` for intranet SCA when internal evidence is required.
- Marking M9-T02 **Done** without org scan reference.
