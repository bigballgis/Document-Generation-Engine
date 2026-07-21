# System Normalization Program (SYS-NORM) — 2026-07

| Field | Value |
| --- | --- |
| **Program ID** | `SYS-NORM` |
| **Created** | 2026-07-21 |
| **Status** | **In Progress** — Waves **0–3 Done**; Waves **4–8 Not Started** |
| **Formal phase** | **None** (not a P-phase; tracked here + ledger) |
| **Wave 0 leaf** | TM **#143** · `sys-norm-charter` → **Done** (MAIN merge `f8e898ad` / feature `28d4abe1`; worktree **REMOVED**) |
| **Wave 1 leaf** | TM **#145** · `sys-norm-shell-fluid-nav` → **Done** (MAIN merge `7a62be44` / feature `f1594f2a` + e2e `ce2cb9f0`; worktree **REMOVED**). Handoff briefly cited `#144` — **#144** remains PTA Done; Wave 1 = **#145**. |
| **Wave 2 leaf** | TM **#146** · `sys-norm-hub-ia` → **Done** (MAIN merge `5d77db80` / feature `992f6822`; worktree **REMOVED**; branch `feat/sys-norm-hub-ia` deleted) |
| **Wave 3 leaf** | TM **#147** · `sys-norm-external-ops` → **Done** (MAIN merge `18a9e3b2` / feature `f21dda5e`; worktree **REMOVED**; origin/main **PUSHED**) |
| **Sole-active** | **cleared** (no SYS-NORM delivery leaf In Progress) |
| **Batch (Wave 3)** | **solo** · `member_task_ids: ["147"]` · `proposed_slice_id: sys-norm-external-ops` — **closed** |
| **Batch (Wave 2)** | **solo** · `member_task_ids: ["146"]` · `proposed_slice_id: sys-norm-hub-ia` — **closed** |
| **Batch (Wave 1)** | **solo** · `member_task_ids: ["145"]` · `proposed_slice_id: sys-norm-shell-fluid-nav` — **closed** |
| **Next queue head** | `sys-norm-test-artifacts` (Wave 4) — **Not Started** / **not** activated |
| **Queue (Waves 4–8)** | Program-plan **Not Started** only — **no** TM pending stubs (register next free TM id at each wave activation) |
| **Parked worktrees** | Prior hub WIP worktrees **removed** — do **not** revive or fold stale hub WIP |
| **CE umbrella** | TM **#53** remains **in-progress** registry-only — **not** this program's delivery leaf |
| **Behavior SoT** | [system-normalization-program.md](../behavior/system-normalization-program.md) |
| **Wave 1 BDD** | [sys-norm-shell-fluid-nav.md](../behavior/sys-norm-shell-fluid-nav.md) (**ready** / delivered; **BDD-SYS-NORM-W1-001…016**) |
| **Wave 2 BDD** | [sys-norm-hub-ia.md](../behavior/sys-norm-hub-ia.md) (**ready** / delivered; **BDD-SYS-NORM-W2-001…018**) |
| **Wave 3 BDD** | [sys-norm-external-ops.md](../behavior/sys-norm-external-ops.md) (**ready** / delivered; **BDD-SYS-NORM-W3-001…018**; TM **#147** Done `18a9e3b2` / `f21dda5e`) |
| **Role ADR** | [ADR-0070](../adr/authorization-security/0070-role-compression-six-roles.md) (**Accepted**) |
| **D1 ADR** | [ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md) (**Accepted**) |

---

## 1. North star / mission

Normalize the **whole management system** so layout, Template Package Hub IA, External
services, testing artifacts, navigation/governance (including D1 brand/entity retirement),
roles, UAT→PROD promotion, and L1 terminology are consistent with the **confirmed** remedi
decisions of **2026-07-21** — and fold similar issues **N1–N23** into the same serial wave
queue without reopening locked forks.

**Wave 0 mission:** durable program SoT + Accepted ADRs + index/activation notes.
**No** production code, `mvn`/`pnpm` gates, E2E, or deploy for the charter leaf itself.

**Wave 1 mission (Done):** system-wide fluid management layout; Security nav trim + D1
nav hide (ADR-0071); nav icon contract; Edit/More; EntityLink N1/N2/N3 (+ users groups).
**N18** Legal-hold actor EntityLink **explicitly deferred** (not claimed Done).

**Wave 2 mission (Done — #146):** Template (+ Master N14 parity) Hub IA — Properties
drawer; remove Overview/Dependencies/External access hub tabs; per-version Dependencies;
API jump model A (`/api/packages/:templateId/settings` shell); Dev blank-surface; locale
de-dupe; legacy apiAccess redirects. Delivered **2026-07-21** (`5d77db80` / `992f6822`).

**Wave 3 mission (Done — #147):** External services dashboard; separate invocation records
page; complete package API settings (`/api/packages/:templateId/settings`); External services
nav; redirects; group-scope fail-closed; honest empty/error. Delivered **2026-07-21**
(`18a9e3b2` / `f21dda5e`). Wave 4 testing artifacts remains **Not Started** / **not** activated.

---

## 2. Confirmed decisions (LOCKED — 2026-07-21)

> Do not reopen in delivery. Implementation waves realize these facts.

| Area | Confirmed decision |
| --- | --- |
| **Layout** | All management pages are **fluid** (system-wide). Prior catalog=fluid / detail=contained default for management `AppPageLayout` is superseded by this program. |
| **Hub IA** | Hub primary = **Version lines** only (fluid). **Properties** → right drawer (former Overview). **Remove** hub tabs: Overview, Dependencies, External access. **Dependencies** on per-version surfaces. **API model A:** package-level API settings SoT under External services; hub **API settings** jump; per-version perspective + deep-link; **forbidden** per-version ApiPolicy entities. |
| **External services** | Invocation records = **separate page** (dashboard-like). Package API settings = single edit surface (off hub External access tab). |
| **Testing artifacts** | On published + history Testing: download DOCX/PDF; durable `previewId` / artifact keys; read-only ≠ no download for authorized viewers. |
| **D1 brands/entities** | **Retire** Document brands + Legal entities **product surfaces**. Letterhead/logo/seal live in **Letterhead (master)**. Keep **Legal holds**. Shell REDBC/GREENBC remain UI-only. No brand/entity sidecar in promotion pack. Runtime simplify = Wave 6; nav removal may start Wave 1 after ADR-0071 Accepted. Supersedes ADR-0065 **product surface** (historical impl retained). |
| **Roles (6-role compression)** | Keep: `TEMPLATE_TESTER`, `LEGAL_REVIEWER`, `AUDIT_ADMIN`, `GLOBAL_ADMIN`, `GROUP_ADMIN` (absorbs `TEMPLATE_APPROVER`). Merge: `TEMPLATE_APPROVER` → `GROUP_ADMIN` (privilege accept). Merge: `MASTER_DESIGNER` ∪ `TEMPLATE_AUTHOR` → `DOCUMENT_AUTHOR` (ID locked; **L1 display name finalizable** — Pending). Matrix + BDD before code (Wave 5). See ADR-0070. |
| **Export / promotion pack** | Dependency closure; two-phase P2 masters (no skip APPROVED); no brand/entity sidecar; no secrets; import as DRAFT on PROD; re-test/re-approve/re-publish; dry-run UI in Wave 7. |
| **Terminology** | L1 EN **Letterhead** / ZH **母版**; purge user-facing Master mix on L1; API/L3 may keep `masterId`. Sweep Done = Wave 8 (intent Confirmed in Wave 0). |
| **Other UX** | Dev editor blank whiteboard → redirects + honest empty; asset library empty → seed or honest empty; Users Authorized groups → EntityLink; table Actions Edit/More alignment; clause locale metadata de-duplication. |

**Pending (not confirmed — do not promote):** see behavior charter §3 (e.g. `DOCUMENT_AUTHOR` L1 labels; Wave 1 nav-hide vs Wave 6 hard-delete cutover detail; promotion dry-run UX; per-capability matrix cells).

---

## 3. Wave table (0–8)

| Wave | TM | Slice id | Status | Focus | Evidence / gates |
| --- | --- | --- | --- | --- | --- |
| **0** | **#143** | `sys-norm-charter` | **Done** (`f8e898ad` / `28d4abe1`) | Program plan + ADR-0070/0071 + indexes; decision lock | Docs-only — gates N/A |
| **1** | **#145** | `sys-norm-shell-fluid-nav` | **Done** (`7a62be44` / `f1594f2a` / `ce2cb9f0`) | Fluid all pages; nav icons + contract; Security = audit + legal holds; brands/entities nav hide (ADR-0071); Edit/More; EntityLink N1/N2/N3; **N18 deferred** | FE gates + E2E 7/7 + UIUX PASS + Stage 5/10 DEPLOY_OK |
| **2** | **#146** | `sys-norm-hub-ia` | **Done** (`5d77db80` / `992f6822`) | Template (+ Master parity) Hub Properties drawer; remove wrong tabs; version Dependencies; API jump model A; Dev blank-surface; locale de-dupe; legacy apiAccess | FE gates + E2E 8/8 + UIUX PASS + Stage 5/10 DEPLOY_OK |
| **3** | **#147** | `sys-norm-external-ops` | **Done** (`18a9e3b2` / `f21dda5e`) | External services dashboard; invocation records page; package API settings completion; nav; redirects | FE gates + E2E 10/10 + UIUX PASS + Stage 5/10 DEPLOY_OK |
| **4** | *(register at activate)* | `sys-norm-test-artifacts` | **Not Started** | Batch test history handles; download on published/history Testing | BE/FE + E2E as scoped |
| **5** | *(register at activate)* | `sys-norm-roles` | **Not Started** | Implement ADR-0070; matrix rewrite + FE role labels + migration | Matrix + Wave 5 BDD **ready** before code |
| **6** | *(register at activate)* | `sys-norm-d1-brands` | **Not Started** | Runtime/management retirement per ADR-0071 | Wave 6 BDD **ready** before code |
| **7** | *(register at activate)* | `sys-norm-promotion-pack` | **Not Started** | UAT→PROD pack + dry-run UI per §2 promotion facts | Wave 7 BDD |
| **8** | *(register at activate)* | `sys-norm-demo-seed-terms` | **Not Started** | Asset seed / honest empty; L1 terminology sweep; remaining N* | Wave 8 BDD |

Per-wave implementation BDD stubs are **pending-wave** until authored at wave start
([behavior charter §8](../behavior/system-normalization-program.md)), except Wave 1
(**ready/Done** — [sys-norm-shell-fluid-nav.md](../behavior/sys-norm-shell-fluid-nav.md)),
Wave 2 (**ready/Done** — [sys-norm-hub-ia.md](../behavior/sys-norm-hub-ia.md)
**BDD-SYS-NORM-W2-001…018**; TM **#146** Done `5d77db80` / `992f6822`), and Wave 3
(**ready/Done** — [sys-norm-external-ops.md](../behavior/sys-norm-external-ops.md)
**BDD-SYS-NORM-W3-001…018**; TM **#147** Done `18a9e3b2` / `f21dda5e`).

---

## 4. Similar-issue backlog (N1–N23) → waves

| ID | Theme | Wave |
| --- | --- | --- |
| N1 | EntityLink — Task hub `entityName` / `groupCode` | **1 Done** |
| N2 | EntityLink — Catalog `groupCode` columns | **1 Done** |
| N3 | Actions — Users/Groups Edit+More | **1 Done** |
| N4 | Metadata dup — Template overview locale variants | **2 Done** |
| N5 | Metadata dup — Release detail status / lifecycle | **2 Done** |
| N6 | Dual surface — Legacy `#apiAccess` vs hub External access | **2 Done** (redirect to settings shell) / 3 (settings home panels) |
| N7–N9 | EntityLink / redundancy — Overview groupCode; hub header; External ID column | **2 Done** |
| N10 | Nav contract — every nav item maps to icon | **1 Done** |
| N11 | Route key — legal-entities (moot after D1) | **1 Done** (nav hide) / 6 (retire) |
| N12 | Filter toolbar — Brand list (moot after D1) | **1 Done** (nav hide) / 6 |
| N13 | Seed — Legal hold empty catalog story | 8 |
| N14 | Hub IA parity — Master hub same debt | **2 Done** |
| N15 | Empty design — Master revision empty design summary | 2 / 8 |
| N16–N17 | Terminology — EN Master mix; EN/ZH L1 | 8 |
| N18 | EntityLink — Legal hold actor | **Deferred** (explicit; not in Wave 1 Done claim) — later wave + BDD |
| N19–N20 | EntityLink — where-used; MasterImpact | 1 / 2 (residual) |
| N21 | Journey — Role journey timeline silent empty | 1 / 8 |
| N22 | Actions — Catalog row action pattern | **1** (partial via Edit/More primitive; catalog-wide pattern may continue) |
| N23 | Docs/seed — `demo-images` bypass vs managed asset | 8 |

---

## 5. Done criteria

### 5.1 Per wave

| Wave | Done when |
| --- | --- |
| **0** | Behavior charter + this plan + ADR-0070/0071 **Accepted** + indexes/ledger activation; Wave 0 leaf merged; **no** product code required |
| **1** | Fluid layout + nav/security IA + EntityLink/Actions targets green; FE gates + E2E; doc-sync — **met 2026-07-21** (`7a62be44`); N18 deferred with evidence |
| **2** | Hub IA model A + Properties drawer + tab removals + version Dependencies; Master parity as scoped; E2E — **met 2026-07-21** (`5d77db80` / `992f6822`) |
| **3** | External services invocation page + package API settings surface; hub redirects |
| **4** | Published/history Testing downloads durable artifacts |
| **5** | Six-role catalog live; migration audited; matrix rewritten; ROLE BDD green |
| **6** | Brand/entity product surfaces + runtime simplify per ADR-0071; Legal holds kept |
| **7** | Promotion pack + dry-run UI per confirmed design facts |
| **8** | Seed/honest empty + L1 Letterhead/母版 sweep + remaining N* closed or explicitly deferred with evidence |

### 5.2 Program Done

All Waves **0–8** **Done**; N1–N23 closed or explicitly deferred with evidence; ADR-0070/0071
still governing; checklist **#3b** / **#5a** **not** flipped by this program; **not** go-live.

---

## 6. Hard caps / serial single-lane

- **Batch Recommendation caps (prefer split):** ≤ **3** `member_task_ids` per leaf;
  ≤ **2** domains (e.g. FE+BE) per leaf; avoid leaves touching **>25** files — prefer
  further **split** over mega-merge.
- **Single-lane serial:** at most **one** delivery leaf heavy work
  (`mvn verify` / full FE gates / `docker-deploy-queue` / E2E) at a time on this host.
- Waves **0→8** execute **serially**; Batch Recommendation **split** — do **not** mega-merge
  FE/BE domains into Wave 0.
- Do **not** fold new work into an In Progress leaf; park sibling worktrees must not absorb
  this queue.
- Wave 0 is **docs-only** — no production backend/frontend code; runtime evidence **N/A**.
- Do **not** flip go-live checklist **#3b** / **#5a**.
- Role matrix rewrite = **Wave 5** only (Wave 0 records Confirmed intent + ADR).
- D1 runtime delete = **Wave 6**; FE nav hide may start **Wave 1** after ADR-0071 Accepted (**Wave 1 nav hide Done**).
- Do **not** invent NFR SLOs in this program.

---

## 7. Links

| Doc | Role |
| --- | --- |
| [system-normalization-program.md](../behavior/system-normalization-program.md) | Behavior / decision-acceptance SoT |
| [sys-norm-shell-fluid-nav.md](../behavior/sys-norm-shell-fluid-nav.md) | Wave 1 BDD (**ready** / delivered) |
| [sys-norm-hub-ia.md](../behavior/sys-norm-hub-ia.md) | Wave 2 BDD (**ready** / delivered) |
| [sys-norm-external-ops.md](../behavior/sys-norm-external-ops.md) | Wave 3 BDD (**ready** / delivered; TM **#147** Done `18a9e3b2` / `f21dda5e`) |
| [0070-role-compression-six-roles.md](../adr/authorization-security/0070-role-compression-six-roles.md) | Role compression (Accepted) |
| [0071-retire-document-brand-legal-entity-surfaces.md](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md) | D1 retire product surfaces (Accepted) |
| [0065-legal-entity-document-brand-variants.md](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md) | Historical IBL-E4; product surface superseded by 0071 |
| [permission-matrix.md](../security/permission-matrix.md) | Current 8-role baseline until Wave 5; Wave 1 ADR-0071 nav-hide cross-ref |
| [catalog-navigation-ux.md](../product/catalog-navigation-ux.md) | Hub IA intent + Wave 1 layout/nav-hide status |
| [business-terminology-guide.md](../product/business-terminology-guide.md) | L1 Letterhead/母版 intent |
| [execution-sync-ledger.md](./execution-sync-ledger.md) | Activation / evidence mirror |
| [docs/README.md](../README.md) | Index |
