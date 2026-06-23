# P10 — Runtime Document Download (Detailed Plan)

**Phase status:** Done | **Depends on:** P9

## Behavior goal

Authorized API callers download previously generated documents via
`GET /api/{environment}/v1/documents/{documentId}/download` with credential +
AD Group + template-level secondary authorization. Download window is 15 minutes
from generation completion; expired downloads return `410 DOWNLOAD_URL_EXPIRED`.

## Key tasks

| ID | Task | Status |
| --- | --- | --- |
| P10-T01 | Document lookup by documentId scoped to credential template | Done |
| P10-T02 | Stream artifact from object storage with expiry enforcement | Done |
| P10-T03 | Download audit summary (no full URL / secret in logs) | Done |
| P10-T04 | Integration tests: success, 404, 410, 403 | Done |

**Maps to:** P7-T05, M4-T01–T04

**Exit:** Sync-generated document downloadable within 15-minute window; gates green.

**Evidence:** `RuntimeDocumentController`, `DocumentDownloadService`, Flyway V10, `TemplatePlatformSliceTest` download scenarios.
