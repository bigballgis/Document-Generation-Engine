# PD-6 / #138 Stage 10 Deploy Evidence

- **Captured at:** 2026-07-20T13:07:58+08:00
- **Verdict:** DEPLOY_OK
- **Slice:** pd6-true-non-specimen-reissue / Task Master #138
- **Worktree:** `d:\working\DGE-pd6-true-non-specimen-reissue`
- **Branch:** `feat/pd6-true-non-specimen-reissue`
- **Git tip (HEAD):** `da4ab10b` (`da4ab10b7b61ef006a04801ebdab21d9676c873d`) — docs(IBL-E7): mark #134 Done; Wave IBL-E Done residual closeout
- **Deploy source:** Working tree dirty (30 paths). Host `mvn package` / image build used worktree files including uncommitted PD-6 changes (not HEAD-only).
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "PD-6/#138 stage10 deploy evidence (canonical project)"` (full build; not `-SkipBuild`)
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)

## Attempt log

1. FAILED — worktree `.env` from `.env.example` had insecure JWT default (BDD-OPS-JWT-SECRET-001). Synced `JWT_SECRET` from MAIN.
2. FAILED — missing `COMPOSE_PROJECT_NAME` caused fixed-name conflict (`docgen-minio`); orphan worktree-named network/volumes removed.
3. SUCCESS — canonical project + full host package + image rebuild + recreate backend/frontend.

## Health

| Probe | Result |
| --- | --- |
| `GET http://localhost:8080/healthz` | HTTP **200** — `{"status":"UP"}` |
| `GET http://localhost:4173/` | HTTP **200** (1202 bytes) |

Raw: [healthz.json](./healthz.json)

## Images

| Image | Id | Created (UTC) |
| --- | --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:d206845793e905dfe03d78eb4d22b507e8c2c2ac8d0b22952193f6b7b06c7fbb` | 2026-07-20T05:05:53Z |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:2166d3a55521193f8ff519e992bdb1a7868866d06ec6b7eb73930b1e67464b8c` | 2026-07-19T23:45:59Z |

## Compose ps

See [compose-ps.txt](./compose-ps.txt). All six services healthy; ports `8080` / `4173`.

## Queue status (post-deploy)

See [queue-status.txt](./queue-status.txt):

```
DEPLOY_QUEUE: idle (no lock)
Pending ticket files: 0
```

## Scope

- `frontend_ui_in_scope=false` — E2E not required for this stage.
- Backend behavior-changing slice — full queued deploy required and completed.
