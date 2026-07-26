---
id: BDD-AI-SCALE-MODULE-MAP
title: Module map — agent retrieval SoT
status: ready
date: 2026-07-26
bdd_readiness: ready
task_ids: [166]
placement: ISOLATED
worktree_path: D:/working/DGE-ai-scale-remediation-g1
branch: feat/ai-scale-remediation-g1
slice: ai-scale-remediation-g1
user_confirmation: 2026-07-26 「按你的建议整改吧」
---

# Module Map — Agent Retrieval — BDD behavior spec

| Field | Value |
| --- | --- |
| **Slice** | `ai-scale-remediation-g1` (governance + docs scaffold; this file = retrieval contract) |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-26 |
| **Actor** | Parent agent / implementer specialists (**not** end user) |
| **Owning artifact (to exist)** | [`docs/architecture/module-map.md`](../architecture/module-map.md) |
| **Related SoT** | [`module-boundaries.md`](../architecture/module-boundaries.md), [`ai-development-guide.md`](../architecture/ai-development-guide.md) |
| **Product E2E / UIUX / runtime API** | **`not-applicable`** |
| **Formal phase** | **None** — do not invent a P-phase |

---

## Classification

Agent-governance behavior: how agents **locate** backend packages and frontend major
directories before coding or deep audits. This is **not** product UI/API behavior.

| Surface | Applicability |
| --- | --- |
| Product management UI / Playwright E2E | **not-applicable** |
| Product UIUX review | **not-applicable** |
| Backend Java / Flyway / OpenAPI product behavior | **not-applicable** |
| Agent retrieval / architecture docs | **in scope** |
| Evidence for this leaf | docs consistency / index; no product deploy/E2E |

---

## 1. Actor / role

| Actor | Role |
| --- | --- |
| **Parent agent** | Prefers module map before whole-repo grep when locating ownership |
| **backend-engineer / frontend-engineer / rendering-engineer** | Uses map to place changes and respect forbidden cross-deps |
| **architecture-reviewer / code-quality-reviewer** | Uses map + boundaries when judging coupling |
| **explore** (built-in) | May still deep-scan; should start from map when the question is “which module owns X?” |
| **End user (bank OA)** | **Out of scope** |

---

## 2. Goal

1. Maintain a durable **module map** SoT at `docs/architecture/module-map.md`.
2. Agents **prefer that map** over unscoped whole-repo grep when answering “where does
   capability X live?” or choosing an implementer package.
3. Map lists **backend** `com.bank.docgen.*` packages and **frontend** major `frontend/src/*`
   directories with purpose, key paths, and **forbidden cross-deps** (especially
   rendering isolated from lifecycle / authorization / API governance).
4. Keep map consistent with [module-boundaries.md](../architecture/module-boundaries.md)
   (capability ownership) without replacing that view’s boundary narrative.

---

## 3. Trigger

| # | Trigger |
| --- | --- |
| T1 | Agent must locate which module owns a change before writing code |
| T2 | Agent routes work to `backend-engineer` vs `frontend-engineer` vs `rendering-engineer` |
| T3 | Architecture / code-quality review asks about package coupling |
| T4 | New package or major frontend directory is added (map must be updated in same change set) |

**Non-triggers:** product user journeys; grep for a known symbol/path already in hand;
read-only Q&A that does not need module placement.

---

## 4. Preconditions

- `docs/architecture/module-map.md` exists and is indexed from `docs/README.md` /
  `docs/architecture/README.md` (G1 scaffold creates it if missing).
- Map rows cover current major packages under `backend/.../com/bank/docgen/` and major
  dirs under `frontend/src/`.
- Forbidden cross-deps explicitly restate: **rendering must not own lifecycle,
  authorization, or API-governance rules** (align tech-stack + module-boundaries).

---

## 5. Primary journey

1. Agent receives a delivery or audit request that requires module placement.
2. Agent opens `docs/architecture/module-map.md` (and boundaries view if needed).
3. Agent selects owning package/dir from the map.
4. Agent proceeds to specialist implementation or review **without** starting with an
   unscoped whole-repo file dump.
5. If the change adds/renames a major module path, the map is updated in the same change.

---

## 6. Confirmed decisions

### 6.1 Preference order (confirmed)

1. `docs/architecture/module-map.md` for **location / ownership** questions.
2. `docs/architecture/module-boundaries.md` for **capability Does/Does-not-own**.
3. Targeted path/symbol search **after** map narrows the scope.
4. Whole-repo unscoped grep is a **last resort**, not the default first step for
   module location.

### 6.2 Required map columns (confirmed)

Each backend package / frontend major dir row must include at least:

| Column | Meaning |
| --- | --- |
| **Module / path** | e.g. `com.bank.docgen.rendering`, `frontend/src/views` |
| **Purpose** | One-line ownership summary |
| **Key paths** | Representative entrypoints (service/package or view/composable roots) |
| **Forbidden cross-deps** | Explicit “must not depend on / must not own” notes |

### 6.3 Rendering isolation (confirmed)

Map **must** state that `rendering` stays isolated from lifecycle, authorization, and
API-governance logic (same rule as tech-stack guardrails / module-boundaries).

### 6.4 Drift policy (confirmed)

Adding or renaming a major `com.bank.docgen.*` package or `frontend/src/*` top-level
directory without updating the module map in the **same change set** is a documentation
defect (fail review / block Done for that leaf).

---

## 7. System responses

### Success

- Agent cites module map (path + row) when stating ownership.
- Implementation lands in the mapped package/dir.
- Map remains indexed and reachable from `docs/README.md`.

### Fail-closed / refuse

- Refuse to invent a second competing “module index” outside `module-map.md`.
- Refuse to treat ad-hoc README folders or random skill notes as ownership SoT when they
  contradict the map (surface conflict; update map or ask).
- Refuse claiming product E2E/deploy evidence for this retrieval contract.

---

## 8. Boundary and exception behavior

| Case | Expected behavior |
| --- | --- |
| Symbol already known (exact path in handoff) | May open that path directly; map still preferred for *new* placement |
| Map missing a brand-new package | Update map in same leaf **before** Done; do not leave orphan packages |
| Map vs boundaries conflict | Surface conflict; boundaries win for capability ownership; map updated to match |
| Deep defect hunt needing repo-wide scan | `explore` / targeted ripgrep allowed **after** map-scoped hypothesis |
| Product API/UI change | Out of scope for this BDD; full product pipeline applies |

---

## 9. Observable evidence

| Evidence | Form |
| --- | --- |
| Module map artifact | `docs/architecture/module-map.md` with required columns |
| Index reachability | Links from `docs/README.md` and architecture index |
| Agent routing notes | Handoff / review cites map row for ownership |
| Product runtime | **None required** |

---

## 10. Acceptance scenarios (Given / When / Then)

### MM-01 — Prefer map over whole-repo grep

**Given** an agent must locate which backend package owns template lifecycle transitions  
**And** `docs/architecture/module-map.md` exists  
**When** the agent starts retrieval  
**Then** it opens the module map first  
**And** does **not** begin with an unscoped whole-repo grep as the default step  
**And** selects the owning package from the map before coding.

### MM-02 — Map lists backend packages

**Given** packages exist under `com.bank.docgen.*` (e.g. `rendering`, `template`, `authorization`)  
**When** a reader opens the module map  
**Then** each major package appears with purpose, key paths, and forbidden cross-deps.

### MM-03 — Map lists frontend major dirs

**Given** major directories exist under `frontend/src/` (e.g. `views`, `composables`, `stores`)  
**When** a reader opens the module map  
**Then** each major dir appears with purpose and key paths.

### MM-04 — Rendering isolation stated

**Given** the module map documents `com.bank.docgen.rendering`  
**When** an agent reads forbidden cross-deps for rendering  
**Then** the map states rendering must not own lifecycle, authorization, or API-governance rules.

### MM-05 — New package updates map same change

**Given** a delivery leaf introduces a new major `com.bank.docgen.*` package  
**When** the leaf claims Done  
**Then** `module-map.md` includes the new package in the same change set  
**And** indexes remain valid.

### MM-06 — Specialist routing uses map

**Given** a change is clearly in the rendering worker boundary per the map  
**When** the parent routes stage 4  
**Then** it prefers `rendering-engineer` over a generic backend placement  
**And** cites the map (or boundaries) as the reason.

### MM-07 — Map does not replace boundaries view

**Given** an agent needs “Does Not Own” capability narrative  
**When** consulting docs  
**Then** it still uses `module-boundaries.md` for capability ownership  
**And** uses `module-map.md` for concrete path/package location  
**And** does not delete or fork boundaries into an unofficial third SoT.

### MM-08 — Product E2E N/A for this contract

**Given** only the module-map retrieval contract is in scope  
**When** gate planning runs for this governance leaf  
**Then** Playwright product E2E and Docker acceptance deploy are **N/A**  
**And** evidence is docs/index consistency.

---

## 11. Out of scope

- TemplateImport / i18n / mega-test code peels
- Product API or management UI behavior changes
- Automatic CI that fails builds solely because a package is “not yet on the map”
  (review/Done gate for the introducing leaf is enough for G1)

---

## 12. Traceability

| Source | Link |
| --- | --- |
| User confirmation | 2026-07-26 「按你的建议整改吧」 — AI-scale remediation G1 |
| Boundaries SoT | [module-boundaries.md](../architecture/module-boundaries.md) |
| AI reading path | [ai-development-guide.md](../architecture/ai-development-guide.md) |
| Sibling G1 behaviors | [lightweight-delivery-lane.md](./lightweight-delivery-lane.md), [ai-scale-docs-conventions.md](./ai-scale-docs-conventions.md) |
| Tech-stack isolation | `.cursor/rules/tech-stack-guardrails.mdc` |

```
bdd_readiness: ready
task_ids: [166]
open_questions: []
product_e2e_uiux_backend: not-applicable
frontend_ui_in_scope: false
```
