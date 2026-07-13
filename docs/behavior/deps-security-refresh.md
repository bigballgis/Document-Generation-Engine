# deps-security-refresh — Third-party dependency security hygiene (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `deps-security-refresh` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-13 |
| **Formal phase** | **None** (no sole-active; do **not** activate CD-3) |
| **Task Master** | **#49** (`done`) |
| **Placement** | Merged to `main` — was ISOLATED `D:/working/DGE-deps-security-refresh` · `feat/deps-security-refresh` (removed stage 11) |
| **Evidence** | [docs/evidence/deps-security-refresh](../evidence/deps-security-refresh/README.md); [deps-security-refresh-frontend-audit.md](../operations/deps-security-refresh-frontend-audit.md); merge `08c7d56` / tip `cb28237` |

---

## Why BDD is not-applicable

This slice delivers **ops/security hygiene on third-party dependencies** (Maven + pnpm), not a product behavior change:

- No actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- No change to generation, authoring, publish, runtime response contracts, or OpenAPI envelopes.
- Outcomes are **vulnerability audit + baseline-safe version bumps + gate-green proof** (and documented exceptions for Critical/High that cannot be remediated within ADR baselines) — not new user-facing acceptance thresholds.
- Patch/minor upgrades within accepted stack ADRs are **supply-chain hygiene**, verified by existing quality gates — not a new Given/When/Then product contract.

Analogous readiness: [slim-knip-scan](./slim-knip-scan.md) / [LR-D6 load smoke](./lrp-d6-load-smoke.md) — engineering/ops slices with `bdd_readiness: not-applicable`.

---

## What is in scope

| Deliverable | Intent |
| --- | --- |
| **Backend (Maven)** | Deep dependency / CVE-oriented audit; upgrade within ADR baselines (Spring Boot **stay on 3.3.x**, currently `3.3.13`; ShedLock **stay on 6.x**, currently `6.10.0`) |
| **Frontend (pnpm)** | Audit + baseline-safe upgrades within ADR-0022 / ADR-0029 / tech-stack guardrails (no major Vue / Vite line jump without confirmation) |
| **Remediation** | Critical/High remediated **or** explicit exception documented (risk, reason, residual, owner, expiry — M9-T03 metadata pattern) |
| **Gates** | Backend `mvn verify` + frontend `lint` / `type-check` / `test` / `build` green after upgrades |
| **Governance** | No major framework jumps; no ADR baseline rewrite without user confirmation |

---

## Confirmed constraints (session)

| Constraint | Status |
| --- | --- |
| Spring Boot remains **3.3.x** (no Boot 3.4+/3.5 major jump) | Confirmed |
| ShedLock remains **6.x** | Confirmed |
| No major **Vue / Vite** line jump without user confirmation + ADR update | Confirmed |
| Formal phase remains **None**; do **not** activate CD-3 | Confirmed |
| Security hygiene only — **not** production go-live | Confirmed |
| Stay within accepted tech-stack ADRs / company-approved repos | Confirmed ([ADR-0028](../adr/technology-stack/0028-backend-platform-stack-baseline.md), [ADR-0029](../adr/technology-stack/0029-frontend-application-stack-baseline.md), tech-stack guardrails) |
| Critical/High in changed dependency scope blocked unless remediated or exception-recorded | Confirmed ([quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md)) |
| This slice does **not** close M9-T02 org intranet SCA upload | Confirmed |
| This slice does **not** reopen SOR-C05 (Done) | Confirmed |

### Pending residuals (post-#49)

| Item | Notes |
| --- | --- |
| Vitest GHSA-5xrq-8626-4rwp Critical | Documented exception — cleanup Task Master **#50**; expires **2026-10-13** |
| Org SCA upload ticket / scan ID | Remains M9-T02 org-gate pending — out of scope for #49 Done |
| Any major-line stack shift (Boot 3.4+, ShedLock 7.x, Vue/Vite/Vitest major) | **Deferred** — requires user confirmation + ADR; do not invent |

---

## Acceptance bullets (ops hygiene — not product G/W/T)

These are **delivery acceptance** criteria for the engineering slice — not product BDD scenarios for TDD Red of new user journeys.

1. **Given** the feature worktree `DGE-deps-security-refresh`  
   **When** Maven and pnpm dependency trees are audited for known Critical/High issues  
   **Then** findings are listed with severity and current coordinates; Critical/High are remediated via baseline-safe bumps **or** an exception is recorded with rationale.

2. **Given** proposed upgrades within ADR baselines (Boot 3.3.x, ShedLock 6.x, no unconfirmed Vue/Vite major, no unconfirmed stack switch)  
   **When** versions are updated in `backend/pom.xml` / `frontend/package.json` (and lockfiles)  
   **Then** no intentional product UX/API/permission behavior change is introduced.

3. **Given** quality gates after upgrades  
   **When** implementer finishes the slice  
   **Then** `mvn -B -ntp -f backend/pom.xml verify` and `pnpm -C frontend lint` · `type-check` · `test` · `build` are green.

4. **Given** any Critical/High finding left open  
   **When** the slice is closed  
   **Then** an exception note exists (finding id/CVE, residual risk, why upgrade is blocked by baseline/compat, owner, expiry) — do not silently ignore.

---

## Explicit non-goals

- No product UI/API/permission/audit behavior change.
- No Spring Boot major line jump beyond **3.3.x**.
- No ShedLock major jump beyond **6.x**.
- No major Vue / Vite line jump without confirmation + ADR.
- No inventing company registry / LDAP / Kafka coordinates.
- No production go-live claim; launch checklist overall remains **NO-GO** (#3b Word residual).
- No activating **CD-3**.
- Do **not** touch worktree `DGE-audit-governance`.
- Formal phase remains **None**.
- Do **not** rewrite Accepted ADR decisions to “record progress”; only confirm upgrades stay inside baselines (ADR change only if user confirms a stack shift).
- Do **not** mark M9-T02 / M9-T03 Done solely because #49 lands remediation or exception notes.

---

## Deferred ADR notes (for implementers)

Do **not** edit Accepted ADR decision text to reflect hygiene progress. If a finding appears to require a major-line move, **stop and confirm** — prepare an ADR draft only after user confirmation:

| Potential shift | Current confirmed ceiling | Owning ADR / note | Action if blocked |
| --- | --- | --- | --- |
| Spring Boot **3.4+ / 3.5** | Stay on **3.3.x** (`3.3.13`) | [ADR-0028](../adr/technology-stack/0028-backend-platform-stack-baseline.md) (records Boot **3.x**; session narrows to 3.3.x) | Exception or wait for confirmed ADR update |
| ShedLock **7.x** | Stay on **6.x** (`6.10.0`) | [ADR-0044](../adr/operations/0044-deployment-topology-v1.md) appendix (6.x verified with Boot 3.3) | Exception or wait for confirmed ADR update |
| Vue / Vite **major** line | Patch/minor within Vue 3 + Vite baseline | [ADR-0022](../adr/technology-stack/0022-basic-technology-stack-baseline.md), [ADR-0029](../adr/technology-stack/0029-frontend-application-stack-baseline.md) | Exception or wait for confirmed ADR update |

---

## Traceability

| Artifact | Role |
| --- | --- |
| [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) | Critical/High block + #49 upgrade policy |
| [m9-task-sheet.md](../architecture/m9-task-sheet.md) | M9-T01 Done; M9-T02 In Progress; M9-T03 Not Started |
| [m9-t02-closure-plan.md](../architecture/m9-t02-closure-plan.md) | Step 3 related remediation via #49 |
| [docs/evidence/security/README.md](../evidence/security/README.md) | SBOM / SCA evidence index |
| [system-optimization-review-2026-07.md](../plan/system-optimization-review-2026-07.md) | SOR-C05 Done — feeds M9-T02; #49 related only |
| [ADR-0028](../adr/technology-stack/0028-backend-platform-stack-baseline.md) | Backend stack baseline at #49: Java 21 + Spring Boot **3.x**; **amended 2026-07-13** to Java **25** + Boot **4.x** / **4.1.0** (Task **#51**) |
| [ADR-0029](../adr/technology-stack/0029-frontend-application-stack-baseline.md) | Frontend stack baseline (Vue 3 / Vite / pnpm) |
| `.cursor/rules/tech-stack-guardrails.mdc` | No ad-hoc framework/runtime switches |
| Task Master **#49** | **Done** (2026-07-13) — in-repo hygiene; residual exception → **#50** |
| Launch checklist | Unchanged overall **NO-GO** — this slice is not a go-live closer |

```
bdd_readiness: not-applicable
task_ids: [49]
owning_doc: docs/behavior/deps-security-refresh.md
```
