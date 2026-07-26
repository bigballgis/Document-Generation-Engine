# Behavior: FOS-W12 CI Gates Tell the Truth

**Status:** Confirmed for delivery  
**Traceability:** TM #182 · `fos-ci-gates-tell-truth` · W12-1…W12-4

## Goal

CI and smoke evidence are honest: Linux Surefire is race-safe, plan docs cite CI run ids,
Playwright smoke cannot be vacuously green, and smoke specs target KEEP-8 masters.

## Acceptance scenarios

### W12-1 — Temp-dir isolation
Given LibreOffice normalization / DockerExec PDF tests  
When Surefire runs on Linux CI  
Then temp directory counts use `@TempDir`-scoped roots (not a shared global tmp race).

### W12-2 — No invented GREEN
Given gate evidence for this leaf  
When recorded in plan/ledger  
Then notes cite a `gh` run id/URL or explicitly state local-only / BLOCKED.

### W12-3 — Smoke fail-not-skip
Given CI/`E2E_REQUIRE_STACK=1` and stack unreachable  
When smoke starts  
Then the job fails (probe/globalSetup) — it does not skip-all to green.  
A min-executed reporter rejects zero executed functional tests.

### W12-4 — KEEP-8 retarget
Given master DOCX replace smoke  
When selecting a master  
Then it uses Meridian Wholesale FOL Master (not purged Demo Retail Letterhead).

## Deploy honesty

GitHub-hosted Playwright Smoke is expected **BLOCKED**/fail when no lab stack is attached.
Constitution Gates (mvn/pnpm) are the merge bar when smoke is environment-blocked.
