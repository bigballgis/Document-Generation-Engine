# Slim R7-backend — remaining soft service hotspot peels

Branch: `feat/slim-r7-backend` (base `main` @ `ccf6859`)
Date: 2026-07-13

## Approach

- Behavior-preserving package-private `*Support` collaborators in the same packages (constructed by parent, not Spring beans)
- Public constructors / method signatures unchanged on facades; `@Transactional` entry points stay on Spring services
- Soft budget: Java warn at 400; push clear seams toward **&lt;300** non-blank lines
- `StructuredContentDocxWriteSession`: light peel only (cursor / block-type / numbering-prefix helpers) — OOXML write orchestration untouched
- `ApiPolicyCommandSupport`: safe peel of repetitive domain-save path into `ApiPolicyDomainSaveSupport`
- `ManagementAuditRecorder`: domain bodies already peeled in R5; densify facade forwarding (public event-type constants retained)

## LOC before / after (touched hotspots)

Non-blank lines (`Where-Object { $_.Trim() -ne '' }`):

| File | Before | After | Delta |
|------|-------:|------:|------:|
| `ManagementAuditRecorder.java` | 349 | 195 | −154 |
| `PreviewGenerationService.java` | 347 | 237 | −110 |
| `MasterDocumentService.java` | 340 | 251 | −89 |
| `TemplateBindingConfigurationService.java` | 325 | 283 | −42 |
| `ApiPolicyCommandSupport.java` | 324 | 133 | −191 |
| `TemplateService.java` | 313 | 296 | −17 |
| `TableComponentService.java` | 311 | 57 | −254 |
| `StructuredContentDocxWriteSession.java` | 305 | 260 | −45 |

## Extracted collaborators (new)

| Collaborator | Role | LOC |
|--------------|------|----:|
| `PreviewRecordMappingSupport` | Preview view/summary mapping, warnings JSON, variables hash/resolve | 150 |
| `MasterDocumentCatalogSupport` | Catalog list + status parse | 94 |
| `MasterDocumentReviewSupport` | Submit/decide review bodies | 83 |
| `TemplateBindingStatusSupport` | Binding status + variable/structured validation helpers | 113 |
| `ApiPolicyDomainSaveSupport` | Domain save commands + shared save/json/retention helpers | 260 |
| `TemplateAccessGuardSupport` | Readable/writable template + draft/author guards | 45 |
| `TableComponentValidationSupport` | Table layout/parse/walk/issue helpers | 281 |
| `StructuredContentDocxCursorSupport` | Insert paragraph/table, block-type, numbering prefix | 61 |

## Residuals

- **main/java files ≥300 non-blank: none** after this peel
- Facades still own orchestration / public `@Transactional` entry points
- `ManagementAuditRecorder` remains a thin transactional facade over existing domain supports (+ densified forwarding)
- `MasterDocumentService` retains a `GroupAccessService` field for the management-authorization contract ratchet
- High-risk OOXML write path in `StructuredContentDocxWriteSession.writeBlockNodes` unchanged in semantics

## Verify

- Full: `mvn -B -ntp -f backend/pom.xml verify` — **BUILD SUCCESS** (Tests run: 1351, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)
