# boot-4-1-upgrade — Spring Boot 4.1.0 + Java 25 platform baseline (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `boot-4-1-upgrade` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-13 |
| **Status** | **Done** (2026-07-14) — merge `993c287`; feature tip `e9bf43c`; worktree removed |
| **Formal phase** | **None** (no sole-active) |
| **Task Master** | **#51** (`done`) |
| **Placement** | Was **ISOLATED** — `D:/working/DGE-boot-4-1-upgrade` · `feat/boot-4-1-upgrade` (removed after stage 11 merge to `main`) |
| **ADR baseline** | [ADR-0028](../adr/technology-stack/0028-backend-platform-stack-baseline.md) — **amended 2026-07-13** (Boot **3.x → 4.x**, pin **4.1.0**; second amendment **Java 21 → 25**, user said “25”). Implemented on `main` at merge `993c287`. |
| **Gate evidence** | `mvn verify` **GREEN** 1357/0/0/7 (Temurin 25); architecture **PASS_WITH_NOTES**; **DEPLOY_OK_WITH_NOTES** — `GET /healthz` **200**; runtime Boot **4.1.0** + Java **25.0.3** |

---

## Actor / goal / trigger

| Field | Value |
| --- | --- |
| **Actor / role** | Platform engineer / operator (not an end-user product role) |
| **Goal** | Move Maven Spring Boot parent to **4.1.0**, adopt **Java 25** (`release 25`), keep `mvn verify` green, start the app with `/healthz` OK, and document the amended ADR-0028 + stack baseline |
| **Trigger** | Explicit user confirmation to upgrade Spring Boot to **4.1.0** (user said “4.10”; interpreted as **4.1.0**), then explicit confirmation of Java **25** (user said “25”) for the same slice |
| **Preconditions** | Isolated worktree on `feat/boot-4-1-upgrade`; ADR-0028 **amended** to **Java 25 + Spring Boot 4.x** (pin **4.1.0**); Task Master **#49** (`deps-security-refresh`) left the Boot parent on the **3.3.13** line / Java **21** until this upgrade slice implements the parent + toolchain bump |

---

## Why BDD is not-applicable

This slice is a **platform / ops infrastructure baseline upgrade**, not a product behavior change:

- No new actor journey, management UI surface, public/runtime API contract, permission rule, or audit semantics.
- No intentional change to generation, authoring, publish, or management response contracts for end users.
- Outcomes are **BOM/parent pin + Java 25 toolchain + dependency co-upgrades + green verify + ADR/stack doc sync**, not new product acceptance thresholds.
- Product Given/When/Then BDD scenarios would invent UI/API journeys that this upgrade does not own; regression is covered by existing backend (and related) quality gates.

Analogous readiness: [slim-knip-scan](./slim-knip-scan.md) / [LR-D2 backup-restore](./lrp-d2-backup-restore.md) / [LR-D6 load smoke](./lrp-d6-load-smoke.md) — tooling/ops/evidence slices with `bdd_readiness: not-applicable`.

---

## What is in scope (platform only)

| Deliverable | Intent |
| --- | --- |
| **Boot parent** | `spring-boot-starter-parent` **3.3.x → 4.1.0** |
| **Java toolchain** | Maven compile **`release 25`**; runtime / container JRE → Temurin **25** (prefer `eclipse-temurin:25-jre-alpine` or jammy equivalent) |
| **Companion pins** | Align Boot 4 / Spring Framework 7–compatible dependency versions (starters, test stack, ShedLock **7.7.0**, optional `*-spring-boot3` artifacts if renamed, etc.) as required for green verify |
| **Config hygiene** | Resolve Boot 4 property renames / deprecated keys that break start or tests |
| **Docs** | Amend [ADR-0028](../adr/technology-stack/0028-backend-platform-stack-baseline.md) to **Java 25 + Boot 4.x** (pin **4.1.0**); sync stack / tech-log / ops image docs; document Jackson 2 bridge + ShedLock appendix history |
| **Ops acceptance** | `mvn verify` GREEN; app starts; `GET /healthz` OK |

### Transitional seam — `spring-boot-jackson2` (Jackson 3 deferred)

Boot **4** defaults to Jackson **3**. This slice keeps runtime on Jackson **2**:

| Field | Value |
| --- | --- |
| **Confirmed for #51** | Depend on `org.springframework.boot:spring-boot-jackson2`; application code remains on `com.fasterxml.jackson.*` |
| **Deferred** | Migration to Boot 4 default Jackson **3** |
| **Rationale** | Parent bump + companion co-upgrades already carry Framework 7 risk; Jackson 3 package/API move is a separate migration |
| **Optional follow-up** | Task Master note only (e.g. future hygiene task): remove `spring-boot-jackson2`, adopt Jackson 3, green gates — **do not** invent a formal phase |
| **ADR / ledger** | [ADR-0028 amendment](../adr/technology-stack/0028-backend-platform-stack-baseline.md); seam row in [execution-sync-ledger.md](../plan/execution-sync-ledger.md) |

### Migration risks (implementer awareness — not product BDD)

| Risk | Notes |
| --- | --- |
| **Spring Framework 7 / Boot 4 breaking changes** | API removals, package moves, servlet/Jakarta deltas, security config style changes |
| **Java 25 toolchain** | Compiler `release 25`, bytecode, Checkstyle/PMD/SpotBugs/JaCoCo, Temurin **25** runtime images |
| **Dependency co-upgrades** | BOM-managed libs + third-party starters (e.g. Resilience4j, springdoc, MapStruct tooling, ShedLock **6.x → 7.7.0**) may need version bumps or artifact-id changes |
| **Jackson default** | Boot 4 Jackson 3 deferred via `spring-boot-jackson2` — see transitional seam above; do not treat Jackson 3 as in-scope for #51 |
| **Property renames** | Application / test / Compose-injected Spring properties may fail start if obsolete keys remain |
| **Test failures** | `@SpringBootTest`, MockMvc/Security test helpers, Testcontainers alignment, Checkstyle/PMD/SpotBugs/JaCoCo on new bytecode |

### Rollback

1. Revert Maven parent (and companion pins) to **Spring Boot 3.3.13** (or the last known-good 3.3.x pin on the integration line) and restore Java **21** / Temurin **21** if rolling back the whole #51 slice.
2. Redeploy the prior acceptance/runtime image via the single-host deploy queue (`docker-deploy-queue.ps1`).
3. Do **not** leave a mixed Boot 3/4 parent + unmanaged overrides, or a mixed Java 21/25 compiler vs runtime pin.

---

## Acceptance bullets (ops — not product G/W/T)

These are **delivery / ops acceptance** criteria for the platform slice — **not** product BDD scenarios for TDD Red of new user journeys.

1. **Given** the feature worktree `DGE-boot-4-1-upgrade` on `feat/boot-4-1-upgrade`  
   **When** the implementer sets Spring Boot parent to **4.1.0**, Java toolchain to **`release 25`**, and applies required co-upgrades  
   **Then** `mvn -B -ntp -f backend/pom.xml verify` is **GREEN**.

2. **Given** a built backend artifact on **Java 25 + Boot 4.1.0**  
   **When** the application starts against the accepted dependency stack  
   **Then** the process stays up and `GET /healthz` returns success (200 / platform health contract).

3. **Given** ADR-0028 and stack documentation  
   **When** the docs-first stage completes (and the pom/image bumps land in the same change set as the upgrade)  
   **Then** ADR-0028 (and mirrored stack docs) record **Java 25 + Spring Boot 4.x** with pin **4.1.0**, without inventing unrelated stack switches.

4. **Given** a failed upgrade or red gates  
   **When** operators roll back  
   **Then** parent returns to **3.3.13** (companion pins restored; Java/runtime restored to prior Temurin **21** line if applicable) and the prior image is redeployed — no half-migrated BOM left on `main`.

---

## Explicit non-goals

- No intentional end-user product UI/API/permission/audit behavior change.
- No inventing formal phase / sole-active program; formal phase remains **None**.
- No production go-live claim; launch checklist overall remains **NO-GO** unless separately closed.
- No activating **CD-3**.
- Do **not** touch worktree `DGE-audit-governance`.
- Do **not** replace PostgreSQL, Redis, MinIO, or Vue baselines in this slice.
- Do **not** migrate to Jackson **3** in this slice (bridge stays; optional follow-up Task only).
- Do **not** implement pom/Docker/code in the docs-first stage — toolchain pins remain **backend-engineer**.

---

## Traceability

| Artifact | Role |
| --- | --- |
| [ADR-0028](../adr/technology-stack/0028-backend-platform-stack-baseline.md) | Accepted backend platform baseline — **amended 2026-07-13** to Boot **4.x** / pin **4.1.0**; second amendment **Java 25**; Jackson 2 via `spring-boot-jackson2` documented as transitional |
| [ADR-0044](../adr/operations/0044-deployment-topology-v1.md) appendix | LR-B2 ShedLock **6.10.0** record preserved; historical note for #51 co-upgrade to **7.7.0** |
| [technology-stack-decisions.md](../architecture/technology-stack-decisions.md) | Architecture stack log mirroring ADR-0028 (runtime row updated 2026-07-13) |
| User confirmation (2026-07-13) | Upgrade target **4.1.0** (from “4.10”); Java **25** (user said “25”) |
| Task Master **#51** | Owning task for this slice (`done` — merge `993c287`) |
| Task Master **#49** | Prior deps/security refresh — left Boot on **3.3.13** / Java **21**; does **not** close Boot 4 / Java 25 migration |
| `backend/pom.xml` + Dockerfiles | Implementation surface for parent + Java **25** toolchain + Temurin **25** images (backend-engineer) |

```
bdd_readiness: not-applicable
task_ids: [51]
owning_doc: docs/behavior/boot-4-1-upgrade.md
```
