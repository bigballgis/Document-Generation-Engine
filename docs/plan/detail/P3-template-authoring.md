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
| P3-T06 | Template package hub + version line navigation (BDD-TEMPLATE-PACKAGE-NAV-001) — hub, dev editor, release read-only, clone API, lifecycle deep-link compat | Done (2026-07-01) |

**Exit:** Draft template with valid schema, bindings, and content ready for test generation.

**Backend slice evidence:** `TemplateController` management API, Flyway V5/V13, `TestDataSetController`, `TemplatePlatformSliceTest`, `POST .../rules/validate`; **P3-T06:** `TemplateVersionLineController`, `TemplateCurrentVersionResolver`, `TemplateVersionLineService` (+15 tests; verify **588**).

**Frontend slice evidence:** `TemplateAuthoringPanel`, `TemplateRuleConfigurator`, `TemplateTestDataSetPanel` on dev editor (`TemplateDevVersionEditorView`); **P3-T06:** `TemplatePackageHubView`, `TemplateVersionLinesPanel`, `TemplateReleaseDetailView`; API field alignment `devVersionId` on version lines + clone; hub clone respects API `cloneable`; OpenAPI `version-lines` / dev / release / clone in `openapi-v1.yaml`; Playwright `template-package-nav.spec.ts` **9/9** (BDD S1–S8 全场景), `P21-T06a-template-detail-tabs.spec.ts` **3/3**, `fol-corporate-catalog.spec.ts` **5/5** (Docker @4173, 2026-07-01); Flyway `V34__corp_only_template_author_seed.sql` (`10000008` CORP-only author for S6); frontend gates **542** Vitest (2026-07-01).
