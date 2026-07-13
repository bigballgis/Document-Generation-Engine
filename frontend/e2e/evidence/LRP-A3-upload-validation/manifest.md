# LR-A3 Upload Validation — E2E Functional Evidence

**Task:** LR-A3 / `lrp-a3-upload-validation` / #9  
**BDD:** `docs/behavior/lrp-a3-master-docx-upload-validation.md` (A4, A7 + smoke)  
**Config:** `playwright.docker.config.ts` (`http://127.0.0.1:4173`)  
**Date:** 2026-07-10

## Command

```powershell
pnpm -C frontend exec playwright test e2e/LRP-A3-master-docx-upload-validation.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

## Result

| Spec | Scenario | Verdict |
| --- | --- | --- |
| A7 oversized precheck | Replace dialog → readable `masters.upload.errorTooLarge`, submit disabled | **PASS** |
| A7 non-.docx precheck | Replace dialog → readable `masters.upload.errorDocxOnly`, submit disabled | **PASS** |
| A4 nginx HTML 413 | `page.route` fulfills PUT `/masters/*/file` with nginx HTML 413 → ElMessage readable, no raw HTML | **PASS** |
| A4 Spring envelope 413 | Route fulfills JSON envelope `api.error.master.docxTooLarge` → translated ElMessage | **PASS** |
| Smoke valid precheck | Valid replacement fixture → no upload-error, Replace enabled | **PASS** |

**Aggregate:** **5/5 passed** (~12.3s)

## Notes

- A4 uses Playwright network mock for gateway/Spring 413 responses (handoff: “proxy or mock”). Live >60m body upload is not exercised (cost/time); mapping behavior matches unit coverage in `errorEnvelope` / `masters` store.
- Oversized A7 uses a forged `File.size` via `DataTransfer` (Playwright rejects in-memory buffers > 50MB).
- Hub opened via `demoMasterDetailPath` to avoid catalog pagination hiding `Demo Retail Letterhead`.

## Artifacts

| Path | Kind |
| --- | --- |
| `frontend/e2e/LRP-A3-master-docx-upload-validation.spec.ts` | Spec |
| `frontend/e2e/evidence/LRP-A3-upload-validation/A7-oversized-precheck.png` | Screenshot |
| `frontend/e2e/evidence/LRP-A3-upload-validation/A7-non-docx-precheck.png` | Screenshot |
| `frontend/e2e/evidence/LRP-A3-upload-validation/A4-nginx-413-readable.png` | Screenshot |
| `frontend/e2e/evidence/LRP-A3-upload-validation/A4-spring-envelope-413.png` | Screenshot |
| `frontend/e2e/evidence/LRP-A3-upload-validation/smoke-valid-precheck.png` | Screenshot |
| `frontend/playwright-report/docker/` | HTML report (last docker run) |

## Product defects

None observed in this run.
