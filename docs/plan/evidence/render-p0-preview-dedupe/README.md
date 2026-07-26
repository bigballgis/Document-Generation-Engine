# Evidence — `render-p0-preview-dedupe` (CRCH W0+W1)

**Date:** 2026-07-26  
**Worktree:** `/home/ubuntu/DGE-render-p0-preview-dedupe` (host cannot write `../DGE-*`; path recorded)  
**Branch:** `feat/render-p0-preview-dedupe`

## Gates

| Gate | Result |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | **PASS** (`/tmp/crch-mvn-verify4.log`) |
| `mvn … verify -Plibreoffice-ci` | **FAIL (honest)** — `soffice` absent on host; 8 mandatory-lane assertions failed as designed (`/tmp/crch-lo-ci.log`). Default `verify` (non-mandatory LO) **PASS**. |
| `pnpm -C frontend lint` | **PASS** |
| `pnpm -C frontend type-check` | **PASS** |
| `pnpm -C frontend test` | **PASS** (1734 tests) |
| `pnpm -C frontend build` | **PASS** |
| Docker `docker-deploy-queue.ps1` | **N/A / BLOCKED** — `docker` / `pwsh` not available on this Linux cloud agent host |
| E2E Playwright (live :4173) | **BLOCKED** on missing Docker stack; specs updated (`CE-U04` → `preview-inline-pdf-section`, exactly-one viewer assert) |
| UIUX screenshots | **BLOCKED** on missing stack; selector updated in UIUX evidence spec |

## W1-6

See [W1-6-legacy-authoring-finding.md](./W1-6-legacy-authoring-finding.md).
