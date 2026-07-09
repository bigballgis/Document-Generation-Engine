---
name: docker-deployment
description: Automated Docker build and deployment workflow for the platform. Use to build images, bring up dependency + app stacks with healthcheck gating, verify health, capture deployment evidence, and define rollback — only after quality gates are green and with no secrets baked into images.
---

# Docker Deployment

Deployment releases an already-green slice. Never use it to bypass gates; never bake secrets.

## Assets

- `docker-compose.yml` — local deps: postgres, redis, kafka, minio, libreoffice (`rendering` profile).
- `docker-compose.prod.yml` — `prod` profile: `docgen-backend`, `docgen-frontend`
  (built from `backend/Dockerfile.packaged`, `frontend/Dockerfile.packaged`).
- Health: backend `/healthz`; compose `healthcheck` + `depends_on: service_healthy`.

## Preconditions (block if unmet)

- Prefer verifying inside Docker after image build (see Workflow). Local gates optional for CI:
  - Backend: `mvn -B -ntp -f backend/pom.xml verify`
  - Frontend: `pnpm -C frontend lint && type-check && test && build`
- E2E functional + UIUX evidence present for user-facing changes.
- No secrets in images / compose / committed env; inject via runtime env.

## Workflow (canonical — user tests only here)

Compile on host (local Maven/pnpm), then images only copy pre-built artifacts —
no Maven/npm inside `docker build`.

```powershell
# From repo root — queued (single Docker host mutex) then host compile + rollout
.\scripts\docker-deploy-queue.ps1
.\scripts\docker-deploy-queue.ps1 -SkipBuild
.\scripts\docker-deploy-queue.ps1 -Status

# Underlying script (debugging / already holding queue)
.\scripts\docker-deploy.ps1
```

Equivalent manual steps:

```bash
docker compose up -d docgen-postgres docgen-redis docgen-minio
mvn -B -ntp -f backend/pom.xml package -Dmaven.test.skip=true   # host compile
pnpm -C frontend build                                           # host compile
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod build --pull=false docgen-backend docgen-frontend
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d docgen-backend docgen-frontend
curl -f http://localhost:8080/healthz
# UI: http://localhost:4173 (NOT Vite dev port 5173)
```

**Rebuild vs re-download:** `build` recompiles changed app layers using local cache. Registry pull happens only for missing base images or when `--pull` is set. Log lines like `load metadata` / `resolve` are registry metadata checks, not always a full download.

## Evidence (capture)

- Image tags/digests, container health status, healthcheck output, exposed ports, versions.

## Rollback readiness (mandatory)

- Record previous known-good image tag/digest before rollout.
- Rollback = redeploy previous tag, re-verify `/healthz` + frontend.
- Flyway migrations are forward-only; never auto-destroy data volumes on rollback.

## Non-negotiables

- No deploy on red gates or missing evidence.
- No secrets baked into images; no committing `.env`.
- No destructive volume ops without explicit user confirmation.
- `prod` profile for release; don't mix local dev creds into release config.

## Related

- `.cursor/agents/build-deploy-agent.md` — routine build/gate/deploy ops (preferred entry point)
- `.cursor/agents/deploy-engineer.md` — rollback planning and release governance
- `.cursor/skills/tdd-feature-delivery/SKILL.md`
