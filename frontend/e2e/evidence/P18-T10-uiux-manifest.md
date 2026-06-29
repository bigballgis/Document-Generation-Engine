# P18-T10 UIUX Evidence Manifest

**Task:** Controlled structured authoring UI — `ControlledStructuredContentEditor`, `PasteCleaningSummaryDialog`, `FidelityWarningList` on template Authoring tab  
**Reviewer:** e2e-uiux-reviewer (evidence captured via `P18-T10-uiux-evidence.spec.ts`)  
**Date:** 2026-06-29  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (no critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm exec playwright test e2e/P18-T10-uiux-evidence.spec.ts --config playwright.docker.config.ts` | **1/1 passed** (11.7s) |
| `pnpm exec playwright test e2e/P18-T10-structured-authoring.spec.ts --config playwright.docker.config.ts` | **5/5 passed** (functional baseline) |

## Screenshot inventory (12)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `P18-T10/screenshots/01-binding-dialog-editor-toolbar-redbc-1440x900.png` | Add binding — controlled editor toolbar (Blocks / Inline / Style / Unavailable) | REDBC |
| 2 | `P18-T10/screenshots/02-binding-dialog-disabled-tooltip-redbc-1440x900.png` | Disabled capability tooltip (Custom HTML) | REDBC |
| 3 | `P18-T10/screenshots/03-binding-dialog-style-picker-open-redbc-1440x900.png` | Style picker dropdown open in binding dialog | REDBC |
| 4 | `P18-T10/screenshots/04-paste-summary-transformed-redbc-1440x900.png` | Paste cleaning summary — transformed category | REDBC |
| 5 | `P18-T10/screenshots/05-paste-summary-blocked-redbc-1440x900.png` | Paste cleaning summary — blocked (Accept disabled) | REDBC |
| 6 | `P18-T10/screenshots/06-binding-dialog-editor-greenbc-1440x900.png` | Binding dialog editor after brand switch | GREENBC |
| 7 | `P18-T10/screenshots/07-paste-summary-greenbc-1440x900.png` | Paste summary — primary actions follow GREENBC | GREENBC |
| 8 | `P18-T10/screenshots/08-preview-fidelity-warnings-populated-redbc-1440x900.png` | Preview section — fidelity warning table populated | REDBC |
| 9 | `P18-T10/screenshots/09-preview-fidelity-filters-applied-redbc-1440x900.png` | Fidelity filters — warning code filter applied | REDBC |
| 10 | `P18-T10/screenshots/10-preview-fidelity-empty-redbc-1440x900.png` | Preview — empty fidelity warnings state | REDBC |
| 11 | `P18-T10/screenshots/11-brand-header-redbc-1440x900.png` | Shell brand slot | REDBC |
| 12 | `P18-T10/screenshots/12-brand-header-greenbc-1440x900.png` | Shell brand slot | GREENBC |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | ✅ | 08–10 |
| Dual-brand REDBC / GREENBC | ✅ | 01 vs 06; 04 vs 07; 11 vs 12 |
| Controlled editor toolbar density | ✅ | 01, 06 — no overlap at 760px dialog width |
| Disabled capabilities + reason tooltips | ✅ | 02 |
| Paste summary modal hierarchy | ✅ | 04, 05, 07 |
| Fidelity warning table + filters | ✅ | 08, 09 |
| Empty fidelity state | ✅ | 10 |
| English-first i18n | ✅ | All surfaces use `templates.structuredEditor.*` / `templates.preview.*` keys |
| No text overflow / overlap at 1440×900 | ✅ | Visual review — tables and toolbars fit dialog bounds |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Fidelity filter empty-result copy** — **Fixed** (2026-06-29): distinct `noMatchingWarnings` i18n key when filters exclude all rows.

2. **Style picker shows raw `styleKey`** — **Fixed** (2026-06-29): i18n labels via `templates.structuredEditor.styleCatalog.keys.*` with fallback.

3. **Paste summary Cancel vs Undo** — **Fixed** (2026-06-29): separate handlers — Cancel emits `cancel`, Undo emits `undo`.

4. **Extend a11y smoke** — **Partial** (2026-06-29): templates list `h1` added to `a11y-smoke.spec.ts`; binding dialog heading deferred.

5. **GREENBC nav active tint** — Same cross-phase note as P14-T03; sidebar active highlight may remain red-tinted on GREENBC. _Rule: dual-brand theming._

### 🟢 Nice to have

1. JSON preview `<details>` fold state screenshot in binding dialog.
2. “Mark viewed” link interaction state on fidelity rows.
3. Paste summary with REMOVED + WARNING multi-row table density.

## Related specs

- Functional: `frontend/e2e/P18-T10-structured-authoring.spec.ts`
- UIUX capture: `frontend/e2e/P18-T10-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/structured-authoring-api.ts`, `frontend/e2e/helpers/uiux-evidence.ts`
