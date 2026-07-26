# FOS-W9 — Contract works first time

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W9 · **Status:** **Done**
**Slice id:** `fos-contract-works-first-time` · worktree `../DGE-fos-contract-works-first-time` · branch `feat/fos-contract-works-first-time`
**Task Master:** **#179** · **delivery_lane:** **full**
**Origin:** C1, C3, C5, C6, C16, C17, C18

---

## Before code

```powershell
git worktree add "..\DGE-fos-contract-works-first-time" -b feat/fos-contract-works-first-time origin/main
```

Persona check: a consuming developer copies the platform artifact and must succeed on the
**first** call against a published template with SYNC_STREAM allowed.

### Tasks

| Id | Sev | Task |
| --- | --- | --- |
| W9-1 | **P0** | Copyable curl uses real credential headers (not Bearer) |
| W9-2 | **P0** | Example mode must be one sync generate actually accepts |
| W9-3 | **P1** | Error catalogue retryable flags + missing codes |
| W9-4 | **P1** | FE shows per-version `variables` schema from contract |
| W9-5 | **P1** | Drop opaque example tokens when no test-data payload |
| W9-6 | **P2** | Align `CredentialSummary` / error catalogue with OpenAPI |

---

## W9-1 — Copyable curl uses Bearer auth

**Severity:** P0
**File:** `frontend/src/utils/contractCopyableExample.ts` (`buildSyncGenerateCurl`)

**Evidence:**
```ts
`-H 'Authorization: Bearer ${ACCESS_TOKEN_PLACEHOLDER}'`
`-H 'Idempotency-Key: …'`
```

Runtime filter expects `X-Api-Credential-Id`, `X-Api-Credential-Secret`,
`X-Access-Account` (`ApiCredentialAuthenticationFilter`). Idempotency key is a **body**
field (`GenerateRequestBody.idempotencyKey`), not a header.

### Implement

Emit the three real headers with placeholders; keep `idempotencyKey` in JSON body only.
Unit test asserts no `Authorization: Bearer` and no `Idempotency-Key` header line.

---

## W9-2 — Example can pick permanently unsupported SYNC_DOWNLOAD_URL

**Severity:** P0
**Files:** `contractCopyableExample.ts` (`pickDefaultMode`),
`OutputModePolicyValidator#validateSyncGenerate` (rejects non-`SYNC_STREAM` and always
rejects sync download URL per ADR-0038)

### Implement

Restrict example mode to modes sync generate accepts. If the policy allows only
`SYNC_DOWNLOAD_URL` + `ASYNC_TASK`, render the async/batch example instead of a doomed
sync curl. Red test with a policy shaped like `docs/api/examples/contract-response.json`.

---

## W9-3 — Standard error catalogue lies about retryable

**File:** `ContractAssemblyViewSupport#standardErrorCodes` — every entry passes
`retryable=false`; omits `RATE_LIMIT_EXCEEDED`, `GENERATION_TIMEOUT`, etc.

### Implement

Derive catalogue rows from the same constants used by exception advice / rate-limit
filter. Include real retryable flags. FE "Retryable" column will then show truth.

---

## W9-4 — Variables schema missing from FE contract panel

**Files:** `frontend/src/types/contract.ts` (`CallerContractVersion`),
`TemplateCallerContractPanel.vue`

OpenAPI says `variables` always present on GET contract; backend projects them; FE type
omits them.

### Implement

Add `variables` to the TS type; render a per-version field table (name / required / type /
enumValues). Reuse OA table patterns.

---

## W9-5 — Examples are opaque tokens

**Files:** `ContractAssemblyService#assemble`, `TemplateCallerContractPanel.vue`

### Implement

When no test-data-derived payload exists, show the existing "no test data set" hint
instead of listing raw tokens like `generate-sync-docx`. Do not invent new example files
in this leaf unless already loaded.

---

## W9-6 — Contract JSON vs closed OpenAPI CredentialSummary

Align Java `RuntimeCredentialSummaryView` field names with YAML (`credentialId` vs
`credentialExternalId`); carry `expiresAt` in FE type. Fix catalogue category drift for
`REQUEST_BODY_INVALID` (C18) by sharing constants.

---

## Exit

First-call curl works on acceptance stack against KEEP-8 published template; Vitest for
curl builder; E2E contract panel shows variables; deploy + UIUX. TM **#179** → done.
