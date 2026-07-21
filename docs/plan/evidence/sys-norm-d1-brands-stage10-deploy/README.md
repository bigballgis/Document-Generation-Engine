# Stage 10 deploy evidence — SYS-NORM Wave 6 (`sys-norm-d1-brands` / TM #150)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["150"]` |
| **worktree** | `D:/working/DGE-sys-norm-d1-brands` |
| **branch** | `feat/sys-norm-d1-brands` @ `cbea637b84c18b19e9199c2d1abb668c2d5e6fd0` |
| **local_time** | 2026-07-21T17:01+08:00 |
| **command** | `.\scripts\docker-deploy-queue.ps1 -SkipBuild -Reason "SYS-NORM Wave 6 #150 stage10 deploy evidence (sys-norm-d1-brands) SkipBuild"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | acquired → released; idle after; no bypass |
| **mvn verify** | **not run** |
| **mode** | SkipBuild health re-check / evidence |

## Previous known-good (Stage 5)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:aef9c6667aa150c31b9c5f34da55c485512762165a8580a5b1e8fd9825fe9929` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:816ddf72dcdec07669a7892eb03189514742a926214140674fb1651221fe7d0f` |

## Post-SkipBuild digests (local Id / RepoDigests)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:b7aa7c1dcc463a137a6da2f84bbd2d4f962948fceb6b86f9b0f52ca46585270f` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:d835a201ee6d9ce396bd40800351f3b6a6a3bd3132098a5197aeb84994970eed` |

Layer `Created` timestamps unchanged from Stage 5 (CACHED). Manifest-list Ids refreshed on image re-export.

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (len=1202) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## FE asset markers (served index)

- `assets/index-D2T0kj0X.js`
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
2. Pre-SkipBuild digests matched Stage 5 Wave 6 images; queue was idle.
3. Stack healthy after recreate; queue idle.
4. KAFKA_IMAGE bitnamilegacy warning unchanged (local/dev only).
5. Evidence left uncommitted in feature worktree (orchestrator commit before merge).
6. No merge. Not claiming Done.

### Next

Feature-branch commit (include this evidence dir), then Stage **11** — `integration-merger` (merge worktree → main + cleanup), then MAIN post-task doc-sync / commit-review.
