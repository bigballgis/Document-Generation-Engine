# IBL-D5 / #127 — Legal hold test depth evidence

| Field | Value |
| --- | --- |
| **Slice** | `ibl-d5-legalhold-depth` |
| **Task Master** | **#127** |
| **Finding** | **F23** — deepen `legalhold` beyond 2 thin classes (create / enforce / block) |
| **BDD** | [ibl-d5-legalhold-depth.md](../../../behavior/ibl-d5-legalhold-depth.md) (`not-applicable`) |
| **Product SoT** | [ce-g04-legal-hold.md](../../../behavior/ce-g04-legal-hold.md) (unchanged) |
| **frontend_ui_in_scope** | false |
| **Playwright 9→N** | **OUT** |
| **Go-live / #3b / #5a / Wave D Done** | **not** claimed from this evidence alone |

## Suite (legalhold package)

| Class | Path | Tests | Role |
| --- | --- | --- | --- |
| `LegalHoldServiceTest` | prior | 9 | Thin create/release baseline (kept) |
| `LegalHoldExemptionServiceTest` | prior | 8 | Thin exemption baseline (kept) |
| `LegalHoldCreateDepthTest` | **new** | 16 | Create / list / get validation + fail-closed |
| `LegalHoldEnforceBlockMatrixTest` | **new** | 11 | Enforce hits + block matrix (released / OOW / mismatch) |
| `LegalHoldRetentionEnforceBridgeTest` | **new** | 7 | Real exemption bridged into retention cleaners |
| `LegalHoldControllerWebTest` | **new** | 6 | API create / release / 401 / 403 / 404 / 422 / 409 |
| `LegalHoldExceptionAdviceTest` | **new** | 4 | HTTP mapping for domain exceptions |
| **Total** | | **61** | Prior **17** → **+44** |

## How to reproduce

```powershell
# Focused legalhold package
mvn -B -ntp -f backend/pom.xml -Pdev-fast test "-Dtest=com.bank.docgen.legalhold.**"

# Full gate
mvn -B -ntp -f backend/pom.xml verify
```

## Artifacts

| File | Role |
| --- | --- |
| [latest-summary.md](./latest-summary.md) | Human mirror of verify + suite counts |
| [latest-summary.json](./latest-summary.json) | Machine-readable summary |
