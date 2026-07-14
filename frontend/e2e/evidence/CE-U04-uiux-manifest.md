# CE-U04 UIUX evidence manifest

**Task:** CE-U04 — In-app PDF preview (pdf.js)  
**Slice:** `ce-u04-inline-pdf-preview`  
**BDD:** [ce-u04-inline-pdf-preview.md](../../docs/behavior/ce-u04-inline-pdf-preview.md)

## Viewports

| Frame | Size | Brand |
| --- | --- | --- |
| Desktop decision | 1920×1080 | REDBC + GREENBC |

## Screenshots

| # | File | Subject |
| --- | --- | --- |
| 1 | `01-side-by-side-inline-pdf-redbc-1920x1080.png` | Full side-by-side authoring with inline PDF |
| 2 | `02-inline-pdf-viewer-crop-redbc-1920x1080.png` | Inline PDF viewer crop (page toolbar + canvas) |
| 3 | `01-side-by-side-inline-pdf-greenbc-1920x1080.png` | GREENBC dual-brand parity |
| 4 | `02-inline-pdf-viewer-crop-greenbc-1920x1080.png` | GREENBC viewer crop |

## Components

- `InlinePdfPreviewViewer.vue`
- `AuthoringPreviewPane.vue`
- `TemplatePreviewPanel.vue`
- `useInlinePdfPreview.ts`

## Notes

- SPECIMEN watermark visibility deferred to CE-G02 (#73) soft dependency.
- E2E functional: `CE-U04-inline-pdf-preview.spec.ts` — **3/3 PASS** (IPP-001, IPP-002).
- UIUX evidence: `CE-U04-inline-pdf-preview-uiux-evidence.spec.ts` — **1/1 PASS** @1920 REDBC + GREENBC.
- Deploy: **DEPLOY_OK_WITH_NOTES** — acceptance JWT env required; `FRONTEND_PORT=5173`; backend Docker healthcheck wget flake; nginx `.mjs` → `application/javascript` fix for pdf.js worker (CE-U04).
- **Verdict:** PASS_WITH_NOTES (CE-G02 watermark not asserted).
