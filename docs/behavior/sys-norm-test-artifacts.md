---
id: DOC-BEHAVIOR-SYS-NORM-TEST-ARTIFACTS
type: Behavior Spec
status: Confirmed
readiness: ready
program: SYS-NORM
wave: 4
slice: sys-norm-test-artifacts
taskMaster: "148"
related:
  - docs/behavior/published-template-test-artifacts.md
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/plan/detail/sys-norm-test-artifacts.md
  - docs/behavior/ce-u18-batch-test-history.md
  - docs/plan/detail/published-template-test-artifacts.md
---

# SYS-NORM Wave 4 — Published/history Testing durable artifacts (docs-close)

> **Leaf kind:** **docs-close** — product behavior already shipped by TM **#144**
> `published-template-test-artifacts` (MAIN `ac36ecbc` / feature `6bc74ff1`).  
> **Acceptance surface:** [published-template-test-artifacts.md](./published-template-test-artifacts.md)
> (**BDD-PTA-001…009**, `ready` / slice **Done**).  
> **Delivery evidence for §5.1 Wave 4** = **#144 PTA** (E2E TM144 **4/4** + UIUX **PASS** +
> Stage 5/10 **DEPLOY_OK**). This Wave 4 leaf closes the **program registry / BDD stub**
> only — **no** new product FE/BE residual.  
> **Locks:** Charter [system-normalization-program.md](./system-normalization-program.md) §2.4;
> plan [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md)
> §5.1 Wave 4.  
> **ADR:** **N/A** for this wave (no new architectural decision; reuse PTA / CE-U18 contracts).  
> **Formal phase:** **None**.  
> **Do not:** invent Download-on-BatchTestHistory-row beyond PTA; flip **#3b** / **#5a**;
> mark **#53** Done; claim SYS-NORM program Done; activate Wave 5 roles; reopen RTL;
> activate CE-O02.

```
bdd_readiness: ready
frontend_ui_in_scope: false
leaf_kind: docs-close
product_delivery_evidence: TM #144 published-template-test-artifacts (ac36ecbc / 6bc74ff1)
acceptance_sot: docs/behavior/published-template-test-artifacts.md
open_questions: []
owning_doc: docs/behavior/sys-norm-test-artifacts.md
task_ids: ["148"]
queue_slice_id: sys-norm-test-artifacts
scenario_ids:
  - BDD-SYS-NORM-W4-001 … BDD-SYS-NORM-W4-010
scenario_count: 10
pta_map: BDD-SYS-NORM-W4-001…009 → BDD-PTA-001…009 (1:1 by reference)
```

---

## 1. Actor / role

| Actor | Role |
| --- | --- |
| Template author / orchestrator (`authorTemplates`, readable snapshot) | On **PUBLISHED** (prefer **STOPPED** / **DEPRECATED**) release detail → **Testing**: list preview runs; download SUCCEEDED DOCX/PDF; Open preview from batch history when `previewId` present |
| Test decision-maker (`decideTests`, when readable) | Same read-only artifact review; no new write capabilities from Wave 4 |
| Unauthorized / cross-group session | Fail-closed — no history / download leak |
| Delivery / plan steward (this leaf) | Register Wave 4 BDD as `ready`; point program §5.1 at #144 PTA evidence; **no** production code in this leaf |

Actors and authorization semantics are **identical** to PTA §2 — this file does not widen
capability or group scope.

---

## 2. User goal

1. **Program §2.4 / §5.1 Wave 4:** On published/history Testing surfaces, operators can
   **download durable DOCX/PDF** test artifacts; history exposes `previewId` / artifact keys;
   **read-only ≠ no download** for authorized viewers.
2. **Wave 4 leaf goal (docs-close):** Persist Wave BDD as `ready` with explicit 1:1
   acceptance mapping to **BDD-PTA-***; record that product evidence is already **#144**;
   enable plan/registry close without inventing new product scope.

---

## 3. Trigger

| Trigger | Event |
| --- | --- |
| Product (already shipped) | User opens `/templates/{templateId}/releases/{releaseVersion}` → **Testing**; downloads SUCCEEDED preview artifacts; expands batch history → **Open preview** |
| Docs-close leaf | Orchestrator activates `sys-norm-test-artifacts` solely to author/register this BDD + plan/index sync |

---

## 4. Preconditions

- **#144 PTA** slice **Done** on MAIN (`ac36ecbc`); BDD SoT
  [published-template-test-artifacts.md](./published-template-test-artifacts.md) is **ready**.
- User (product path): logged-in management UI; readable snapshot on target template; release
  lifecycle PUBLISHED (or STOPPED/DEPRECATED on same release-detail Testing slot).
- This leaf: **ISOLATED** worktree `feat/sys-norm-test-artifacts`; **no** production FE/BE edits.

---

## 5. Primary journey

### 5.1 Product journey (acceptance = PTA — already delivered)

1. Open published release → **Testing**.
2. See batch test history + preview run history.
3. Download SUCCEEDED DOCX/PDF.
4. (Optional) Open preview from batch sample with `previewId`.
5. No re-enabled mutable authoring Testing (`showAuthoringSection` stays false for
   PUBLISHED/STOPPED/DEPRECATED).

Full steps / system responses: PTA §§7–8.

### 5.2 Wave 4 docs-close journey

1. Author this Wave BDD (`ready`) with W4↔PTA mapping.
2. Cross-link program plan / charter §8 / behavior index (minimal).
3. Hand off to plan-orchestrator / doc-keeper for TM register + registry status — **without**
   new product implementation.

---

## 6. System responses

| Path | Response |
| --- | --- |
| Product success / fail-closed | Per PTA §8 (preview history, downloads, Open preview, auth) |
| This docs-close leaf | Durable markdown SoT only; **no** runtime change |

---

## 7. Acceptance scenarios (Given / When / Then)

> **Normative product G/W/T** live in
> [published-template-test-artifacts.md](./published-template-test-artifacts.md).
> Wave 4 IDs below are the SYS-NORM program acceptance surface and **map 1:1** to PTA.
> Re-running product TDD Red for W4-001…009 is **not** required for this docs-close leaf;
> evidence = #144 PTA gates already green.

### BDD-SYS-NORM-W4-001 — PUBLISHED release Testing shows preview history

**Maps to:** [BDD-PTA-001](./published-template-test-artifacts.md#bdd-pta-001--published-release-testing-显示-preview-历史)

```gherkin
Given #144 PTA delivered TemplatePreviewRunHistoryPanel on release Testing
And the author can read the published template
When the author opens the release Testing Tab
Then preview run history and BatchTestHistoryPanel are visible
And mutable Run preview / Run full test are not re-enabled
And BDD-PTA-001 holds
```

### BDD-SYS-NORM-W4-002 — SUCCEEDED preview DOCX/PDF download

**Maps to:** [BDD-PTA-002](./published-template-test-artifacts.md#bdd-pta-002--succeeded-preview-可下载-docxpdf)

```gherkin
Given BDD-SYS-NORM-W4-001 and a SUCCEEDED preview with durable artifacts
When the author activates Download DOCX / Download PDF
Then the browser obtains the file under the same fail-closed gates as authoring Testing
And BDD-PTA-002 holds
```

### BDD-SYS-NORM-W4-003 — STOPPED/DEPRECATED same behavior (preferred)

**Maps to:** [BDD-PTA-003](./published-template-test-artifacts.md#bdd-pta-003--stoppeddeprecated-发布线同行为优选)

```gherkin
Given a STOPPED or DEPRECATED release on the same TemplateReleaseDetailView Testing slot
When the author opens Testing and downloads SUCCEEDED artifacts
Then behavior matches BDD-SYS-NORM-W4-001/002
And BDD-PTA-003 holds
```

### BDD-SYS-NORM-W4-004 — Async batch sampleResults persist previewId / keys

**Maps to:** [BDD-PTA-004](./published-template-test-artifacts.md#bdd-pta-004--异步批量-sampleresults-持久化-previewid--产物键)

```gherkin
Given an async full test produces at least one SUCCEEDED sample
When the system persists sampleResultsJson
Then successful samples include non-empty previewId and docxKey/pdfKey (or documented equivalents)
And BDD-PTA-004 holds
```

### BDD-SYS-NORM-W4-005 — Batch history Open preview wired

**Maps to:** [BDD-PTA-005](./published-template-test-artifacts.md#bdd-pta-005--批量历史-open-preview-接线可用)

```gherkin
Given release Testing mounts BatchTestHistoryPanel with a sample containing previewId
When the author activates Open preview
Then the matching preview history row is selected and download remains available when SUCCEEDED
And BDD-PTA-005 holds
```

### BDD-SYS-NORM-W4-006 — Open data set non-silent on release surface

**Maps to:** [BDD-PTA-006](./published-template-test-artifacts.md#bdd-pta-006--open-data-set-在-release-面不静默不打开编辑)

```gherkin
Given Open data set is visible in release Testing batch expand
When the author activates it
Then mutable Data sets / authoring Testing are not opened
And feedback is non-silent English-first when no read-only destination exists
And BDD-PTA-006 holds
```

### BDD-SYS-NORM-W4-007 — Fail-closed authorization unchanged

**Maps to:** [BDD-PTA-007](./published-template-test-artifacts.md#bdd-pta-007--fail-closed-授权不变)

```gherkin
Given a session that cannot read the template
When preview history, downloads, or batch history are requested
Then the API rejects (403/404) without cross-group leakage
And BDD-PTA-007 holds
```

### BDD-SYS-NORM-W4-008 — No new PUBLISHED download lifecycle gate

**Maps to:** [BDD-PTA-008](./published-template-test-artifacts.md#bdd-pta-008--不新增-published-下载生命周期阻断)

```gherkin
Given an entitled author and available SUCCEEDED artifacts on a PUBLISHED template
When the existing preview artifact download API is called
Then the request is not rejected by a Wave-4-invented PUBLISHED lifecycle rule
And BDD-PTA-008 holds
```

### BDD-SYS-NORM-W4-009 — PTA non-goals / completion constraints

**Maps to:** [BDD-PTA-009](./published-template-test-artifacts.md#bdd-pta-009--非目标与完成约束)

```gherkin
Given #144 PTA product delivery is complete
Then showAuthoringSection was not flipped true for PUBLISHED/STOPPED/DEPRECATED
And checklist #3b / #5a were not flipped
And RTL was not reopened
And CE-O02 was not claimed Done
And go-live was not claimed
And BDD-PTA-009 holds
```

### BDD-SYS-NORM-W4-010 — Wave 4 docs-close / program registry constraints

```gherkin
Given SYS-NORM §5.1 Wave 4 Done criteria ("Published/history Testing downloads durable artifacts")
And product evidence already exists from TM #144 PTA (MAIN ac36ecbc / feature 6bc74ff1)
When leaf sys-norm-test-artifacts closes as docs-close
Then this BDD file is readiness ready and maps W4-001…009 to BDD-PTA-001…009
And no new product FE/BE beyond PTA is required for Wave 4 acceptance
And ADR for this wave is N/A
And formal phase remains None
And #3b / #5a are not flipped
And #53 is not marked Done
And SYS-NORM program Done is not claimed
And Wave 5 roles are not activated or marked Done by this leaf
```

---

## 8. Boundary / exception

| Scene | Expectation |
| --- | --- |
| Product boundaries | Identical to PTA §10 (empty history, FAILED rows, cleaned artifacts, legacy samples without `previewId`, JSON damage, decideTests read-only) |
| Invented BatchTestHistory-row Download | **Out of scope** — do **not** add beyond PTA Open preview + preview-history downloads |
| Re-implement PTA in this leaf | **Forbidden** — evidence reuse only |
| Wave 5 / D1 / promotion / demo-seed | **Out of scope** |

---

## 9. Observable evidence

| Evidence | Source |
| --- | --- |
| Product UI / Network / API / Vitest / E2E / UIUX | **#144 PTA** — see PTA §11 (E2E **4/4**, UIUX **PASS**, Stage 5/10 **DEPLOY_OK**, `mvn verify` / FE gates as recorded on PTA Done) |
| Program close | This file `ready`; charter §8 Wave 4 points here; plan links Wave 4 BDD; **no** new product gate run required solely for docs-close |
| Non-goals | No #3b/#5a flip; #53 not Done; program not Done; formal phase None |

---

## 10. Traceability

| Source | Relation |
| --- | --- |
| Charter §2.4 Testing artifacts | Confirmed program decision realized by #144 |
| Plan §5.1 Wave 4 | Done when published/history Testing downloads durable artifacts — **satisfied by PTA** |
| [published-template-test-artifacts.md](./published-template-test-artifacts.md) | **Normative product acceptance** (BDD-PTA-001…009) |
| TM **#144** | Product delivery evidence (`ac36ecbc` / `6bc74ff1`) |
| [ce-u18-batch-test-history.md](./ce-u18-batch-test-history.md) | Batch expand / Open preview contract baseline |
| ADR | **N/A** this wave |
| Checklist #3b / #5a; #53; CE-O02; RTL; go-live | Explicit non-goals |

---

## 11. Explicit non-goals (Wave 4 leaf)

| Non-goal | Handling |
| --- | --- |
| New FE Download control on BatchTestHistory rows beyond PTA | **Forbidden** |
| Production FE/BE changes in `sys-norm-test-artifacts` worktree | **Forbidden** (docs-close) |
| Flip #3b / #5a | **Forbidden** |
| Mark #53 Done | **Forbidden** |
| Claim SYS-NORM program Done | **Forbidden** |
| Wave 5 roles / matrix rewrite | **Forbidden** |
| New ADR | **N/A** |
| Formal P-phase activation | **None** |

---

## 12. BDD readiness declaration

**bdd_readiness: ready**

- Scenario IDs: `BDD-SYS-NORM-W4-001` … `BDD-SYS-NORM-W4-010`
- Product acceptance: **by reference** to `BDD-PTA-001` … `BDD-PTA-009`
- Persisted: this file; minimal index/charter cross-links
- **open_questions: []**

**frontend_ui_in_scope:** `false` (docs-close leaf; product UI already evidenced under PTA)  
**owning_doc:** `docs/behavior/sys-norm-test-artifacts.md`  
**task_ids:** `["148"]` · queue `sys-norm-test-artifacts`
