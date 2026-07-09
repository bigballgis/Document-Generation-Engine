# CORE-FORTRESS F2 — Runtime Lightweight (Detailed Plan)

**Program ID:** `CORE-FORTRESS`  
**Phase ID:** `CORE-FORTRESS-F2-RUNTIME-LIGHTWEIGHT`  
**Phase status:** **Done** (2026-07-09)  
**Depends on:** CORE-FORTRESS F1 (**Done**)  
**BDD:** `docs/behavior/core-fortress-f2-runtime-lightweight.md` — **ready**

> **Single-active-phase invariant:** F2 closed. **F4** sole formal `In Progress` (F3 Done 2026-07-09).

---

## 1. Exit criteria

1. Publish snapshots fidelity warning codes on `template_version`.
2. Runtime generation reads cache for published/stopped versions.
3. Lifecycle sync uses bulk repository updates.
4. Idempotency release matching avoids per-version hash recompute.
5. `mvn verify` green.

---

## 4. Task breakdown

| ID | Task | Status |
| --- | --- | --- |
| F2-T01 | BDD spec | **Done** (2026-07-09) |
| F2-T02 | B1 Flyway + entity + publish snapshot | **Done** (V49, `VersionFidelityWarningService.snapshotOnPublish`) |
| F2-T03 | B1 runtime read cache | **Done** (`DocumentGenerationEngine`, `RuntimeGenerationService` → `resolveWarningCodes`) |
| F2-T04 | B5 lifecycle bulk update | **Done** (`TemplateVersionRepository.bulkUpdate*`) |
| F2-T05 | B4 idempotency hash cache | **Done** (V50 `resolved_release_version`; `IdempotencyService.begin` stores release) |
| F2-T06 | Tests + architecture review | **Done** (`VersionFidelityWarningServiceTest`, `RuntimeGenerationServiceIdempotencyReleaseCacheTest`; governance bulk verify) |
| F2-T07 | Doc sync + closeout | **Done** (2026-07-09) |

**Gate evidence:** `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (2026-07-09).

---

## 5. Program roadmap (F3–F8)

| Phase | Name | Status |
| --- | --- | --- |
| F1 | Rendering core correctness | **Done** |
| **F2** | Runtime lightweight | **Done** (2026-07-09) |
| **F3** | Node matrix + expression engine | **Done** (2026-07-09) |
| **F4** | Production rendering hardening | **In Progress** |
| F5 | Async durability + security | Not Started |
| F6 | Frontend kernel refactor | Not Started |
| F7 | Authoring UX | Not Started |
| F8 | Observability + evidence | Not Started |
