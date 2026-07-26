---
id: DOC-ARCH-MODULE-MAP
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
  - documentation-governance
dependsOn:
  - docs/architecture/module-boundaries.md
  - docs/architecture/ai-development-guide.md
related:
  - docs/behavior/module-map-agent-retrieval.md
  - .cursor/rules/tech-stack-guardrails.mdc
  - docs/architecture/quality-gate-threshold-baseline.md
---

# Module Map (Agent Retrieval Entry)

## Purpose

Concrete **package / directory location** map for agents. Prefer this document over
unscoped whole-repo grep when answering “where does capability X live?” or choosing an
implementer package.

| Concern | Document |
| --- | --- |
| **Path / package location** | **This file** (`module-map.md`) |
| **Capability Does / Does-not-own** | [module-boundaries.md](./module-boundaries.md) |
| **AI reading path** | [ai-development-guide.md](./ai-development-guide.md) |

Behavior contract: [module-map-agent-retrieval.md](../behavior/module-map-agent-retrieval.md).

## Agent retrieval entry (confirmed)

1. Open **this map** first for ownership / placement questions.
2. Open [module-boundaries.md](./module-boundaries.md) for capability narrative.
3. Run **targeted** path/symbol search after the map narrows scope.
4. Whole-repo unscoped grep is a **last resort**, not the default first step for module location.
5. When a leaf adds or renames a major `com.bank.docgen.*` package or `frontend/src/*`
   top-level directory, update this map in the **same change set**.

## Global forbidden cross-deps (confirmed)

| Rule | Detail |
| --- | --- |
| **Rendering isolation** | `com.bank.docgen.rendering` must **not** own template lifecycle transitions, authorization / credential policy, or API-governance rules. It consumes explicit rendering tasks and safe references only. |
| **Lifecycle ownership** | Template release / lifecycle stays in `template` (+ related authoring/content modules) — not in `rendering`. |
| **Authorization ownership** | Fail-closed authz / credentials stay in `authorization` (+ `runtime.security` adapters) — not in `rendering`. |
| **API governance** | Route policy / credentials / output-format policy stay in `apimgmt` — not in `rendering` or template composition internals. |
| **Shared kernel** | `sharedkernel` holds stable VOs / envelopes / cross-cutting primitives — **not** business workflow ownership. |

Aligns with [module-boundaries.md](./module-boundaries.md) and tech-stack guardrails.

## Backend — `com.bank.docgen.*`

Canonical root: `backend/src/main/java/com/bank/docgen/`.
Single Maven module; business boundaries are **package** boundaries.

| Module / path | Purpose | Key paths | Forbidden cross-deps |
| --- | --- | --- | --- |
| `com.bank.docgen` (app root) | Spring Boot entry | `DocGenApplication.java` | Must not embed domain rules |
| `apimgmt` | Template-level API policy, credentials summaries, callable routes | `…/apimgmt/{api,domain,service,web,persistence,mapping}` | Must not own template authoring content or rendering OOXML |
| `audit` | Security / lifecycle / generation audit records | `…/audit/{api,domain,service,web,persistence}` | Must not store sensitive plaintext business variables as SoT |
| `authoring` | Structured authoring / content editing support | `…/authoring/structured/**` | Must not read `rendering.persistence` directly |
| `authorization` | Management authz, AD-group resolution use, fail-closed decisions | `…/authorization/management/**` | Must not own external directory sync internals or rendering |
| `collaboration` | Collaboration / review workflows | `…/collaboration/{api,domain,service,web,persistence,scheduler}` | Must not own release publication SoT |
| `contentmodule` | Structured content modules referenced by templates | `…/contentmodule/{api,domain,service,web,persistence}` | Must not own API credential lifecycle |
| `dashboard` | Management dashboard aggregates | `…/dashboard/{api,service,web}` | Must not own domain write workflows |
| `demo` | Demo package / sample asset generators (non-product SoT) | `…/demo/support/**` | Must not become production business-rule home |
| `documentbrand` | Document brand / letterhead assets and policy | `…/documentbrand/{api,domain,service,web}` | Must not own template lifecycle publication |
| `infrastructure` | Cross-cutting infra adapters (config, storage, i18n, resilience, async) | `…/infrastructure/{config,storage,i18n,resilience,async,web}` | Must not hide product domain rules in “utils” |
| `legalhold` | Legal-hold records and controls | `…/legalhold/{api,domain,service,web,persistence}` | Must not own generation orchestration |
| `library` | Group-scoped asset catalog + full-library export | `…/library/{api,domain,service,web,persistence}` | Must not own `StructuredContentImageResolver` protocol or authz SoT |
| `master` | Master DOCX assets, anchors, master review / impact | `…/master/{api,domain,service,web,persistence,rendering}` | Must not own template release lifecycle or API policy |
| `rendering` | DOCX assembly, PDF conversion, preview, rendering diagnostics | `…/rendering/{api,domain,service,web,persistence,listener,scheduler,metrics}` | **Must not** own lifecycle, authorization, or API-governance rules; no template orchestration via `template.service` / credential validation |
| `runtime` | Generation orchestration, async/task coordination, runtime security adapters | `…/runtime/{api,domain,service,web,persistence,messaging,scheduler,security,metrics}` | Must not embed DOCX OOXML details (delegate to `rendering`) |
| `sharedkernel` | Stable VOs, identifiers, error envelope, lifecycle/locale/security primitives | `…/sharedkernel/{api,document,health,lifecycle,locale,security}` | Must not own business workflows |
| `template` | Template composition, variables, lifecycle / release governance ports | `…/template/{api,domain,service,web,persistence,port,event,mapping}` | Must not reach into `rendering` persistence; preview/batch evidence via **port** |

### Specialist routing hints (backend)

| Change focus | Prefer |
| --- | --- |
| DOCX/PDF/LibreOffice/preview worker | `rendering-engineer` → `rendering` |
| Generation task / runtime API | `backend-engineer` → `runtime` (+ `apimgmt` if policy) |
| Template lifecycle / composition | `backend-engineer` → `template` |
| Authz / fail-closed | `backend-engineer` → `authorization` |

## Frontend — `frontend/src/*`

Canonical root: `frontend/src/`. One Vue application; split further only when ownership/build isolation requires it.

| Module / path | Purpose | Key paths | Forbidden cross-deps |
| --- | --- | --- | --- |
| `frontend/src/api` | Thin HTTP clients / OpenAPI wrappers | `api/**`, generated `openapi-v1` consumers | Must not own domain UI state machines |
| `frontend/src/auth` | Session / login / token helpers | `auth/**` | Must not redefine server authz SoT |
| `frontend/src/components` | Reusable OA UI primitives and domain widgets | `components/common/**`, domain folders | Avoid business orchestration that belongs in composables |
| `frontend/src/composables` | Stateful `use*` logic for views | `composables/**` (e.g. template detail controllers) | Must not duplicate Pinia fetch/error parsing |
| `frontend/src/config` | App/runtime config | `config/**` | No secrets in source |
| `frontend/src/constants` | Shared constants | `constants/**` | No i18n copy here |
| `frontend/src/i18n` | English-first locales | `i18n/locales/en.ts` (base), `zh-CN.ts` | No divergent hard-coded English in templates |
| `frontend/src/navigation` | Nav model / menu wiring | `navigation/**` | Must not bypass router permission gates |
| `frontend/src/router` | Vue Router instance / registration | `router/**` | Keep route tables coherent with `routing` |
| `frontend/src/routing` | Route table / meta helpers | `routing/**` | Must not embed server permission SoT |
| `frontend/src/stores` | Pinia stores | `stores/**` | No duplicate HTTP envelope parsing |
| `frontend/src/styles` | Global SCSS entry | `styles/**` | Prefer tokens from `theme` |
| `frontend/src/theme` | Design tokens / brands | `theme/tokens.ts` | No ad-hoc purple/glow marketing themes |
| `frontend/src/types` | Shared TS types | `types/**` | Prefer OpenAPI types when available |
| `frontend/src/utils` | Pure helpers | `utils/**` | Must not become hidden product-rule home |
| `frontend/src/views` | Route-level pages by domain | `views/<domain>/**` | Views own layout; business rules → composables |
| `frontend/src/assets` | Static assets | `assets/**` | — |
| `frontend/src/build` | Build-time helpers | `build/**` | — |
| `frontend/src/dev` | Dev-only helpers | `dev/**` | Must not ship as product behavior SoT |
| `frontend/src/test` | Shared test helpers | `test/**` | — |
| `frontend/src/App.vue` / `main.ts` | App shell bootstrap | root files | Keep thin |

### Specialist routing hints (frontend)

| Change focus | Prefer |
| --- | --- |
| Management UI journey / Vue surface | `frontend-engineer` |
| Pure i18n key add with no journey change | Still `frontend-engineer`; lane may be `full` if user-visible |

## Drift policy

Missing a new major package/dir from this map at Done is a documentation defect for the
introducing leaf. Do **not** invent a second competing module index outside this file.

## Pending questions

None for G1 scaffold. Physical package splits beyond current Maven/Vue layout remain
governed by [module-boundaries.md](./module-boundaries.md) pending questions.
