# Stage 10 deploy evidence — SYS-NORM Wave 3 loadFailed honesty

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["147"]` |
| **worktree** | `D:/working/DGE-sys-norm-external-ops` |
| **branch** | `feat/sys-norm-external-ops` @ `10901432` |
| **local_time** | 2026-07-21T08:23:26+08:00 |
| **utc_time** | 2026-07-21T00:23:26Z |
| **command** | `pwsh .\scripts\docker-deploy-queue.ps1 -Reason "SYS-NORM-W3 stage10 loadFailed honesty"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | acquired → released; idle after; no bypass |
| **rebuild** | full (`mvn package` + `pnpm build` + image package) — FE composable + i18n after last deploy |

## Previous known-good (rollback)

From Stage 5b / nginx SPA prep:

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:aa2d5d8f4937681f42d5b0a74ce0b0b2cfcbbf179cf0cab57536ee790872da3b` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:2e1a331ade71a437dc2ce7ee8ad84bf2de193fa9bb9263b5fc022a25323ac928` |

## Post-redeploy digests (manifest list)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:7f71df96d3c2478294a872c09da4e15b3bd2018e4296593a3dd42f02e9d3d341` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:b1f704dc730e7b78cbb5c3bddbdea4ed2a8843d674da52fc3f82c7a316eff8c2` |

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (len=1202) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## FE asset markers (served index)

- `assets/index-BquduXgs.js`
- `assets/index-DgfinNic.css`
- `assets/vendor-Xw4Mn6wR.js`
- `assets/vue-vendor-YCgdmryA.js`
- `assets/element-plus-DdFImHIC.js`
- `assets/element-plus-CzM4-epj.css`
- `assets/app-vendor-BKMZwS8Q.js`
- `assets/element-icons-D44Rupkn.js`

## Artifacts

- `latest-summary.json`
- `compose-ps.txt`
- `images.txt`
- `fe-asset-markers.txt`

## Notes

1. Full queued rebuild required: FE composable + i18n changed for loadFailed honesty after prior Stage 5b deploy.
2. Backend image layers largely CACHED; new manifest digest published. Frontend image rebuilt with new `dist` (new JS/CSS hashes).
3. Single acceptance stack only — no second compose project / port offsets.
4. KAFKA_IMAGE bitnamilegacy warning unchanged (local/dev only).

## Ready for

Stage **11** — `integration-merger` (merge worktree → main + cleanup), then MAIN post-task doc-sync / commit-review.
