---
id: ADR-0001
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- security
	- api
adrNumber: "0001"
topic: authorization-security
related:
	- docs/security/permission-matrix.md
	- docs/api/contract-outline.md
	- docs/domain/domain-model.md
---

# ADR 0001: DOCX/PDF Dynamic Output Encryption

## Status

Accepted

## Context

The platform generates banking financial letters as DOCX and PDF outputs through template-driven APIs. The user asked whether generated PDF files can be encrypted dynamically by parameters, then confirmed that this capability should be a formal requirement and that DOCX must support encryption as well.

This decision affects product requirements, domain modeling, API behavior, permission design, and future implementation.

## Decision

DOCX and PDF outputs must support dynamic encryption based on API parameters.

Encryption capability must be controlled by API management configuration. API parameters can request encryption only when the relevant API management configuration permits it.

API requests can directly provide passwords for DOCX/PDF dynamic encryption.

Passwords provided by API requests are not stored and must not be written to logs. Passwords are used only during the current generation process. Audit records only whether encryption was enabled and an encryption policy summary.

Dynamic encryption configuration is part of API management capability. It is not configured during template composition or template submission.

The encryption API parameter model is the standard model: `enabled`, `openPassword`, `ownerPassword`, and `permissions`.

The encryption policy summary uses the standard summary: encryption enabled flag, output format, whether `openPassword` was provided, whether `ownerPassword` was provided, and a `permissions` summary.

When `encryption.enabled=true`, `openPassword` is required and `ownerPassword` is optional.

`permissions` uses a unified abstract permission enum that is mapped to the relevant DOCX or PDF encryption capability for the requested output format. The v1 permission values are `ALLOW_PRINT`, `ALLOW_COPY`, `ALLOW_EDIT`, `ALLOW_ANNOTATE`, and `ALLOW_FORM_FILL`. Unsupported permission combinations return `400 ENCRYPTION_PARAMETER_INVALID`.

If `permissions` is provided, `ownerPassword` must also be provided. Otherwise the request returns `400 ENCRYPTION_PARAMETER_INVALID`.

If `encryption.enabled=false` or `enabled` is omitted while `openPassword`, `ownerPassword`, or `permissions` is still provided, the request returns `400 ENCRYPTION_PARAMETER_INVALID`. The platform does not silently ignore encryption subfields in that case.

`openPassword` and `ownerPassword` must each be at least 12 characters and at most 128 characters. If both are provided, they must be different. Passwords that fail this baseline return `400 ENCRYPTION_PARAMETER_INVALID`.

If encryption parameters are valid but encryption processing fails, the platform returns `500 ENCRYPTION_FAILED` with `retryable=true`. Error responses, logs, and audit records must not expose passwords, internal encryption details, or sensitive configuration values.

## Consequences

- Output encryption becomes a first-class product requirement for both DOCX and PDF.
- Dynamic API schemas will need to account for encryption-related request behavior once API contracts are formalized.
- Permission documentation must distinguish template composition permissions from API management permissions for encrypted-output configuration.
- Audit records must not store or log API-provided encryption passwords.
- Audit records must capture whether encryption was enabled and the confirmed encryption policy summary: output format, whether `openPassword` was provided, whether `ownerPassword` was provided, and a `permissions` summary.
- Callers get immediate parameter feedback for weak passwords, missing required password fields, unsupported permission combinations, and inconsistent `enabled` usage.
- `permissions` stays contract-level and format-neutral while still allowing the platform to map to DOCX/PDF-specific encryption capabilities later.
- Encryption processing failures remain retryable platform failures without exposing sensitive implementation or password details.

## Alternatives Considered

- Support only PDF encryption: rejected because DOCX encryption is also required.
- Keep encryption as a future extension: rejected because the capability has been confirmed as a formal requirement.
- Use only fixed template-level encryption strategy: not selected; API-parameter-driven dynamic encryption is required, controlled by API management configuration.
- Allow encryption subfields when `enabled` is false or omitted: rejected because callers could incorrectly believe an encrypted file was produced.
- Make password strength fully caller-defined: rejected because the API needs a clear minimum baseline for financial document outputs.
- Define separate DOCX and PDF permission objects in the baseline: rejected for now because a unified abstract enum keeps the generated API contract simpler while field naming and schema format are still pending.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)