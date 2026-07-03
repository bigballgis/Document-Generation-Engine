# CDP Wave CD-DOC — Documentation Truth Reconciliation

**Program:** [competitiveness-deepening-program.md](../competitiveness-deepening-program.md)  
**Wave:** CD-0 (non-code)  
**Owner default:** `doc-keeper`  
**Status model:** `Not Started` | `In Progress` | `Blocked` | `Done`

> **Lower-tier implementer rule:** Complete tasks **in ID order** unless `Blocked`. Each task = one PR-sized doc change set. Do not edit code. After each batch of 5 tasks, run link check: every `docs/README.md` link resolves.

---

## Task template (all rows follow this)

Each task below is self-contained. Mark Done only after merge + ledger row in `execution-sync-ledger.md` § CDP.

---

| ID | Pri | File + section | Action | Acceptance (verify by grep/read) | Status |
| --- | --- | --- | --- | --- | --- |
| **CD-DOC-T01** | H | `docs/PROJECT-STATUS-RESET.md` §Re-earned L25 | Replace `Active formal phase: None` with `**P22** (P22-DEMO-EXPANSION In Progress; activated 2026-07-03)` | Grep shows P22 not None | Not Started |
| **CD-DOC-T02** | H | `docs/README.md` baseline L3–4 | Remove «P12-API paused T01–T06»; write «P12-API-PACKAGE-ACCESS-INVOCATION **Done** 2026-07-03» | No «paused» in baseline | Not Started |
| **CD-DOC-T03** | H | `docs/plan/README.md` L11, L55, §Paused L88–89 | P12-API slice → **Done**; delete Paused paragraph | plan/README has no API paused | Not Started |
| **CD-DOC-T04** | H | `docs/plan/detail/P22-demo-expansion-rendering-fidelity.md` L7 blockquote | API slice **Done 2026-07-03**; remove «resume after P22» for API slice | P22 header consistent with ledger | Not Started |
| **CD-DOC-T05** | H | `docs/plan/detail/P22-demo-expansion-rendering-fidelity.md` §Changelog | Sync API slice Done | Changelog matches ledger | Not Started |
| **CD-DOC-T06** | H | `docs/requirements/requirements-plan.md` §BDD-TEMPLATE-RISK-PROMPT-UX-001 | Status → **Done (2026-06-29)**; link P12-deferred §P12-BDD-RISK-PROMPT-UX-001 | No «Not yet implemented» | Not Started |
| **CD-DOC-T07** | M | `docs/plan/detail/P19-verifiability-publish-gate.md` §Residual | BDD risk-prompt **Done**; link P12 slice | Residual honest | Not Started |
| **CD-DOC-T08** | M | `docs/plan/execution-sync-ledger.md` §Transitional seams | Add rows: (1) Structured DOCX write → exit P22 Done; (2) Dual page numbering → P22-T03/T04; (3) Paste cleaning not in binding validation → exit ADR or gate wiring | 3 new rows present | Not Started |
| **CD-DOC-T09** | M | Same §Transitional seams L650 area | Change «Residual fidelity depth remains P18» → **P22** | Grep P18 fidelity seam updated | Not Started |
| **CD-DOC-T10** | M | `docs/plan/detail/P18-structured-authoring-fidelity-engine.md` §Exit criteria | Add **§Residual (closed by P22)**: authoring/validation Done; DOCX write → P22-T01 | Exit criteria split clear | Not Started |
| **CD-DOC-T11** | M | `docs/plan/detail/P4-rendering-preview.md` end | Add **§Known gap (P22)**: structured write path; P4-T01 thin assembly | P4 honest about depth | Not Started |
| **CD-DOC-T12** | M | `docs/plan/comprehensive-optimization-roadmap.md` §0 L31–43 | Add P22 In Progress; change «Product workflow completeness → Closed» to «Closed except rendering write path (P22)» | Roadmap not over-claims | Not Started |
| **CD-DOC-T13** | M | `docs/README.md` §Core product | Add rows: `usability-review.md`, `authoring-rendering-first-principles-review.md` | Index lists both | Not Started |
| **CD-DOC-T14** | M | `docs/README.md` §Plan layer | Add: `competitiveness-deepening-program.md`, `P12-deferred-enhancements.md`, `P12-api-package-access-invocation-records.md` | Links resolve | Not Started |
| **CD-DOC-T15** | M | `docs/README.md` §Behavior specifications | API package + template testing specs → **Done** + plan links | Behavior table current | Not Started |
| **CD-DOC-T16** | M | `docs/architecture/orchestration-high-level-plan.md` L59–65 | E12 UX → Done; next work **P22 + CDP** | Orchestration current | Not Started |
| **CD-DOC-T17** | M | `docs/product/usability-review.md` §推荐下一步 | Replace API contract-only next step with P22 demo acceptance + CD-E2E golden path | §93–95 updated | Not Started |
| **CD-DOC-T18** | L | `docs/plan/master-plan.md` after P22 row | Add footnote row linking **CDP** program (non-phase) | master-plan references CDP | Not Started |
| **CD-DOC-T19** | L | `docs/plan/execution-sync-ledger.md` new § CDP | Mirror CD-0…CD-3 status + gate evidence placeholders | Ledger has CDP section | Not Started |
| **CD-DOC-T20** | L | `docs/README.md` §Start here | Row 2a: add CDP as item 2d | CDP discoverable from index | Not Started |

---

## Batch execution guide (lower-tier model)

| Batch | Task IDs | Est. effort | Gate |
| --- | --- | --- | --- |
| **B1 Critical drift** | T01–T06 | 30 min | No grep hit: `Active formal phase: None`, `paused — T01–T06` |
| **B2 Seams & honesty** | T07–T12 | 45 min | P18/P4 exit criteria mention P22 residual |
| **B3 Index & orchestration** | T13–T17 | 45 min | `docs/README.md` links manually spot-checked |
| **B4 CDP wiring** | T18–T20 | 20 min | CDP reachable from README + master-plan |

**Done definition per task:** File edited; no contradiction with `execution-sync-ledger.md`; if status vocabulary changes, same change set updates ledger.

---

## Forbidden

- Reopening P18 phase status to `In Progress` (use P22 + CDP language instead).
- Marking CD-DOC wave Done while T01–T06 still open.
