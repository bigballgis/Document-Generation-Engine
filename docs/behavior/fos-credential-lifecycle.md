# Behavior — FOS-W10 Credential lifecycle matches ADR

**Slice:** `fos-credential-lifecycle` · **TM:** #180 · **Status:** ready  
**Trace:** ADR-0009 (28-day rotation grace), FOS D11, detail `FOS-W10-credential-lifecycle.md`

## Actor / goal

API admin rotates a credential; consuming systems keep working during grace; expired
credentials cannot be rotated into a still-dead secret.

## Acceptance scenarios

### BDD-FOS-W10-001 — Prior secret accepted during 28-day grace

**Given** an ACTIVE credential whose secret was rotated  
**When** a caller authenticates with the **previous** secret before `rotationGracePeriodEndsAt`  
**Then** authentication succeeds and the new secret also succeeds

### BDD-FOS-W10-002 — Prior secret rejected after grace

**Given** a credential whose rotation grace deadline is in the past  
**When** a caller authenticates with the previous secret  
**Then** the platform returns `INVALID_CREDENTIALS` (fail closed)

### BDD-FOS-W10-003 — Rotate gated on effective status; rebase expiry

**Given** a credential whose effective status is `EXPIRED` or `REVOKED`  
**When** an admin attempts rotate  
**Then** the platform rejects the rotate  
**And** a successful rotate on `ACTIVE` / `EXPIRING_SOON` rebases `expiresAt` from now

### BDD-FOS-W10-004 — Expiry visible on create/rotate and credentials UI

**Given** a create or rotate succeeds  
**When** the management UI shows the one-time secret dialog and credentials table  
**Then** `expiresAt` is present; after rotate, `rotationGracePeriodEndsAt` is present when grace is active

### BDD-FOS-W10-005 — Optional shorter expiry + tiered expiry alerts

**Given** create with `expiryDays` between 1 and 365  
**When** credentials approach expiry at 30 / 7 / 1 days  
**Then** alerts escalate severity (INFO → WARNING) within the existing alert model

### BDD-FOS-W10-006 — Copy secret; confirm rotate mentions 28-day grace

**Given** the secret dialog is open  
**When** the operator copies the secret or confirms rotate  
**Then** copy works and the rotate confirm copy mentions the **28-day** old-secret grace
