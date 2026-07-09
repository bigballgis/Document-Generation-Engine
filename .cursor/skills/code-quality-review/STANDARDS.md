# Code Quality Standards — Document Generation Engine

Project-local conventions for `code-quality-reviewer`. Governance rules remain in ADRs and `.cursor/rules/`.

## Principles (user bar: zero waste)

1. **Every line earns its place** — delete before abstracting.
2. **Match the nearest peer** — new code mirrors the file and module it joins.
3. **Extract on the third repetition** — not the first.
4. **Read top-to-bottom** — imports, types, public API, private helpers; no random section order.
5. **Tests prove behavior** — not coverage theater; no duplicate assertion blocks.

## Module coupling (🔴 when violated)

Per `docs/architecture/module-boundaries.md`:

- `rendering` must not depend on `template.service` / `template.persistence` for orchestration
- `authoring` must not read `rendering.persistence` directly
- `template` queries preview/batch evidence via a **port**, not `PreviewRecordRepository`
- Rendering failures use `rendering.*` exceptions + `RenderingExceptionAdvice`, not `TemplateValidationException`
- Shared enums (`FidelityWarningCode`, `PreviewComparisonSeverity`) belong in `sharedkernel` or owning domain — not split across template/rendering

Recommend `ModuleBoundaryArchTest` (ArchUnit) when coupling 🔴 findings recur.

## Backend package layout

```
com.bank.docgen.<module>/
  api/          # DTOs / request-response views exposed to web layer
  domain/       # enums, value objects, domain helpers (no Spring)
  mapping/      # MapStruct
  persistence/  # JPA entities + repositories
  service/      # business logic, @Service
  web/          # @RestController
```

- `rendering.*` — no template lifecycle or API credential imports.
- `runtime.*` — orchestrates generation; does not embed DOCX OOXML details.
- `*Support` — static or stateless helpers only; no `@Transactional` services disguised as Support.

## Frontend layout

```
src/
  api/           # HTTP clients, thin
  components/    # reusable UI; common/ for shell primitives
  composables/   # use* — stateful logic
  stores/        # Pinia — server cache + session
  views/<domain>/ # route pages; delegate to composables
  i18n/locales/  # en.ts base
  theme/         # tokens, brands
```

- Views >300 LOC → split composable or child components.
- `useTemplateDetailController.ts` is a known hotspot — new logic goes to focused composables.

## Naming

| Kind | Convention | Avoid |
| --- | --- | --- |
| Java service | `TemplateLifecycleService` | `TemplateManager`, `Helper` |
| Java test | `TemplateLifecycleServiceTest` | `TestTemplateLifecycle` |
| Vue composable | `useTemplateLifecycleActions.ts` | `templateLifecycle.ts` |
| Pinia store | `templates.ts` → `useTemplatesStore` | duplicate store per view |
| i18n key | `templates.lifecycle.publish` | English sentence as key |

## Comments

- **Keep:** invariants, non-obvious business rules, workarounds with ticket/ADR ref
- **Remove:** `// increment i`, deleted code blocks, outdated phase references

## Imports

- Java: no wildcard imports; static imports only for constants/assertions
- TS: type-only imports `import type`; group: vue → external → `@/` → relative

## Files never commit

- `backend/target/**`
- `frontend/dist/**`
- `node_modules/**`
- `.env`, credentials

## Quality gates (must stay green after cleanup)

```powershell
mvn -B -ntp -f backend/pom.xml verify
pnpm -C frontend lint && pnpm -C frontend type-check && pnpm -C frontend test && pnpm -C frontend build
```

Refactor-only slices: prove no behavior change via existing tests; add none unless fixing a gap.
