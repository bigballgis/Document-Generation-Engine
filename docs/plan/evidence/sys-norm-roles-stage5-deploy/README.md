# Stage 5 deploy evidence — SYS-NORM Wave 5 (`sys-norm-roles` / TM #149)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["149"]` |
| **worktree** | `D:/working/DGE-sys-norm-roles` |
| **branch** | `feat/sys-norm-roles` @ `b475166d547be6ca5744fb821b3f6268396ec6e1` (+ uncommitted Wave 5 WIP packaged into images) |
| **local_time** | 2026-07-21T14:05+08:00 |
| **command** | `.\scripts\docker-deploy-queue.ps1 -Reason "SYS-NORM Wave5 TM149 E2E stack prep (sys-norm-roles)"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | idle after release; no bypass |
| **mvn verify** | **not run** (host sole-active rule; BE already GREEN) |

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (len=1202) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## Image digests (local RepoDigests / Id)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:9bc6f9c5cd8aa0011a82b64c9d314fa11efe6dd51e36920b2487ae844abc7b79` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:ad11339b8f5dc2fa39c45d564628d318aff2820e7560a7a48cd5befa7b02e044` |

## Slice markers

| Marker | Result |
| --- | --- |
| Flyway `V75__sys_norm_role_compression.sql` | present in worktree |
| FE dist six-role codes | `DOCUMENT_AUTHOR`, `GROUP_ADMIN`, `TEMPLATE_TESTER`, `LEGAL_REVIEWER`, `AUDIT_ADMIN`, `GLOBAL_ADMIN` = true |
| Retired role codes in FE dist | `TEMPLATE_APPROVER`, `MASTER_DESIGNER` = false |

## Notes

1. First attempt: FAIL — worktree `.env` from `.env.example` had insecure JWT default (BDD-OPS-JWT-SECRET-001). Remediation: synced non-default `JWT_SECRET` from MAIN `.env` (len=64; value not recorded); set `FRONTEND_PORT=4173` + `COMPOSE_PROJECT_NAME=documentgenerationengine`.
2. Second attempt: **DEPLOY_OK** — host `mvn package` (tests skipped) + `pnpm build` + image package; backend/frontend recreated; stack healthy.
3. KAFKA_IMAGE warning (bitnamilegacy) — local/dev only; unchanged.
4. E2E **not** run in this stage (handoff to `e2e-test-engineer` against http://localhost:4173).
5. No merge.

## Artifact files

- `latest-summary.json`
- `compose-ps.txt` / `compose-ps.json`
- `healthz.json` / `healthz-meta.txt`
- `frontend-4173.txt` / `frontend-4173-headers.txt`
- `backend-image.txt` / `frontend-image.txt` / `digests.txt`
- `fe-role-markers.txt` / `v75-migration-files.txt`
- `queue-status.txt` / `git-head.txt` / `git-branch.txt`

### Next

Stage 6 `e2e-test-engineer` against http://localhost:4173 (BDD-SYS-NORM-ROLE-001…018).
