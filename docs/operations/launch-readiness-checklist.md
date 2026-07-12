# Launch readiness checklist (LR-E2)

**Program:** [Launch Readiness & Deep-Optimization Program (LRP)](../plan/launch-readiness-program.md) §7  
**Slice:** `lrp-e2-launch-checklist` · Task Master **#43**  
**Checklist status:** **Done** (2026-07-12 — merge `ae39fbb`; Task Master #43) — Wave LR-E docs exit gate closed; overall verdict remains **NO-GO**  
**Formal phase:** **None**  
**Purpose:** Ops go / no-go / conditional verdict template with **evidence-linked** rows.  

> **This is not a production go-live claim.** Completing or signing this checklist does **not** authorize production launch by itself. Incomplete evidence for any row → mark that row **NO-GO** (or **UNKNOWN** if the docs do not prove the fact either way). Wave LR-E **Done** means the documentation release-readiness gate (checklist + E1 proof) is closed — **not** production authorization. Overall snapshot remains **NO-GO**.

**Companion draft (historical checkboxes):** [launch-readiness-gate.md](../plan/launch-readiness-gate.md) — prefer **this** file for verdict + evidence links.

---

## How to use

| Verdict | Meaning |
| --- | --- |
| **GO** | Durable docs prove the prerequisite with a live evidence link (merge, ADR Accepted, dated drill, E2E manifest, etc.). |
| **NO-GO** | Evidence missing, incomplete, or docs/code still show the blocking gap. |
| **CONDITIONAL** | Prerequisite met for a documented scope, with an explicit residual / deferred risk that must be accepted or closed before a real production decision. |
| **UNKNOWN** | Docs conflict or do not record enough proof; treat as **NO-GO** for any launch decision until resolved. |

**Rule:** If evidence is incomplete → **NO-GO** for that item. Do not invent green checkmarks.

---

## Checklist (evidence as of 2026-07-12)

| # | Item | Verdict | Evidence (what docs prove today) | Notes / residual |
| --- | --- | --- | --- | --- |
| 1 | **P22** — rendering engine + demo scaffolds Done (ops/runbook foundations as applicable) | **GO** | [master-plan.md](../plan/master-plan.md) P22 **Done** (2026-07-04; T01–T15); [P22 detail](../plan/detail/P22-demo-expansion-rendering-fidelity.md); ops foundations via [runbook.md](./runbook.md) + [ADR-0030](../adr/operations/0030-operational-platform-baseline.md) | P22 is engine/scaffolds; typography excellence tracked under **P23 Done**. Not a production compliance claim. |
| 2 | **CD-2** — CDP browser E2E wave Done (T01–T13) | **GO** | [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md) Wave CD-2 **Done** (merge tip `b2b0899`); [master-plan.md](../plan/master-plan.md); LRP § header | Ledger CDP wave **table** still shows stale “In Progress / T13 Not Started” — documentation drift only; prefer CDP program + master-plan + LRP ledger § LRP sibling note. |
| 3 | **LR-A critical** — A1 / A2 / A3 Done | **GO** | [LRP §3](../plan/launch-readiness-program.md); A1 Done (F4); A2 Done (P23/fonts); A3 merge `e62c210` | Wave LR-A exit also closed A4–A7; **ADR-0042 / ADR-0043 remain Proposed**; Word-vs-LO / XSD / LO24 **deferred** → see row 3b. |
| 3b | **LR-A residuals** — ADR-0042 / 0043 Accepted + Word pagination delta | **NO-GO** | [LRP §3 LR-A5](../plan/launch-readiness-program.md); [launch-readiness-gate.md](../plan/launch-readiness-gate.md) — 0042/0043 **Proposed**; A7 Word pages **n/a** (`ms-word-unavailable-on-host`) | Explicit deferred residual of Wave LR-A exit. Blocks any claim of full rendering-trust closure. |
| 4 | **LR-B critical** — B1 / B3 / B5 / B8 Done | **GO** | [LRP §4](../plan/launch-readiness-program.md); ADR-0044 Accepted (B1); B3 + [LR-E1 manifest](../../frontend/e2e/evidence/LRP-E1-sse-manifest.md); B5 drain smoke in ledger batch 2; B8 prod `/healthz` healthcheck + mem/cpu limits | Ledger batch-2 evidence archived; B6 session renewal Done separately (not in this critical quartet). |
| 5 | **Ledger seams / ADRs** — closed **or** ADR-accepted for v1 | **CONDITIONAL** | [execution-sync-ledger.md](../plan/execution-sync-ledger.md) seams table | **Closed / accepted:** «Security forbidden-route audit» (**LR-D7** `c94a356`); «Async batch transport» (**ADR-0044** branch (b) accepted-for-v1); ShedLock path for schedulers (**LR-B2** + ADR-0044). **Still open (NO-GO if production requires them):** «AD Group resolution» (`ConfigAdGroupResolver` stub); «Paste cleaning ↔ binding validation»; process-local rate limit / Redisson deferred until multi-instance (ADR-documented, not closed). |
| 5a | Seam — AD Group resolution (LDAP/AD adapter) | **NO-GO** | Ledger seams: config-file stub, fail-closed | Production AD/LDAP adapter + tests not evidenced. |
| 5b | Seam — Paste cleaning ↔ binding validation | **NO-GO** | Ledger seams; CD-HARD-T05 adjacency | Not wired to publish gate / no ADR documenting edit-time-only as accepted-for-v1. |
| 6 | **LR-D2** — backup/restore drill evidence | **CONDITIONAL** | [backup-restore-runbook.md § Drill evidence 2026-07-12](./backup-restore-runbook.md#drill-evidence-2026-07-12--executed); merge `3d78bc5`; RPO≈0.933 min / RTO≈4.751 min | **GO for scratch-stack rehearsal.** **NO-GO as production ADR-0030 RPO/RTO compliance** (no WAL/PITR; runbook forbids overclaim). |
| 7 | **LR-B8** — prod compose healthchecks + resource limits | **GO** | [LRP §4 LR-B8](../plan/launch-readiness-program.md); `docker-compose.prod.yml` healthcheck on `/healthz` (start_period 90s) + mem/cpu limits; ledger HostConfig evidence | Confirms **compose** readiness probe wiring — not a live signed production cluster attestation. |
| 8 | **LR-E1** — SSE-through-proxy incremental E2E green | **GO** | [LRP-E1-sse-manifest.md](../../frontend/e2e/evidence/LRP-E1-sse-manifest.md); merge `575d0aa`; Playwright **2/2**; closes CD-PIT-12 browser proof | Scenario A incremental; Scenario B idle ≥60s + ~20s keep-alive. |
| 9 | **`JWT_SECRET`** — explicitly provisioned; **no** compose default fallback | **GO** | Slice `ops-jwt-secret-no-default` (Task Master **#44**); merge `587cd9a` (`587cd9a6258d375ab43280339d58edf6c3430319`); feature tip `283233e` (`283233efc18ea080335c89c42bac3507d62aacb0`). Compose: `docker-compose.prod.yml` uses `${JWT_SECRET:?JWT_SECRET must be set}` (no `:-` default). Guard: `ProductionSecretGuard` + `ProductionSecretGuardTest`. BDD: [BDD-OPS-JWT-SECRET-001](../behavior/ops-jwt-secret-no-default.md). Gates: `mvn verify` **GREEN**; architecture **PASS_WITH_NOTES** (Critical **0**); **DEPLOY_OK** (queued deploy; healthz **200** UP); S2a compose config **fails** without `JWT_SECRET`. Closes LR-B6 🟡#4. | Clearing #9 alone is **not** go-live. Overall checklist remains **NO-GO** (other blockers). |
| 10 | **Kafka image** — company-approved registry (not Docker Hub `bitnamilegacy`) | **NO-GO** | `docker-compose.yml` `image: bitnamilegacy/kafka:3.7` + comment mandating company registry for production; LR-B4 note in [LRP §4](../plan/launch-readiness-program.md) | Dev/local pull path fixed; **production coordinates not evidenced** in-repo. |

### Related inputs (informational — not sole launch blockers)

| Item | Verdict | Evidence |
| --- | --- | --- |
| Wave LR-C usability (C1–C13) | **GO** (wave Done) | [LRP §5](../plan/launch-readiness-program.md); merge tip `bf9cbeb` |
| Wave LR-D ops (D1–D7) | **GO** (wave Done) | [LRP-D detail](../plan/detail/LRP-D-ops-observability.md); merge tip `218dcf1` |
| LR-D5 NFR proposals | **CONDITIONAL** | Authored as **pending** — **not** confirmed SLOs ([non-functional-requirements.md](../requirements/non-functional-requirements.md) §待确认) |
| LR-D6 load smoke | **CONDITIONAL** | Baseline measured; Scenario A `errorRate=0.4` triaged [DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) — not a capacity GO |

---

## Overall verdict template

Fill this block for each readiness review. **Do not** change item verdicts without updating evidence links.

| Field | Value |
| --- | --- |
| Review date (UTC+8) | _YYYY-MM-DD_ |
| Reviewer / role | _name · architecture / ops / eng_ |
| Git tip reviewed | _SHA_ |
| **Overall verdict** | **NO-GO** _(snapshot 2026-07-12 — see blockers below)_ / GO / CONDITIONAL |
| Blocking **NO-GO** items | #3b ADR-0042/0043+Word; #5a AD Group stub; #5b paste↔binding; #10 Kafka company registry (#9 `JWT_SECRET` cleared **GO** — merge `587cd9a`) |
| Accepted **CONDITIONAL** residuals (if any) | _list item # + accepted residual text — or “none”_ |
| Open risks | _e.g. ledger CDP table drift; NFR unconfirmed; D6 errorRate_ |
| Sign-off | _signature / “not signed — docs-only snapshot”_ |

### Snapshot overall verdict (authored with this checklist — 2026-07-12)

| Field | Value |
| --- | --- |
| Review date | 2026-07-12 |
| Reviewer | doc-keeper (LR-E2 stage 3 — docs-only) |
| Git tip | worktree `feat/lrp-e2-launch-checklist` (pre-merge) |
| **Overall verdict** | **NO-GO** |
| Blocking items | **#10** Kafka not on company registry; **#3b** ADR-0042/0043 + Word delta; **#5a** AD Group stub; **#5b** paste↔binding seam (**#9** JWT_SECRET → **GO** 2026-07-12 merge `587cd9a` — not a go-live claim) |
| Conditionals (not flipped to GO) | **#5** mixed seams; **#6** scratch drill ≠ production compliance; NFR/D6 residuals |
| Production go-live claim | **None — forbidden by this checklist** |
| Sign-off | Docs snapshot only — **not** a launch authorization |

---

## Cross-links

| Doc | Role |
| --- | --- |
| [launch-readiness-program.md §7 LR-E2](../plan/launch-readiness-program.md) | Owns task status (In Progress until stage 12) |
| [launch-readiness-gate.md](../plan/launch-readiness-gate.md) | Prerequisite wave summary + historical checkboxes |
| [execution-sync-ledger.md](../plan/execution-sync-ledger.md) | Seams + LRP/CDP evidence |
| [runbook.md](./runbook.md) | Day-2 ops |
| [backup-restore-runbook.md](./backup-restore-runbook.md) | LR-D2 drill SoT |
| **[BDD-OPS-JWT-SECRET-001](../behavior/ops-jwt-secret-no-default.md)** | Checklist **#9** acceptance behavior (slice `ops-jwt-secret-no-default`) |
| [docs/README.md](../README.md) | Index |

---

## Change log

| Date | Change |
| --- | --- |
| 2026-07-12 | Initial checklist authored (LR-E2 / Task Master #43). Honest **NO-GO** overall. |
| 2026-07-12 | LR-E2 / Wave LR-E docs exit gate **Done** (merge `ae39fbb`). Overall verdict remains **NO-GO** — **not** production go-live. |
| 2026-07-12 | Linked [BDD-OPS-JWT-SECRET-001](../behavior/ops-jwt-secret-no-default.md) for item **#9** (slice `ops-jwt-secret-no-default`). Row remains **NO-GO** until implement + evidence. |
| 2026-07-12 | Docs-first operator/deploy alignment for #9 (runbook, `deploy/README.md`, `k8s-config-secrets.md`, `.env.example`, container-hardening) — **still NO-GO**; no implement evidence yet. |
| 2026-07-12 | Item **#9** → **GO** (slice `ops-jwt-secret-no-default`; merge `587cd9a`; Task Master #44). Compose `:?` + `ProductionSecretGuard`; `mvn verify` GREEN; DEPLOY_OK + healthz; S2a refuse missing secret; architecture PASS_WITH_NOTES. Overall checklist remains **NO-GO** — **not** production go-live. |
