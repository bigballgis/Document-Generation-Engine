# IBL-D5 — latest legalhold depth / verify evidence

| Field | Value |
| --- | --- |
| **Recorded** | 2026-07-19 |
| **Worktree** | `D:/working/DGE-ibl-d5-legalhold-depth` · `feat/ibl-d5-legalhold-depth` |
| **Slice status** | **Done** — MAIN merge `6f672271` / feature tip `2e56787e`; Wave IBL-D closed |

## Legalhold suite

| Class | Tests | Failures |
| --- | --- | --- |
| `LegalHoldServiceTest` | 9 | 0 |
| `LegalHoldExemptionServiceTest` | 8 | 0 |
| `LegalHoldCreateDepthTest` | 16 | 0 |
| `LegalHoldEnforceBlockMatrixTest` | 11 | 0 |
| `LegalHoldRetentionEnforceBridgeTest` | 7 | 0 |
| `LegalHoldControllerWebTest` | 6 | 0 |
| `LegalHoldExceptionAdviceTest` | 4 | 0 |
| **Package total** | **61** | **0** |

Delta vs F23 baseline (2 thin classes / 17 tests): **+5 classes / +44 tests**.

## Default `mvn verify`

| Field | Value |
| --- | --- |
| Command | `mvn -B -ntp -f backend/pom.xml verify` |
| Result | **BUILD SUCCESS** |
| Surefire | **2188** tests, **0** failures, **0** errors, **15** skipped |
| Checkstyle | 0 violations |
| SpotBugs / PMD / JaCoCo | check passed (floors) |
| Prior D4 verify | 2144 → **+44** (matches new legalhold tests) |

## Paths covered

- **Create:** validation (reason size, window order, external-id resolve, mismatch, blank IDs), fail-closed non-admin, list/get, web 201/422
- **Enforce:** ACTIVE TEMPLATE_WINDOW / INVOCATION_SET exemption hits on invocation + audit surfaces; retention bridge with real `LegalHoldExemptionService`
- **Block:** released / out-of-window / scope mismatch / null edges / INVOCATION_SET ≠ management audit; double-release 409; 401/403/404

## Residuals / honesty

- Docker Playwright subset **9/162** expansion **out of scope**.
- **Not** claimed: #3b/#5a GO, go-live, Wave E, IBL-B7 Word, IBL program Done. (Wave D Done claimed only via program sync after this leaf.)
- First full-suite attempt hit host-flake `LibreOfficePdfConversionServiceTest` temp-dir accumulation (unrelated to legalhold); cleaned `%TEMP%\docgen-*` and re-ran verify → GREEN. Isolated LO cleanup tests also green.
