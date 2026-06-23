# P7 — Runtime Dynamic API (Detailed Plan)

**Phase status:** Done | **Depends on:** P5, P6

## Behavior goal

External systems call OpenAPI v1 runtime operations with credential + AD Group +
template authorization; sync/async/batch generation; idempotency; download with
secondary auth.

## Key tasks (thin slice first)

| ID | Task | Status |
| --- | --- | --- |
| P7-D01 | Generation orchestration module (validate → authorize → idempotency → render job) | Done |
| P7-T01 | `GET contract` + `GET versions` (M1 scope) | Done |
| P7-T02 | Sync generate explicit version + default route (DOCX, SYNC_STREAM) — **slice** | Done |
| P7-T03 | Idempotency store (Redis + DB dual-write per ADR) | Done |
| P7-T04 | Batch generate + async task lifecycle (post-slice) | Done |
| P7-T05 | Download document with expiry + secondary authorization | Done |
| P7-T06 | Encryption parameter validation + execution | Done (DOCX + PDF agile encryption; batch PDF deferred) |

**Slice exit:** Authorized caller sync-generates DOCX for published template version.

**Backend slice evidence:** `RuntimeTemplateController`, `ApiCredentialAuthenticationFilter`, Flyway V8 idempotency table, `RedisIdempotencyCache` + `IdempotencyService` dual-write, `TemplatePlatformSliceTest` runtime path.

**Note:** Non-test profiles default to Redis idempotency cache (`docgen.idempotency.cache=redis`); test profile uses in-memory cache.
