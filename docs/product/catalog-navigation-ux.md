# Catalog navigation UX — packages vs versions

**Status:** Confirmed (2026-06-24)  
**Scope:** Management UI navigation, master/template list and detail pages.

## Design principle

The management shell exposes **Masters** and **Templates** as top-level catalog entries—not “master versions” or “template versions” as menu labels.

Navigation follows a two-level mental model:

```text
Masters / Templates (menu)
  └── Package list (one row per logical master or template package, keyed by name)
        └── Package detail
              └── Version / revision lines (status, last updated, last actor, …)
```

| Layer | User sees | Backend reality (v1) |
| --- | --- | --- |
| Package list | Master or template **name** (plus group, workflow summary) | One `master_document` or `template` row per package |
| Package detail — version lines | Table of version/revision lines with status and audit fields | **Template:** `template_version` rows with `release_version`. **Master:** single current DOCX revision line + review workflow (no release-version entity) |

## Why this is correct

1. **Matches domain language:** PRD and domain model describe **master documents** and **templates** as managed objects; release versions are an inner concern of templates, not the primary menu identity.
2. **Matches user tasks:** Operators look for “the retail letter template” or “the retail letterhead master”, not an abstract “version catalog” index.
3. **Separates concerns:** The list answers “what packages exist?”; the detail answers “what versions/revisions exist inside this package and who changed them?”
4. **Honest for masters:** Masters do not have template-style release versions. The UI still shows a **revision line** table (current DOCX + workflow status) instead of falsely listing multiple release versions.

## UI mapping

### Navigation

- Group: **Document content** (`nav.groups.content`)
- Items: **Masters** (`/masters`), **Templates** (`/templates`)

### Master package list

Columns emphasize the **package**: name, workflow status, anchor count, last updated, last updated by.

### Master package detail

1. Package header (name, group, actions: download, replace file, review)
2. **Revision lines** — current DOCX line (status, file, anchors, updated at/by)
3. Anchor catalog, review history, impact analysis

### Template package list

Columns: name, external ID, in-flight workflow status, current release version, release version count, last updated, last updated by.

### Template package detail

Default tab: **Versions** (release version lines). Other tabs: overview & workflow, authoring, API access.

## Terminology (en)

| Avoid in nav / list titles | Prefer |
| --- | --- |
| Master versions | Masters |
| Template versions | Templates |
| Version catalog | Masters / Templates (under Document content) |
| Version status (on package list) | Workflow status |

## Related docs

- `docs/domain/domain-model.md` §2.5 (master), §2.10–2.11 (template versions)
- `docs/plan/detail/P2-master-management.md`
- `docs/plan/detail/P16-lifecycle-version-governance.md`

## Pending (out of scope for this UX slice)

- True multi-revision history for masters (each file replace as a persisted revision row)
- Master anchor catalog versioning aligned with OpenAPI `anchor-catalogs` admin contract
- URL rename (`/masters` / `/templates` retained for stability)
