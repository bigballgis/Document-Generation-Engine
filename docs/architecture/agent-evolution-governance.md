---
id: DOC-ARCH-AGENT-EVOLUTION-GOVERNANCE
type: Architecture View
status: Proposed
sourceOfTruth: true
owners:
  - architecture
  - documentation-governance
dependsOn:
  - docs/document-as-software.md
  - docs/governance.md
  - docs/architecture/ai-development-guide.md
related:
  - .github/agents/fullstack-orchestrator.agent.md
  - .github/agents/agent-evolution.agent.md
  - .github/agents/task-planning.agent.md
  - .github/agents/e2e-test-execution.agent.md
  - .github/agents/e2e-uiux-test.agent.md
  - .github/agents/deploy-execution.agent.md
  - .github/agents/git-preflight-review.agent.md
  - .github/agents/git-commit-executor.agent.md
  - .github/agents/git-push-executor.agent.md
  - scripts/agent-evolution-reminder.ps1
---

# Agent Evolution Governance

## Purpose

Define how the agent system evolves safely as model capabilities change, failure modes shift, and workflow friction appears.

The objective is to allow continuous micro-tuning without breaking governance, quality gates, or traceability.

## Scope

This document governs changes to:

- agent role definitions under `.github/agents/`
- agent orchestration rules in `fullstack-orchestrator`
- specialist test-routing boundaries, including Playwright E2E execution, E2E UIUX evidence, frontend unit tests, and static scan agents
- git workflow agent roles for preflight review, commit, and push checkpoints
- governance and quality hook scripts under `scripts/`

It does not replace product, domain, permission, or API source-of-truth ownership.

## Evolution Triggers

Start an evolution cycle when one or more signals appear:

1. Model upgrade or runtime behavior change impacts execution quality.
2. Repeated non-compliant outputs from the orchestrator or specialized agents.
3. Recurrent false positive or false negative hook decisions.
4. Repeated routing mistakes between planning, execution, test, E2E UIUX, deployment, and scan agents.
5. Stable productivity regression across similar task types.

## Evolution Threshold Baseline

Use the following default thresholds to trigger a mandatory evolution cycle:

1. Non-compliant output threshold
   - Trigger if there are 3 or more non-compliant outputs within the last 10 comparable tasks.
2. Hook precision threshold
   - Trigger if false positive or false negative decisions reach 15% or higher across the last 20 reviewed hook events.
3. Routing threshold
   - Trigger if there are 2 or more confirmed misrouting incidents within the last 10 delegated tasks.
4. Model upgrade threshold
   - Trigger automatically after model upgrade and complete one focused stabilization review within 48 hours.

If evidence quality is low, mark the threshold assessment as pending and collect one additional comparable sample before patching.

## Controlled Change Protocol

1. Incident capture
   - Capture the smallest reproducible evidence with task context and expected behavior.
2. Classification
   - Label failure mode as routing, governance gate, quality gate, output format, or execution drift.
3. Minimal patch design
   - Prefer the smallest prompt or hook change that addresses the classified failure.
4. Validation plan
   - Define the narrowest checks that prove improvement and no obvious regressions.
5. Rollback plan
   - Define explicit rollback trigger and previous-state restoration steps.
6. Documentation update
   - Record accepted change and rationale in this file before treating it as durable.

## Safety Rules

- Never relax governance or quality gates without explicit user confirmation.
- Never bundle unrelated tuning changes in one evolution patch.
- Always preserve single-entry orchestration and task-id traceability.
- Keep confirmed facts and pending hypotheses separate.

## Evolution Log Template

Use this template for every accepted evolution adjustment:

- Change ID: `AE-YYYYMMDD-XX`
- Trigger: `<model-upgrade|repeated-failure|hook-regression|routing-regression|other>`
- Affected files: `<paths>`
- Confirmed evidence: `<short summary>`
- Patch summary: `<what changed>`
- Validation evidence: `<checks and outcomes>`
- Rollback condition: `<when to rollback>`
- Status: `<active|rolled-back|superseded>`

Canonical machine-readable record schema:

```json
{
   "changeId": "AE-YYYYMMDD-XX",
   "checkpointType": "monthly|threshold|model-upgrade",
   "trigger": "model-upgrade|repeated-failure|hook-regression|routing-regression|monthly-review",
   "action": "patched|no-change",
   "affectedFiles": ["path/to/file"],
   "validationStatus": "passed|failed|pending",
   "rollbackStatus": "not-needed|ready|executed",
   "rationale": "short rationale",
   "timestamp": "YYYY-MM-DD"
}
```

## Cadence Baseline

- Run one lightweight monthly evolution review on the first business day of each month, even if no trigger threshold fires.
- The monthly review should only confirm health status unless incident evidence justifies a patch.

## Monthly Review Record Requirement

Every monthly review must produce one evolution log record using the same `AE-YYYYMMDD-XX` format, including when no patch is applied.

Monthly review minimum fields:

- Change ID
- Checkpoint type: `monthly`
- Trigger evidence summary
- Decision: `patched` or `no-change`
- Validation status
- Rollback readiness

Monthly review records must include both:

- Human-readable summary using the Evolution Log Template.
- One machine-readable JSON record that conforms to the canonical schema.

## Current Confirmed Baseline

- Monthly evolution review cadence is enabled.
- Numeric trigger thresholds are enabled as the default evolution policy.
- Monthly review records are mandatory and use `AE-YYYYMMDD-XX` traceable IDs.
- E2E UIUX evidence has a dedicated specialist agent so visual, responsive, accessibility, theme, brand, screenshot, text-overflow, overlap, and interaction-polish checks are not conflated with generic Playwright execution or frontend unit tests.
- Frontend execution must be driven by an explicit UX contract and visual evidence handoff, not theme tokens alone; page/workflow tasks require target user/job, primary journey, interaction states, visual acceptance, responsive/accessibility scope, and UIUX evidence expectations before implementation claims are accepted.
- Task completion is governed at Task ID granularity: every completed Task ID with file changes requires scoped preflight, scoped commit, and push after green required gates unless the user explicitly opts out or a blocking condition is present.
- Enterprise OA frontend delivery uses task-first interaction baselines: URL-backed operational state, mature data tables, safe batch operations, workflow evidence, permission-aware forbidden states, recovery-first interactions, AI/automation review controls, and keyboard-first efficiency.
- Development execution must enter through `docs/architecture/orchestration-high-level-plan.md`; requests outside the active epic or confirmed planned directions require a high-level plan refresh before task-planning or implementation.
- Behaviour Driven Design/Development is a project-level constitution: behavior-changing plans, requirements, tests, and implementation must start from explicit behavior specifications and user confirmation when behavior is unclear.

## Evolution Records

### AE-20260622-01 Frontend UX Contract Enforcement

- Change ID: `AE-20260622-01`
- Trigger: `other`
- Affected files: `.github/agents/frontend-execution.agent.md`, `.github/agents/fullstack-orchestrator.agent.md`, `.github/agents/task-planning.agent.md`, `docs/architecture/agent-evolution-governance.md`
- Confirmed evidence: User reported frontend agent output quality issues: weak visual quality, weak AI/workflow interaction layout, and missing user-friendly interaction constraints.
- Patch summary: Add explicit UX contract, visual acceptance, interaction-state, layout-quality, and UIUX evidence handoff requirements to frontend planning, orchestration, and execution.
- Validation evidence: Pending until prompt files are edited, documentation validation passes, and one frontend page/workflow delegation is dry-run reviewed.
- Rollback condition: Roll back if the added constraints cause repeated false blockers or prevent small non-visual frontend changes from proceeding.
- Status: `active`

```json
{
   "changeId": "AE-20260622-01",
   "checkpointType": "threshold",
   "trigger": "routing-regression",
   "action": "patched",
   "affectedFiles": [
      ".github/agents/frontend-execution.agent.md",
      ".github/agents/fullstack-orchestrator.agent.md",
      ".github/agents/task-planning.agent.md",
      "docs/architecture/agent-evolution-governance.md"
   ],
   "validationStatus": "pending",
   "rollbackStatus": "ready",
   "rationale": "Frontend UI output quality requires explicit UX contract, visual acceptance, interaction-state coverage, and UIUX evidence handoff across planning, orchestration, and execution.",
   "timestamp": "2026-06-22"
}
```

### AE-20260622-02 Task-Scoped Closure And Enterprise OA Interaction Hardening

- Change ID: `AE-20260622-02`
- Trigger: `other`
- Affected files: `.github/copilot-instructions.md`, `.github/agents/fullstack-orchestrator.agent.md`, `.github/agents/task-planning.agent.md`, `.github/agents/frontend-execution.agent.md`, `.github/agents/e2e-uiux-test.agent.md`, `.github/agents/git-preflight-review.agent.md`, `.github/agents/git-commit-executor.agent.md`, `.github/agents/git-push-executor.agent.md`, `.github/skills/task-auto-workflow/SKILL.md`, `.github/skills/git-change-management/SKILL.md`, `.github/skills/frontend-design-redbc-oa/SKILL.md`, `.github/skills/frontend-page-spec-redbc-oa/SKILL.md`, `.github/skills/frontend-engineering/SKILL.md`, `docs/git-workflow.md`, `docs/architecture/tdd-delivery-workflow.md`, `docs/architecture/agent-evolution-governance.md`
- Confirmed evidence: User asked to strengthen single-task completion commits and to refresh OA/enterprise UI interaction practice for the system. Existing workflow wording was stage-level and `docs/git-workflow.md` conflicted with standing task-level automation.
- Patch summary: Enforce Task ID scoped preflight/commit/push, add UIUX evidence manifests, and add enterprise OA interaction baselines for operational filters, tables, batch actions, workflow evidence, permission states, recovery, AI/automation review controls, and keyboard-first use.
- Validation evidence: Pending until documentation validation, scoped diff review, and one real Task ID canary confirms the new closure and UIUX evidence loop.
- External source refresh: Attempted official-source refresh for NN/g heuristics, W3C WAI accessibility principles, IBM Carbon data table patterns, and Ant Design enterprise principles on 2026-06-22; enterprise proxy returned HTTP 403. Local rules use stable non-experimental practice baselines and should be source-refreshed when network access is available.
- Rollback condition: Roll back or narrow if Task ID scoped closure causes repeated false blockers, or if enterprise UIUX rules block small non-visual maintenance changes.
- Status: `active`

```json
{
   "changeId": "AE-20260622-02",
   "checkpointType": "threshold",
   "trigger": "routing-regression",
   "action": "patched",
   "affectedFiles": [
      ".github/copilot-instructions.md",
      ".github/agents/fullstack-orchestrator.agent.md",
      ".github/agents/task-planning.agent.md",
      ".github/agents/frontend-execution.agent.md",
      ".github/agents/e2e-uiux-test.agent.md",
      ".github/agents/git-preflight-review.agent.md",
      ".github/agents/git-commit-executor.agent.md",
      ".github/agents/git-push-executor.agent.md",
      ".github/skills/task-auto-workflow/SKILL.md",
      ".github/skills/git-change-management/SKILL.md",
      ".github/skills/frontend-design-redbc-oa/SKILL.md",
      ".github/skills/frontend-page-spec-redbc-oa/SKILL.md",
      ".github/skills/frontend-engineering/SKILL.md",
      "docs/git-workflow.md",
      "docs/architecture/tdd-delivery-workflow.md",
      "docs/architecture/agent-evolution-governance.md"
   ],
   "validationStatus": "pending",
   "rollbackStatus": "ready",
   "rationale": "Task completion needs Task ID scoped closure, and enterprise OA frontend delivery needs source-aware interaction rules plus UIUX evidence manifests to avoid weak operational UI outcomes.",
   "timestamp": "2026-06-22"
}
```

### AE-20260622-03 Orchestration Plan Entry Enforcement

- Change ID: `AE-20260622-03`
- Trigger: `other`
- Affected files: `docs/architecture/orchestration-high-level-plan.md`, `.github/copilot-instructions.md`, `.github/agents/fullstack-orchestrator.agent.md`, `.github/agents/task-planning.agent.md`, `.github/skills/task-auto-workflow/SKILL.md`, `docs/architecture/ai-development-guide.md`, `docs/documentation-architecture.md`, `docs/README.md`, `docs/architecture/README.md`, `docs/architecture/internal-tool-selection-matrix.md`, `docs/architecture/agent-evolution-governance.md`
- Confirmed evidence: User requested the highest-level plan document be locked so all development tasks either continue unfinished planned work or update/add plan direction before execution.
- Patch summary: Promote `docs/architecture/orchestration-high-level-plan.md` to the scoped orchestration execution source of truth and add high-level plan gates across default instructions, orchestrator, task planning, task-auto workflow, AI guide, documentation architecture, indexes, and routing matrix.
- Validation evidence: Pending until documentation validation, scoped diff review, and a canary request confirms `continue` resumes the active epic while an out-of-plan request refreshes the plan first.
- Rollback condition: Roll back or narrow if the gate blocks read-only analysis, plan-only exploration, or small maintenance work that has an explicit existing high-level direction.
- Status: `active`

```json
{
   "changeId": "AE-20260622-03",
   "checkpointType": "threshold",
   "trigger": "routing-regression",
   "action": "patched",
   "affectedFiles": [
      "docs/architecture/orchestration-high-level-plan.md",
      ".github/copilot-instructions.md",
      ".github/agents/fullstack-orchestrator.agent.md",
      ".github/agents/task-planning.agent.md",
      ".github/skills/task-auto-workflow/SKILL.md",
      "docs/architecture/ai-development-guide.md",
      "docs/documentation-architecture.md",
      "docs/README.md",
      "docs/architecture/README.md",
      "docs/architecture/internal-tool-selection-matrix.md",
      "docs/architecture/agent-evolution-governance.md"
   ],
   "validationStatus": "pending",
   "rollbackStatus": "ready",
   "rationale": "All execution-oriented development needs a mandatory highest-level orchestration entry so agents either continue unfinished planned work or refresh the plan before task generation and implementation.",
   "timestamp": "2026-06-22"
}
```

### AE-20260622-04 BDD Constitution And Behavior-Spec Planning Gate

- Change ID: `AE-20260622-04`
- Trigger: `other`
- Affected files: `docs/architecture/orchestration-high-level-plan.md`, `docs/document-as-software.md`, `docs/governance.md`, `docs/documentation-architecture.md`, `docs/architecture/ai-development-guide.md`, `docs/architecture/tdd-delivery-workflow.md`, `.github/copilot-instructions.md`, `.github/instructions/document-generation-system.instructions.md`, `.github/agents/fullstack-orchestrator.agent.md`, `.github/agents/task-planning.agent.md`, `.github/agents/document-governance.agent.md`, `.github/skills/task-auto-workflow/SKILL.md`, `.github/skills/documentation-governance/SKILL.md`, `.github/skills/test-engineering-tdd/SKILL.md`, `docs/README.md`, `docs/architecture/agent-evolution-governance.md`
- Confirmed evidence: User requested Behaviour Driven Design/Development become project constitution and required planning agents to solidify user behavior, ask for clarification when requirements are unclear, and synchronize source documents before execution.
- Patch summary: Add G01-BDD to the high-level plan, promote BDD into the document-as-software charter, add behavior-spec governance and traceability rules, require BDD readiness in task planning and orchestration, and make TDD Red tests derive from behavior scenarios.
- Validation evidence: Pending until documentation validation, scoped diff review, and canary planning confirms unclear behavior blocks for user confirmation while clear behavior produces BDD-ready tasks.
- Rollback condition: Roll back or narrow if BDD gates block non-behavioral maintenance despite `BDD readiness: not-applicable`, or if behavior specs are duplicated rather than traced to owning documents.
- Status: `active`

```json
{
   "changeId": "AE-20260622-04",
   "checkpointType": "threshold",
   "trigger": "routing-regression",
   "action": "patched",
   "affectedFiles": [
      "docs/architecture/orchestration-high-level-plan.md",
      "docs/document-as-software.md",
      "docs/governance.md",
      "docs/documentation-architecture.md",
      "docs/architecture/ai-development-guide.md",
      "docs/architecture/tdd-delivery-workflow.md",
      ".github/copilot-instructions.md",
      ".github/instructions/document-generation-system.instructions.md",
      ".github/agents/fullstack-orchestrator.agent.md",
      ".github/agents/task-planning.agent.md",
      ".github/agents/document-governance.agent.md",
      ".github/skills/task-auto-workflow/SKILL.md",
      ".github/skills/documentation-governance/SKILL.md",
      ".github/skills/test-engineering-tdd/SKILL.md",
      "docs/README.md",
      "docs/architecture/agent-evolution-governance.md"
   ],
   "validationStatus": "pending",
   "rollbackStatus": "ready",
   "rationale": "Planning and implementation quality require explicit user behavior, acceptance scenarios, source-document synchronization, and BDD-ready tasks before TDD and implementation.",
   "timestamp": "2026-06-22"
}
```

### AE-20260623-01 Deploy Evidence Fail-Closed For Frontend-Visible Changes

- Change ID: `AE-20260623-01`
- Trigger: `routing-regression`
- Affected files: `.github/agents/deploy-execution.agent.md`, `docs/architecture/agent-evolution-governance.md`
- Confirmed evidence: Frontend-visible changes could be discussed with browser-refresh guidance but without explicit docker redeploy and runtime evidence, creating a deployment-claim ambiguity.
- Patch summary: Harden deploy execution with fail-closed frontend-visible path detection and mandatory proof bundle requirements: redeploy outcome, post-deploy verify outcome, runtime confirmation evidence, and frontend container startedAt/health evidence; explicitly prohibit treating browser refresh advice as deployment evidence; require blocked output with next commands when proof is missing.
- Validation evidence: Documentation structure validation command completed successfully: `pwsh -NoProfile -File ./scripts/validate-doc-structure.ps1`.
- Rollback condition: Roll back or narrow if the new gate blocks non-deployment advisory tasks that do not claim deployment success.
- Status: `active`

```json
{
   "changeId": "AE-20260623-01",
   "checkpointType": "threshold",
   "trigger": "routing-regression",
   "action": "patched",
   "affectedFiles": [
      ".github/agents/deploy-execution.agent.md",
      "docs/architecture/agent-evolution-governance.md"
   ],
   "validationStatus": "passed",
   "rollbackStatus": "ready",
   "rationale": "Frontend-visible changes must not be claimed as deployed without explicit redeploy, verification, runtime confirmation, and container health/start-time evidence.",
   "timestamp": "2026-06-23"
}
```

## Current Pending Questions

- Whether threshold windows should be tuned per domain (frontend/backend/governance) after enough stable evidence accumulates.
