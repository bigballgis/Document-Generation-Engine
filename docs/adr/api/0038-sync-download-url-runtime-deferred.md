# ADR 0038: SYNC_DOWNLOAD_URL runtime delivery deferred (v1)

**Status:** Accepted  
**Date:** 2026-06-23  
**Deciders:** Maintainer (Batch B default — comprehensive optimization roadmap COR-B01)

## Context

OpenAPI v1 and PRD enumerate `SYNC_DOWNLOAD_URL` as an output mode. The v1 runtime
implementation delivers synchronous generation via `SYNC_STREAM` (response body) only.
Examples and milestone task sheets historically marked download-URL delivery Done at
contract level, not at runtime behavior.

## Decision

1. **Contract enum retained** — `SYNC_DOWNLOAD_URL` stays in OpenAPI/PRD enums for
   forward compatibility and policy configuration vocabulary.
2. **Runtime v1 behavior** — Sync/batch paths reject `SYNC_DOWNLOAD_URL` with
   `422` / `OUTPUT_MODE_NOT_ALLOWED` until a dedicated implementation slice lands
   (tracked as COR-B01 implementation, post COR-B02).
3. **Documentation** — Contract outline and runtime runbook state that download-URL
   sync delivery is **not** available in v1 runtime; callers must use `SYNC_STREAM`
   or `ASYNC_TASK`.

## Consequences

- No misleading `DownloadUrlResponse` from generate endpoints in v1.
- Policy seeds may still list modes for future enablement; runtime validates against
  implemented modes.
- Full implementation requires storage-backed one-time URLs, expiry, and tests — separate slice.

## Traceability

- COR-B01 (comprehensive-optimization-roadmap.md)
- ADR 0005 (download security), ADR 0012 (enum naming)
