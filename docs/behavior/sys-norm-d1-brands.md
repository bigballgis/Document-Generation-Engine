---
id: DOC-BEHAVIOR-SYS-NORM-D1-BRANDS
type: Behavior Spec
status: Confirmed
readiness: ready
program: SYS-NORM
wave: 6
slice: sys-norm-d1-brands
taskMaster: "150"
related:
  - docs/adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/behavior/ibl-e4-entity-document-brands.md
  - docs/behavior/sys-norm-shell-fluid-nav.md
  - docs/adr/template-lifecycle/0065-legal-entity-document-brand-variants.md
  - docs/product/catalog-navigation-ux.md
  - docs/security/permission-matrix.md
---

# SYS-NORM Wave 6 — D1 DocumentBrand / LegalEntity runtime retirement (ADR-0071)

> **Slice:** `sys-norm-d1-brands` · TM **#150** → **Done** (MAIN merge `64b0a650`; worktree **REMOVED**).  
> **Historical placement:** branch `feat/sys-norm-d1-brands` · worktree `D:/working/DGE-sys-norm-d1-brands` · **ISOLATED**.  
> **Locks:** [ADR-0071 Accepted](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)
> (2026-07-21); charter [system-normalization-program.md](./system-normalization-program.md)
> §2.5 / §7; plan [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md)
> Wave 6 **Done**.  
> **Prior:** Wave 1 nav hide **Done** ([sys-norm-shell-fluid-nav.md](./sys-norm-shell-fluid-nav.md)
> **BDD-SYS-NORM-W1-003 / W1-004**); Waves 0–5 **Done**.  

> **Historical product surface:** [ibl-e4-entity-document-brands.md](./ibl-e4-entity-document-brands.md)
> / [ADR-0065](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md) —
> **superseded for ongoing product surface** by ADR-0071; historical impl evidence retained.  
> **Formal phase:** **None**.  
> **Do not:** flip checklist **#3b** / **#5a**; mark **#53** Done; claim SYS-NORM program Done;
> implement Wave 7 promotion pack / Wave 8 seed / parked UX items in this leaf.

```
bdd_readiness: ready
frontend_ui_in_scope: true
open_questions: []
pending_non_blocking: []
owning_doc: docs/behavior/sys-norm-d1-brands.md
task_ids: ["150"]
queue_slice_id: sys-norm-d1-brands
scenario_ids:
  - BDD-SYS-NORM-D1-001 … BDD-SYS-NORM-D1-020
scenario_count: 20
batch_recommendation:
  decision: solo
  member_task_ids: ["150"]
  proposed_slice_id: sys-norm-d1-brands
  shared_acceptance_surface: D1 brand/entity retirement
  vetoes_applied:
    - checklist-#3b/#5a
    - CE-O02
    - "#53"
    - Wave-7-promotion
    - Wave-8-seed
    - parked-reminder/asset/binding/refkey
```

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  rationale: >
    sole-active cleared; next program wave; ADR-0071 Accepted;
    different domain from parked UX items
  member_task_ids: ["150"]
  proposed_slice_id: sys-norm-d1-brands
  shared_acceptance_surface: D1 brand/entity retirement
  evidence_amortization: one verify+E2E+deploy for D1
  on_red_split_hint: >
    If runtime simplify fails, peel FE nav cleanup already done in Wave 1;
    keep BE retire atomic
```

| IN（本叶） | OUT（明确禁止 / 后续叶） |
| --- | --- |
| Hard retire Document brands + Legal entities **product surfaces** (routes, catalogs, management APIs, FE consumers) | Wave 7 promotion pack dry-run UI / full pack exporter |
| Runtime simplify: generate / preview / test-generation **no longer** resolve Letterhead/logo/seal via LegalEntity→DocumentBrand | Wave 8 demo seed / L1 terminology sweep |
| Letterhead/logo/seal governance via **Letterhead (master)** only | Parked reminder / asset isolation / binding editor / auto `referenceKey` |
| Keep **Legal holds** nav + behavior | Flip checklist **#3b** / **#5a** |
| Shell `REDBC` / `GREENBC` remain UI-only (regression) | Mark **#53** Done; claim SYS-NORM program Done |
| Durable hard delete (or equivalent irreversible retire) of brand/entity persistence | Reintroduce DocumentBrand/LegalEntity management UX |
| Promotion/export paths **must not require** brand/entity sidecar catalogs | CE-O02 / RTL / invent Word host evidence |
| Gates: BE verify + FE four gates + E2E/UIUX + queued deploy | Silent rewrite of ADR-0065 Decision body |

---

## 1. Actor / role

| Actor | Role / scope |
| --- | --- |
| Platform / group administrator (`GLOBAL_ADMIN` / `GROUP_ADMIN`) | Expects brand/entity catalogs gone; manages logo/seal via Letterhead (master); still uses Legal holds |
| Document author (`DOCUMENT_AUTHOR`) | Authors templates / letterheads without brand/entity MDM; no allow-list brand picker |
| API caller (published template credential) | generate / preview without DocumentBrand resolve; may still send opaque `context.legalEntityCode` |
| Template tester (`TEMPLATE_TESTER`) | test-generation same simplified resolve as runtime |
| Shell operator | Switches REDBC/GREENBC UI chrome only |
| Unauthorized / legacy catalog client | Fail-closed on retired management surfaces |

Group isolation and existing authorization fail-closed rules remain.

---

## 2. User goal

1. Complete ADR-0071 **D1** after Wave 1 nav hide: remove Document brands + Legal entities
   as **required product surfaces** (UI + management API + runtime catalog dependency).  
2. Drive document letterhead / logo / seal from **Letterhead (master)** assets and workflows.  
3. Keep **Legal holds**; keep shell themes UI-only and orthogonal.  
4. Ensure promotion/export dependency closure **does not require** brand/entity sidecar
   catalogs (Wave 7 owns dry-run UX).  
5. Provide implementable Given/When/Then for TDD Red (BE + FE) with
   `frontend_ui_in_scope=true`.

---

## 3. Trigger

- Wave 6 deployment / Flyway (or equivalent) retires brand/entity persistence and APIs.  
- Operator opens management shell after Wave 6 (nav already hid brands/entities in Wave 1).  
- Operator bookmarks a legacy brands/entities route.  
- Client calls DocumentBrand / LegalEntity management APIs.  
- Client calls generate / preview / test-generation with or without `context.legalEntityCode`.  
- Operator manages logo/seal via Letterhead (master).  
- Operator switches REDBC/GREENBC.  
- Export / dependency-closure path is inspected for brand/entity sidecar requirement.

---

## 4. Preconditions

- ADR-0071 **Accepted** (LOCKED); charter §2.5 / §7 unchanged.  
- Waves **0–5** Done; Wave 1 nav hide **Done** (W1-003 / W1-004 soft state).  
- This Wave 6 BDD = **`ready`** before TDD Red / production code.  
- Letterhead (master) management surfaces exist and remain in scope for logo/seal governance.  
- Legal holds surfaces remain in scope (not retired).  
- Formal phase **None**; checklist **#3b** / **#5a** untouched; **#53** not Done;
  SYS-NORM program **not** Done.

---

## 5. Primary journey

1. Implementers encode D1-001…020 as failing tests (TDD Red).  
2. Backend retires DocumentBrand / LegalEntity management APIs (fail-closed stable codes).  
3. Runtime / preview / test-generation stop LegalEntity→DocumentBrand catalog resolve;
   document brand slots use Letterhead (master) assets only.  
4. Durable migration hard-deletes (or irreversibly retires) brand/entity tables / seeds /
   `defaultLegalEntityCode` / template `allowedDocumentBrandCodes` enforcement.  
5. Frontend hard-removes brand/entity routes, catalogs, pickers, and API consumers;
   legacy deep links no longer serve product catalogs (closes W1-004 soft allowance).  
6. Legal holds + Letterhead (master) + shell theme switcher remain usable.  
7. E2E proves absence of brand/entity product surfaces, letterhead path still works,
   legal holds kept, shell themes orthogonal; queued deploy evidence recorded.

### 5.1 System responses (success)

| Surface | Response |
| --- | --- |
| Nav | Brands/entities remain absent (Wave 1); no reintroduction |
| Legacy FE routes | No product catalog (404 / honest gone / redirect **away** from dual-catalog UX) |
| Management APIs | Brand/entity CRUD + group default legal-entity APIs fail-closed (**404** or **410**) with stable codes |
| Runtime resolve | No DocumentBrand application from LegalEntity catalogs; letterhead/logo/seal from Letterhead (master) |
| `context.legalEntityCode` | Whitelist-accepted as opaque optional string; **non-driving** for brand resolve (see §6.2) |
| Template allow-list | `allowedDocumentBrandCodes` no longer gates generate |
| Legal holds | Unchanged / available |
| Shell themes | REDBC/GREENBC UI-only chrome unchanged |
| Promotion/export | No required brand/entity sidecar catalogs |

### 5.2 Confirmed cutover rules (Wave 6 locks — resolves charter P-Q3)

| ID | Rule |
| --- | --- |
| **D1-C1** | **Product surfaces retired:** Document brands + Legal entities are not required management catalogs, routes, or nav items going forward (ADR-0071 Decision 1). |
| **D1-C2** | **Wave 1 → Wave 6:** Wave 1 hid nav; Wave 6 **hard retires** routes/API/runtime (closes W1-004 soft “may still resolve”). |
| **D1-C3** | **Letterhead SoT:** Logo / seal / letterhead legal presentation assets are governed via **Letterhead (master)** flows — not DocumentBrand MDM. |
| **D1-C4** | **Runtime simplify:** generate / preview / test-generation **must not** look up LegalEntity → DocumentBrand catalogs to apply document brand slots. |
| **D1-C5** | **`legalEntityCode` transitional (non-driving):** Optional `context.legalEntityCode` remains ADR-0013 whitelist-accepted as opaque non-PII context when present; it **must not** trigger catalog resolve, **must not** 422 with `LEGAL_ENTITY_UNKNOWN` / `LEGAL_ENTITY_INACTIVE` / `DOCUMENT_BRAND_*` from retired catalogs, and **must not** change UI chrome. Omitted field remains valid. |
| **D1-C6** | **Management API fail-closed:** DocumentBrand / LegalEntity list/create/update/get and group `defaultLegalEntityCode` write/read product APIs return **404** or **410** with stable codes (e.g. `DOCUMENT_BRAND_SURFACE_RETIRED` / `LEGAL_ENTITY_SURFACE_RETIRED` — implement one family and document in OpenAPI). No silent empty success that implies catalogs still exist. |
| **D1-C7** | **Allow-list retired:** Template `allowedDocumentBrandCodes` is ignored for generate gates; FE brand allow-list editors removed; writes fail-closed or strip without resurrecting catalog UX (implement one; OpenAPI aligned). |
| **D1-C8** | **Durable hard delete:** Wave 6 removes brand/entity persistence (and related seeds / group default legal-entity binding) so catalogs cannot be listed after deploy. Prefer irreversible schema/data retire in the same leaf as API kill. |
| **D1-C9** | **Keep Legal holds** (ADR-0071 Decision 3). |
| **D1-C10** | **Shell themes orthogonal** (ADR-0071 Decision 4) — regression of W1/E4 chrome orthogonality. |
| **D1-C11** | **No brand/entity sidecar** required for promotion/export dependency closure (ADR-0071 Decision 5); Wave 7 owns dry-run UI. |
| **D1-C12** | **Supersession honesty:** Do not claim ADR-0065 “never happened”; product direction follows D1; historical IBL-E4 evidence retained. |
| **D1-C13** | **Governance vetoes:** Do not flip **#3b/#5a**; do not mark **#53** Done; do not claim SYS-NORM program Done; do not implement Wave 7/8/parked items in this leaf. |

### 5.3 Boundary / fail-closed

- Retired management surfaces: fail-closed (**404/410**), not 200 empty catalogs.  
- Runtime must not fall back to UI `REDBC`/`GREENBC` assets as document brand MDM.  
- Cross-group authorization remains fail-closed.  
- Removing Legal holds under D1 is **forbidden**.  
- Reintroducing DocumentBrand/LegalEntity management UX as a required surface is **forbidden**.

---

## 6. Acceptance scenarios

### 6.1 Charter D1-001…005 (expanded — IDs stable)

#### BDD-SYS-NORM-D1-001 — Product surfaces retired

**Given** ADR-0071 is Accepted and Wave 6 is deployed  
**When** an authorized operator uses management navigation and attempts to open Document brands
or Legal entities product surfaces  
**Then** those catalog routes are **absent** from nav (Wave 1) and **not** available as
product catalogs (Wave 6 hard retire)  
**And** operators manage letterhead / logo / seal via **Letterhead (master)** flows  
**And** Legal holds navigation remains available

#### BDD-SYS-NORM-D1-002 — Supersede ADR-0065 management UX requirement

**Given** ADR-0071 is Accepted  
**When** future delivery cites document-brand product direction  
**Then** new work follows D1 (letterhead-in-master)  
**And** does not reintroduce DocumentBrand/LegalEntity management UX as a required surface  
**And** ADR-0065 remains historical Accepted for IBL-E4 evidence with **product surface
superseded** by ADR-0071 (Decision body not silently rewritten)

#### BDD-SYS-NORM-D1-003 — Shell themes orthogonal

**Given** D1 retirement is in effect  
**When** an operator switches REDBC / GREENBC  
**Then** only management UI chrome changes  
**And** the switcher is not reintroduced as DocumentBrand MDM  
**And** generate/preview document assets are not driven by the UI theme codes

#### BDD-SYS-NORM-D1-004 — Runtime simplify fail-closed / letterhead resolve

**Given** Wave 6 runtime simplify is deployed  
**When** generate / preview / test-generation runs  
**Then** resolution **does not** depend on LegalEntity→DocumentBrand catalogs  
**And** letterhead / logo / seal application uses **Letterhead (master)** (and existing
non-catalog binding rules)  
**And** retired catalog error codes (`LEGAL_ENTITY_UNKNOWN`, `LEGAL_ENTITY_INACTIVE`,
`DOCUMENT_BRAND_INACTIVE`, `DOCUMENT_BRAND_NOT_ALLOWED`) are **not** produced from catalog
lookup  
**And** UI chrome assets are never used as a silent DocumentBrand substitute

#### BDD-SYS-NORM-D1-005 — Export packs omit brand/entity sidecar

**Given** promotion pack design (charter §2.7) and D1  
**When** export / dependency-closure behavior is evaluated after Wave 6  
**Then** the path **does not require** DocumentBrand/LegalEntity sidecar catalogs  
**And** letterhead/master dependency rules still follow two-phase P2 (no skip APPROVED)  
**And** Wave 7 dry-run UI remains **out of scope** for this leaf

### 6.2 FE hard retire (closes Wave 1 soft routes)

#### BDD-SYS-NORM-D1-006 — Legacy brand/entity routes hard-retired

**Given** Wave 1 nav hide already removed brands/entities from `navGroupsCatalog`  
**When** a user opens a bookmarked legacy Document brands or Legal entities path after Wave 6  
**Then** the product catalog UI is **not** served (404, honest gone page, or redirect to a
non-catalog surface such as Letterhead hub — implement one consistent pattern)  
**And** this **closes** Wave 1 soft allowance in **BDD-SYS-NORM-W1-004**

#### BDD-SYS-NORM-D1-007 — FE API consumers and pickers removed

**Given** Wave 6 frontend delivery  
**When** an operator uses template / group / letterhead management UI  
**Then** there is no DocumentBrand catalog page, LegalEntity catalog page, entity→brand
picker, or template `allowedDocumentBrandCodes` brand-list editor as a required surface  
**And** no FE module continues to call retired brand/entity management APIs for happy-path UX

#### BDD-SYS-NORM-D1-008 — N11 / N12 moot after hard retire

**Given** Wave 6 hard retire is complete  
**When** backlog items N11 (legal-entities routeKey) and N12 (brand list filters) are reviewed  
**Then** they are **moot** (no product surface left to polish)  
**And** Done evidence for Wave 6 does not claim them as separate feature work

### 6.3 Management API + durable delete

#### BDD-SYS-NORM-D1-009 — DocumentBrand management API fail-closed

**Given** Wave 6 is deployed  
**When** a client calls DocumentBrand list/create/update/get management endpoints  
**Then** the API rejects fail-closed with **404** or **410** and a stable retired-surface
error code  
**And** the response does **not** return a usable catalog payload implying the surface lives

#### BDD-SYS-NORM-D1-010 — LegalEntity / defaultLegalEntity management API fail-closed

**Given** Wave 6 is deployed  
**When** a client calls LegalEntity management endpoints or group `defaultLegalEntityCode`
product APIs  
**Then** the API rejects fail-closed with **404** or **410** and a stable retired-surface
error code  
**And** no new LegalEntity↔DocumentBrand binding can be persisted

#### BDD-SYS-NORM-D1-011 — Durable hard delete of brand/entity persistence

**Given** Wave 6 migration has run  
**When** persistence for DocumentBrand / LegalEntity (and related seeds /
`defaultLegalEntityCode` binding) is inspected  
**Then** catalogs are durably removed or irreversibly retired so they cannot be listed  
**And** re-deploy does not resurrect dual-catalog product surfaces without a new ADR

### 6.4 Runtime / context / allow-list

#### BDD-SYS-NORM-D1-012 — Omit legalEntityCode succeeds without catalog resolve

**Given** a callable published template version and Wave 6 simplify deployed  
**When** generate (or preview) **omits** `context.legalEntityCode`  
**Then** the call does **not** fail for missing DocumentBrand/LegalEntity catalogs  
**And** letterhead/logo/seal come from Letterhead (master) / existing non-catalog bindings  
**And** no `PLATFORM_DEFAULT` DocumentBrand catalog lookup is required

#### BDD-SYS-NORM-D1-013 — Present legalEntityCode is non-driving

**Given** Wave 6 simplify deployed  
**When** generate submits a non-blank `context.legalEntityCode` (any string that was formerly
a catalog code or unknown)  
**Then** the field is whitelist-accepted as opaque context (no `400` for the known field)  
**And** it does **not** drive DocumentBrand catalog resolve  
**And** it does **not** produce retired catalog 422 codes (`LEGAL_ENTITY_UNKNOWN`,
`LEGAL_ENTITY_INACTIVE`, `DOCUMENT_BRAND_INACTIVE`, `DOCUMENT_BRAND_NOT_ALLOWED`)  
**And** UI chrome is unchanged by the value

#### BDD-SYS-NORM-D1-014 — Template allowedDocumentBrandCodes no longer gates

**Given** a template still carries historical `allowedDocumentBrandCodes` metadata (if any)  
**When** generate / preview / test-generation runs after Wave 6  
**Then** brand allow-list catalog enforcement does **not** block the call  
**And** FE no longer presents brand allow-list as a required editor

#### BDD-SYS-NORM-D1-015 — Same simplify on preview and test-generation

**Given** Wave 6 runtime simplify  
**When** preview and template test-generation execute  
**Then** they share the **same** non-catalog letterhead/master resolve semantics as runtime
generate  
**And** failure codes for retired catalog paths are absent

### 6.5 Keep / orthogonal / letterhead

#### BDD-SYS-NORM-D1-016 — Legal holds remain

**Given** D1 Wave 6 retirement  
**When** an authorized operator opens Security / Legal holds  
**Then** Legal holds navigation and core hold behavior remain available  
**And** Legal holds are **not** removed or folded into brand/entity retirement

#### BDD-SYS-NORM-D1-017 — Letterhead (master) remains logo/seal governance path

**Given** D1 Wave 6 retirement  
**When** an authorized `DOCUMENT_AUTHOR` / admin manages letterhead logo or seal assets  
**Then** they do so via **Letterhead (master)** flows  
**And** they are not redirected into a DocumentBrand catalog as the required surface

### 6.6 Governance + i18n + evidence

#### BDD-SYS-NORM-D1-018 — Program governance locks (non-goals)

**Given** Wave 6 leaf `sys-norm-d1-brands`  
**When** the leaf is completed and evidence is recorded  
**Then** checklist **#3b** / **#5a** are **not** flipped to GO  
**And** umbrella **#53** is **not** marked Done  
**And** SYS-NORM program is **not** claimed Done (Waves 7–8 remain)  
**And** Wave 7 promotion pack, Wave 8 seed/terms, and parked UX items are **not**
implemented in this leaf

#### BDD-SYS-NORM-D1-019 — English-first i18n for retirement UX

**Given** Wave 6 adds or changes user-facing strings for gone routes / redirects / empty
honest states  
**When** those strings ship  
**Then** English is the primary base language (i18n English-first)  
**And** Bank OA visual language is preserved (no reintroduction of dual-catalog IA)

#### BDD-SYS-NORM-D1-020 — Gates and observable evidence

**Given** Wave 6 implementation claims Done  
**When** gates and evidence are reviewed  
**Then** backend `mvn verify` is green  
**And** frontend `lint` / `type-check` / `test` / `build` are green  
**And** Playwright functional + UIUX cover retired surfaces + legal holds kept + shell
theme orthogonality (as applicable)  
**And** queued Docker deploy evidence exists for the acceptance stack  
**And** plan/ledger/doc-sync reflect Wave 6 without claiming program Done

---

## 7. Observable evidence

| Evidence | Proves |
| --- | --- |
| Nav + route probes | Brands/entities absent; Legal holds present |
| API contract / integration tests | Management brand/entity APIs 404/410 retired codes |
| Runtime / preview / test tests | No LegalEntity→DocumentBrand resolve; letterhead/master path |
| Migration tests | Durable hard delete / irreversible retire |
| Playwright + UIUX | User-facing retirement; shell themes orthogonal |
| Deploy queue logs | Stage 5/10 acceptance stack |
| Plan / ledger | Wave 6 Done ≠ program Done; #3b/#5a/#53 untouched |

---

## 8. Traceability

| Source | Link / note |
| --- | --- |
| User / ADR lock | ADR-0071 Accepted 2026-07-21 (LOCKED) |
| Program charter | [system-normalization-program.md](./system-normalization-program.md) §2.5 / §7 |
| Program plan | [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) Wave 6 |
| Wave 1 nav hide | [sys-norm-shell-fluid-nav.md](./sys-norm-shell-fluid-nav.md) W1-003 / W1-004 |
| Historical IBL-E4 | [ibl-e4-entity-document-brands.md](./ibl-e4-entity-document-brands.md) (product surface superseded) |
| Historical ADR | [ADR-0065](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md) |
| Permissions | [permission-matrix.md](../security/permission-matrix.md) §5.3 — retire notes with Wave 6 impl |
| Context whitelist | ADR-0013 — `legalEntityCode` opaque non-driving after D1-C5 |
| Terminology | Letterhead / 母版 — [business-terminology-guide.md](../product/business-terminology-guide.md) |

---

## 9. Handoff — stage_done_definition (plan-orchestrator)

Wave 6 BDD stage is **done** when:

1. This file is **`bdd_readiness: ready`** with scenarios **D1-001…020**.  
2. Charter §7 / §8 point to this leaf as Wave 6 runtime SoT (stubs promoted).  
3. Plan Wave 6 row notes BDD **ready**; TM **#150** registered and activated (stage 2).  
4. `frontend_ui_in_scope=true` understood for FE + E2E owners.  
5. Next: **doc-keeper** then **backend-engineer** / **frontend-engineer** (one leaf;
   BE retire atomic; FE hard route/API consumer cleanup) **without** activating
   Wave 7/8/parked work.

**Not** required for BDD stage: production Java/Vue, `mvn verify` (impl stages).
