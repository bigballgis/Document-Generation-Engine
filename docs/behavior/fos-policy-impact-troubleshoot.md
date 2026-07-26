# Behavior: FOS-W11 Policy Impact Troubleshoot

**Status:** Confirmed for delivery  
**Traceability:** TM #181 · `fos-policy-impact-troubleshoot` · W11-1…W11-8

## Goal

Operators can preview policy impact with actionable warnings, troubleshoot generate failures with honest error codes, and roll back a policy version when previewed as safe.

## Acceptance scenarios

### W11-1 — Narrowing warnings
Given a draft that narrows formats/modes/AD groups or lowers batch limit  
When impact preview runs  
Then warnings include machine-readable codes (`OUTPUT_FORMATS_NARROWED`, `OUTPUT_MODES_NARROWED`, `AD_GROUPS_NARROWED`, `BATCH_LIMIT_LOWERED`).

### W11-2 — Default route honesty
Given current and candidate default route targets  
When impact preview is shown  
Then UI/API expose `currentDefaultRouteTarget` and `candidateDefaultRouteTarget` (not only prose).

### W11-3 — Expected error codes
Given hard-block findings that previously used a generic code  
When mapped for operators  
Then expected code is `TEMPLATE_VALIDATION_FAILED` (not a fake policy-only code).

### W11-4 — Shape validation
Given generate request missing `output` or `variables`  
When validated  
Then API returns `REQUEST_BODY_INVALID` with fieldErrors (422 VALIDATION).

### W11-5 — Output mode policy
Given sync+batch when mode not allowed  
When validated  
Then error code is `OUTPUT_MODE_NOT_ALLOWED`.

### W11-6 — Failed sync never empty
Given a failed sync invocation  
When error is mapped  
Then envelope always has category/code/messageKey (fallback INTERNAL_ERROR).

### W11-7 — Duplicate itemId honesty
Given ADR-0004 / OpenAPI  
When documenting ITEM_ID_DUPLICATED  
Then status is 422 (not 409).

### W11-8 — Rollback
Given an operator with policy manage  
When rollback is previewed and confirmed  
Then `previewPolicyRollback` / `rollbackPolicy` complete and UI surfaces the action.

## Deploy honesty

Docker/E2E may be BLOCKED (0 images). Unit/integration gates must pass.
