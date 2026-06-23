---
name: docker-deployment
description: Automated Docker build and deployment workflow for the platform. Use to build images, bring up dependency + app stacks with healthcheck gating, verify health, capture deployment evidence, and define rollback — only after quality gates are green and with no secrets baked into images.
---

# Docker Deployment

Deployment releases an already-green slice. Never use it to bypass gates; never bake secrets.

## Assets

- `docker-compose.yml` — local deps: postgres, redis, kafka, minio, libreoffice (`rendering` profile).
- `docker-compose.prod.yml` — `prod` profile: `docgen-backend`, `docgen-frontend`.
- `backend/Dockerfile`, `frontend/Dockerfile`.
- Health: backend `/healthz`; compose `healthcheck` + `depends_on: service_healthy`.

## Preconditions (block if unmet)

- Backend: `mvn -B -ntp -f backend/pom.xml verify` green.
- Frontend: `pnpm -C frontend lint && type-check && test && build` green.
- E2E functional + UIUX evidence present for user-facing changes.
- No secrets in images / compose / committed env; inject via runtime env.

## Workflow

```bash
# 1. Build images
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod build

# 2. Start dependencies, wait for healthy
docker compose up -d docgen-postgres docgen-redis docgen-minio

# 3. Roll out app stack (health-gated)
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d

# 4. Verify health
curl -f http://localhost:8080/healthz
# frontend on FRONTEND_PORT (default 4173)
```

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

- `.cursor/agents/deploy-engineer.md`
- `.cursor/skills/tdd-feature-delivery/SKILL.md`
