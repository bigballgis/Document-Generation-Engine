# CORE-FORTRESS F1 — Rendering Core Correctness (Detailed Plan)

**Program ID:** `CORE-FORTRESS`  
**Phase ID:** `CORE-FORTRESS-F1-RENDERING-CORRECTNESS`  
**Phase status:** **Done** (closed 2026-07-09; T01–T07; gates **GREEN**)  
**Depends on:** P23 (**Done**), P22/P18 (**Done**), LRP LR-A (program — font baseline LR-A2 Done)  
**BDD:** `docs/behavior/core-fortress-f1-rendering-correctness.md` — **ready** (`BDD-CORE-FORTRESS-F1-001`)

> **Single-active-phase invariant:** **CORE-FORTRESS F1** is the sole formal phase `In Progress`. P23 remains **Done**. LRP/CDP remain parallel programs (not formal phases).

---

## 1. North star

**Structured content → DOCX fidelity must be single-path, verifiable, and fail-closed everywhere** — body, table cells, headers, footers, module refs, and image/seal assets. No silent content loss. No dual render engines.

This phase is the foundation of the full CORE-FORTRESS program (F1→F8). Subsequent phases (runtime lightweight, LO hardening, frontend refactor) depend on a solid rendering kernel.

---

## 2. Scope (in) / out (out)

| In scope (F1) | Out of scope (later phases) |
| --- | --- |
| A1 dual renderer unify + safety net | B1 publish-time fidelity cache (F2) |
| A2 contentModuleRef fail-closed | Expression engine (F3) |
| A3 image/seal MinIO + fail-closed | LO connection pool (F4) |
| JaCoCo branch floor proposal for rendering package | Frontend god-controller (F6) |
| Dedicated `StructuredContentDocxWriterTest` | E2E/UIUX (backend-only phase) |

---

## 3. Exit criteria

1. **A1 safety net green** before refactor merge: dedicated writer test suite + header/footer/table-cell POI assertions.
2. **A1 unified engine**: no independent plain-text renderer in `DocxAssembler`; all anchor regions use `StructuredContentDocxWriter`.
3. **A2 fail-closed**: `CONTENT_MODULE_STRUCTURE_MISSING` at runtime; publish gate blocks empty pinned refs.
4. **A3 production resolver**: Spring bean + `ObjectStoragePort`; missing assets fail-closed; demo tier explicit only.
5. **Green gates:** `mvn -B -ntp -f backend/pom.xml verify` — Checkstyle/PMD/SpotBugs 0, JaCoCo floors met.
6. **Doc sync:** master-plan, ledger, behavior spec, indexes updated.

---

## 4. Task breakdown

| ID | Owner | Task | Depends on | Status |
| --- | --- | --- | --- | --- |
| **F1-T01** | behavior-spec-author | **BDD behavior spec** — `core-fortress-f1-rendering-correctness.md` | — | **Done** (2026-07-08; readiness `ready`) |
| **F1-T02** | backend-engineer | **A1 safety net** — `StructuredContentDocxWriterTest` + extend `DocxAssemblerTest` for table-cell/header/footer fidelity; TDD red→green on current dual-track code | F1-T01 | **Done** (2026-07-08; writer **18** tests; assembler **+6** incl. 3 `@Disabled` for F1-T03; `-Pdev-fast` **48** run, 3 skipped, BUILD SUCCESS) |
| **F1-T03** | backend-engineer | **A1 unify renderers** — remove plain-text fallback path; single writer for all anchor regions; refactor green | F1-T02 | **Done** (2026-07-08; table/header/footer unified writer; 3 disabled tests enabled; `-Pdev-fast` **45** run, 0 skip) |
| **F1-T04** | backend-engineer | **A2 contentModuleRef fail-closed** — throw `CONTENT_MODULE_STRUCTURE_MISSING`; i18n key; `PublishGateService` block | F1-T01 | **Done** (2026-07-09; writer fail-closed + publish gate `validateForPublishGate_blocksEmptyPinnedStructure`; i18n key) |
| **F1-T05** | backend-engineer | **A3 image/seal resolver** — Spring bean, `ObjectStoragePort`, fail-closed error codes, demo tier flag | F1-T01 | **Done** (2026-07-09; `@Component` resolver + MinIO + demo tier; `StructuredContentImageResolverTest` **7** tests) |
| **F1-T06** | architecture-reviewer | **Boundary review** — rendering isolation, fail-closed posture, no sensitive data in errors | F1-T03, F1-T04, F1-T05 | **Done** (2026-07-09; rendering isolated; fail-closed A2/A3; preview unwraps DocxAssemblyException; no sensitive refs in errors) |
| **F1-T07** | post-task-doc-sync | **Plan + ledger closeout** — mark F1 Done when exit criteria met | F1-T06 + green gates | **Done** (2026-07-09; `mvn verify` **1088** run, 3 skipped, BUILD SUCCESS) |

### Recommended wave order

```text
Wave 0 — BDD + plan (Done)
  F1-T01

Wave 1 — Safety net (mandatory before refactor)
  F1-T02

Wave 2 — Core changes (A2/A3 can parallel after T02 green; A1 merge after T02)
  F1-T03 (A1 unify)
  F1-T04 (A2 fail-closed) — parallel OK
  F1-T05 (A3 resolver) — parallel OK

Wave 3 — Review + closeout
  F1-T06 → F1-T07
```

---

## 5. CORE-FORTRESS program roadmap (context)

| Phase | Name | Status |
| --- | --- | --- |
| **F1** | Rendering core correctness | **Done** (2026-07-09; T01–T07; gates **GREEN**) |
| F2 | Runtime lightweight (publish cache, idempotency, lifecycle bulk) | **Done** (2026-07-09) |
| F3 | Node matrix + expression engine | **Done** (2026-07-09) |
| **F4** | Production rendering hardening (LO pool, fonts, pagination) | **In Progress** |
| F5 | Async durability + security depth | Not Started |
| F6 | Frontend kernel refactor | Not Started |
| F7 | Authoring UX (dirty guard, side-by-side preview) | Not Started |
| F8 | Observability, SLO, DR, evidence bundle | Not Started |

---

## 6. Gate commands

| Context | Command |
| --- | --- |
| TDD inner loop | `mvn -B -ntp -f backend/pom.xml -Pdev-fast test -Dtest=StructuredContentDocxWriterTest,DocxAssemblerTest` |
| Full backend gate | `mvn -B -ntp -f backend/pom.xml verify` |

---

## 7. Traceability

- Behavior: [core-fortress-f1-rendering-correctness.md](../../behavior/core-fortress-f1-rendering-correctness.md)
- Master plan: [master-plan.md](../master-plan.md) § CORE-FORTRESS
- Ledger: [execution-sync-ledger.md](../execution-sync-ledger.md)
- First principles: [authoring-rendering-first-principles-review.md](../../product/authoring-rendering-first-principles-review.md)
