# P6 — API Management (Detailed Plan)

**Phase status:** Done | **Depends on:** P5

## Behavior goal

Admins manage per-template API policy: credentials, AD Group authorization, output
modes/formats, batch limits, encryption capabilities, default route target — with
impact preview, policyVersion, and audit.

## Key tasks

| ID | Task | Status |
| --- | --- | --- |
| P6-D01 | Template-level API policy aggregate + policyVersion | Done |
| P6-D02 | Credential lifecycle (create/rotate/revoke, one-time secret display) | Done |
| P6-D03 | AD Group resolver adapter (fail-closed; cache per PRD) | Done |
| P6-T01 | API management UI (domain navigation + detail panels) | Done |
| P6-T02 | Policy domain save with impact preview (hard block vs warning) | Done |
| P6-T03 | Default route target governance + audit | Done |
| P6-T04 | Caller contract view (non-sensitive policy + credential summary) | Done |

**Exit:** Published template has callable policy configured and auditable.

**Backend slice evidence:** `ApiManagementController`, Flyway V7, `ConfigAdGroupResolver` (config-based, fail-closed).
