# Evidence — bank-letter-demo-refresh (#141 / Wave A)

**Status:** **Done** (2026-07-20) — MAIN merge `aa88170f` · feature tip `5ae9575a` · worktree **REMOVED**  
**Placement (historical):** `D:/working/DGE-bank-letter-demo-refresh` · `feat/bank-letter-demo-refresh`  
**Agent:** build-deploy-agent (Stage 4/10) + rendering-engineer; Stage 12 MAIN doc-sync  
**Date:** 2026-07-20

## Status (honest)

| Step | Result |
| --- | --- |
| Backend `mvn verify` | **GREEN** — 2312 tests, 0 fail, 15 skipped (`-Dsurefire.argLine=-Xmx1536m`); includes `TemplateLifecyclePublishVersionSelectionTest` 2/2 |
| Frontend / E2E / UIUX | **N/A** — `frontend_ui_in_scope=false` |
| Architecture | **PASS_WITH_NOTES** `merge_go=true` |
| Docker `docker-deploy-queue.ps1` | **DEPLOY_OK** — backend image rebuilt+recreated; healthz UP; UI :4173 200 |
| Ops wipe | **import overwrite** — **not** DROP; **no SQL hotfix** |
| `import-all-demos.ps1` | **OK** (8 packages + FOL) |
| `publish-all-demos.ps1` | **OK** — 13/13 PUBLISHED |
| `generate-all-demos.ps1` | **OK** — **13/13 SUCCESS** (no SQL hotfix) |
| Same-release publish supersede + `DEACTIVATE_VERSION` audit | **Landed** (feature commit) |

## Commands + results

```text
# cwd: D:/working/DGE-bank-letter-demo-refresh (historical)

mvn -B -ntp -f backend/pom.xml verify "-Dsurefire.argLine=-Xmx1536m"
# → Tests run: 2312, Failures: 0, Errors: 0, Skipped: 15
# → TemplateLifecyclePublishVersionSelectionTest: 2, Failures: 0
# → BUILD SUCCESS

# FE/E2E skipped (frontend_ui_in_scope=false)

.\scripts\docker-deploy-queue.ps1 -Reason "Wave A #141 Java fix redeploy bank-letter-demo-refresh"
# → DEPLOY_OK; healthz {"status":"UP"} · UI http://localhost:4173 → 200

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
| [compose-ps.txt](./compose-ps.txt) / [images.txt](./images.txt) / [healthz.json](./healthz.json) | Stack evidence |
| `generated_*.docx` | 13 DOCX artifacts |

## Deploy notes

- Backend image rebuilt from worktree JAR (publish STOP + release_version finder).
- Frontend image unchanged (cached package) — expected for this leaf.
- Canonical compose project `documentgenerationengine`; ports 8080 / 4173.
- No SQL hotfix applied between import → publish → generate.

## Closeout

1. Stage 11 `integration-merger` → MAIN `aa88170f` · worktree removed.
2. Stage 12 MAIN doc-sync — Wave A **Done**; sole-active cleared; **#142** remains pending.
