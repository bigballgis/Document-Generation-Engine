---
name: docker-deploy-queue
description: Single-Docker-host deploy queue — serialize docker-deploy.ps1 so only one acceptance stack (8080/4173) runs at a time. Use before any docker-deploy from agents or parallel worktrees.
---

# Docker Deploy Queue (Single Host)

This machine has **one Docker engine** and one canonical acceptance stack
(`8080` backend, `4173` frontend). Parallel worktrees must **not** start a second
compose project or invent port offsets for routine acceptance.

## Rule

All calls to `.\scripts\docker-deploy.ps1` (and equivalent compose up for prod profile)
go through the queue helper:

```powershell
.\scripts\docker-deploy-queue.ps1              # full deploy
.\scripts\docker-deploy-queue.ps1 -SkipBuild    # restart only
.\scripts\docker-deploy-queue.ps1 -ForceRebuild
.\scripts\docker-deploy-queue.ps1 -Status       # show lock / queue
.\scripts\docker-deploy-queue.ps1 -EnqueueOnly -Reason "F5 acceptance"
```

## Lock file

- Path: `.cursor/runtime/docker-deploy.lock` (gitignored)
- Holds: owner pid, worktree path, reason, started_at, heartbeat
- Stale lock: if owner process is dead, queue script may break the lock with a warning

## Agent contract (`build-deploy-agent`)

1. Prefer `docker-deploy-queue.ps1` over raw `docker-deploy.ps1`.
2. If lock busy: report `DEPLOY_QUEUED` / wait with timeout; do not start a second stack.
3. Never set ad-hoc `COMPOSE_PROJECT_NAME` for parallel acceptance on this host.
4. Capture evidence after the single stack is healthy.

## What may run in parallel (no Docker)

- `mvn -Pdev-fast test` in **different worktrees**
- `pnpm -C frontend test` in different worktrees
- Read-only reviews

## Related

- `scripts/docker-deploy.ps1` — underlying deploy
- `scripts/docker-deploy-queue.ps1` — mutex + optional wait
- `.cursor/agents/build-deploy-agent.md`
- `.cursor/skills/worktree-isolation/SKILL.md`
