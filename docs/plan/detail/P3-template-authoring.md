# P3 — Template Authoring (Detailed Plan)

**Phase status:** Done | **Depends on:** P2

## Behavior goal

Template authors create templates from approved masters via a step wizard: anchor
binding, variable schema, structured content, conditions/loops, test data sets.

## Key tasks

| ID | Task | Status |
| --- | --- | --- |
| P3-D01 | Template + dev version model; anchor binding validation states | Done |
| P3-D02 | Variable schema types (text, number, amount, date, enum, bool, list, object) | Done |
| P3-D03 | Structured content node matrix (v1 conservative set per PRD 6.5.1) | Done |
| P3-T01 | Wizard UI shell (steps 1–4 minimum for slice) | Done |
| P3-T02 | Variable schema editor + validation | Done |
| P3-T03 | Anchor binding to approved master `anchorId` directory | Done |
| P3-T04 | Condition/loop rule configurator (no external API calls) | Done (validate-only UI + management API) |
| P3-T05 | Test data set CRUD (desensitized/synthetic default) | Done |

**Exit:** Draft template with valid schema, bindings, and content ready for test generation.

**Backend slice evidence:** `TemplateController` management API, Flyway V5/V13, `TestDataSetController`, `TemplatePlatformSliceTest`, `POST .../rules/validate`.

**Frontend slice evidence:** `TemplateAuthoringPanel`, `TemplateRuleConfigurator`, `TemplateTestDataSetPanel` on `TemplateDetailView`.
