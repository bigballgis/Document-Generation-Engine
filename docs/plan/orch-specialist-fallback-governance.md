# Specialist Runtime Fallback — governance note

| Field | Value |
| --- | --- |
| Slice | `orch-specialist-fallback` |
| Date | 2026-07-16 |
| Formal phase | **None** (agent governance — not CE-O01) |
| Status | **Done** after merge + MAIN doc-sync |

## Purpose

Harden routing when Cursor `Task` lacks project specialist enums or the subagent API
fails: formal ladder to `generalPurpose` / documented inline checklist with auditable
`runtime_routing`, without skipping delivery gates.

## Anchors

| Artifact | Path |
| --- | --- |
| Skill | `.cursor/skills/specialist-runtime-fallback/SKILL.md` |
| Behavior | [docs/behavior/specialist-runtime-fallback.md](../behavior/specialist-runtime-fallback.md) |
| Routing rule | `.cursor/rules/subagent-routing-mandate.mdc` |
| Orchestrator | `.cursor/agents/delivery-orchestrator.md` |
| Pipeline handoff | `.cursor/skills/delivery-pipeline/SKILL.md` |

## Explicit non-goals

- Changing fleet model pin (`cursor-grok-4.5-high-fast`)
- Activating Task Master **#81** CE-O01
- Making `generalPurpose` the default when native specialists are available
