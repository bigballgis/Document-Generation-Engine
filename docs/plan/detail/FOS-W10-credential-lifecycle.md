# FOS-W10 — Credential lifecycle matches ADR

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W10 · **Status:** **Not Started**
**Slice id:** `fos-credential-lifecycle` · worktree `../DGE-fos-credential-lifecycle` · branch `feat/fos-credential-lifecycle`
**Task Master:** **#180** · **delivery_lane:** **full** (security-sensitive — never light)
**Origin:** C2, C4, C11, C12, C22
**Confirmed (FOS D11 / OD-FOS-4):** rotation grace period = **28 days** (amends ADR-0009; was 7). W10-1 is **unblocked**.

---

## Before code

```powershell
git worktree add "..\DGE-fos-credential-lifecycle" -b feat/fos-credential-lifecycle origin/main
```

Read amended `docs/adr/api-management/0009-api-credential-lifecycle.md` (28-day grace).

### Tasks

| Id | Sev | Task |
| --- | --- | --- |
| W10-1 | **P0** | Rotation grace period — prior hash usable for **28 days** |
| W10-2 | **P0** | Rotate gates on effective status; rebase `expiresAt` |
| W10-3 | **P1** | Surface `expiresAt` in UI + create/rotate responses |
| W10-4 | **P1** | Optional shorter expiry on create + tiered alerts |
| W10-5 | **P2** | Copy button + confirm on Rotate/Revoke |

---

## W10-1 — Rotate instantly kills the old secret → 28-day grace

**Severity:** P0 · **Unblocked** (D11)
**File:** `ApiCredentialEntity#rotateSecret` — single `secret_hash`, no prior retention.
OpenAPI defines `rotationGracePeriodEndsAt`.

### Implement

1. Flyway: retain previous secret hash + `rotation_grace_period_ends_at` (or equivalent).
2. On rotate: store new hash as current; keep prior hash; set grace deadline = now + **28 days**.
3. Auth filter accepts **either** hash until the deadline; after deadline, only the new hash.
4. Surface `rotationGracePeriodEndsAt` on rotate response + contract credential summary.
5. Constants/docs/tests must say **28 days**, not 7.

Red tests: old secret works during grace; fails after; new secret always works; deadline
is ~28 days from rotation instant.

### Do NOT

- Do not invent a credential-level `ROTATING` status (ADR-0009 already rejected that).
- Do not keep a 7-day constant anywhere for rotation grace after this leaf.

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
DTOs). Column in panel; show in one-time secret dialog. Also show
`rotationGracePeriodEndsAt` after rotate when present.

---

## W10-4 — No shorter expiry; flat 30-day alert

**Files:** `ApiCredentialCommandSupport#createCredential`,
`ApiAccessAlertQueryService`

ADR allows shorter expiry and 30/7/1 day **expiry** reminders (unchanged by D11 — those
are not the rotation grace). Implement optional `expiryDays` bounded by `MAX_EXPIRY_DAYS`;
escalate alert severity at 7 and 1 day before **credential expiry**.

---

## W10-5 — Secret dialog lacks copy; Rotate/Revoke unconfirmed

**File:** `CredentialsPanel.vue`

Add copy button via existing `copyText` helper; `ElMessageBox` confirm on Rotate/Revoke;
guard re-revoke. Confirm copy should mention the **28-day** old-secret grace when rotating.

---

## Exit

Security-focused architecture review mandatory. `mvn verify` + FE gates + E2E credential
issue/rotate happy path (assert grace field present); deploy. ADR-0009 already amended to
28 days before/with this leaf. TM **#180** → done.
