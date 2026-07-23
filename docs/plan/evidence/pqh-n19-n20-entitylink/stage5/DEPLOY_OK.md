# Stage 5 — E2E stack prep evidence

- **slice**: pqh-n19-n20-entitylink
- **worktree**: `D:/working/DGE-pqh-n19-n20-entitylink`
- **branch**: `feat/pqh-n19-n20-entitylink`
- **tip**: `1e023a356c58533a0ca57ead5e8171d79c19dc46` (short `1e023a35`)
- **captured_at**: 2026-07-23T12:33:20+08:00
- **result**: DEPLOY_OK

## Deploy

- Queue: `.\scripts\docker-deploy-queue.ps1 -Reason "stage5 pqh-n19-n20-entitylink E2E stack prep"`
- `COMPOSE_PROJECT_NAME=documentgenerationengine`
- `FRONTEND_PORT=4173`
- JWT_SECRET: synced from MAIN `.env`; non-default check = **True** (length 64; value not recorded)

## Health

| Probe | URL | Status |
| --- | --- | --- |
| Backend healthz | http://localhost:8080/healthz | **200** |
| Frontend UI | http://localhost:4173/ | **200** |

healthz body:
```
{"status":"UP"}
```

## Images

- backend: `sha256:ed9b4bb12484bf17514eef91c0efa3a9cb0b1af77d02fb576c74442d489ccd36` created `2026-07-23T04:31:43.290898086Z`
- frontend: `sha256:ce73e0c9ce904569fa3eead0d33c5e0537d4db29258119b9a0c611d4a1c1ddac` created `2026-07-23T04:31:44.726095276Z`

## compose ps

```
NAME              IMAGE                                      COMMAND                   SERVICE           CREATED              STATUS                        PORTS docgen-backend    documentgenerationengine-docgen-backend    "java -jar app.jar"       docgen-backend    About a minute ago   Up About a minute (healthy)   0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp docgen-frontend   documentgenerationengine-docgen-frontend   "/docker-entrypoint-鈥?   docgen-frontend   About a minute ago   Up 46 seconds (healthy)       0.0.0.0:4173->8080/tcp, [::]:4173->8080/tcp docgen-kafka      bitnamilegacy/kafka:3.7                    "/opt/bitnami/script鈥?   docgen-kafka      36 hours ago         Up 36 hours (healthy)         0.0.0.0:9092->9092/tcp, [::]:9092->9092/tcp docgen-minio      minio/minio:RELEASE.2024-12-18T13-15-44Z   "/usr/bin/docker-ent鈥?   docgen-minio      36 hours ago         Up 36 hours (healthy)         0.0.0.0:9000-9001->9000-9001/tcp, [::]:9000-9001->9000-9001/tcp docgen-postgres   postgres:16-alpine                         "docker-entrypoint.s鈥?   docgen-postgres   36 hours ago         Up 36 hours (healthy)         0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp docgen-redis      redis:7-alpine                             "docker-entrypoint.s鈥?   docgen-redis      36 hours ago         Up 36 hours (healthy)         0.0.0.0:6379->6379/tcp, [::]:6379->6379/tcp
```

## URLs for stages 6–7

- UI: http://localhost:4173
- API health: http://localhost:8080/healthz
- Login (seed): 10000001 / ChangeMe123! (GLOBAL_ADMIN)

Playwright not run in this stage.
