# FOS-W13 — Default verify honesty

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W13 · **Status:** **Not Started**
**Slice id:** `fos-default-verify-honesty` · worktree `../DGE-fos-default-verify-honesty` · branch `feat/fos-default-verify-honesty`
**Task Master:** **#183** · **delivery_lane:** **full**
**Origin:** E2, E4, E6, E7, E8, E9, E10, E11
**Complement:** [CRCH W5](../core-render-compute-hardening-program.md) §8 owns LO version pin,
default LO smoke, PDF→PNG lane, Word-vs-LO baseline, conversion soak — **do not duplicate**

---

## Before code

```powershell
git worktree add "..\DGE-fos-default-verify-honesty" -b feat/fos-default-verify-honesty origin/main
```

Coordinate with CRCH W5: if CRCH W5 detail sheet lands first, skip any overlapping task.

### Tasks

| Id | Sev | Task |
| --- | --- | --- |
| W13-1 | **P0** | Flyway + Postgres schema validation in CI (testcontainers lane) |
| W13-2 | **P0** | Mark SYNTHETIC PDF assertions as harness self-tests |
| W13-3 | **P1** | Un-defer or honestly label deferred PDF assertions for complex packages |
| W13-4 | **P1** | Golden package: real condition-inside-loop |
| W13-5 | **P1** | MinIO adapter Testcontainers smoke |
| W13-6 | **P1** | Redis Testcontainers for idempotency / rate-limit |
| W13-7 | **P1** | FTS round-trip on real Postgres |
| W13-8 | **P1** | JaCoCo package floors for weakest core packages |

---

## W13-1 — 77 migrations never validated in default/CI gates

**Evidence:** `application-test.yml` has `flyway.enabled: false` + H2 `ddl-auto: create-drop`;
`surefire.excludedGroups=testcontainers`; existing Flyway smoke only asserts migrate success.

### Implement

Add/extend a `@Tag("testcontainers")` test: Flyway migrate on Postgres → boot with
`ddl-auto: validate` (or equivalent schema assert). Wire `-Ptestcontainers` (or the
existing profile name) into a CI job. Do not enable Testcontainers inside every
developer’s default `verify` if the ratchet plan forbids it — but CI must run it.

---

## W13-2 — SYNTHETIC PDF assertions pretend to be product proof

**Files:** `GoldenCorpusActiveRunner.synthesizePdfFromDocxText`, packages with
`"pdfSource": "SYNTHETIC"`

### Implement

Relabel SYNTHETIC PDF assertions as harness self-tests (separate surefire group / naming
/ documentation in `golden-corpus/README.md`). Product PDF claims move behind the LO lane
(CRCH W5-2). Chinese package must not claim CJK proof via a strip-non-ASCII stub.

---

## W13-3 — Deferred PDF halves

Un-defer minimal assertions under LO lane for packages that claim multi-page / QR /
attachments — or mark them explicitly `productPdf: pending-CRCH-W5` in JSON so agents
stop treating them as covered.

---

## W13-4 — "nested-clauses" corpus is not nested

Add a corpus package with `conditionBlock` inside `loopBlock`, false-branch, empty
collection. Name it honestly.

---

## W13-5 — Production MinIO untested

`MinioObjectStorage` is `matchIfMissing = true` production default; zero tests.

### Implement

Testcontainers MinIO: put/get/delete (+ presign if trivial). Keep filesystem provider for
default unit tests.

---

## W13-6 — Redis paths mock-only

Testcontainers Redis covering `RedisIdempotencyCache` + coordinated rate limit service
(the production siblings of in-memory test config).

---

## W13-7 — FTS tests assert the mock received the enum

Move FULL_TEXT assertions into Postgres testcontainers with index-then-search.

---

## W13-8 — Bundle-only JaCoCo hides weak packages

Add PACKAGE-element rules starting at measured floors for `authoring`, `apimgmt`,
`template` (read current JaCoCo report first; do not invent 0.90 overnight — ratchet).
Update `coverage-ratchet-plan.md` FE threshold quote drift.

---

## Exit

CI job evidence for testcontainers lane; corpus honesty; package floors recorded; no
duplication of CRCH W5 five items. TM **#183** → done.
