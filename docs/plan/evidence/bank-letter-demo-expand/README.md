# Evidence — bank-letter-demo-expand (#142 / Wave B)

**Status:** **Done** (2026-07-20) — Stage 11 merge `288ce98f` + Stage 12 MAIN doc-sync; worktree **REMOVED**  
**Placement (historical):** `D:/working/DGE-bank-letter-demo-expand` · `feat/bank-letter-demo-expand`  
**Agent:** build-deploy-agent (Stage 4 / 5 / 10) → integration-merger → post-task-doc-sync  
**Date:** 2026-07-20

## Intent

Archive import → publish → generate evidence for the expanded demo registry (**20** runtime `externalId`s = Wave A **13** + Wave B **7**). Catalogue expand **does not** replace PRD §6.7 eight families. Commitment ≠ FOL / `CORP-FOL-OFFER`.

| Item | Value |
| --- | --- |
| Behavior SoT | [bank-letter-demo-expand.md](../../../behavior/bank-letter-demo-expand.md) (`BDD-DEMO-EXPAND-001`…`016`) |
| Registry SoT | [deploy/demo-shared/README.md](../../../../deploy/demo-shared/README.md) |
| Prior Wave A evidence | [bank-letter-demo-refresh/](../bank-letter-demo-refresh/README.md) (**Done** 13/13) |
| Hard vetoes | Do **not** flip checklist **#3b/#5a GO**; do **not** reopen RTL / CE-O02; do **not** invent Word-host evidence; do **not** claim go-live |

## Status (honest)

| Step | Result |
| --- | --- |
| Backend `mvn verify` | **GREEN** — 2340 tests, 0 fail, 15 skipped (`-Dsurefire.argLine=-Xmx1536m`) |
| Frontend lint / type-check / build | **GREEN** (registry + e2e fixture sync in scope) |
| Frontend vitest | **RED (baseline, unrelated)** — `apiErrorCatalog` (9 missing `api.error` keys) + `openapiCodegenParity`; Wave B not blocked per Stage 4 guidance |
| Frontend E2E / UIUX | **N/A** — `frontend_ui_in_scope=false` for this leaf |
| Docker `docker-deploy-queue.ps1` | **DEPLOY_OK** — `COMPOSE_PROJECT_NAME=documentgenerationengine`; backend image rebuilt; healthz UP; UI `:5173` 200 (lab port map; `:4173` not bound) |
| Ops wipe | **import overwrite** — **not** DROP; **no SQL hotfix** |
| `import-all-demos.ps1` | **OK** (8 Wave A + 7 Wave B packages + full-flow seed) |
| `publish-all-demos.ps1` | **OK** — **20/20 PUBLISHED** |
| `generate-all-demos.ps1` | **OK** — **20/20 SUCCESS** |
| Architecture review | **merge_go** |
| Stage 11 merge | **Done** — MAIN `288ce98f`; worktree **REMOVED** |

## Wave B externalIds (+7)

| externalId | groupCode | API AD group |
| --- | --- | --- |
| `DEMO-FACILITY-AMENDMENT` | CORP | `CORP_API` |
| `DEMO-KYC-CDD-NOTICE` | RETAIL | `RETAIL_API` |
| `DEMO-ACCOUNT-CLOSURE` | RETAIL | `RETAIL_API` |
| `DEMO-COMMITMENT-LETTER` | CORP | `CORP_API` |
| `DEMO-FORMAL-DEMAND` | CORP | `CORP_API` |
| `DEMO-COVENANT-WAIVER` | CORP | `CORP_API` |
| `DEMO-INSURANCE-ENDORSEMENT` | RETAIL | `RETAIL_API` |

## Commands + results

```text
# cwd: D:/working/DGE-bank-letter-demo-expand (historical worktree)

mvn -B -ntp -f backend/pom.xml verify "-Dsurefire.argLine=-Xmx1536m"
# → Tests run: 2340, Failures: 0, Errors: 0, Skipped: 15 · BUILD SUCCESS

pnpm -C frontend lint          # GREEN
pnpm -C frontend type-check    # GREEN
pnpm -C frontend test          # RED — openapi parity / api.error catalog baseline (unrelated)
pnpm -C frontend build         # GREEN

$env:COMPOSE_PROJECT_NAME = 'documentgenerationengine'
# JWT_SECRET: non-default ≥32-byte value in .env (copied from MAIN acceptance; not committed)
.\scripts\docker-deploy-queue.ps1 -Reason "Wave B #142 bank-letter-demo-expand Stage5/10"
# → DEPLOY_OK; healthz {"status":"UP"} · UI http://localhost:5173 → 200

.\deploy\import-all-demos.ps1     # OK (after locale/UUID/list-helper fixes)
.\deploy\publish-all-demos.ps1    # 20/20
.\deploy\generate-all-demos.ps1   # 20/20 SUCCESS  (exit 0)
```

## Evidence files

| File | Purpose |
| --- | --- |
| [CONTENT-SPOTCHECK.md](./CONTENT-SPOTCHECK.md) | Per-template sizeBytes + Meridian / placeholder scan |
| [generated-docx-manifest.json](./generated-docx-manifest.json) | Full generate run machine output (20/20) |
| [all-demos-publish-summary.json](./all-demos-publish-summary.json) | 20/20 publish |
| [spotcheck-sizes.json](./spotcheck-sizes.json) | Compact size + Meridian/placeholder scan |
| [compose-ps.txt](./compose-ps.txt) / [images.txt](./images.txt) / [healthz.json](./healthz.json) | Stack evidence |
| `generated_*.docx` | 20 DOCX artifacts |

## Deploy notes

- Canonical compose project `documentgenerationengine` (worktree directory name would otherwise create a conflicting project).
- Backend image rebuilt from this worktree JAR; frontend image refreshed from registry sync build.
- Ports: backend `8080`; frontend lab map `5173→8080` (not `4173` on this host config).
- No SQL hotfix applied between import → publish → generate.

## Closeout

1. Stage 11 `integration-merger` → MAIN `288ce98f` + worktree remove — **Done**.
2. Stage 12 MAIN doc-sync — **Done**; sole-active **cleared**; do **not** flip **#3b/#5a GO** / CE-O02 / RTL.
3. Stage 13 `post-task-commit-review` — handoff (push by default).
