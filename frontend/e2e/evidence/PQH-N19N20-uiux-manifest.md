# PQH-N19N20 UIUX Evidence Manifest — Where-used + MasterImpact EntityLink

**Task:** PQH N19–N20 / TM #161 — EntityLink on content-module where-used + MasterImpact  
**Slice:** `pqh-n19-n20-entitylink` (`feat/pqh-n19-n20-entitylink`)  
**Worktree:** `D:/working/DGE-pqh-n19-n20-entitylink`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-23  
**Viewport:** desktop evidence from Stage 6 functional captures (fullPage)  
**Stack:** Docker `http://127.0.0.1:4173` + `:8080` — UP  
**Tip:** `fc50f06e`  
**Verdict:** **PASS_WITH_NOTES** (Critical = 0)

## Surfaces checked

| # | Surface | Brands |
| --- | --- | --- |
| 1 | Content module Where used — EntityLink name + group | REDBC only |
| 2 | Where used — fail-closed plain text / wildcard | REDBC |
| 3 | MasterImpact — EntityLink + fail-closed | REDBC only |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `e2e/PQH-N19N20-entitylink.spec.ts` (docker) | **7/7 passed** |
| Dedicated `*-uiux-evidence.spec.ts` | **Not present** — visual review of Stage 6 PNGs |
| `a11y-smoke.spec.ts` | **Not re-run this stage** — EntityLinkCell focus ring + labels reviewed in code/screenshots |

## Screenshot inventory

Path: `frontend/e2e/evidence/PQH-N19N20/`

| File | State |
| --- | --- |
| `PQH-N19N20-012-where-used-entity-links.png` | Linked template + group |
| `PQH-N19N20-007-where-used-group-plain-text.png` | Group plain (no identity route) |
| `PQH-N19N20-004-where-used-template-plain-text.png` | Template plain (no template route) |
| `PQH-N19N20-007-where-used-group-wildcard-plain.png` | `*` never links |
| `PQH-N19N20-013-master-impact-entity-link.png` | Impact EntityLink |
| `PQH-N19N20-013-master-impact-fail-closed.png` | Impact plain text |

## Findings

### Critical
_None._

### Suggestion
1. Capture GREENBC for where-used + MasterImpact before closing dual-brand DoD.
2. Add `PQH-N19N20-uiux-evidence.spec.ts` (1440×900 + `switchBrand`) for durable Stage 7 automation.

### Nice to have
- Ellipsis / wrap strategy on `EntityLinkCell` for narrow columns.
- Tokenize `MasterImpactPanel` spacing.
- Avoid UUID-as-label fallback in impact when `name` absent.

## References
- `docs/architecture/ux-entity-display-constitution.md`
- `.cursor/skills/frontend-oa-design/SKILL.md`
- `.cursor/skills/frontend-entity-display/SKILL.md`
