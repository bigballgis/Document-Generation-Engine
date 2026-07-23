# System Normalization Program (SYS-NORM) — 2026-07

| Field | Value |
| --- | --- |
| **Program ID** | `SYS-NORM` |
| **Created** | 2026-07-21 |
| **Status** | **Done** — Waves **0–8 Done** (TM **#152** Wave 8 merge `8aca145b` / feature `7df6c563`) |
| **Formal phase** | **None** (not a P-phase; tracked here + ledger) |
| **Wave 0 leaf** | TM **#143** · `sys-norm-charter` → **Done** (MAIN merge `f8e898ad` / feature `28d4abe1`; worktree **REMOVED**) |
| **Wave 1 leaf** | TM **#145** · `sys-norm-shell-fluid-nav` → **Done** (MAIN merge `7a62be44` / feature `f1594f2a` + e2e `ce2cb9f0`; worktree **REMOVED**). Handoff briefly cited `#144` — **#144** remains PTA Done; Wave 1 = **#145**. |
| **Wave 2 leaf** | TM **#146** · `sys-norm-hub-ia` → **Done** (MAIN merge `5d77db80` / feature `992f6822`; worktree **REMOVED**; branch `feat/sys-norm-hub-ia` deleted) |
| **Wave 3 leaf** | TM **#147** · `sys-norm-external-ops` → **Done** (MAIN merge `18a9e3b2` / feature `f21dda5e`; worktree **REMOVED**; origin/main **PUSHED**) |
| **Wave 4 leaf** | TM **#148** · `sys-norm-test-artifacts` → **Done** (docs-close; MAIN merge `dac9dcd9` / feature tip `5c71acc0`; product evidence **#144** PTA `ac36ecbc` / `6bc74ff1`; worktree **REMOVED**) |
| **Wave 5 leaf** | TM **#149** · `sys-norm-roles` → **Done** (MAIN merge `febb95b3`; worktree **REMOVED**) |
| **Wave 6 leaf** | TM **#150** · `sys-norm-d1-brands` → **Done** (MAIN merge `64b0a650`; worktree **REMOVED**) |
| **Wave 7 leaf** | TM **#151** · `sys-norm-promotion-pack` → **Done** (MAIN merge `11356c63` / feature `f795b04a`; worktree **REMOVED**) |
| **Wave 8 leaf** | TM **#152** · `sys-norm-demo-seed-terms` → **Done** (MAIN merge `8aca145b` / feature `7df6c563`; worktree **REMOVED**) |
| **Sole-active** | **cleared for SYS-NORM** — host sole-active also **cleared** for PQH Leaf 1 (**#159**+**#160** Done `ab382c02` / `ee0893fe`); next PQH queue **#161** pending ([post-queue-hardening-program-2026-07.md](./post-queue-hardening-program-2026-07.md)). Residual N18+L1 TM **#157**+**#158** → **Done** (`a4f59c4d` / `b54281b1`). Waves **0–8** remain **Done** (do **not** reopen) |
| **Batch (Wave 8)** | **solo** · `member_task_ids: ["152"]` · `proposed_slice_id: sys-norm-demo-seed-terms` · vetoes: checklist-#3b/#5a / CE-O02 / #53 / parked-UX-not-in-W8 — **closed** |
| **Batch (Wave 7)** | **solo** · `member_task_ids: ["151"]` · `proposed_slice_id: sys-norm-promotion-pack` · vetoes: checklist-#3b/#5a / CE-O02 / #53 / Wave-8 / parked UX — **closed** |
| **Batch (Wave 6)** | **solo** · `member_task_ids: ["150"]` · `proposed_slice_id: sys-norm-d1-brands` · vetoes: checklist-#3b/#5a / CE-O02 / #53 / Wave-7 / Wave-8 / parked UX — **closed** |
| **Batch (Wave 5)** | **solo** · `member_task_ids: ["149"]` · `proposed_slice_id: sys-norm-roles` — **closed** |
| **Batch (Wave 4)** | **split** · `member_task_ids: ["148"]` · `proposed_slice_id: sys-norm-test-artifacts` · vetoes: do-not-merge-wave4-with-roles / checklist-#3b/#5a / CE-O02 / #53 — **closed** |
| **Batch (Wave 3)** | **solo** · `member_task_ids: ["147"]` · `proposed_slice_id: sys-norm-external-ops` — **closed** |
| **Batch (Wave 2)** | **solo** · `member_task_ids: ["146"]` · `proposed_slice_id: sys-norm-hub-ia` — **closed** |
| **Batch (Wave 1)** | **solo** · `member_task_ids: ["145"]` · `proposed_slice_id: sys-norm-shell-fluid-nav` — **closed** |
| **Next queue head** | **empty under SYS-NORM** — N19–N20 / N22 residuals queued under **PQH** TM **#161** / **#162** (do **not** reopen Waves 0–8). Residual N18+L1 **closed** (**#157**+**#158** Done `a4f59c4d`); §4a parked UX queue **empty** |
| **Batch (§4a N18+L1)** | **merge** · `member_task_ids: ["157","158"]` · `proposed_slice_id: sys-norm-n18-role-l1` · vetoes: checklist-#3b/#5a / CE-O02 / #53 / #119-Word-host / #106-umbrella — **closed** |
| **Batch (§4a BEI)** | **merge** · `member_task_ids: ["155","156"]` · `proposed_slice_id: binding-editor-ia` · vetoes: checklist-#3b/#5a / CE-O02 / #53 / N18-unless-trivial — **closed** |
| **Batch (§4a ALGI)** | **solo** · `member_task_ids: ["154"]` · `proposed_slice_id: asset-library-group-isolation` · vetoes: Binding editor / Auto `referenceKey` / checklist-#3b/#5a / CE-O02 / #53 — **closed** |
| **Queue (Wave 8)** | BDD SoT [sys-norm-demo-seed-terms.md](../behavior/sys-norm-demo-seed-terms.md) **BDD-SYS-NORM-W8-001…018** — wave **Done**; detail [detail/sys-norm-demo-seed-terms.md](./detail/sys-norm-demo-seed-terms.md) |
| **Parked worktrees** | Prior hub WIP worktrees **removed** — do **not** revive or fold stale hub WIP |
| **CE umbrella** | TM **#53** remains **in-progress** registry-only — **not** this program's delivery leaf; **do not** mark **#53** Done |
| **Behavior SoT** | [system-normalization-program.md](../behavior/system-normalization-program.md) |
| **Wave 1 BDD** | [sys-norm-shell-fluid-nav.md](../behavior/sys-norm-shell-fluid-nav.md) (**ready** / delivered; **BDD-SYS-NORM-W1-001…016**) |
| **Wave 2 BDD** | [sys-norm-hub-ia.md](../behavior/sys-norm-hub-ia.md) (**ready** / delivered; **BDD-SYS-NORM-W2-001…018**) |
| **Wave 3 BDD** | [sys-norm-external-ops.md](../behavior/sys-norm-external-ops.md) (**ready** / delivered; **BDD-SYS-NORM-W3-001…018**; TM **#147** Done `18a9e3b2` / `f21dda5e`) |
| **Wave 4 BDD** | [sys-norm-test-artifacts.md](../behavior/sys-norm-test-artifacts.md) (**ready** / docs-close **Done**; **BDD-SYS-NORM-W4-001…010** → PTA; TM **#148** Done `dac9dcd9` / `5c71acc0`; product **#144** `ac36ecbc` / `6bc74ff1`) |
| **Wave 5 BDD** | [sys-norm-roles.md](../behavior/sys-norm-roles.md) (**ready** / delivered; **BDD-SYS-NORM-ROLE-001…018**; TM **#149** Done `febb95b3`) |
| **Wave 6 BDD** | [sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md) (**ready** / delivered; **BDD-SYS-NORM-D1-001…020**; TM **#150** Done `64b0a650`) |
| **Wave 7 BDD** | [sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md) (**ready** / delivered; **BDD-SYS-NORM-PP-001…020**; TM **#151** Done `11356c63` / `f795b04a`) |
| **Wave 8 BDD** | [sys-norm-demo-seed-terms.md](../behavior/sys-norm-demo-seed-terms.md) (**ready** / delivered; **BDD-SYS-NORM-W8-001…018**; TM **#152** Done `8aca145b` / `7df6c563`; **N18 deferred at exit**) |
| **Residual N18+L1 BDD** | [sys-norm-n18-role-l1.md](../behavior/sys-norm-n18-role-l1.md) (**ready**/shipped; **BDD-N18-L1-001…012**; TM **#157**+**#158** **Done** `a4f59c4d` / `b54281b1`; P-Q1 Confirmed delivered) |
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
**N18** Legal-hold actor EntityLink was **explicitly deferred** at Wave 1 exit (not claimed
Done then). Residual delivery now **Done** on leaf `sys-norm-n18-role-l1`
(TM **#157**+**#158** merge `a4f59c4d` / `b54281b1`; BDD [sys-norm-n18-role-l1.md](../behavior/sys-norm-n18-role-l1.md)
**BDD-N18-L1-001…012**) — does **not** reopen Wave 1 Done.

**Wave 2 mission (Done — #146):** Template (+ Master N14 parity) Hub IA — Properties
drawer; remove Overview/Dependencies/External access hub tabs; per-version Dependencies;
API jump model A (`/api/packages/:templateId/settings` shell); Dev blank-surface; locale
de-dupe; legacy apiAccess redirects. Delivered **2026-07-21** (`5d77db80` / `992f6822`).

**Wave 3 mission (Done — #147):** External services dashboard; separate invocation records
page; complete package API settings (`/api/packages/:templateId/settings`); External services
nav; redirects; group-scope fail-closed; honest empty/error. Delivered **2026-07-21**
(`18a9e3b2` / `f21dda5e`).

**Wave 4 mission (Done — #148 docs-close):** Published/history Testing durable artifacts —
§5.1 satisfied by prior PTA delivery **#144** (`ac36ecbc` / `6bc74ff1`). This leaf closed
the program registry + Wave 4 BDD stub only (`frontend_ui_in_scope=false`; gates **N/A** /
reuse #144).

**Wave 5 mission (Done — #149):** Implement ADR-0070 six-role compression — matrix
rewrite (doc-keeper **before** code) + migration + JWT/capabilities + FE role surfaces.
Delivered **2026-07-21** (`febb95b3`).

**Wave 6 mission (Done — #150):** ADR-0071 D1 runtime/management retirement —
retire DocumentBrand/LegalEntity product surfaces; letterhead/master owns logo/seal; keep
Legal holds; shell themes UI-only; runtime simplify fail-closed. Delivered **2026-07-21**
(`64b0a650`; BDD [sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md) **D1-001…020**).

**Wave 7 mission (Done — #151):** UAT→PROD promotion pack + dry-run UI per §2
promotion facts; extend CE-E01/E03. Delivered **2026-07-21** (`11356c63` / `f795b04a`;
BDD [sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md) **PP-001…020**).

**Wave 8 mission (Done — #152):** Asset Library honest empty + optional demo/验收
managed-asset seed; N23 docs; N13 legal-hold empty; L1 Letterhead/母版 (N16–N17); N15/N21
honest empties. Delivered **2026-07-22** (`8aca145b` / `7df6c563`; BDD
[sys-norm-demo-seed-terms.md](../behavior/sys-norm-demo-seed-terms.md) **W8-001…018**).
**N18** was deferred at Wave 8 exit (did not block program Done). Residual N18 +
`DOCUMENT_AUTHOR` L1 now **Done** on `sys-norm-n18-role-l1` (TM **#157**+**#158**
merge `a4f59c4d` / `b54281b1`; P-Q1 L1 Confirmed delivered). Program Waves **0–8 → Done**.

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
| **Roles (6-role compression)** | Keep: `TEMPLATE_TESTER`, `LEGAL_REVIEWER`, `AUDIT_ADMIN`, `GLOBAL_ADMIN`, `GROUP_ADMIN` (absorbs `TEMPLATE_APPROVER`). Merge: `TEMPLATE_APPROVER` → `GROUP_ADMIN` (privilege accept). Merge: `MASTER_DESIGNER` ∪ `TEMPLATE_AUTHOR` → `DOCUMENT_AUTHOR` (ID locked; **L1 display name Confirmed** EN **Document author** / ZH **文档作者** — P-Q1 closed & **delivered**; BDD-N18-L1-008…010; governance synced; runtime i18n on TM **#158** / `sys-norm-n18-role-l1` **Done** `a4f59c4d`). Wave 5 **Done** (TM **#149** `febb95b3`; BDD **ready** [sys-norm-roles.md](../behavior/sys-norm-roles.md)); matrix + catalog/migration/JWT/FE landed. See ADR-0070. |
| **Export / promotion pack** | Dependency closure; two-phase P2 masters (no skip APPROVED); no brand/entity sidecar; no secrets; import as DRAFT on PROD; re-test/re-approve/re-publish; dry-run UI in Wave 7. |
| **Terminology** | L1 EN **Letterhead** / ZH **母版**; purge user-facing Master mix on L1; API/L3 may keep `masterId`. Sweep **Done** = Wave 8 (TM **#152** `8aca145b`; SSOT [business-terminology-guide.md](../product/business-terminology-guide.md)). |
| **Other UX** | Dev editor blank whiteboard → redirects + honest empty; **asset library:** product default honest empty + optional demo/验收 managed seed (off in prod; [demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md)); **N23** `demo-images` ≠ Asset Library; N13/N15/N21 honest empties; Users Authorized groups → EntityLink; table Actions Edit/More alignment; clause locale metadata de-duplication. **N18 Done** (TM **#157**+**#158** / `sys-norm-n18-role-l1` `a4f59c4d` / `b54281b1`). Parked UX §4a Reminder/Asset/Binding/refKey → **Done**. |

**Pending (not confirmed — do not promote):** none for this residual leaf. P-Q1 L1 EN **Document author** / ZH **文档作者** **Confirmed** and **delivered** (BDD-N18-L1-008…010; ADR-0070 / matrix / terminology / domain synced; runtime i18n + Legal-hold EntityLink on TM **#157**+**#158** **Done** `a4f59c4d`). **P-Q4** promotion dry-run UX is **resolved** in [sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md) §5.1. Wave 1 nav-hide vs Wave 6 hard-delete cutover is **resolved** in [sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md) §5.2.

---

## 3. Wave table (0–8)

| Wave | TM | Slice id | Status | Focus | Evidence / gates |
| --- | --- | --- | --- | --- | --- |
| **0** | **#143** | `sys-norm-charter` | **Done** (`f8e898ad` / `28d4abe1`) | Program plan + ADR-0070/0071 + indexes; decision lock | Docs-only — gates N/A |
| **1** | **#145** | `sys-norm-shell-fluid-nav` | **Done** (`7a62be44` / `f1594f2a` / `ce2cb9f0`) | Fluid all pages; nav icons + contract; Security = audit + legal holds; brands/entities nav hide (ADR-0071); Edit/More; EntityLink N1/N2/N3; **N18 deferred at exit** (residual now `sys-norm-n18-role-l1`) | FE gates + E2E 7/7 + UIUX PASS + Stage 5/10 DEPLOY_OK |
| **2** | **#146** | `sys-norm-hub-ia` | **Done** (`5d77db80` / `992f6822`) | Template (+ Master parity) Hub Properties drawer; remove wrong tabs; version Dependencies; API jump model A; Dev blank-surface; locale de-dupe; legacy apiAccess | FE gates + E2E 8/8 + UIUX PASS + Stage 5/10 DEPLOY_OK |
| **3** | **#147** | `sys-norm-external-ops` | **Done** (`18a9e3b2` / `f21dda5e`) | External services dashboard; invocation records page; package API settings completion; nav; redirects | FE gates + E2E 10/10 + UIUX PASS + Stage 5/10 DEPLOY_OK |
| **4** | **#148** | `sys-norm-test-artifacts` | **Done** (docs-close; MAIN `dac9dcd9` / feature `5c71acc0`; product **#144** `ac36ecbc` / `6bc74ff1`; worktree **REMOVED**) | Published/history Testing durable DOCX/PDF; program registry close | Docs-only this leaf — reuse #144 E2E/deploy |
| **5** | **#149** | `sys-norm-roles` | **Done** (`febb95b3`) | Implement ADR-0070; FE role labels + migration + JWT (matrix rewrite stage 3) | BE **2357/0/0**; FE lint/type-check/test(**1639**)/build **GREEN**; E2E **7/7**; UIUX **PASS**; Stage 5/10 **DEPLOY_OK**; arch Critical=0 `merge_go`; CQ allow merge |
| **6** | **#150** | `sys-norm-d1-brands` | **Done** (`64b0a650`) | Runtime/management retirement per ADR-0071 | BE **2370** + Flyway **V76**; FE lint/type-check/test(**1634**)/build **GREEN**; E2E **16/16**; UIUX **PASS_WITH_NOTES** Critical=0; Arch/CQ **PASS_WITH_NOTES** Critical=0; Stage 5/10 **DEPLOY_OK** |
| **7** | **#151** | `sys-norm-promotion-pack` | **Done** (`11356c63` / `f795b04a`) | UAT→PROD pack + dry-run UI per §2 promotion facts | BE **2381**; FE lint/type-check/test(**~1640**)/build **GREEN**; E2E W7 **4/4** + P14 **2/2**; UIUX **PASS_WITH_NOTES** Critical=0; Arch/CQ **PASS_WITH_NOTES** Critical=0; Stage 5/10 **DEPLOY_OK** |
| **8** | **#152** | `sys-norm-demo-seed-terms` | **Done** (`8aca145b` / `7df6c563`) | Asset honest empty + optional demo/验收 seed; L1 Letterhead/母版; N13/N15/N16–N17/N21/N23; **N18 deferred at exit** (residual later **Done** `a4f59c4d`) | BE **2391**; FE lint/type-check/test(**1652**)/build **GREEN**; E2E W8 **5/5**; UIUX **PASS_WITH_NOTES** Critical=0; Arch **merge_with_notes** Critical=0; Stage 5/10 **DEPLOY_OK** |

Per-wave implementation BDD: Waves **1–8 ready/Done**
([sys-norm-demo-seed-terms.md](../behavior/sys-norm-demo-seed-terms.md)
**BDD-SYS-NORM-W8-001…018**; TM **#152** Done `8aca145b` / `7df6c563`; detail [detail/sys-norm-demo-seed-terms.md](./detail/sys-norm-demo-seed-terms.md)).
Prior waves: Wave 1
([sys-norm-shell-fluid-nav.md](../behavior/sys-norm-shell-fluid-nav.md)), Wave 2
([sys-norm-hub-ia.md](../behavior/sys-norm-hub-ia.md) **W2-001…018**), Wave 3
([sys-norm-external-ops.md](../behavior/sys-norm-external-ops.md) **W3-001…018**), Wave 4
([sys-norm-test-artifacts.md](../behavior/sys-norm-test-artifacts.md) **W4-001…010** → PTA),
Wave 5 ([sys-norm-roles.md](../behavior/sys-norm-roles.md) **ROLE-001…018**), Wave 6
([sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md) **D1-001…020**), Wave 7
([sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md) **PP-001…020**).

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
| N11 | Route key — legal-entities (moot after D1) | **1 Done** (nav hide) / **6 Done** (retire) |
| N12 | Filter toolbar — Brand list (moot after D1) | **1 Done** (nav hide) / **6 Done** |
| N13 | Seed — Legal hold empty catalog story | **8 Done** |
| N14 | Hub IA parity — Master hub same debt | **2 Done** |
| N15 | Empty design — Master revision empty design summary | **8 Done** |
| N16–N17 | Terminology — EN Master mix; EN/ZH L1 | **8 Done** |
| N18 | EntityLink — Legal hold actor | **Done** — TM **#157**+**#158** merge leaf `sys-norm-n18-role-l1` (`a4f59c4d` / `b54281b1`; BDD **ready**/shipped [sys-norm-n18-role-l1.md](../behavior/sys-norm-n18-role-l1.md) **BDD-N18-L1-001…012**; does **not** reopen Waves 0–8 Done) |
| N19–N20 | EntityLink — where-used; MasterImpact | **Queued under PQH** TM **#161** (Leaf 2; `pending` — do **not** reopen Waves 0–8) — [post-queue-hardening-program-2026-07.md](./post-queue-hardening-program-2026-07.md) |
| N21 | Journey — Role journey timeline silent empty | **8 Done** |
| N22 | Actions — Catalog row action pattern | **1** partial (Edit/More); catalog-wide remainder **queued under PQH** TM **#162** (Leaf 3; `pending`) — [post-queue-hardening-program-2026-07.md](./post-queue-hardening-program-2026-07.md) |
| N23 | Docs/seed — `demo-images` bypass vs managed asset | **8 Done** ([demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md) + seeder) |

---

## 4a. Post-program parked queue (serial deliver candidates)

SYS-NORM Waves **0–8** are **Done**. Remaining §4a items activate only via explicit deliver +
Batch Recommendation (one sole-active leaf at a time). Do **not** claim Parked items Done.
Do **not** fold into CE umbrella **#53**. Do **not** flip checklist **#3b** / **#5a**.
Do **not** activate CE-O02.

| Note | Theme | Status |
| --- | --- | --- |
| Reminder timing | System / Team settings IA for reminder timing | **Done** — TM **#153** `reminder-timing-settings-ia` (MAIN merge `d213834f` / feature `807d8213`; worktree **REMOVED**; Batch **solo** closed; BDD **ready**/shipped — [reminder-timing-settings-ia.md](../behavior/reminder-timing-settings-ia.md) **BDD-RT-IA-001…016**; detail [detail/reminder-timing-settings-ia.md](./detail/reminder-timing-settings-ia.md)) |
| Asset library group isolation | Group-scoped asset library isolation | **Done** — TM **#154** `asset-library-group-isolation` (MAIN merge `c12a0687` / feature `5b48117f`; worktree **REMOVED**; Batch **solo** closed; BDD **ready**/shipped — [asset-library-group-isolation.md](../behavior/asset-library-group-isolation.md) **BDD-ALGI-001…018**; migration **ALGI-M1**; CE-E02 §15 amendment; detail [detail/asset-library-group-isolation.md](./detail/asset-library-group-isolation.md); Gates BE **2400**/FE **1672**/E2E **6/6**/UIUX **PASS**/Arch **PASS_WITH_NOTES**/Stage 5+10 **DEPLOY_OK**) |
| Binding editor re-layout | Binding editor layout remedi | **Done** — TM **#155** merge leaf `binding-editor-ia` (MAIN merge `9f2378ad` / feature `9e318d9c`; worktree **REMOVED**; Batch **merge** closed; BDD **ready**/shipped — [binding-editor-ia.md](../behavior/binding-editor-ia.md) **BDD-BEI-001…020**; detail [detail/binding-editor-ia.md](./detail/binding-editor-ia.md); Gates FE **1697**/E2E **9/9**/UIUX **PASS_WITH_NOTES**/Arch **PASS_WITH_NOTES**/CQ Critical FIXED/Stage 5+10 **DEPLOY_OK**) |
| Auto `referenceKey` generation | Auto-generate `referenceKey` | **Done** — TM **#156** (merged under `binding-editor-ia` with **#155**; same merge SHAs / BDD / detail / gates) |

**Residual (not parked-UX; does not reopen program Waves 0–8 Done):** **N18** Legal-hold
actor EntityLink + **DOCUMENT_AUTHOR** L1 lock — TM **#157**+**#158** merge leaf
`sys-norm-n18-role-l1` → **Done** (MAIN merge `a4f59c4d` / feature `b54281b1`; worktree
**REMOVED**; Batch **merge** closed; BDD **ready**/shipped
[sys-norm-n18-role-l1.md](../behavior/sys-norm-n18-role-l1.md) **BDD-N18-L1-001…012**;
detail [detail/sys-norm-n18-role-l1.md](./detail/sys-norm-n18-role-l1.md); Gates FE **1710**/
E2E SYS-NORM-N18 **5/5**/UIUX **PASS_WITH_NOTES**/Arch **merge_with_notes**/Stage 5+10
**DEPLOY_OK**).

**Sole-active (host delivery leaf):** **cleared** — prior NON-CE **PQH** Leaf 1 TM
**#159**+**#160** `pqh-f8-format-date-tz` → **Done** (`ab382c02` / `ee0893fe`); next PQH
queue head **#161** N19–N20 **pending** (not activated) — see
[post-queue-hardening-program-2026-07.md](./post-queue-hardening-program-2026-07.md).
SYS-NORM Waves **0–8** remain **Done** (do **not** reopen as In Progress). Residual N18+L1
TM **#157**+**#158** → **Done** (`a4f59c4d` / `b54281b1`). N19–N20 / N22 queue under PQH
(**#161** / **#162** `pending`). Prior §4a Binding editor + Auto `referenceKey` TM
**#155**+**#156** → **Done** (`9f2378ad` / `9e318d9c`). §4a Asset library TM **#154** →
**Done** (`c12a0687` / `5b48117f`). SYS-NORM Wave 8 TM **#152** → **Done** `8aca145b` /
`7df6c563`. §4a Reminder timing TM **#153** → **Done** (`d213834f` / `807d8213`). Parked UX
queue **empty**. Do **not** fold into **#53**; do **not** flip **#3b** / **#5a**; do **not**
activate CE-O02 / **#119**.

---

## 5. Done criteria

### 5.1 Per wave

| Wave | Done when |
| --- | --- |
| **0** | Behavior charter + this plan + ADR-0070/0071 **Accepted** + indexes/ledger activation; Wave 0 leaf merged; **no** product code required |
| **1** | Fluid layout + nav/security IA + EntityLink/Actions targets green; FE gates + E2E; doc-sync — **met 2026-07-21** (`7a62be44`); N18 deferred at wave exit (residual later **Done** on `sys-norm-n18-role-l1` `a4f59c4d`) |
| **2** | Hub IA model A + Properties drawer + tab removals + version Dependencies; Master parity as scoped; E2E — **met 2026-07-21** (`5d77db80` / `992f6822`) |
| **3** | External services invocation page + package API settings surface; hub redirects |
| **4** | Published/history Testing downloads durable artifacts — **met 2026-07-21** via **#144** PTA (`ac36ecbc` / `6bc74ff1`); program close TM **#148** |
| **5** | Six-role catalog live; migration audited; matrix rewritten; ROLE BDD green — **met 2026-07-21** (`febb95b3`) |
| **6** | Brand/entity product surfaces + runtime simplify per ADR-0071; Legal holds kept — **met 2026-07-21** (`64b0a650`; BDD [sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md) **D1-001…020**; TM **#150**) |
| **7** | Promotion pack + dry-run UI per confirmed design facts — **met 2026-07-21** (`11356c63` / `f795b04a`; BDD [sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md) **PP-001…020**; TM **#151**) |
| **8** | Seed/honest empty + L1 Letterhead/母版 sweep + remaining N* closed or explicitly deferred with evidence — **met 2026-07-22** (`8aca145b` / `7df6c563`; BDD [sys-norm-demo-seed-terms.md](../behavior/sys-norm-demo-seed-terms.md) **W8-001…018**; TM **#152**; **N18 deferred at wave exit** → residual leaf `sys-norm-n18-role-l1` later **Done** `a4f59c4d` / `b54281b1`) |

### 5.2 Program Done

**Met 2026-07-22.** All Waves **0–8** **Done**; N1–N23 closed or explicitly deferred with
evidence at program close (**N18** was deferred then; residual **N18 + DOCUMENT_AUTHOR L1**
now **Done** on `sys-norm-n18-role-l1` `a4f59c4d` / `b54281b1` — does **not** reopen Waves
0–8 Done); ADR-0070/0071 still governing; P-Q1 L1 labels **Confirmed** and **delivered**;
checklist **#3b** / **#5a** **not** flipped by this program; **not** go-live; umbrella
**#53** remains in-progress registry-only.

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
| [sys-norm-test-artifacts.md](../behavior/sys-norm-test-artifacts.md) | Wave 4 BDD (**ready** / docs-close **Done**; TM **#148** `dac9dcd9` / `5c71acc0`; **BDD-SYS-NORM-W4-001…010** → PTA; product evidence **#144**) |
| [detail/sys-norm-test-artifacts.md](./detail/sys-norm-test-artifacts.md) | Wave 4 plan detail (docs-close) |
| [detail/sys-norm-roles.md](./detail/sys-norm-roles.md) | Wave 5 plan detail (**Done** · TM **#149** `febb95b3`) |
| [detail/sys-norm-d1-brands.md](./detail/sys-norm-d1-brands.md) | Wave 6 plan detail (**Done** · TM **#150** `64b0a650`) |
| [detail/sys-norm-promotion-pack.md](./detail/sys-norm-promotion-pack.md) | Wave 7 plan detail (**Done** · TM **#151** `11356c63` / `f795b04a`) |
| [detail/sys-norm-demo-seed-terms.md](./detail/sys-norm-demo-seed-terms.md) | Wave 8 plan detail (**Done** · TM **#152** `8aca145b` / `7df6c563`) |
| [sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md) | Wave 7 BDD **ready**/delivered — **BDD-SYS-NORM-PP-001…020** (TM **#151** Done `11356c63` / `f795b04a`) |
| [sys-norm-demo-seed-terms.md](../behavior/sys-norm-demo-seed-terms.md) | Wave 8 BDD **ready**/delivered — **BDD-SYS-NORM-W8-001…018** (TM **#152** Done `8aca145b` / `7df6c563`; N18 deferred at exit) |
| [sys-norm-n18-role-l1.md](../behavior/sys-norm-n18-role-l1.md) | Residual N18 + DOCUMENT_AUTHOR L1 BDD **ready**/shipped — **BDD-N18-L1-001…012** (TM **#157**+**#158** **Done** `a4f59c4d` / `b54281b1`) |
| [detail/sys-norm-n18-role-l1.md](./detail/sys-norm-n18-role-l1.md) | Residual N18 + L1 plan detail (**Done** · TM **#157**+**#158** `a4f59c4d` / `b54281b1`) |
| [published-template-test-artifacts.md](../behavior/published-template-test-artifacts.md) | Wave 4 product acceptance SoT (**BDD-PTA-001…009**; TM **#144** Done) |
| [0070-role-compression-six-roles.md](../adr/authorization-security/0070-role-compression-six-roles.md) | Role compression (Accepted) |
| [0071-retire-document-brand-legal-entity-surfaces.md](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md) | D1 retire product surfaces (Accepted) |
| [0065-legal-entity-document-brand-variants.md](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md) | Historical IBL-E4; product surface superseded by 0071 |
| [permission-matrix.md](../security/permission-matrix.md) | **Six-role rewrite + runtime catalog Done** (Wave 5 `febb95b3`); Wave 6 D1 §5.3 retire **Done** (TM **#150** `64b0a650`); nav-hide Wave 1 cross-ref retained |
| [sys-norm-roles.md](../behavior/sys-norm-roles.md) | Wave 5 BDD **ready**/delivered — **BDD-SYS-NORM-ROLE-001…018** (TM **#149** Done `febb95b3`) |
| [sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md) | Wave 6 BDD **ready**/delivered — **BDD-SYS-NORM-D1-001…020** (TM **#150** Done `64b0a650`) |
| [catalog-navigation-ux.md](../product/catalog-navigation-ux.md) | Hub IA intent + Wave 1 layout/nav-hide + Reminder timing System/Team IA (**#153 Done**) |
| [detail/reminder-timing-settings-ia.md](./detail/reminder-timing-settings-ia.md) | Post-program §4a Reminder timing leaf (**Done** · TM **#153** `d213834f` / `807d8213`) |
| [reminder-timing-settings-ia.md](../behavior/reminder-timing-settings-ia.md) | Reminder timing IA BDD **ready**/shipped — **BDD-RT-IA-001…016** |
| [detail/asset-library-group-isolation.md](./detail/asset-library-group-isolation.md) | Post-program §4a Asset library group isolation leaf (**Done** · TM **#154** `c12a0687` / `5b48117f`) |
| [asset-library-group-isolation.md](../behavior/asset-library-group-isolation.md) | Asset library group isolation BDD **ready**/shipped — **BDD-ALGI-001…018** |
| [detail/binding-editor-ia.md](./detail/binding-editor-ia.md) | Post-program §4a Binding editor + Auto `referenceKey` merge leaf (**Done** · TM **#155**+**#156** `9f2378ad` / `9e318d9c`) |
| [binding-editor-ia.md](../behavior/binding-editor-ia.md) | Binding editor IA + auto `referenceKey` BDD **ready**/shipped — **BDD-BEI-001…020** |
| [ce-e02-asset-library.md](../behavior/ce-e02-asset-library.md) | CE-E02 historical + §15 ALGI amendment |
| [business-terminology-guide.md](../product/business-terminology-guide.md) | L1 Letterhead/母版 + `DOCUMENT_AUTHOR` L1 Confirmed (P-Q1) |
| [execution-sync-ledger.md](./execution-sync-ledger.md) | Activation / evidence mirror |
| [docs/README.md](../README.md) | Index |
