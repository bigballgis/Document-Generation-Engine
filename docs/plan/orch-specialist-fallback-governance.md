# Specialist Runtime — retry then GP downgrade

| Field | Value |
| --- | --- |
| Slice | `orch-specialist-retry-then-gp` |
| Date | 2026-07-16 |
| Formal phase | **None** |
| Placement | MAIN (`main-only`) |
| Status | **Done** (2026-07-16; committed with CE-G04 MAIN closeout) |
| Supersedes | `orch-specialist-retry-only` (retry→BLOCKED; opt-in only GP) |

## Policy

**Retry named specialist (≤3) → GP/inline under contract.**  
User may forbid downgrade with `禁止降级` / `no-gp-fallback`.  
Early opt-in: `允许降级` / `allow-gp-fallback` (skip waiting for full budget).

## Anchors

| Artifact | Path |
| --- | --- |
| Skill | `.cursor/skills/specialist-runtime-fallback/SKILL.md` |
| Behavior | [docs/behavior/specialist-runtime-fallback.md](../behavior/specialist-runtime-fallback.md) |
| Routing | `.cursor/rules/subagent-routing-mandate.mdc` |

## Honest limit

Retry fixes API flakes. It cannot invent Task enum entries when Cursor did not load
`.cursor/agents` — then GP under injected contract (or BLOCKED if user forbade downgrade).
Orphan worktree roots without `.cursor/agents/` commonly empty the project enum until
Open Folder / Reload / new chat.
