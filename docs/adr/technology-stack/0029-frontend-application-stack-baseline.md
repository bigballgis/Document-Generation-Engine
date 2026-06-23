---
id: ADR-0029
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0029"
topic: technology-stack
related:
  - docs/architecture/README.md
  - docs/architecture/runtime-view.md
  - docs/architecture/ai-development-guide.md
  - docs/adr/technology-stack/0022-basic-technology-stack-baseline.md
---

# ADR 0029: Frontend Application Stack Baseline

## Status

Accepted

## Context

The management frontend needs a stable application stack so the documented architecture, implementation planning, and future UI work stay aligned.

The user confirmed a Vue-based frontend with Element Plus, Pinia, Vue Router 4, Axios, pnpm, SCSS/CSS Modules, and a Vue testing stack. This ADR records those accepted frontend application baseline decisions so they no longer remain as session-only notes in the technology decision log.

## Decision

The confirmed frontend application baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| Frontend UI stack | Element Plus + Pinia + Vue Router 4 + Axios + pnpm | Main management UI application stack baseline. |
| Frontend styling | SCSS + CSS Modules | Frontend styling baseline for component-scoped styling and maintainable UI composition. |
| Frontend testing | Vitest + Vue Test Utils + Playwright | Unit, component, and end-to-end testing baseline. |
| Frontend package manager lock strategy | Enforce pnpm-lock.yaml | Lockfile baseline for the frontend toolchain. |

These decisions are accepted as the frontend application foundation. More specialized frontend architecture choices remain pending until they are explicitly confirmed and synchronized into follow-up ADRs or architecture views.

## Consequences

- The frontend stack is now documented as an accepted baseline rather than scattered session notes.
- The management frontend can be implemented consistently with the documented runtime view and AI development guide.
- Future frontend toolchain or UI stack changes should be made by updating this ADR and the affected architecture views together.

## Alternatives Considered

- Keeping the frontend stack only in the technology decision log: rejected because durable decisions need an ADR.
- Splitting styling, testing, and package management into separate ADRs: rejected for this baseline because they form one cohesive frontend application foundation.
- Using a different component library or state-management stack: not selected because the user confirmed the current Vue-centered baseline.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Runtime View](../../architecture/runtime-view.md)
- [AI Development Guide](../../architecture/ai-development-guide.md)
- [Basic Technology Stack Baseline ADR](./0022-basic-technology-stack-baseline.md)