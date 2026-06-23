---
id: ADR-0014
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
adrNumber: "0014"
topic: api
related:
	- docs/api/openapi-v1.yaml
	- docs/api/README.md
---

# ADR 0014: API OpenAPI v1 Contract Scope

## Status

Accepted

## Context

The dynamic API contract has enough confirmed decisions to produce the first formal OpenAPI 3.1 YAML file and example set. Before writing that contract, the project needed to decide whether contract discovery and callable version listing are included in the first OpenAPI file, how those discovery paths are named, which request headers represent API credentials and access account identity, and how `traceId` is sourced.

## Decision

The formal v1 OpenAPI file is maintained at [openapi-v1.yaml](../../api/openapi-v1.yaml). Example requests and responses are maintained under [examples](../../api/examples/README.md).

The v1 OpenAPI file covers:

- API contract discovery.
- Callable release version listing.
- Explicit-version and default single generation.
- Explicit-version and default batch generation.
- Async task query and cancellation.
- Generated document download.

API contract discovery uses `GET /api/{environment}/v1/templates/{templateId}/contract`. It returns contract summary, paths, default-route summary, API policy summary, callable versions, schema names, error code summary, and example indexes. It does not embed the full OpenAPI YAML document in the response.

Callable version listing uses `GET /api/{environment}/v1/templates/{templateId}/versions`. This API returns the current authorized view of callable release versions for the template and is not a back-office version management list.

The v1 request headers for authentication and caller identity are `X-Api-Credential-Id`, `X-Api-Credential-Secret`, and `X-Access-Account`. The credential secret must not be logged, audited, returned, or displayed.

The optional tracing request header is `X-Trace-Id`. If callers provide it, the platform uses that value as the response and audit `traceId`. If callers omit it, the platform generates `traceId`.

## Consequences

- API consumers have one machine-readable OpenAPI 3.1 contract for the confirmed v1 dynamic API baseline.
- Contract discovery and callable version listing become part of the formal API surface rather than only a documentation or portal concept.
- Credential header names are stable enough for generated clients and integration documentation.
- Upstream systems can correlate calls through `X-Trace-Id`, while the platform still guarantees a `traceId` when the caller does not provide one.
- Back-office version management remains separate from the caller-facing callable version list.

## Alternatives Considered

- Exclude contract discovery and callable version listing from OpenAPI v1: rejected because both are confirmed caller-facing capabilities.
- Use `/api-contract` for contract discovery: rejected because `/contract` is shorter and clearly scoped by template.
- Use `/callable-versions` for callable versions: rejected in favor of `/versions`; the response semantics clarify that this is the caller-facing callable list, not a management list.
- Use `Authorization: Bearer` for the API credential secret: not selected for v1 because the confirmed baseline uses explicit API credential ID and secret headers.
- Always generate `traceId` inside the platform: rejected because upstream systems benefit from passing a trace ID when available.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Draft](../../api/contract-outline.md)
- [OpenAPI v1](../../api/openapi-v1.yaml)
- [API Examples](../../api/examples/README.md)