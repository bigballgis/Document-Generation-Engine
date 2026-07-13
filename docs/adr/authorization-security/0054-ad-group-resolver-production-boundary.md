---
id: ADR-0054
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - security
  - architecture
adrNumber: "0054"
topic: authorization-security
related:
  - docs/adr/authorization-security/0010-ad-group-authorization-resolution.md
  - docs/security/permission-matrix.md
  - docs/behavior/ops-ad-group-stub-close.md
  - docs/operations/launch-readiness-checklist.md
  - docs/operations/runbook.md
  - docs/plan/execution-sync-ledger.md
---

# ADR 0054: AD Group Resolver Production Boundary (Config Stub vs Directory Adapter)

## Status

Accepted

## Context

ADR-0010 defines request-time AD Group cache and fail-closed `503 AD_GROUP_RESOLUTION_FAILED`
semantics. It does **not** decide whether a YAML account→group map may stand in for an
enterprise directory on acceptance or production paths.

The current implementation exposes `AdGroupResolver` with a single durable bean,
`ConfigAdGroupResolver`, driven by `docgen.ad-group-resolver.type` (default `config`) and
`docgen.ad-group-resolver.account-groups` demo mappings (`svc-caller`, `e2e-runtime-caller`).
That stub is appropriate for local development and automated tests. It is **not** a
production directory.

Launch-readiness checklist **#5a** and the ledger seam «AD Group resolution» still require
an honest production boundary: either a real directory adapter is configured, or the process
refuses to start on acceptance/production paths. Company LDAP/AD hostnames, bind DNs,
schemas, and credentials remain **UNKNOWN** in this repository and must not be invented.

Behavior source of truth for this slice: [BDD-OPS-AD-GROUP-STUB-001](../../behavior/ops-ad-group-stub-close.md)
(decisions ADG-C1…C10; scenarios S1–S4). User confirmation (2026-07-12) accepted this
direction for Task Master **#46** / slice `ops-ad-group-stub-close`.

## Decision

1. **Config-file resolver is local/dev/test only.**
   `docgen.ad-group-resolver.type=config` and `ConfigAdGroupResolver` (including
   `account-groups` YAML maps) are allowed only on pure local/automation paths:
   Spring `dev` / `local` / `test` (and equivalent documented test profiles). Demo account
   mappings may remain in `application.yml` / `application-test.yml` when clearly marked
   **local/test only**.

2. **Acceptance/production must not silently use the config stub.**
   When the process is on an acceptance or production path (`prod` Spring profile active,
   **or** `docgen.environment` / `APP_ENVIRONMENT` outside `dev`/`local`/`test`),
   `type=config` (or any default that resolves to the config stub) **fails closed at
   startup** (`IllegalStateException` or equivalent), **unless** the unique LAB ONLY
   override in Decision 7 is explicitly enabled. Absent that override, the process must
   not serve traffic as if it had production directory resolution. Acceptance compose
   mixes such as `SPRING_PROFILES_ACTIVE=prod,dev` with `APP_ENVIRONMENT=dev` must
   **not** bypass this refuse by soft-environment alone (same honesty pattern as JWT
   production secret guard).

3. **Production/acceptance requires a directory adapter SPI that is configured, or fail-closed.**
   Explicit non-`config` resolver types (reserved names such as `ldap` / `directory` or
   other SPI identifiers) are allowed on acceptance/production **only** when the matching
   directory adapter bean exists **and** required directory configuration is complete.
   If the type is unimplemented or not fully configured, startup **fails closed** with the
   same hard-refuse semantics — no silent fallback to `ConfigAdGroupResolver` + YAML maps.

4. **Company LDAP/AD coordinates = UNKNOWN.**
   Documentation, samples, and tests must not invent company LDAP/AD hostnames, bind
   credentials, or schemas and present them as accepted production facts. Operators supply
   company directory coordinates when available.

5. **Does not supersede ADR-0010.**
   Request-time cache TTL, non-caching of failures, use of non-expired cache on transient
   directory failure, and `503 AD_GROUP_RESOLUTION_FAILED` when no usable cache exists
   remain governed by ADR-0010. This ADR only bounds **which resolver implementation may
   run** on which environment path. Startup refuse for stubs is separate from request-time
   directory outage handling for a real adapter.

6. **Checklist #5a residual.**
   Closing the silent-stub production path (ADR + startup guard) may support an honest
   **CONDITIONAL** verdict after implement evidence. **GO** for #5a still requires a
   non-invented directory adapter implementation plus operator/directory evidence when
   the company directory is available. Clearing the stub alone is **not** production
   go-live; overall checklist remains **NO-GO** while other blockers remain open.

7. **LAB ONLY exception (unique, explicit).**
   Property `docgen.ad-group-resolver.allow-config-stub-on-prod-profile=true`
   (environment shorthand `DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB`, compose/bind
   `DOCGEN_AD_GROUP_RESOLVER_ALLOW_CONFIG_STUB_ON_PROD_PROFILE`) is the **sole**
   authorized exception that permits `type=config` / `ConfigAdGroupResolver` on a
   prod-shaped local acceptance stack (e.g. docker compose with `prod` profile for
   E2E). It **MUST** be absent or `false` in any claimed production deployment.
   When the override is active, the guard **WARN**-logs that this is **not**
   production directory resolution. The override does **not** constitute enterprise
   AD/LDAP resolution and does **not** satisfy checklist **#5a GO**.

## Consequences

- Acceptance/production stacks cannot quietly authorize via demo YAML group maps.
- Developers and E2E retain a simple config map for local and test profiles.
- Local docker acceptance may opt in to the LAB ONLY override (Decision 7) with WARN
  logs; claimed production must omit it.
- Operators must supply a real directory adapter configuration when available; until then
  prod-shaped starts refuse rather than pretending directory resolution works (unless the
  explicit LAB override is set for local acceptance only).
- ADR-0010 request-time semantics stay stable for future real adapters.
- Checklist #5a and the ledger seam stay open (or CONDITIONAL after evidence) until a
  real adapter ships — this ADR alone does not close the seam or flip #5a; the LAB
  override does not satisfy #5a GO.

## Alternatives Considered

- **Invent fake company LDAP hostnames / schema / credentials in-repo as “production”
  facts:** rejected — company coordinates are UNKNOWN; inventing them would falsify
  launch-readiness evidence.
- **Keep a silent production config stub** (allow `type=config` on acceptance/prod as
  long as the map is non-empty): rejected — a non-empty demo map is still not a
  directory; it masks checklist #5a risk. Local docker acceptance must use the
  **explicit** LAB ONLY override (Decision 7) with WARN logs — never a silent default.
- **Implement full production LDAP/AD client now without a confirmed directory
  source of truth:** rejected — out of scope for this boundary ADR; residual remains
  “real adapter when directory available.”

## Related Documents

- [ADR-0010 AD Group Authorization Resolution](./0010-ad-group-authorization-resolution.md)
- [BDD-OPS-AD-GROUP-STUB-001](../../behavior/ops-ad-group-stub-close.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Launch readiness checklist](../../operations/launch-readiness-checklist.md) (#5a)
- [Production runbook](../../operations/runbook.md)
- [Execution sync ledger](../../plan/execution-sync-ledger.md) (transitional seam «AD Group resolution»)
