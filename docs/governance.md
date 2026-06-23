# Documentation Governance

## Purpose

This project uses a document-as-software operating model: documentation is the durable system definition, and implementation code is a replaceable realization of that definition.

The goal is to keep the project aligned with confirmed requirements, prevent design or implementation drift, and preserve the ability to rebuild the system from structured documentation.

## Governing Ideas

The documentation structure follows these practical ideas:

- Document as software: documentation is treated as a first-class project asset, not a companion artifact.
- Docs-as-code: documentation changes are versioned, reviewed, and traceable.
- Rebuildability: future implementation should be reproducible from documentation without treating old code as the source of truth.
- Production-grade real functionality by default: delivery acceptance requires durable persistence, real integration behavior, and deploy/runtime evidence.
- Single responsibility docs: each document has a clear job.
- Decision records: durable decisions should be captured as ADRs.
- Cross-document indexing: docs must be discoverable and linked from an index.
- Confirmed vs pending separation: assumptions must not become requirements.
- Behaviour Driven Design/Development: behavior-changing work must be planned from explicit user behavior and acceptance scenarios before implementation.

## Document Responsibilities

| Document | Responsibility |
| --- | --- |
| `docs/requirements/requirements-plan.md` | Raw confirmed requirements and pending questions |
| `docs/document-as-software.md` | Project charter for document-as-software principles, AI development rules, and rebuildability standards |
| `docs/documentation-architecture.md` | Documentation knowledge model, metadata schema, source-of-truth map, traceability model, indexes, and validation rules |
| `docs/product/PRD.md` | Product requirements and user-visible behavior |
| `docs/domain/domain-model.md` | Domain concepts, relationships, states, lifecycle rules, API contract concepts |
| `docs/security/permission-matrix.md` | Role permissions, group isolation, API authorization, and audit permissions |
| `docs/api/README.md` | Future API contract documentation area |
| `docs/architecture/README.md` | Architecture index and source-of-truth boundaries for architecture views |
| `docs/architecture/tdd-delivery-workflow.md` | Fixed engineering workflow and mandatory TDD quality gates for implementation work |
| `docs/README.md` | Documentation index and navigation |
| `docs/git-workflow.md` | Git version control workflow for documentation and future code changes |
| `docs/adr/<topic>/*.md` | Architecture or product decision records |
| `.github/copilot-instructions.md` | Always-on project documentation rules for Copilot |
| `.github/skills/documentation-governance/SKILL.md` | On-demand workflow for documentation updates and drift checks |
| `.github/skills/git-change-management/SKILL.md` | On-demand workflow for Git status review, commit preparation, and remote sync |

## Update Rules

### Rule 0: Keep Documentation Authoritative

When a change affects behavior, architecture, API contracts, permissions, security, data handling, operations, or implementation assumptions, update the relevant documentation before claiming the work is complete.

Implementation code must not silently become the source of truth. If code and documentation disagree, surface the conflict, preserve the latest confirmed user decision, update the affected documentation, and then align the implementation.

### Rule 1: Update Raw Requirements First

When the user confirms a requirement, update `docs/requirements/requirements-plan.md` first. Then propagate the same confirmed rule to the relevant structured documents.

For behavior-changing requirements, persist a behavior specification before task planning: actor/role, user goal, trigger, preconditions, user journey or operation steps, system responses, acceptance scenarios, Given/When/Then or equivalent, boundary and exception scenarios, observable evidence, and traceability.

For technology selection confirmations, update `docs/architecture/technology-stack-decisions.md` immediately in the same turn, then propagate to ADRs and architecture docs as needed.

For implementation planning and coding, treat accepted ADR technology choices as authoritative baseline. Do not infer framework/runtime truth from temporary repository state (for example, incomplete `pom.xml` or scaffolding drift) when it conflicts with accepted ADRs.

### Rule 2: Keep Pending Questions Separate

If the user has not confirmed a behavior, keep it in a pending/open-question section. Do not write it as a product rule.

If the behavior cannot be specified clearly enough for acceptance scenarios, ask the user for confirmation before task planning or implementation.

### Rule 3: Update All Affected Documents Together

Examples:

- A template lifecycle change affects `docs/requirements/requirements-plan.md`, `docs/product/PRD.md`, and `docs/domain/domain-model.md`.
- A role or permission change affects `docs/requirements/requirements-plan.md`, `docs/product/PRD.md`, and `docs/security/permission-matrix.md`.
- An API authorization change affects `docs/requirements/requirements-plan.md`, `docs/product/PRD.md`, `docs/domain/domain-model.md`, and `docs/security/permission-matrix.md`.
- A new durable design decision may also require an ADR.
- An architecture boundary change affects `docs/architecture/README.md` and the relevant architecture view, and may require an ADR when it records a durable decision.

When a change touches more than one source-of-truth layer, update the owner documents together so functional requirements, non-functional requirements, and technology selections remain separate but consistent.

### Rule 4: Maintain Links

When a document is added, renamed, moved, or retired, update `docs/README.md` in the same change.

Follow [documentation-architecture.md](documentation-architecture.md) when deciding whether to add a document, split a document, add metadata, or move files.

### Rule 5: Use ADRs for Durable Decisions

Create an ADR when a decision is expected to shape future design or implementation, especially for:

- API authorization model.
- Template lifecycle model.
- Versioning model.
- Environment migration model.
- DOCX/PDF encryption model.
- Document rendering strategy.
- Permission and group isolation strategy.

New ADRs should follow the metadata and topic taxonomy rules in [ADR Index](adr/README.md) and be placed under the matching `docs/adr/<topic>/` directory. Future physical ADR reorganizations require explicit confirmation plus index, link, and validation updates.

### Rule 6: Mark Conflicts Explicitly

If two documents disagree, do not silently reconcile them. Add a pending question and ask for confirmation.

If session history and repository documents disagree, repository documents are authoritative for current execution. Session history can only be used as input to propose updates, not as accepted fact.

### Rule 7: Avoid Premature Technical Choices

Do not add technologies, libraries, frameworks, database choices, or deployment models unless the user explicitly asks for technical design.

### Rule 8: Version Documentation Through Git

Documentation changes are project changes and should be tracked through Git. Use [git-workflow.md](git-workflow.md) for commit, diff, staging, and sync rules.

Do not commit or push changes unless the user explicitly asks or a repository workflow policy, such as `task-auto-workflow`, grants standing task-level automation for the current request.

### Rule 9: Enforce Fixed TDD Delivery Workflow

For implementation work, follow [Fixed TDD Delivery Workflow](architecture/tdd-delivery-workflow.md) as a blocking process gate.

Minimum required sequence:

1. Generate tasks.
2. Generate task sheet.
3. Review required skills.
4. Implement with strict Red-Green-Refactor TDD.
5. Run requirement tests.
6. Perform code review and static scans.
7. Commit.
8. Push.

If TDD order is broken, tests fail, or static scans contain blocking findings, do not commit or push.

### Rule 10: Per-Answer Persistence for Stack Decisions

When the user is confirming technology stack items step by step:

1. Ask one question at a time.
2. After each user answer, update `docs/architecture/technology-stack-decisions.md` immediately before asking the next question.
3. Mark each item with explicit status (`ADR Accepted`, `Session Confirmed, ADR Pending`, or `Pending Confirmation`).
4. Do not ask the same item again once it is persisted, unless the user explicitly requests re-open.

### Rule 11: Enterprise Package Manager and Deployment Entrypoints

Frontend automation must use the pinned pnpm baseline through direct `pnpm` or the shared resolver in `scripts/frontend-pnpm-command.ps1`. Do not use `corepack pnpm` in repository scripts or workflows, because Corepack can fetch package managers from public registry endpoints without honoring the same approved npm configuration path used by the installed pnpm executable.

Before diagnosing frontend install or audit failures, inspect effective npm configuration and the relevant `.npmrc` scope first. Treat registry, proxy, CA, and authentication configuration as the first evidence source; do not infer a package-manager version problem until that configuration path is checked.

Local container builds that need npm registry access must use BuildKit secrets for npm configuration on every Dockerfile `RUN` layer that can contact the npm registry. Do not copy `.npmrc`, proxy credentials, or registry auth material into tracked files, Docker image layers, or generated logs.

Local compose service names must use the `docgen-*` prefix and stay synchronized across compose files, verification scripts, and README instructions.

### Rule 12: Production-Grade Real-Functionality Gate

Production-grade real functionality is mandatory by default and treated as a constitutional project rule.

Completion claims for behavior-changing work must satisfy all gates below unless owning source-of-truth documents explicitly classify the scope as transitional and non-production:

1. Persistence gate: persistence-sensitive behavior must be backed by durable storage boundaries; in-memory-only completion is not accepted as production closure.
2. Integration gate: behavior must be exercised through real integration paths, not only mock/stub assertions.
3. Deployment gate: user-visible changes require redeploy evidence and runtime verification evidence before deployment-complete claims.
4. Evidence gate: completion reports must include command outcomes and runtime/operational evidence references.

The following patterns are disallowed for production-complete claims:

- in-memory-only user/session stores as final-state closure
- mock/stub/fake adapters as final-state closure without explicit transitional classification
- browser-refresh advice used as substitute for deployment evidence

## Quality Gates

Before completing a documentation update, verify:

- The change is either confirmed or listed as pending.
- Behavior-changing work has a behavior specification or an explicit not-applicable reason.
- All affected documents were updated.
- The change does not weaken the rebuildability standard in [document-as-software.md](document-as-software.md).
- The change follows the document type, ownership, traceability, and validation rules in [documentation-architecture.md](documentation-architecture.md).
- Terminology is consistent.
- Links still point to existing files.
- No feasibility discussion was promoted to confirmed requirement.
- Any durable decision has an ADR or is listed as needing one.
- Implementation details have not displaced the documented source of truth.
- Functional requirements, non-functional requirements, and technology selections are not mixed in a single owning document unless the file is explicitly acting as an index or cross-reference.
- Git workflow changes are reflected in `docs/git-workflow.md` and `.github/skills/git-change-management/SKILL.md`.
- Implementation workflow changes are reflected in `docs/architecture/tdd-delivery-workflow.md`, `.github/skills/task-auto-workflow/SKILL.md`, and `.github/skills/git-change-management/SKILL.md`.
- Frontend automation uses direct `pnpm` or `scripts/frontend-pnpm-command.ps1`; `scripts/validate-doc-structure.ps1` blocks `corepack pnpm` in scripts and workflows.
- Production-grade real-functionality gate is satisfied, or the scope is explicitly marked transitional/non-production with residual risks in owning source documents.
- Run `./scripts/validate-doc-structure.ps1` when changing documentation structure, indexes, ADRs, governance rules, or document-as-software behavior.

## Terminology

Use the same terms across documents:

- 母版
- 锚点
- 模板
- 开发版本
- 发布版本
- 模板级授权
- API 凭证
- AD Group
- 全局管理员
- 分组管理员
- 母版设计人员
- 模板编排人员
- 测试人员
- 审批人员
- 审计管理员

## Current Open Governance Notes

- The project follows the [Document as Software Charter](document-as-software.md): documentation is the durable system definition, AI-authored documentation is allowed under governance rules, and future implementation must remain rebuildable from documentation.
- Documentation structure follows [Documentation Architecture](documentation-architecture.md), and baseline structure checks are automated by [validate-doc-structure.ps1](../scripts/validate-doc-structure.ps1).
- Stable repetitive project helper operations should be handled through script automation first, and the scripts should be kept current as the workflow evolves.
- Frontend package-manager automation uses direct `pnpm` or the shared resolver and never `corepack pnpm`; local frontend container builds use BuildKit npmrc secrets, and local compose service names use the `docgen-*` prefix.
- DOCX/PDF 动态加密已纳入正式需求，且 API 可以直接传入密码；密码不落库、不进日志，只在本次生成过程中使用；动态加密配置属于 API 管理功能，不属于模板编排或模板提交功能；加密参数采用 enabled、openPassword、ownerPassword、permissions 标准模型；`enabled=true` 时 `openPassword` 必填，`ownerPassword` 可选，`permissions` 采用统一抽象权限枚举并要求同时传入 `ownerPassword`；密码长度基线为 12 到 128 字符，open/owner 同时传入时必须不同；加密参数错误返回 `400 ENCRYPTION_PARAMETER_INVALID`，加密处理失败返回 `500 ENCRYPTION_FAILED` 且 `retryable=true`；加密策略摘要采用标准摘要。
- 基础技术栈基线已通过 [ADR 0022](adr/technology-stack/0022-basic-technology-stack-baseline.md) 记录；PRD、领域模型和权限矩阵继续保持技术中立，未确认的实现选型保持待确认状态。
- 文档结构已拆分为 requirements, product, domain, security, API, governance, and ADRs.
- Checkstyle 基线债治理已确认过渡参数（2026-06-10）：允许按变更范围 delta-clean 作为阶段性收口标准；每波至少降低 200 条 warning；warning 降至 300 时恢复全量严格阻断。
