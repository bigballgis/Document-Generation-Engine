# Catalog navigation UX — packages vs versions

**Status:** Confirmed (2026-06-25)  
**Phase A implementation:** Done (2026-06-25) — hub + revision detail routes, revision-lines API, E2E 4/4.  
**Scope:** Management UI navigation, master/template list and detail pages.  
**Traceability:** BDD-MASTER-REVISION-NAV-001 (master revision two-page split).

## Design principle

The management shell exposes **Masters** and **Templates** as top-level catalog entries—not “master versions” or “template versions” as menu labels.

Navigation follows a package-first mental model (masters add a third level — revision line detail):

```text
Masters / Templates (menu)
  └── Package list (one row per logical master or template package, keyed by name)
        └── Package hub (master or template package detail)
              └── Version / revision lines (status, last updated, last actor, …)
                    └── Revision line detail (master only — anchors, review history)
```

| Layer | User sees | Backend reality (v1) |
| --- | --- | --- |
| Package list | Master or template **name** (plus group, workflow summary) | One `master_document` or `template` row per package |
| Package hub — version lines | Paginated table of version/revision lines with status and audit fields | **Template:** `template_version` rows with `release_version`. **Master (Phase A):** paginated API may return only the **current** DOCX revision line — honest until Phase B adds full history |
| Revision line detail (master) | Overview, anchor catalog, review history for one revision line | **Master:** keyed by `revisionLineId`; hub retains package-level actions and impact analysis |

## Why this is correct

1. **Matches domain language:** PRD and domain model describe **master documents** and **templates** as managed objects; release versions are an inner concern of templates, not the primary menu identity.
2. **Matches user tasks:** Operators look for “the retail letter template” or “the retail letterhead master”, not an abstract “version catalog” index.
3. **Separates concerns:** The list answers “what packages exist?”; the hub answers “what revision lines exist and what is the package impact?”; revision detail answers “what anchors and review history belong to this line?”
4. **Honest for masters (Phase A):** Masters do not have template-style release versions. The hub shows a paginated revision-line table that may contain only the current DOCX line until Phase B adds full history.

## UI mapping

### Navigation

- Group: **Document content** (`nav.groups.content`)
- Items: **Masters** (`/masters`), **Templates** (`/templates`)

### Master package list

Columns emphasize the **package**: name, workflow status, anchor count, last updated, last updated by.

### Master package hub — `/masters/:masterId`

Package-level surface (BDD-MASTER-REVISION-NAV-001). Row click on a revision line navigates to revision detail.

1. **Package header** — name, group, workflow status
2. **Package actions** — download, replace file, submit/review (when permitted)
3. **Revision lines** — paginated table (Phase A: typically one current DOCX line — status, source file summary, anchor count, updated at/by)
4. **Impact analysis** — referenced templates summary (unchanged from P2-T04)

Does **not** host the full anchor catalog or review history; those live on the revision detail route.

### Master revision line detail — `/masters/:masterId/revisions/:revisionLineId`

Revision-scoped surface for one DOCX revision line.

1. **Revision overview** — status, source file, updated at/by, link back to package hub
2. **Anchor catalog** — filterable table for this revision line
3. **Review history** — audit trail for this revision line

### Master revision history — phased delivery

| Phase | Scope | API / UI honesty | Status |
| --- | --- | --- | --- |
| **A** | Two-page split (hub + revision detail); paginated revision-lines API | May return **only the current revision line**; UI must not imply multiple historical rows exist when the API does not provide them | **Done (2026-06-25)** — `MasterPackageHubView`, `MasterRevisionDetailView`, `MasterRevisionLineController`, Flyway V22, E2E 4/4 |
| **B** | True multi-revision history — each file replace persisted as a revision row | Full paginated history on hub; revision detail deep-links to any historical line | Not Started |

Phase B requires a persisted master revision entity and list API; tracked separately from this navigation UX slice.

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

## Pending (out of scope for Phase A)

- **Phase B — full master revision history** (see phased table above; each file replace as a persisted revision row)
- Master anchor catalog versioning aligned with OpenAPI `anchor-catalogs` admin contract
- URL rename (`/masters` / `/templates` retained for stability)
