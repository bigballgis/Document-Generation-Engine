# FOS-W10 — Credential lifecycle matches ADR

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W10 · **Status:** **Not Started**
**Slice id:** `fos-credential-lifecycle` · worktree `../DGE-fos-credential-lifecycle` · branch `feat/fos-credential-lifecycle`
**Task Master:** **#180** · **delivery_lane:** **full** (security-sensitive — never light)
**Origin:** C2, C4, C11, C12, C22
**Open decision:** **OD-FOS-4** — confirm ADR-0009 7-day grace still required before implementing W10-1

---

## Before code

```powershell
git worktree add "..\DGE-fos-credential-lifecycle" -b feat/fos-credential-lifecycle origin/main
```

Read `docs/adr/api-management/0009-api-credential-lifecycle.md` end-to-end. If OD-FOS-4 is
unanswered, implement W10-2…W10-5 only and leave W10-1 **Blocked**.

### Tasks

| Id | Sev | Task |
| --- | --- | --- |
| W10-1 | **P0** | Rotation grace period (prior hash usable until deadline) — **OD-FOS-4** |
| W10-2 | **P0** | Rotate gates on effective status; rebase `expiresAt` |
| W10-3 | **P1** | Surface `expiresAt` in UI + create/rotate responses |
| W10-4 | **P1** | Optional shorter expiry on create + tiered alerts |
| W10-5 | **P2** | Copy button + confirm on Rotate/Revoke |

---

## W10-1 — Rotate instantly kills the old secret

**Severity:** P0 · **Blocked on OD-FOS-4**
**File:** `ApiCredentialEntity#rotateSecret` — single `secret_hash`, no prior retention.
ADR-0009 requires 7-day grace; OpenAPI even defines `rotationGracePeriodEndsAt`.

### Implement (after confirmation)

Retain previous hash + grace deadline; auth filter accepts either hash until deadline;
surface deadline on rotate response + contract summary. Flyway for new columns.
Tests: old secret works during grace; fails after; new secret always works.

---

## W10-2 — Rotate ignores effective EXPIRING/EXPIRED

**Severity:** P0
**File:** `ApiCredentialCommandSupport#rotateCredential`

Guard uses persisted `ACTIVE` only; nothing writes `EXPIRED`; past `expiresAt` still
rotates and leaves expiry unchanged → consumer gets a still-dead secret.

### Implement

Gate on `ApiCredentialLifecycleSupport.resolveEffectiveStatus(...)`. On successful
rotate, re-base `expiresAt` from now using the same default/max helpers as create.
Red tests for past-expiry rotate behaviour.

---

## W10-3 — Expiry invisible in credentials UI / handoff payloads

**Files:** `frontend/src/types/templateApiAccess.ts`, `CredentialsPanel.vue`,
`ApiCredentialCreatedView`, `RotateCredentialResponse`, FE create/rotate handlers

### Implement

Add `expiresAt` end-to-end (API views already have it on summary — extend create/rotate
DTOs). Column in panel; show in one-time secret dialog.

---

## W10-4 — No shorter expiry; flat 30-day alert

**Files:** `ApiCredentialCommandSupport#createCredential`,
`ApiAccessAlertQueryService`

ADR allows shorter expiry and 30/7/1 day reminders. Implement optional `expiryDays`
bounded by `MAX_EXPIRY_DAYS`; escalate alert severity at 7 and 1 day.

---

## W10-5 — Secret dialog lacks copy; Rotate/Revoke unconfirmed

**File:** `CredentialsPanel.vue`

Add copy button via existing `copyText` helper; `ElMessageBox` confirm on Rotate/Revoke;
guard re-revoke.

---

## Exit

Security-focused architecture review mandatory. `mvn verify` + FE gates + E2E credential
issue/rotate happy path; deploy. TM **#180** → done (or Done-with-OD residual).
