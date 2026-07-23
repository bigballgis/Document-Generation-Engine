# Stage 10 deploy evidence — PQH Leaf 3 N22 catalog row actions (TM #162)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["162"]` |
| **worktree** | `D:/working/DGE-pqh-n22-catalog-row-actions` |
| **branch** | `feat/pqh-n22-catalog-row-actions` @ `c0cc57a0` |
| **evidence_tip** | `c0cc57a0` (E2E/UIUX evidence commits after Stage 5; app runtime still N22 from Stage 5) |
| **stage5_runtime_tip** | `c094d513` (product FE) |
| **local_time** | 2026-07-23T19:36:33+08:00 |
| **command** | `.\scripts\docker-deploy-queue.ps1 -SkipBuild -Reason "Stage10 PQH N22 #162 health recheck tip c0cc57a0 (images match Stage5)"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | acquired → released; idle after; no bypass |
| **mvn verify** | **not run** |
| **mode** | SkipBuild health re-check / evidence |

## Previous known-good (Stage 5)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:ac50ebecdf42191b7f975d61b307ddeed7281785a2c72ed017ba918971c2b454` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:fec2e9a139da5b7dc816899d4c575a533be83e5402ce95fca533b0e1ceaf3668` |

## Post-SkipBuild digests (local Id / manifest list)

| Image | Id |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:c9aa310196e36938978c9107cd91751713a2eabdd2bdfd13bcd43ff7a5dcef2a` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:c28f788431f236047715acbe939aef7a96298a56743d4eb20a30ad7dacbf034b` |

Docker `COPY` layers were **CACHED** (jar/dist content unchanged from Stage 5). Manifest-list Ids refreshed on SkipBuild image re-export (same pattern as prior Stage 10 leaves).

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

Chunk filenames match Stage 5 evidence (`ApiInvocationsView-BbLet8Dv.js`, `AssetLibraryListView-CSEbCDKq.js`, `index-CdDevzTw.js`).

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

1. Pre-check: queue idle; :8080/:4173 healthy; running images matched Stage 5 digests → chose `-SkipBuild`.
2. Queued SkipBuild only — no second compose project / port offsets.
3. KAFKA_IMAGE bitnamilegacy warning unchanged (local/dev only).
4. **No merge.** Task **#162** not marked Done.

### Next

Stage **11** — `integration-merger` (merge worktree → main + cleanup), then MAIN post-task doc-sync / commit-review.
