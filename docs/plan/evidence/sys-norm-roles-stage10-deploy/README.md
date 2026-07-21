# Stage 10 deploy evidence — SYS-NORM Wave 5 (`sys-norm-roles` / TM #149)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["149"]` |
| **worktree** | `D:/working/DGE-sys-norm-roles` |
| **branch** | `feat/sys-norm-roles` @ `3b208c61af7e9debeead8772a6a02e868e4c404a` |
| **local_time** | 2026-07-21T14:47+08:00 |
| **command** | `.\scripts\docker-deploy-queue.ps1 -SkipBuild -Reason "SYS-NORM Wave5 TM149 stage10 evidence SkipBuild"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | acquired → released; idle after; no bypass |
| **mvn verify** | **not run** |
| **mode** | SkipBuild health re-check / evidence |

## Previous known-good (Stage 5)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:9bc6f9c5cd8aa0011a82b64c9d314fa11efe6dd51e36920b2487ae844abc7b79` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:ad11339b8f5dc2fa39c45d564628d318aff2820e7560a7a48cd5befa7b02e044` |

## Post-SkipBuild digests (local Id / RepoDigests)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:163617876c0aa051732eaec3f35c7ebe2c5420af091f04a8d24dfcc0b873bb8a` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:63b4c73d210a27f8e2f42d2c1733a4b690de4176d55b8556c082410e909f69ac` |

Layer `Created` timestamps unchanged from Stage 5 (CACHED). Manifest-list Ids refreshed on image re-export.

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (len=1202) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## FE asset markers (served index)

- `assets/index-DoiaxH4V.js`
- `assets/index-DgfinNic.css`
- `assets/vendor-Xw4Mn6wR.js`
- `assets/vue-vendor-YCgdmryA.js`
- `assets/element-plus-DdFImHIC.js`
- `assets/element-plus-CzM4-epj.css`
- `assets/app-vendor-BKMZwS8Q.js`
- `assets/element-icons-D44Rupkn.js`

## Artifact files

- `latest-summary.json`
- `compose-ps.txt` / `compose-ps.json` / `compose-ls.txt`
- `healthz.json` / `healthz-meta.txt`
- `frontend-4173.txt` / `frontend-4173-headers.txt`
- `backend-image.txt` / `frontend-image.txt` / `digests.txt` / `images.txt`
- `backend-imagetools.txt` / `frontend-imagetools.txt`
- `backend-runtime.txt` / `frontend-runtime.txt`
- `fe-asset-markers.txt`
- `queue-status.txt` / `git-head.txt` / `git-branch.txt` / `git-log.txt`

## Notes

1. Queued SkipBuild only — no `mvn verify`; no second compose project / port offsets.
2. Stack healthy after recreate; queue idle.
3. KAFKA_IMAGE bitnamilegacy warning unchanged (local/dev only).
4. No merge.

### Next

Stage **11** — `integration-merger` (merge worktree → main + cleanup), then MAIN post-task doc-sync / commit-review.
