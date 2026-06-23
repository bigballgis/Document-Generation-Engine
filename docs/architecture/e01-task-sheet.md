---
id: DOC-ARCH-E01-TASK-SHEET.MD
type: Architecture View
status: Accepted
sourceOfTruth: false
owners:
  - architecture
  - implementation
dependsOn:
  - docs/plan/master-plan.md
  - docs/PROJECT-STATUS-RESET.md
  - docs/architecture/implementation-task-plan.md
---

# E01 Task Sheet (Master & Template Authoring Core)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Product core: master management, template wizard, structured editing, variables, rules, preview entry.

## Baseline

- **Scope:** Epic E01 鈥?maps to P2, P3, P4
- **Priority:** P0
- **Exit criteria:** User creates master, builds template, runs test generation with preview.

## Dependencies

- P1 login
- P0 foundation

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| E01-T01 | P0 | Master document | DOCX upload + anchor catalog extraction | Anchors stable anchorId; group isolated | Done |
| E01-T02 | P0 | Master document | Master review workflow | Only APPROVED masters referenceable | Done |
| E01-T03 | P0 | Template composition | Template wizard steps 1鈥? | English i18n; role-scoped | Done |
| E01-T04 | P0 | Template composition | Variable schema editor + validation | Publish candidate schema locked | Done |
| E01-T05 | P0 | Template composition | Structured content nodes (v1 matrix) | Unsupported nodes block publish | Done |
| E01-T06 | P0 | Template composition | Condition/loop configurator | No external API in rules | Done |
| E01-T07 | P0 | Rendering worker | Test generation + preview record | Authoritative preview artifacts stored | Done |

## Gate commands (when implementation exists)

- Backend: `mvn -B -ntp -f backend/pom.xml verify`
- Frontend: `pnpm -C frontend lint` / `type-check` / `test` / `build`

## Evidence

| Evidence slot | Status |
| --- | --- |
| Unit / integration tests | Done |
| Contract / OpenAPI conformance | Done |
| Quality gate logs | Done |
| Plan status sync | Done |
