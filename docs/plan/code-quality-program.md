# Code Quality Program (CODE-QUALITY) 「全面提升代码质量」

**Program ID:** `CODE-QUALITY`  
**Created:** 2026-07-09  
**Status:** **Done** (2026-07-09 — CQ-01A…CQ-08 complete; ArchUnit **11/11**; `mvn verify` **GREEN**; `pnpm` gates **GREEN**)  
**North star:** Behavior-preserving structural hygiene — decouple module boundaries, shrink god classes, DRY test/demo infrastructure, normalize naming — grounded in **code-quality-reviewer** + **explore** audit consensus.

**BDD:** **`not-applicable`** — behavior-unchanging refactor only; gates are existing `mvn verify` + `pnpm` frontend suite + targeted regression tests per slice.

**Authoritative task sheet:** [detail/CODE-QUALITY-code-hygiene.md](./detail/CODE-QUALITY-code-hygiene.md) — task IDs prefixed **`CQ-*`** only.

| Sibling program | Relationship |
| --- | --- |
| [CORE-FORTRESS program roadmap](./detail/CORE-FORTRESS-program-roadmap.md) | **Done** (F1–F8; 2026-07-09) — do not reopen |
| [Launch Readiness Program (LRP)](./launch-readiness-program.md) | Sibling cross-cutting program — **not** sole active; LR-* tasks coordinate when overlapping (e.g. rendering trust) |
| [Competitiveness Deepening Program (CDP)](./competitiveness-deepening-program.md) | Sibling — CD-* tasks remain separate |
| [System optimization review (SOR)](./system-optimization-review-2026-07.md) | Historical inventory; CQ-* supersedes open SOR-F/SOR-D hygiene rows where duplicated |

---

## Session routing (read first)

| Work stream | Where it runs | CODE-QUALITY owns |
| --- | --- | --- |
| **CQ-01A / CQ-01B** (Wave 1 boundary decoupling) | **Done** (2026-07-09) | `sharedkernel.document` anchor; 5 ports + 4 adapters; ArchUnit **11/11** |
| **CQ-02…CQ-08** | **Done** (2026-07-09) | WriteSession extract; audit DRY; Vue decompose; demo test base; AccessService naming; PDF DRY; E2E stack-readiness |
| **CORE-FORTRESS F6** composables | **Done** — reuse, do not re-split | `useTemplateLifecycleGates`, `useTemplateDetailTabs`, etc. — **CQ-04** built on F6 kernel |

**Formal phase note:** `master-plan.md` has **no** formal program `In Progress` (2026-07-09). New work → `.taskmaster/tasks/tasks.json`.

---

## Audit provenance

Findings consolidated from:

- `.cursor/agents/code-quality-reviewer.md` + `.cursor/skills/code-quality-review/SKILL.md` (module coupling, size budgets, DRY patterns)
- Full-repo **explore** audit consensus (2026-07-09) — rendering↔authoring import cycles, `StructuredContentDocxWriter` god class, `ManagementAuditRecorder` entity construction duplication, `ApiPolicyDomainEditor.vue` oversize, demo `*MasterDocxAssetGeneratorTest` copy-paste, `*AccessSupport` sprawl, LibreOffice/DockerExec PDF twins, E2E stack-readiness boilerplate

---

## Wave map (summary)

| Wave | Tasks | Status | Gate |
| --- | --- | --- | --- |
| **1** | CQ-01A, CQ-01B | **Done** (2026-07-09) | ArchUnit **11/11** + 32 targeted preview/batch tests |
| **2** | CQ-02, CQ-03, CQ-07 | **Done** (2026-07-09) | `mvn verify` + slice regression |
| **3** | CQ-04, CQ-05, CQ-06, CQ-08 | **Done** (2026-07-09) | `pnpm` gates + E2E helper migration (49 specs) |

Detail, dependencies, and per-task owners: [CODE-QUALITY-code-hygiene.md](./detail/CODE-QUALITY-code-hygiene.md).
