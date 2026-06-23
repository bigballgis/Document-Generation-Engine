---
id: ADR-0031
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0031"
topic: api
related:
  - docs/architecture/README.md
  - docs/architecture/runtime-view.md
  - docs/architecture/security-view.md
  - docs/api/contract-outline.md
  - docs/api/openapi-v1.yaml
  - docs/adr/api/0003-api-routing-and-batch-overrides.md
  - docs/adr/api/0004-api-idempotency-strategy.md
  - docs/adr/api/0005-api-response-delivery-and-download-security.md
  - docs/adr/api/0006-api-error-model.md
  - docs/adr/api/0011-api-schema-and-response-envelope.md
  - docs/adr/api/0013-api-contract-visibility-audit-and-context.md
  - docs/adr/api/0014-api-openapi-v1-contract-scope.md
  - docs/adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md
---

# ADR 0031: API Platform Hardening Baseline

## Status

Accepted

## Context

The dynamic API contract already has accepted routing, idempotency, response, error, and authorization ADRs. The remaining technology log entries need a stable platform baseline for API versioning, request/response behavior, CORS, timeouts, compression, signing, cache behavior, and client-facing request constraints.

## Decision

The confirmed API platform hardening baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| API versioning strategy | URI explicit versioning (/v1) | Use explicit major-version routing. |
| API timeout baseline strategy | Read requests 3s / write requests 5s | Endpoint-class timeout baseline. |
| API rate limiting strategy | Token Bucket (dual dimensions: tenant + user) | Logical rate-limit baseline. |
| Rate limit counter storage strategy | Redis (centralized counters) | Counter storage baseline. |
| Rate limit exceed response strategy | Return 429 with standard Retry-After header | Rate-limit response baseline. |
| API request body size limit strategy | No request body size limit | Request-body-size baseline. |
| API response compression strategy | Enable Gzip/Brotli with content-type and payload-size thresholds | Conditional response compression baseline. |
| API CORS strategy | Strict Origin whitelist (environment-specific configuration) | Allowlist by environment. |
| API retry strategy | Auto-retry idempotent read operations only (exponential backoff + jitter) | Safe automatic retry baseline. |
| API circuit-breaker and degradation strategy | Enable dependency-level circuit breakers + rate-limit degradation with recoverable errors | Dependency resilience baseline. |
| API observability correlation ID strategy | Enforce propagation/generation of TraceId and CorrelationId, and write to logs and response headers | Correlation propagation baseline. |
| API cache-control headers strategy | Return explicit Cache-Control/ETag/Last-Modified by resource type | Resource-specific cache metadata baseline. |
| API content negotiation strategy | Content negotiation based on Accept header, defaulting to application/json | JSON default representation baseline. |
| API documentation publishing strategy | Auto-generate and publish API docs with main-branch release | Docs publish with main-branch release baseline. |
| API change compatibility validation strategy | CI-enforced OpenAPI breaking-change checks | CI compatibility gate baseline. |
| API schema validation failure handling strategy | Return 400 + structured field-level error details | Structured schema validation failure baseline. |
| API security response headers baseline strategy | Enable standard security response headers baseline (HSTS, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, CSP) | Security headers baseline. |
| API anti-replay strategy for request source timestamp | Enforce timestamp + validity window check (e.g., 5 minutes) + signature | Time-window replay protection baseline. |
| API request signing algorithm strategy | HMAC-SHA256 | Request signing baseline. |
| API request signing key rotation strategy | Support dual-key overlap-window rotation (new and old keys validated in parallel) | Key rotation baseline. |
| API signature field canonicalization strategy | Fixed canonical order: method, path, sorted query params, key headers, request body hash | Canonicalization baseline. |
| API time synchronization strategy | Server authoritative time; client time is for display only | Server time is authoritative. |
| API client caching strategy | Cache only static metadata and read-only dictionaries | Client cache baseline. |
| API client local storage strategy | Only temporary session state and non-sensitive preferences; no sensitive data at rest | Client storage baseline. |
| API idempotency key transport strategy | Request header: Idempotency-Key | Transport baseline for idempotent requests. |

These decisions are accepted as the API platform hardening foundation. They complement the routing, idempotency, error, response, and authorization ADRs rather than replacing them.

## Consequences

- The API contract can rely on a stable platform behavior baseline for interoperability and client integration.
- Retry, timeout, signing, and cache behavior become predictable across caller implementations.
- Future API platform changes should update this ADR and the affected API/security views together.

## Alternatives Considered

- Keep these choices only in the technology decision log: rejected because durable decisions need an ADR.
- Split every API platform concern into a separate ADR: rejected because the policy set is tightly coupled and easier to maintain as one baseline.
- Leave request signing, rate limiting, and timeout behavior open until implementation starts: rejected because they shape the API surface now.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)
- [OpenAPI v1](../../api/openapi-v1.yaml)
- [Runtime View](../../architecture/runtime-view.md)
- [Security View](../../architecture/security-view.md)
- [Basic Technology Stack Baseline ADR](../technology-stack/0022-basic-technology-stack-baseline.md)