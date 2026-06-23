---
id: ADR-0027
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - documentation-governance
adrNumber: "0027"
topic: documentation-governance
related:
  - docs/adr/README.md
  - docs/adr/documentation-governance/0026-adr-metadata-taxonomy-and-migration-plan.md
  - docs/documentation-architecture.md
  - scripts/validate-doc-structure.ps1
---

# ADR 0027: ADR Topic Directory Organization

## Status

Accepted

## Context

ADR 0026 established ADR metadata, an accepted topic taxonomy, and a migration plan. After reviewing whether flat ADR storage was sufficient, the user explicitly confirmed physical topic-directory migration with the instruction `做吧`.

The project treats documentation as software. A physical documentation reorganization must therefore be a governed change: it needs an accepted decision, index updates, link repair, and validation rather than a cosmetic folder shuffle.

## Decision

Physically organize numbered ADR files under `docs/adr/<topic>/`, where `<topic>` matches the accepted `topic` frontmatter value from [ADR 0026](0026-adr-metadata-taxonomy-and-migration-plan.md) and the taxonomy in [ADR Index](../README.md).

The ADR root keeps only cross-topic governance files such as [ADR Index](../README.md) and [ADR Template](../0000-template.md). Numbered ADRs should not remain directly under `docs/adr/` unless a later ADR explicitly changes this rule.

New numbered ADRs must be created in the topic directory that matches their `topic` frontmatter. If an ADR's topic changes later, the file path, ADR index, documentation index, links, and validation expectations must be updated in the same change.

Future physical ADR reorganizations require explicit user confirmation and an ADR or equivalent accepted governance record before files are moved.

## Consequences

- ADR discovery becomes easier for humans and AI agents because topic is visible in both metadata and path.
- Link churn becomes an explicit validation concern for any ADR movement.
- The ADR index and global documentation index become mandatory navigation surfaces after topic migration.
- Validation must scan ADRs recursively and ensure numbered ADR paths match their metadata topics.
- Future ADR creation has a clearer placement rule.

## Alternatives Considered

- Keep all ADRs flat and rely only on frontmatter: rejected because the user preferred stronger AI-friendly structure after metadata backfill was accepted.
- Add only topic headings in the ADR index: rejected because it would improve navigation but not physical retrieval or path-based grouping.
- Reorganize by lifecycle status: rejected because almost all current ADRs are accepted, while topic better reflects retrieval and ownership needs.
- Reorganize by system layer: rejected because several decisions cross system layers but still have a clear governance topic.

## Related Documents

- [ADR Index](../README.md)
- [ADR Metadata, Topic Taxonomy, and Migration Plan](0026-adr-metadata-taxonomy-and-migration-plan.md)
- [Documentation Architecture](../../documentation-architecture.md)
- [Documentation Index](../../README.md)
- [Project AI Instructions](../../../.github/copilot-instructions.md)
- [Documentation Structure Validation Script](../../../scripts/validate-doc-structure.ps1)