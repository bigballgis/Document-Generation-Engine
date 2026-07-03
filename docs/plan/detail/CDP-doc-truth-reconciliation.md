# CDP Wave CD-DOC — Documentation Truth Reconciliation

**Program:** [competitiveness-deepening-program.md](../competitiveness-deepening-program.md)  
**Wave:** CD-0 (non-code)  
**Owner default:** `doc-keeper`  
**Status model:** `Not Started` | `In Progress` | `Blocked` | `Done`

> **Lower-tier implementer rule:** Complete tasks **in ID order** unless `Blocked`. Each task = one PR-sized doc change set. Do not edit code. After each batch of 5 tasks, run link check: every `docs/README.md` link resolves.

---

## Task status (2026-07-04)

| ID | Status |
| --- | --- |
| CD-DOC-T01 … T20 | **Done** |

All rows below satisfied in repo as of 2026-07-04 CD-0 batch close.

---

| ID | Pri | File + section | Action | Acceptance (verify by grep/read) | Status |
| --- | --- | --- | --- | --- | --- |
| **CD-DOC-T01** | H | `docs/PROJECT-STATUS-RESET.md` §Re-earned L25 | P22 + CDP session split | Grep shows P22 not None | **Done** |
| **CD-DOC-T02** | H | `docs/README.md` baseline | P12-API Done; CDP active | No «paused» in baseline | **Done** |
| **CD-DOC-T03** | H | `docs/plan/README.md` | P12-API Done; no Paused API | plan/README clean | **Done** |
| **CD-DOC-T04** | H | P22 detail blockquote | API slice Done | Header consistent | **Done** |
| **CD-DOC-T05** | H | P22 §Changelog | Sync API Done | Changelog current | **Done** |
| **CD-DOC-T06** | H | requirements-plan risk BDD | Done + link P12 | No «Not yet implemented» | **Done** |
| **CD-DOC-T07** | M | P19 §Residual | BDD Done | Residual honest | **Done** |
| **CD-DOC-T08** | M | ledger §Transitional seams | 3 new rows | Rows present | **Done** |
| **CD-DOC-T09** | M | ledger P19 row | P18→P22 fidelity | Grep updated | **Done** |
| **CD-DOC-T10** | M | P18 §Exit + Residual | P22 write path | Split clear | **Done** |
| **CD-DOC-T11** | M | P4 §Known gap | P22 write path | P4 honest | **Done** |
| **CD-DOC-T12** | M | comprehensive-optimization-roadmap §0 | P22 + CDP | Not over-claims | **Done** |
| **CD-DOC-T13** | M | docs/README Core product | usability + first principles | Listed | **Done** |
| **CD-DOC-T14** | M | docs/README Plan layer | CDP + P12 detail links | Links resolve | **Done** |
| **CD-DOC-T15** | M | docs/README Behavior | Done + new BDD rows | Table current | **Done** |
| **CD-DOC-T16** | M | orchestration-high-level-plan | E12 Done; CDP+P22 | Current | **Done** |
| **CD-DOC-T17** | M | usability-review §推荐下一步 | CDP + E2E + P22 | Updated | **Done** |
| **CD-DOC-T18** | L | master-plan CDP footnote | CDP link | Present | **Done** |
| **CD-DOC-T19** | L | ledger § CDP | CD-0…CD-3 mirror | Section added | **Done** |
| **CD-DOC-T20** | L | docs/README Start here | CDP item 2a | Discoverable | **Done** |

---

## Forbidden

- Reopening P18 phase status to `In Progress` (use P22 + CDP language instead).
