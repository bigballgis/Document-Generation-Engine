# Specialist Runtime — retry-first policy

| Field | Value |
| --- | --- |
| Slice | `orch-specialist-retry-only` |
| Date | 2026-07-16 |
| Formal phase | **None** |
| Status | **Done** after merge |
| Supersedes | Auto `FALLBACK_GENERAL_PURPOSE` default from `orch-specialist-fallback` |

## Policy

**Retry named specialist (≤3) → BLOCKED.** No automatic `generalPurpose` downgrade.
User may opt in with `允许降级` / `allow-gp-fallback`.

## Anchors

| Artifact | Path |
| --- | --- |
| Skill | `.cursor/skills/specialist-runtime-fallback/SKILL.md` |
| Behavior | [docs/behavior/specialist-runtime-fallback.md](../behavior/specialist-runtime-fallback.md) |
| Routing | `.cursor/rules/subagent-routing-mandate.mdc` |

## Honest limit

Retry fixes API flakes. It cannot invent Task enum entries when Cursor did not load
`.cursor/agents` — then BLOCKED + recovery hints (or explicit user opt-in GP).
