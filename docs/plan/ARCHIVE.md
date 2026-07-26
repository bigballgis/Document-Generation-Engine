---
id: DOC-PLAN-ARCHIVE
type: Plan Index
status: Active
sourceOfTruth: false
owners:
  - documentation-governance
  - plan-orchestrator
dependsOn:
  - docs/plan/README.md
  - docs/behavior/ai-scale-docs-conventions.md
related:
  - docs/architecture/ai-development-guide.md
  - AGENTS.md
---

# Plan Archive — Closed Programs (Progressive Disclosure)

## Purpose

Thin index of **closed / historical** plan programs under `docs/plan/`. Parent agents
must **not** load entire closed-program bodies by default when starting unrelated work.

Behavior: [ai-scale-docs-conventions.md](../behavior/ai-scale-docs-conventions.md)
(ADC-04…ADC-06).

## First read (live)

1. [docs/README.md](../README.md) delivery focus line
2. Active Task Master sole-active / queue head (`.taskmaster/tasks/tasks.json`)
3. [module-map.md](../architecture/module-map.md) for package placement
4. Owning behavior + detail for the **current** leaf only
5. [execution-sync-ledger.md](./execution-sync-ledger.md) — **header / sole-active notes first**

Open a closed program below **only** when the task names that program, residual, or audit.

## Closed / historical programs (linked, not first-read dumps)

Status vocabulary: `Done` / archived. Rows stay reachable; bodies stay in place
(physical moves are **out of scope** unless a later task explicitly scopes them).

| Program | Entry | Status (archive label) |
| --- | --- | --- |
| CORE-FORTRESS (F1–F8) | [detail/CORE-FORTRESS-program-roadmap.md](./detail/CORE-FORTRESS-program-roadmap.md) | **Done** / archived |
| CODE-QUALITY | [code-quality-program.md](./code-quality-program.md) | **Done** / archived |
| Launch readiness (LRP A–E) | [launch-readiness-program.md](./launch-readiness-program.md) | Planned waves **Done** / archived history |
| Contract drift (CDP) | see plan index / CDP docs | Planned waves **Done** / archived history |
| Post-queue hardening (PQH) | [post-queue-hardening-program-2026-07.md](./post-queue-hardening-program-2026-07.md) | **Done** / archived |
| System normalization (SYS-NORM) | [system-normalization-program-2026-07.md](./system-normalization-program-2026-07.md) | Waves 0–8 **Done** / archived |
| Formal phases P0–P23 | [master-plan.md](./master-plan.md) + `detail/P*.md` | Historical phase record — load only when named |

## Live / not archive by default

| Program | Entry | Note |
| --- | --- | --- |
| Frontline Operability & Solidity (FOS) | [frontline-operability-solidity-program-2026-07.md](./frontline-operability-solidity-program-2026-07.md) | **Not Started** (plan on main) — live queue, not archived |
| AI-Scale Remediation | [ai-scale-remediation-program-2026-07.md](./ai-scale-remediation-program-2026-07.md) | Leaf 1 **In Progress** — live |
| CORE-EXCELLENCE | CE plan docs under `docs/plan/` | Active delivery program (not a formal P-phase) |
| Intl bank letter readiness (IBL) | [intl-bank-letter-readiness-program.md](./intl-bank-letter-readiness-program.md) | Residual / blocked items may remain — open only when task concerns IBL |

## Confirmed vs pending

| Confirmed | Pending |
| --- | --- |
| Progressive disclosure: closed programs are historical; thin index first | Physical relocation of closed docs into an `archive/` folder |
| Ledger header-first read | Auto-CI that fails solely because an agent loaded a closed program |

## Maintenance

When a program moves to **Done** with no remaining sole-active leaf, add or refresh its
row here as **Done / archived** in the same doc-sync change when practical.
