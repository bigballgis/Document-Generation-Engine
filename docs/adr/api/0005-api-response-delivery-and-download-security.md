---
id: ADR-0005
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
	- security
adrNumber: "0005"
topic: api
related:
	- docs/api/contract-outline.md
	- docs/security/permission-matrix.md
	- docs/architecture/security-view.md
---

# ADR 0005: API Response Delivery and Download Security

## Status

Accepted

## Context

The dynamic API supports synchronous file streams, synchronous download URLs, asynchronous task acceptance, and asynchronous result retrieval. These response modes need consistent troubleshooting metadata, secure file retrieval, and predictable result retention.

Synchronous file streams are a special case because the response body carries the generated DOCX/PDF file rather than a structured response object. The API still needs to expose enough metadata for callers and support teams to trace, audit, and diagnose the generation result.

## Decision

Synchronous file stream responses place the file content in the response body and carry core metadata in response headers.

The confirmed file-stream response header metadata is `auditId`, `traceId`, `requestId`, `idempotencyStatus`, `documentId`, `templateId`, `routeType`, `resolvedReleaseVersion`, `output.format`, and `output.mode`.

Synchronous download URLs and asynchronous result download URLs are valid for exactly 15 minutes. The validity period is fixed and cannot be overridden through API management configuration.

Downloading a file requires secondary authorization: the platform validates API credential, AD Group, template-level authorization, URL validity, and result validity at download time.

Downloading a generated file does not re-check whether the original release version is still callable. A generated result does not become unavailable during download solely because the release version is later disabled or because the default route target changes.

Download URLs can be used multiple times during their validity period.

Download URLs cannot be configured as one-time links in v1. Download responses include `download.expiresAt` and `download.oneTime`; `download.oneTime` is always `false` under the v1 strategy.

Download, task, and replay-related API time fields use ISO 8601 with an explicit timezone offset, for example `2026-06-03T16:30:00+08:00`. This applies to `download.expiresAt`, `task.acceptedAt`, `task.updatedAt`, `task.expiresAt`, and `originalRequestAt`. The API must not return timezone-less local timestamps or Unix-only timestamps for these fields.

API responses to authorized callers return a usable `download.url`. Logs, audit records, management UI, and API contract examples must display masked download URLs and must not show the complete usable URL.

The download-address file retrieval endpoint does not reissue a new URL after a download URL expires. For repeated generation submission that hits the same successful synchronous download-url idempotency record, the platform returns the original URL when it is still valid; if the original URL has expired and the generated result is still retained, the repeated-submission response may issue a new download URL without creating a new document.

Asynchronous task query returns task status, response metadata, successful result information, or unified error details. Asynchronous batch task query also returns batch summary and per-item success/failure details.

Asynchronous tasks and generated results are retained for 7 days by default.

The platform does not proactively notify callers or administrators before 7-day generated result cleanup. Cleanup is recorded in audit.

## Consequences

- File-stream callers can receive a plain file response while still getting traceable metadata through headers.
- Download URLs are not treated as sufficient authorization by themselves; callers must remain authorized at download time.
- Multiple downloads are possible during the 15-minute URL validity period, which supports operational retries and downstream retrieval flows.
- The platform must retain asynchronous task and result state for 7 days by default, while still keeping the download-address file retrieval endpoint non-reissuing after URL expiry.
- Repeated synchronous download-url generation requests with the same successful `idempotencyKey` can reuse the original URL or receive a newly issued URL when the original has expired but retained result data still exists.
- API clients can parse download and task timestamps consistently because time fields include an explicit timezone offset.
- Operational views and audit trails reduce accidental exposure of complete usable download URLs through mandatory masking.
- Result cleanup remains operationally simple because there is no proactive notification workflow before the 7-day cleanup.

## Alternatives Considered

- Return multipart responses for synchronous file streams: rejected because it complicates common file-download clients and integrations.
- Return only the file stream without metadata: rejected because support, audit, and caller troubleshooting need stable correlation identifiers.
- Treat possession of a download URL as sufficient authorization: rejected because generated financial documents require authorization checks at retrieval time.
- Make download URLs one-time by default: rejected because the confirmed behavior allows multiple downloads during the validity period.
- Allow API management to configure one-time download URLs: rejected for v1 because it creates template-specific retrieval behavior and complicates downstream retry flows.
- Reissue expired download URLs from the download-address file retrieval endpoint: rejected for v1 because retrieval stays a strict short-lived URL access flow.
- Return Unix-only timestamps for download and task times: rejected because callers need human-readable timestamps with explicit timezone offsets.
- Show complete usable download URLs in logs, audit records, management UI, or contract examples: rejected because generated financial document links should not be unnecessarily exposed outside the authorized API response.
- Proactively notify callers or administrators before generated result cleanup: rejected because 7-day retention is fixed and cleanup traceability is handled through audit.
- Re-check release-version callable status during download: rejected because a successfully generated result should remain retrievable during the short download window even if the release version is later disabled or the default route target changes.
- Return only task status from asynchronous query: rejected because callers need result or error details without stitching together multiple calls for the normal workflow.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Draft](../../api/contract-outline.md)