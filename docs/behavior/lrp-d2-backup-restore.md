# LR-D2 — Backup/restore runbook + drill (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `lrp-d2-backup-restore` |
| **Plan** | [LRP-D §LR-D2](../plan/detail/LRP-D-ops-observability.md#lr-d2--backuprestore-runbook--drill) |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-12 |
| **Formal phase** | None (Wave LR-D In Progress) |
| **Task Master** | **#39** (in-progress) · plan id **LR-D2** |

---

## Why BDD is not-applicable

LR-D2 delivers **operational documentation + a timed rehearsal**, not a product behavior change:

- No new user-facing journey, management UI surface, API contract, permission rule, or audit semantics.
- No change to generation, authoring, or runtime **response contracts** — the drill exercises existing Postgres + MinIO + app health against a **scratch / local Docker** stack.
- Outcomes are a **runbook** plus **dated drill evidence** (durations vs ADR-0030 RPO/RTO targets), not new product acceptance thresholds or confirmed SLOs.
- Preferred deliverables are **docs + scripts**; inventing cloud services outside the stack, or claiming RPO/RTO compliance without a timed drill, is out of scope.

Plan authority: [LRP-D-ops-observability.md](../plan/detail/LRP-D-ops-observability.md) §LR-D2 — **BDD: not-applicable — operational documentation + rehearsal evidence.**

Program row: [launch-readiness-program.md](../plan/launch-readiness-program.md) § wave map — LR-D2 BDD column **`not-applicable`**.

---

## What is in scope (ops only)

| Deliverable | Intent |
| --- | --- |
| **Runbook** | `docs/operations/backup-restore-runbook.md` — pg dump/restore (+ prod WAL/snapshot guidance), MinIO bucket strategy, Redis as rebuildable cache, secrets handling, Flyway forward-only compensating migration + blue-green app rollback cross-link |
| **Drill procedure** | Restore into a scratch stack; verify `/healthz` + one document round-trip (retrievable prior artifact or regenerated equivalent) |
| **Drill evidence** | One executed local Docker drill with date, duration, verifier; compare measured times to ADR-0030 RPO ≤15 min / RTO ≤30 min (record reality — scratch ≠ production compliance) |

**Environment constraints (from plan):**

- Local Docker / scratch stack only — never shared/production destructive restore without an explicit confirmation gate.
- Do **not** invent cloud services outside the documented stack.
- Feeds **LR-E2** launch checklist (backup item → dated drill record) — checklist itself is out of this slice.

---

## Acceptance scenarios (plan §LR-D2 G/W/T)

These are **ops rehearsal acceptance** criteria for runbook + drill evidence — **not** product BDD Given/When/Then for TDD Red of new user-facing behavior. No product actor journeys are invented here.

### Scenario A — Scratch-stack restore serves health + document

- **Given** the backup/restore runbook
- **When** an operator follows it on a scratch stack
- **Then** the restored stack serves `/healthz` 200 and a previously generated document (or regenerated equivalent) is retrievable

### Scenario B — Launch-checklist evidence pointer

- **Given** the drill evidence record
- **When** LR-E2 builds the launch checklist
- **Then** the backup item resolves to a dated drill record with measured durations vs ADR-0030 targets

---

## Explicit non-goals

- No product requirement inventing backup/restore as a **management-UI** or **runtime API** feature.
- No claiming ADR-0030 RPO/RTO **compliance** without a timed drill evidence section.
- No destructive restore scripts without an explicit confirmation gate.
- No activating LR-D3 / LR-D4 / LR-E / CD-3 from this readiness record.
- No marking LR-D2 **Done** in this readiness record alone — Done requires runbook merged + drill executed + evidence + doc sync + commit review (plan §LR-D2).

---

## Traceability

| Artifact | Role |
| --- | --- |
| [LRP-D §LR-D2](../plan/detail/LRP-D-ops-observability.md) | Authoritative task row + G/W/T |
| [ADR-0030](../adr/operations/0030-operational-platform-baseline.md) | RPO ≤15 min / RTO ≤30 min **targets** |
| [backup-restore-runbook.md](../operations/backup-restore-runbook.md) | Canonical runbook + **EXECUTED** 2026-07-12 drill evidence (scratch scope; not production compliance) |
| `docs/operations/runbook.md`; `deploy/README.md`; `deploy/blue-green-runbook.md` | Ops index + blue-green / deploy cross-links |
| LR-E2 launch checklist | Downstream consumer of dated drill evidence |

```
bdd_readiness: not-applicable
task_ids: [LR-D2 / lrp-d2-backup-restore, Task Master #39]
runbook_status: drill-executed-2026-07-12 (scratch scope; not production compliance; Done deferred to post-merge doc-sync)
```
