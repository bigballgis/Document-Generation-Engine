# P5 — Lifecycle Governance (Detailed Plan)

**Phase status:** Done (thin slice) | **Depends on:** P4

> **P5 vs P19 boundary (2026-06-24):** P5 **Done** covers the lifecycle **state machine**,
> basic lifecycle UI, in-app transition actions, and a **thin-slice** publish-gate checklist
> panel in `TemplateDetailView`. It does **not** include P19 scope: live server-side publish
> gate blocking, multi-sample batch test + coverage thresholds, controlled test/approval
> opinion forms, structured risk prompts, or full publish-summary dialog content. Those gaps
> are tracked in [P19](./P19-verifiability-publish-gate.md) and COR-T01–T04.

## Behavior goal

Full template lifecycle: submit test → test decision → submit approval → approval
decision → pending release → publish; with publish gate checklist, in-app todos,
and audit trail.

## Key tasks

| ID | Task | Status |
| --- | --- | --- |
| P5-D01 | Template state machine (DRAFT through DEPRECATED per domain model) | Done |
| P5-D02 | Test/approval opinion forms (controlled templates, not rich-text editor) | Done |
| P5-D03 | Publish gate checklist aggregation | Done (thin slice UI in TemplateDetailView) |
| P5-T01 | Submit for test + tester queue todo | Done |
| P5-T02 | Test pass/fail with evidence confirmation | Done |
| P5-T03 | Submit for approval + approver queue todo | Done |
| P5-T04 | Approval pass/fail + pending release | Done |
| P5-T05 | Publish with semver selection + release version lock | Done |
| P5-T06 | Lifecycle audit events (non-sensitive summaries) | Done |

**Exit:** Template reaches published release version with audit evidence chain.

**Evidence:** `TemplateLifecycleService`, lifecycle endpoints on `TemplateController`,
`template_lifecycle_record` table; publish gate panel (`templates.publishGate.*` i18n keys)
in `TemplateDetailView.vue`.
