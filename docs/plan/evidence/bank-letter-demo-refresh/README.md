# Evidence — bank-letter-demo-refresh (#141 / Wave A)

**Placement:** `D:/working/DGE-bank-letter-demo-refresh` · `feat/bank-letter-demo-refresh`  
**Agent:** build-deploy-agent (Stage 4/10 follow-up after Java fix)  
**Date:** 2026-07-20

## Status (honest)

| Step | Result |
| --- | --- |
| Backend `mvn verify` | **GREEN** — 2312 tests, 0 fail, 15 skipped (`-Dsurefire.argLine=-Xmx1536m`); includes `TemplateLifecyclePublishVersionSelectionTest` 2/2 |
| Frontend | **N/A this leaf** — `frontend_ui_in_scope=false`; no `frontend/` diffs in Java-fix change set (lint/type-check/build not re-run) |
| Docker `docker-deploy-queue.ps1` | **DEPLOY_OK** — backend image rebuilt+recreated; healthz UP; UI :4173 200 |
| Ops wipe | **import overwrite** — **not** DROP; **no SQL hotfix** |
| `import-all-demos.ps1` | **OK** (8 packages + FOL) |
| `publish-all-demos.ps1` | **OK** — 13/13 PUBLISHED |
| `generate-all-demos.ps1` | **OK** — **13/13 SUCCESS** (no SQL hotfix) |

**Do not merge** from this agent. Durable generate path proven post-redeploy.

## Commands + results

```text
# cwd: D:/working/DGE-bank-letter-demo-refresh

mvn -B -ntp -f backend/pom.xml verify "-Dsurefire.argLine=-Xmx1536m"
# → Tests run: 2312, Failures: 0, Errors: 0, Skipped: 15
# → TemplateLifecyclePublishVersionSelectionTest: 2, Failures: 0
# → BUILD SUCCESS

# FE skipped (no frontend/ changes; frontend_ui_in_scope=false)

.\scripts\docker-deploy-queue.ps1 -Reason "Wave A #141 Java fix redeploy bank-letter-demo-refresh"
# → DEPLOY_OK; backend sha256:0c1ebf9735464da326293463dfb6e273877e5528410c9288d252fe7b6daf57e0
# → healthz {"status":"UP"} · UI http://localhost:4173 → 200

.\deploy\import-all-demos.ps1     # OK
.\deploy\publish-all-demos.ps1    # 13/13
.\deploy\generate-all-demos.ps1   # 13/13 SUCCESS  (exit 0)
```

## Evidence files

| File | Purpose |
| --- | --- |
| [CONTENT-SPOTCHECK.md](./CONTENT-SPOTCHECK.md) | Per-template sizeBytes + Meridian / placeholder scan |
| [generated-docx-manifest.json](./generated-docx-manifest.json) | Full generate run machine output (13/13) |
| [all-demos-publish-summary.json](./all-demos-publish-summary.json) | 13/13 publish |
| [spotcheck-sizes.json](./spotcheck-sizes.json) | Compact size + Meridian/placeholder scan |
| [import-all-demos.log](./import-all-demos.log) | Import log |
| [publish-all-demos.log](./publish-all-demos.log) | Publish log |
| [generate-all-demos.log](./generate-all-demos.log) | Generate log |
| [compose-ps.txt](./compose-ps.txt) / [images.txt](./images.txt) / [healthz.json](./healthz.json) | Stack evidence |
| `generated_*.docx` | 13 DOCX artifacts |

## Deploy notes

- Backend image rebuilt from worktree JAR (publish STOP + release_version finder).
- Frontend image unchanged (cached package) — expected for this leaf.
- Canonical compose project `documentgenerationengine`; ports 8080 / 4173.
- No SQL hotfix applied between import → publish → generate.

## Next (orchestrator / not this agent)

1. Stage 11 `integration-merger` when parent ready (this agent: **Do NOT merge**).
2. MAIN doc-sync + commit-review after merge.
