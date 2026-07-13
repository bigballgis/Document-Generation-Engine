# BDD: Cross-package API access alerts

**Status:** `ready`  
**BDD ID:** `BDD-API-ACCESS-CROSS-PACKAGE-ALERTS-001`  
**Slice:** P13-EXTERNAL-SERVICES-EXCELLENCE Phase 3 (D01)

## Actor

**GROUP_ADMIN** / **GLOBAL_ADMIN** with API policy management scope (group-scoped fail-closed).

## Goal

External services overview (`/api/policies`) surfaces **actionable cross-package alerts** (and optional readiness summary cards) — not a second template catalog. Still ADR-0040 package-first: edit on hub **External access**, overview = monitor only.

**Extension (2026-07-14):** [api-ops-discoverability.md](./api-ops-discoverability.md) — `MISSING_AD_GROUP` also covers in-scope **`PENDING_RELEASE`** (empty AD Group); credential alerts remain **PUBLISHED**-scoped; overview may show lightweight summary counts without becoming a catalog.

## Acceptance scenarios

### SCEN-ALERT-01 — Missing AD Group (required)

- **Given** an in-scope `PUBLISHED` or `PENDING_RELEASE` template with empty `allowedAdGroups` (policy row present)
- **When** administrator opens `/api/policies`
- **Then** warning alert row with template name, external ID, deep link to `?tab=apiAccess`

### SCEN-ALERT-02 — Expiring credential (required)

- **Given** published template with credential in EXPIRING_SOON (≤ 30 days)
- **When** overview loads alerts
- **Then** alert lists credential external ID, expiry, hub deep link

### SCEN-ALERT-03 — Zero credentials (required)

- **Given** published callable template with zero non-revoked credentials
- **When** overview loads
- **Then** alert recommends creating access key with hub link

### SCEN-ALERT-04 — No false catalog (boundary)

- **Given** many published / pending-release templates in scope
- **When** overview opens
- **Then** optional readiness summary cards + alerts table + deep links only; **not** a full paginated catalog duplicate («Browse templates» is exit navigation only)

### SCEN-ALERT-05 — Group scope (boundary)

- **Given** GROUP_ADMIN authorized for RETAIL only
- **When** alerts fetch
- **Then** no cross-group template leakage
