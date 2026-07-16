---
id: DOC-ARCH-DATA-STORAGE-VIEW
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
dependsOn:
  - docs/domain/domain-model.md
  - docs/security/permission-matrix.md
  - docs/adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md
  - docs/adr/technology-stack/0022-basic-technology-stack-baseline.md
related:
  - docs/architecture/runtime-view.md
  - docs/architecture/async-messaging-view.md
  - docs/architecture/security-view.md
---

# Data and Storage View

## Purpose

This view defines storage responsibility boundaries for durable data, cache data, generated assets, and sensitive data handling.

## Storage Responsibilities

| Storage Area | Owns | Must Not Store |
| --- | --- | --- |
| Relational Database | Master metadata, template metadata, lifecycle state, API policies, idempotency records, task metadata, audit records, release summaries, **Asset Library catalog** (`library_asset` — CE-E02) | API credential secrets in plaintext, output encryption passwords, raw sensitive template variable values unless explicitly approved and protected |
| Object Storage or File Service | DOCX masters, generated DOCX/PDF files, previews, large rendering artifacts, temporary outputs subject to retention rules, **ACTIVE Asset Library bytes** keyed by `assetKey` (CE-E02) | Authorization decisions as the only source of truth, unbounded retention without policy |
| Redis or Cache Platform | AD Group successful resolution cache, short-lived coordination data, short-lived download/task acceleration data where approved | Failed AD Group resolution cache, expired authorization data as an authorization basis, sensitive plaintext passwords |
| Kafka | Task identifiers, event identifiers, safe summaries, policy versions, trace metadata, retry/dead-letter messages | Full request bodies, raw variables, generated documents, full download URLs, API credential secrets, encryption passwords, full AD Group member lists |
| Logs and Traces | Operational diagnostics, trace IDs, safe summaries, error categories | Sensitive plaintext, full request bodies, generated document content, encryption passwords |

## Retention Baseline

- Async tasks and generated results default to 7 days where confirmed by API requirements.
- Download URLs are short-lived and require authorization on use.
- Audit records are durable and must follow future retention and compliance policy.
- Cache entries must not outlive the confirmed authorization cache rules.

## Data Rules

- Sensitive data defaults to protected or summarized handling until explicitly classified otherwise.
- Generated documents are assets, not message payloads.
- Idempotency records store safe hashes and summaries rather than raw sensitive payloads.
- Authorization caches must fail closed when dependencies are unavailable and no valid cache exists.
- During E05 integration hardening, temporary in-memory storage seams may remain for local/test usage only and must be replaced or explicitly fail-closed for production paths.

## External data services in Kubernetes (P15 / ADR-0030)

PostgreSQL, Redis, Kafka, and MinIO are **not** deployed by the application Helm chart. Operators
provision them outside the release namespace; the chart exposes host/port/bucket settings via
ConfigMap and credentials via Secret keys (`POSTGRES_*`, `MINIO_*`, etc.). Encryption-at-rest and
KMS-managed keys follow ADR-0030; in-transit protection uses TLS 1.2+ to external endpoints and
at the Ingress.

| Topic | Guide |
| --- | --- |
| Operational baseline (backup, DR, encryption) | [ADR-0030](../adr/operations/0030-operational-platform-baseline.md) |
| Canonical deployment guide | [deploy/README.md](../../deploy/README.md) |
| ConfigMap / Secret wiring | [k8s-config-secrets.md](../../deploy/k8s-config-secrets.md) |
| Service DNS and external endpoints | [k8s-ingress-tls.md](../../deploy/k8s-ingress-tls.md) |

## Confirmed — Asset Library catalog storage (CE-E02)

**Closed (2026-07-16):** platform shared Asset Library catalog ownership for CE-E02.

- **Owns:** relational catalog row in `library_asset` (metadata: `asset_key`, `asset_class`, `status`, content type/size/sha256, upload actor/time) and the MinIO object bytes for **ACTIVE** keys (logical `assetKey` ≡ resolvable object key).
- **Does not own:** `StructuredContentImageResolver` protocol, authorization/permission policy source of truth, or generation orchestration.
- **PK exception (intentional):** `library_asset` uses natural-key PK `asset_key` (not UUID). Aligns catalog identity with the resolvable MinIO key grammar (`E02-C1` / `E02-C2`); UUID surrogate is out of scope for CE-E02.
- Disable removes resolvable object bytes and marks `DISABLED`; binary content is not retained as a durable archive in this slice.

Behavior SoT: [ce-e02-asset-library.md](../behavior/ce-e02-asset-library.md). Module boundary: [module-boundaries.md](./module-boundaries.md).

## Confirmed — template test data PII storage (CE-G03)

**Closed (2026-07-15):** former pending question *“Whether any template test data may be stored after masking or must always be synthetic.”*

Template Test Data Set variable values **may** be persisted in the relational Test Data Set store when:

1. The field is not PII-tagged (`piiCategory = NONE` / unset), or
2. PII-tagged fields use author **`SYNTHETIC`** attestation (synthetic or desensitized test values — the default path), or
3. PII-tagged fields use **`EXPLICIT_SENSITIVE`** with reason + secondary confirm + durable management audit (audit must **not** store variable plaintext — only keys, categories, `variablesHash`, reason).

There is **no** approved third path of “mask then silently store” without attestation. Behavior SoT: [ce-g03-testdata-pii.md](../behavior/ce-g03-testdata-pii.md) **G03-C14**. ADR-0020 plaintext bans continue to apply to logs, audit summaries, contract examples, exports, and unauthorized display.

## Confirmed — invocation parameters retention (CE-G06 / ADR-0057)

**Closed (2026-07-16):** architecture Critical conflict between ADR-0020’s ban on plaintext template-variable persistence and CE-G06 regenerate-by-invocation (and ADR-0040 caller reconciliation).

`api_invocation_record.parameters_storage` **may** retain **sanitized** template variable values for:

1. Caller-facing invocation reconciliation (ADR-0040), and
2. Server-internal CE-G06 regenerate replay.

Constraints (confirmed):

- Encryption passwords are never stored.
- Retention TTL equals the invocation-record retention clock; purge destroys the column with the row.
- Management list/detail/CSV, management audit, logs, exports, and contract examples must **not** expose variables (HIST C6).
- Column / application encryption-at-rest for this column is **deferred** (pending KMS; align ADR-0045) — **not** a CE-G06 Done gate.

Authority: [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md). Behavior: [ce-g06-audit-reproducible.md](../behavior/ce-g06-audit-reproducible.md) **G06-C21**.

## Pending Questions

- Final rollout evidence and owner sequence for replacing remaining in-memory persistence adapters across runtime and governance paths.
- Bank KMS / column-encryption ownership for `parameters_storage` (deferred from ADR-0057; not blocking CE-G06).
