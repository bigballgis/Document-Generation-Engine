# Behavior — FOS-W9 Contract works first time

| Field | Value |
| --- | --- |
| **Slice** | `fos-contract-works-first-time` |
| **Task Master** | **#179** |
| **bdd_readiness** | **ready** |
| **frontend_ui_in_scope** | **yes** |
| **delivery_lane** | **full** |
| **Source** | [FOS-W9-contract-works-first-time.md](../plan/detail/FOS-W9-contract-works-first-time.md) |

## Goal

A consuming developer copies the platform contract artifact and succeeds on the first
call against a published template when SYNC_STREAM is allowed — real credential headers,
accepted modes, honest error catalogue, visible per-version variables.

## Acceptance scenarios

### BDD-FOS-W9-001 — Copyable curl uses credential headers
**Given** a caller contract panel with a generate URL  
**When** the copyable curl is rendered  
**Then** it includes `X-Api-Credential-Id`, `X-Api-Credential-Secret`, `X-Access-Account`  
**And** it does not include `Authorization: Bearer` or an `Idempotency-Key` header  
**And** `idempotencyKey` appears only in the JSON body

### BDD-FOS-W9-002 — Example mode is sync-generate-safe
**Given** a policy that allows only `SYNC_DOWNLOAD_URL` and `ASYNC_TASK`  
**When** the copyable example is built  
**Then** the example uses async batch-generate with `ASYNC_TASK`  
**And** does not emit a doomed sync `SYNC_DOWNLOAD_URL` curl

### BDD-FOS-W9-003 — Error catalogue retryable flags tell the truth
**Given** the contract error catalogue  
**When** callers inspect retryable  
**Then** rate-limit / generation timeout / service-unavailable entries are retryable  
**And** `REQUEST_BODY_INVALID` is categorized as VALIDATION

### BDD-FOS-W9-004 — Per-version variables render
**Given** a contract with callable versions that include `variables`  
**When** the contract panel loads  
**Then** each version shows a field table (key / type / required / enum / description)

### BDD-FOS-W9-005 — No opaque example tokens without test data
**Given** no test-data-derived payload  
**When** the examples section renders  
**Then** the no-test-data hint is shown  
**And** raw tokens like `generate-sync-docx` are not listed

### BDD-FOS-W9-006 — CredentialSummary aligns with OpenAPI
**Given** a contract with credential summary  
**When** JSON is serialized  
**Then** the credential id field is `credentialId` and `expiresAt` is present on the FE type
