# Stage 10 deploy evidence — PQH Leaf 3 N22 catalog row actions (TM #162)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["162"]` |
| **worktree** | `D:/working/DGE-pqh-n22-catalog-row-actions` |
| **branch** | `feat/pqh-n22-catalog-row-actions` @ `e60b488f` |
| **evidence_tip** | `e60b488f` (E2E/UIUX evidence commits after Stage 5; app runtime still N22 from Stage 5) |
| **stage5_runtime_tip** | `c094d513` (product FE) |
| **prior_stage10_tip** | `c0cc57a0` (refreshed for current tip) |
| **local_time** | 2026-07-23T19:58:49+08:00 |
| **command** | Locked `.\scripts\docker-deploy.ps1 -SkipBuild` (queue `-SkipBuild` splat does not forward switch; mutex held) |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | acquired → released; idle after; no bypass |
| **mvn verify** | **not run** |
| **mode** | SkipBuild health re-check / evidence refresh |

## Previous known-good (Stage 5)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:ac50ebecdf42191b7f975d61b307ddeed7281785a2c72ed017ba918971c2b454` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:fec2e9a139da5b7dc816899d4c575a533be83e5402ce95fca533b0e1ceaf3668` |

## Post-SkipBuild digests (local Id)

| Image | Id |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:7a76976af1eae43ec924d908de68507ebeb3ead75e47da209fc2bb253254ca7f` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:3a09d46600ae0abbd1171515767a9153db18997c12e541c420827e67fa437cd3` |

SkipBuild restarted existing images only (no host compile / image rebuild).

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (ContentLength=1202) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## Slice markers (served FE)

| Marker | Present |
| --- | --- |
| `TableEditMoreActions` | yes |
| `AssetLibraryListView` | yes |
| `LegalHoldListView` | yes |
| `ApiInvocationsView` | yes |

## Artifact files

- `latest-summary.json`
- `compose-ps.txt` / `compose-ps.json` / `compose-ls.txt`
- `healthz.json` / `healthz-meta.txt`
- `frontend-4173.txt` / `frontend-4173-headers.txt`
- `backend-image.txt` / `frontend-image.txt` / `digests.txt` / `images.txt` / `digest-compare.txt`
- `backend-imagetools.txt` / `frontend-imagetools.txt`
- `backend-runtime.txt` / `frontend-runtime.txt`
- `fe-slice-markers.txt` / `fe-asset-markers.txt`
- `queue-status.txt` / `git-head.txt` / `git-branch.txt` / `git-status-sb.txt` / `git-log-1.txt` / `git-log.txt`

## Notes

1. Pre-check: tip `e60b488f` is evidence-only after Stage 5 app runtime; :8080/:4173 healthy → SkipBuild.
2. `docker-deploy-queue.ps1 -SkipBuild` currently does **not** forward `-SkipBuild` to `docker-deploy.ps1` (string array splat); Stage 10 used **locked** direct `docker-deploy.ps1 -SkipBuild`.
3. Single compose project; no port offsets / second stack.
4. KAFKA_IMAGE bitnamilegacy warning unchanged (local/dev only).
5. **No merge.** Task **#162** not marked Done.

### Next

Stage **11** — `integration-merger` (merge worktree → main + cleanup), then MAIN post-task doc-sync / commit-review.
