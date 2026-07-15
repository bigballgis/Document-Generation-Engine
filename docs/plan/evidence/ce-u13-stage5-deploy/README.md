# CE-U13 Stage 5 Deploy Evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-15T21:51:12+08:00
- **Worktree:** D:/working/DGE-ce-u13-variable-rename @ 9052d670 (9052d670c9f6e85dd8e5a60b152a7967d2b444b5)
- **Compose project:** documentgenerationengine
- **Health:** /healthz=200 ; UI :4173=200
- **FE asset in image:** /usr/share/nginx/html/assets/ControlledStructuredContentEditor-Bhc-Pd4b.js
- **Stage 10:** SkipBuild OK if stack unchanged

## Queue
```

```

## Compose ps
```
NAME              IMAGE                                      COMMAND                   SERVICE           CREATED              STATUS                        PORTS docgen-backend    documentgenerationengine-docgen-backend    "java -jar app.jar"       docgen-backend    About a minute ago   Up About a minute (healthy)   0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp docgen-frontend   documentgenerationengine-docgen-frontend   "/docker-entrypoint-鈥?   docgen-frontend   About a minute ago   Up 39 seconds (healthy)       0.0.0.0:4173->8080/tcp, [::]:4173->8080/tcp docgen-kafka      bitnamilegacy/kafka:3.7                    "/opt/bitnami/script鈥?   docgen-kafka      7 hours ago          Up 3 hours (healthy)          0.0.0.0:9092->9092/tcp, [::]:9092->9092/tcp docgen-minio      minio/minio:RELEASE.2024-12-18T13-15-44Z   "/usr/bin/docker-ent鈥?   docgen-minio      8 hours ago          Up 8 hours (healthy)          0.0.0.0:9000-9001->9000-9001/tcp, [::]:9000-9001->9000-9001/tcp docgen-postgres   postgres:16-alpine                         "docker-entrypoint.s鈥?   docgen-postgres   8 hours ago          Up 8 hours (healthy)          0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp docgen-redis      redis:7-alpine                             "docker-entrypoint.s鈥?   docgen-redis      8 hours ago          Up 8 hours (healthy)          0.0.0.0:6379->6379/tcp, [::]:6379->6379/tcp
```
