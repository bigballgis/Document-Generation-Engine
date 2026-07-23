# Stage 10 — deploy evidence

- **slice**: pqh-n19-n20-entitylink
- **worktree**: `D:/working/DGE-pqh-n19-n20-entitylink`
- **branch**: `feat/pqh-n19-n20-entitylink`
- **tip**: `293562e99dc66a859709e2aa16734321eddb3382` (short `293562e9`)
- **captured_at**: 2026-07-23T12:54:13+08:00
- **result**: DEPLOY_OK
- **mode**: queued health re-check with `COMPOSE_PROJECT_NAME=documentgenerationengine`

## Deploy

- Queue: `.\scripts\docker-deploy-queue.ps1 -SkipBuild -Reason "stage10 pqh-n19-n20-entitylink deploy evidence"`
- First attempt without `COMPOSE_PROJECT_NAME` conflicted with the live `documentgenerationengine` stack (container name reuse). Retry with `COMPOSE_PROJECT_NAME=documentgenerationengine` succeeded.
- Host compile + image bake used **CACHED** app layers (no FE/BE code drift vs Stage 5 content). Containers recreated and healthy.
- `COMPOSE_PROJECT_NAME=documentgenerationengine`
- `FRONTEND_PORT=4173`
- JWT_SECRET: present in worktree `.env`; non-default check = **True** (length 64; value not recorded)

## Health

| Probe | URL | Status |
| --- | --- | --- |
| Backend healthz | http://localhost:8080/healthz | **200** |
| Frontend UI | http://localhost:4173/ | **200** |

healthz body:
```
{"status":"UP"}
```

## Images (post Stage 10 recreate)

- backend: `sha256:f9f1f2986648057267be0f6c09ee3489bd8396bed5348aafca198b44ed639f26` created `2026-07-23T04:52:58.730392407Z`
- frontend: `sha256:1ca5feadc48e1ced295c12e3c87b4c970e10ae3da0f3fb481d832115e3939951` created `2026-07-23T04:53:00.150192722Z`

## compose ps

```
NAME              IMAGE                                      SERVICE           STATUS                        PORTS
docgen-backend    documentgenerationengine-docgen-backend    docgen-backend    Up (healthy)                  0.0.0.0:8080->8080/tcp
docgen-frontend   documentgenerationengine-docgen-frontend   docgen-frontend   Up (healthy)                  0.0.0.0:4173->8080/tcp
docgen-kafka      bitnamilegacy/kafka:3.7                    docgen-kafka      Up (healthy)                  0.0.0.0:9092->9092/tcp
docgen-minio      minio/minio:RELEASE.2024-12-18T13-15-44Z   docgen-minio      Up (healthy)                  0.0.0.0:9000-9001->9000-9001/tcp
docgen-postgres   postgres:16-alpine                         docgen-postgres   Up (healthy)                  0.0.0.0:5432->5432/tcp
docgen-redis      redis:7-alpine                             docgen-redis      Up (healthy)                  0.0.0.0:6379->6379/tcp
```

## Prior Stage 5

- Stage 5 tip at deploy: `1e023a35`
- Current tip `293562e9` = Stage 5 evidence + UIUX manifest commit (docs only; no ForceRebuild required for app code)

## Ready for

- Stage 11 `integration-merger`
