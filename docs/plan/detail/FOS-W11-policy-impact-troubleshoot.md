# FOS-W11 — Policy impact & troubleshooting

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W11 · **Status:** **Done**
**Slice id:** `fos-policy-impact-troubleshoot` · worktree `../DGE-fos-policy-impact-troubleshoot` · branch `feat/fos-policy-impact-troubleshoot`
**Task Master:** **#181** · **delivery_lane:** **full**
**Origin:** C7, C8, C9, C10, C13, C14, C15, C20, C21

---

## Before code

```powershell
git worktree add "..\DGE-fos-policy-impact-troubleshoot" -b feat/fos-policy-impact-troubleshoot origin/main
```

### Tasks

| Id | Sev | Task |
| --- | --- | --- |
| W11-1 | **P1** | Impact preview warns when allowances narrow |
| W11-2 | **P1** | Wire existing policy rollback API into UI |
| W11-3 | **P1** | Always persist failed sync invocation rows |
| W11-4 | **P1** | Request-shape errors use VALIDATION + fieldErrors |
| W11-5 | **P1** | Reject blank `requestId` |
| W11-6 | **P1** | Align `OUTPUT_MODE_NOT_ALLOWED` on sync + batch |
| W11-7 | **P1** | Align `ITEM_ID_DUPLICATED` status with ADR or update ADR |
| W11-8 | **P2** | Impact UI: real error codes + i18n contractDiff |

---

## W11-1 — "Safe" impact for breaking narrowings

**File:** `ApiPolicyImpactPreviewService#buildWarnings`

Today only default-route warnings; output-format/mode/batch/AD-group narrowings detect
as changed areas but produce **no** warning → `safe` summary.

### Implement

Emit warnings whenever a changed area **narrows** an allowance (formats/modes removed,
batch limit lowered, AD groups removed). Keep hard-block behaviour for non-callable
default route. Red tests per narrowing type.

---

## W11-2 — Rollback endpoints exist with no UI

**Backend:** `ApiManagementPolicyController` rollback preview/commit already exist.
**Frontend:** no view/store action (grep `rollback` in `frontend/src` → audit tests only).

### Implement

Wire rollback preview + commit into the policy domain console beside impact preview.
Reuse confirm patterns. E2E: save a change → rollback → prior policyVersion restored.

---

## W11-3 — Failed sync invocations sometimes not recorded

**Files:** `FailedSyncInvocationErrorMapper#from` (returns `null`),
`RuntimeTemplateSyncSupport#recordFailedSingleInvocation`

### Implement

Fall back to a generic envelope (`INTERNAL_ERROR`, retryable per factory) instead of
`null`, so a row is always written for frontline troubleshooting.

---

## W11-4 — Missing body fields look like template validation

**File:** `RuntimeGenerateRequestSupport#validateGenerateRequest` →
`TemplateExceptionAdvice` maps to `TEMPLATE_VALIDATION_FAILED` with `fieldErrors=null`.

### Implement

Route request-shape checks through `errorEnvelopeFactory.validationError(...)` with
`FieldError` naming the field (as `VariableValidationException` does).

---

## W11-5 — `requestId` required in OpenAPI/ADR but not validated

Reject blank `requestId` in the same validation block (VALIDATION field error).

---

## W11-6 — Mode-not-allowed code differs sync vs batch

Have `OutputModePolicyValidator` emit `OUTPUT_MODE_NOT_ALLOWED` on both paths.

---

## W11-7 — `ITEM_ID_DUPLICATED` status drift

ADR-0004 says 400; advice returns 422. **Pick 422** (matches runtime validation family)
and amend ADR-0004 in the same change set (doc-keeper discipline).

---

## W11-8 — Fake expected error codes + raw contractDiff

Point `apiPolicyImpactFindings.ts` at real `ApiErrorCodes`. Return structured current/
candidate route versions from backend and render via i18n (not raw
`currentTarget=…,candidateTarget=…`).

---

## Exit

Impact preview honest; rollback usable; invocation row always present on sync failure;
runtime error model consistent; gates + E2E policy edit/rollback; arch review. TM **#181** → done.
