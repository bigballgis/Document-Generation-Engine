# FOS — Frontline Operability & Solidity Program

| Field | Value |
| --- | --- |
| **Program ID** | `frontline-operability-solidity-2026-07` (short: **FOS**) |
| **Created** | 2026-07-26 |
| **Status** | **In Progress** (W1 Done; product remediation serial) |
| **Formal phase** | **None** (NON-CE remediation program — not a P-phase) |
| **Sole-active leaf** | **none** (cleared after FOS-W1 merge; next Batch may activate Leaf 2) |
| **Origin** | User-commissioned deep review (2026-07-26): make **existing** core maximally **易用** and **扎实** for a bank frontline employee who must orchestrate the most complex bank template and hand it off for other systems to call — **no new product features** |
| **Audit baseline commit** | `f29211c5` (MAIN tip when program authored; locate code by quoted snippets, not line numbers) |
| **Model note** | Program authored with Opus 5–class deep audits; detail sheets are written so a **lower-tier implementer** can execute TDD without improvisation |
| **Next queue head** | FOS **Leaf 8** `fos-concurrency-integrity` / TM **#178** → **Done** (`b6307d02` / feat `954933cc`); next **Leaf 9** `fos-contract-works-first-time` (TM **#179**) |
| **Upstream** | [CRCH](./core-render-compute-hardening-program.md) owns rendering P0 + preview dedupe (W0+W1) and compute/DOCX/harness designs (W2/W3/W5). FOS **supersedes CRCH W4** (authoring IA) — see §6 |

---

## 0. How to use this plan (READ THIS FIRST)

This plan is for an implementer with **no prior context**. Do not invent scope.

### 0.1 Execution order — strictly serial

```
CRCH W0+W1  (already planned — execute FIRST if still Not Started)
            detail/CRCH-W0-rendering-correctness.md
            + detail/CRCH-W1-preview-consolidation.md
            slice: render-p0-preview-dedupe

FOS Leaf 1  detail/FOS-W1-screens-tell-truth.md          ← start FOS here
FOS Leaf 2  detail/FOS-W2-author-can-start.md
FOS Leaf 3  detail/FOS-W3-authoring-blocks-work.md
FOS Leaf 4  detail/FOS-W4-gates-explain-themselves.md
FOS Leaf 5  detail/FOS-W5-time-locale-honesty.md
FOS Leaf 6  detail/FOS-W6-lifecycle-cannot-corrupt.md
FOS Leaf 7  detail/FOS-W7-generation-fails-closed.md
FOS Leaf 8  detail/FOS-W8-concurrency-integrity.md
FOS Leaf 9  detail/FOS-W9-contract-works-first-time.md
FOS Leaf 10 detail/FOS-W10-credential-lifecycle.md
FOS Leaf 11 detail/FOS-W11-policy-impact-troubleshoot.md
FOS Leaf 12 detail/FOS-W12-ci-gates-tell-truth.md
FOS Leaf 13 detail/FOS-W13-default-verify-honesty.md
FOS Leaf 14 detail/FOS-W14-demo-literacy-path.md
FOS Leaf 15 detail/FOS-W15-word-foundation-honesty.md
            + evidence: detail/fos-word-foundation-deep-review-2026-07.md
```

**Single-lane serial:** at most **one** FOS leaf In Progress. Do **not** fold a new leaf into an In Progress leaf. Do **not** auto-activate the next leaf after Done.

### 0.2 Non-negotiable house rules

| Rule | Detail |
| --- | --- |
| **No new features** | Fix, wire, label, fail-closed, align contract — do **not** invent capabilities (no new node types, no new API modes, no addressBlock / multi-doc package, no RTL). |
| **Worktree** | Every leaf: `../DGE-<slice-id>` on `feat/<slice-id>` from `origin/main`. Never implement on MAIN. |
| **TDD** | For every task: failing test first → observe red for the stated reason → smallest fix → green. |
| **No drive-by** | Change only files the task names. If another file must change, stop and report. |
| **English-first i18n** | Any user-facing string → both `frontend/src/locales/en.ts` and `zh-CN.ts`. Never hardcode display text. |
| **Bank OA lock** | Reuse existing tokens/patterns (`.cursor/skills/frontend-oa-design/SKILL.md`). |
| **Locate by snippet** | Line numbers drift; use the quoted code. |
| **delivery_lane** | Default **`full`** (UI / runtime / OpenAPI / Flyway). Only claim **`light`** when BDD proves E1–E5 of [lightweight-delivery-lane](../behavior/lightweight-delivery-lane.md). |

### 0.3 Gate commands (before Done)

```powershell
mvn -B -ntp -f backend/pom.xml verify
pnpm -C frontend lint
pnpm -C frontend type-check
pnpm -C frontend test
pnpm -C frontend build
.\scripts\docker-deploy-queue.ps1   # required when delivery_lane=full
```

### 0.4 If a "failing" test unexpectedly PASSES before you implement

**Stop. Do not invent a fix.** Report the task id, what you tried, and what you observed.

---

## 1. Why this program exists

Six independent Opus-class read-only audits (2026-07-26) walked the real journey:

> Dashboard → create template from approved letterhead → declare variables → author structured content
> (conditions / loops / clauses / tables) → bind → test data → preview → submit test → approve →
> publish → API policy → credential → hand a copyable contract to another system → that system
> generates and downloads → frontline troubleshoots via invocation records.

**Verdict:** the skeleton is strong (lifecycle, fidelity warnings, publish gate, OA shell, English-first
discipline). Failures concentrate in **seams** — labels that never ship, buttons that silently no-op,
fail-open generation, a handoff curl that uses the wrong auth scheme, and a green local gate that
does not match red CI.

This program does **not** add features. It makes the **existing** path usable by a non-IT bank
employee and fail-closed under real complexity.

---

## 2. Confirmed product decisions (user, 2026-07-26)

| # | Decision | Consequence |
| --- | --- | --- |
| D1 | **No new product features** | Remediations only: wire existing endpoints, fix labels, fail closed, align ADR/OpenAPI/code |
| D2 | Persona = **bank frontline employee (non-IT)** who must orchestrate the **most complex** template and hand it off | Every task is judged by that persona, not by developer convenience |
| D3 | Plan must be executable by a **lower-tier model** | Detail sheets use CRCH-W0 style: exact files, snippets, red-first tests, Do NOT lists |
| D4 | Merge plan into **existing** docs system | NON-CE program (like PQH / CRCH / AI-SCALE); formal phase stays **None** |
| D5 | **CRCH W4 superseded by FOS** | Authoring IA / guide-vs-tab / test-data dialog / path autocomplete live under FOS W2–W4; do not execute CRCH §7 independently |
| D6 | **CRCH W0+W1 stay first** | Rendering P0 + preview dedupe remain the upstream merged slice; FOS Leaf 1 starts after that slice is Done **or** when the host sole-active is free and the orchestrator chooses FOS first — **never parallel writers** |
| D7 | AI-SCALE peels **#167–#169** stay queued | Do **not** auto-activate; FOS W1 may **add** i18n keys but must **not** perform the `en.ts` mega-split (that is #168) |
| D8 | **OD-FOS-1 / CRCH OD-1 → 行内 (inline seals)** (user 2026-07-26) | Do **not** implement absolute/`CTAnchor` seal placement. CRCH must **re-scope** authorized-area BLOCKER validation so it does not imply a coordinate guarantee the renderer does not provide (see CRCH W0-7) |
| D9 | **OD-FOS-2 / CRCH OD-2 → PDF page-number stamping stays default OFF** (user 2026-07-26) | Do **not** flip `docgen.rendering.pdf-page-number-stamping-enabled` default to `true`. W0-4 still makes stamping correct when enabled |
| D10 | **OD-FOS-3 → `PENDING_RELEASE → DRAFT` non-destructive return NOT allowed** (user 2026-07-26) | FOS W6-8 is **out of scope / non-goal**. Stuck PENDING_RELEASE remediation remains `abandonInFlightDev` (destructive) — do not invent a return transition |
| D11 | **OD-FOS-4 → credential rotation grace = 28 days** (user 2026-07-26) | Amends **ADR-0009** (was 7 days). FOS W10-1 implements prior-hash retention + 28-day grace; OpenAPI/`rotationGracePeriodEndsAt` must match |

---

## 3. Open decisions — BLOCKED pending user input

**None.** OD-FOS-1…4 were answered by the user on 2026-07-26 (see D8–D11). Do not re-litigate.
If a future decision appears, add it here — do not invent product behaviour.

---

## 4. Wave overview

| Wave | Leaf / slice id | Name | Focus (audit ids) | Detail | Status |
| --- | --- | --- | --- | --- | --- |
| **W1** | `fos-screens-tell-truth` | Screens tell the truth | D1,D4,D5,D6,D7,D16,A4,A22,A23,A24 | [FOS-W1](detail/FOS-W1-screens-tell-truth.md) | **Done** |
| **W2** | `fos-author-can-start` | Author can start & navigate | A1,A2,A16,A17,A18,A21,A25,A26 | [FOS-W2](detail/FOS-W2-author-can-start.md) | **Done** |
| **W3** | `fos-authoring-blocks-work` | Authoring blocks work | A5,A6,A7,A9,A10,A11,A12 | [FOS-W3](detail/FOS-W3-authoring-blocks-work.md) | **Done** |
| **W4** | `fos-gates-explain-themselves` | Gates & actions explain themselves | A3,A8,A13,A14,A15,A19,A20,A27,D10,D12,D13,D14 | [FOS-W4](detail/FOS-W4-gates-explain-themselves.md) | **Done** |
| **W5** | `fos-time-locale-honesty` | Time & locale honesty | D2,D3,D9,D15,D17 | [FOS-W5](detail/FOS-W5-time-locale-honesty.md) | **Done** |
| **W6** | `fos-lifecycle-cannot-corrupt` | Lifecycle cannot corrupt | B1,B3,B4,B5,B6,B13,B19,B23 (B16 → **non-goal** per D10) | [FOS-W6](detail/FOS-W6-lifecycle-cannot-corrupt.md) | **Done** |
| **W7** | `fos-generation-fails-closed` | Generation fails closed | B2,B7,B8,B9,B10,B15 | [FOS-W7](detail/FOS-W7-generation-fails-closed.md) | **Done** |
| **W8** | `fos-concurrency-integrity` | Concurrency & integrity | B11,B12,B17,B18,B20 | [FOS-W8](detail/FOS-W8-concurrency-integrity.md) | **Done** |
| **W9** | `fos-contract-works-first-time` | Contract works first time | C1,C3,C5,C6,C16,C17,C18 | [FOS-W9](detail/FOS-W9-contract-works-first-time.md) | **Not Started** |
| **W10** | `fos-credential-lifecycle` | Credential lifecycle matches ADR | C2,C4,C11,C12,C22 + **28-day grace (D11)** | [FOS-W10](detail/FOS-W10-credential-lifecycle.md) | **Not Started** |
| **W11** | `fos-policy-impact-troubleshoot` | Policy impact & troubleshooting | C7,C8,C9,C10,C13,C14,C15,C20,C21 | [FOS-W11](detail/FOS-W11-policy-impact-troubleshoot.md) | **Not Started** |
| **W12** | `fos-ci-gates-tell-truth` | CI gates tell the truth | E1,E5,E14,E15 | [FOS-W12](detail/FOS-W12-ci-gates-tell-truth.md) | **Not Started** |
| **W13** | `fos-default-verify-honesty` | Default verify honesty | E2,E4,E6,E7,E8,E9,E10,E11 — **complements** CRCH W5 (do not duplicate LO pin / perceptual / soak) | [FOS-W13](detail/FOS-W13-default-verify-honesty.md) | **Not Started** |
| **W14** | `fos-demo-literacy-path` | Demo literacy path | E19,E20,E13 — docs + entry point only | [FOS-W14](detail/FOS-W14-demo-literacy-path.md) | **Not Started** |
| **W15** | `fos-word-foundation-honesty` | Word foundation honesty | WF-1…WF-8 — typography / letterhead / nested render honesty; evidence [deep review](detail/fos-word-foundation-deep-review-2026-07.md) | [FOS-W15](detail/FOS-W15-word-foundation-honesty.md) | **Not Started** |

Severity: **P0** = cannot complete job / wrong document / data loss / security fail-open / stuck state.
**P1** = tribal knowledge or undiagnosable failure. **P2** = polish.

---

## 5. Serial queue & Task Master map

| Leaf | TM | Slice id | Status | Notes |
| --- | --- | --- | --- | --- |
| Umbrella | **#170** | `fos-program` | **pending** | Registry only — never mark Done until program exit |
| Leaf 1 | **#171** | `fos-screens-tell-truth` | **done** | Merged `8dfdb0ba` / feat `22a4b4ba` |
| Leaf 2 | **#172** | `fos-author-can-start` | **done** | Merged `f5622e3f` / feat `f6d2903c` | |
| Leaf 3 | **#173** | `fos-authoring-blocks-work` | **done** | Merged `7418f2e4` / feat `f932a2a3` |
| Leaf 4 | **#174** | `fos-gates-explain-themselves` | **done** | `1960da96` / `17c4e396` |
| Leaf 5 | **#175** | `fos-time-locale-honesty` | **done** | `19f4f917` / `78a0bf8d` |
| Leaf 6 | **#176** | `fos-lifecycle-cannot-corrupt` | **done** | `23bd31d1` / `2b9a389d`; W6-8 non-goal (D10) |
| Leaf 7 | **#177** | `fos-generation-fails-closed` | **done** | `d251381f` / `c5244720`; deploy/E2E BLOCKED (0 images) |
| Leaf 8 | **#178** | `fos-concurrency-integrity` | **done** | `b6307d02` / `954933cc`; deploy/E2E BLOCKED (0 images) |
| Leaf 9 | **#179** | `fos-contract-works-first-time` | **pending** | |
| Leaf 10 | **#180** | `fos-credential-lifecycle` | **pending** | W10-1 unblocked — **28-day** grace (D11 / ADR-0009 amended) |
| Leaf 11 | **#181** | `fos-policy-impact-troubleshoot` | **pending** | |
| Leaf 12 | **#182** | `fos-ci-gates-tell-truth` | **pending** | May run earlier if host CI is blocking all delivery — orchestrator may promote |
| Leaf 13 | **#183** | `fos-default-verify-honesty` | **pending** | After / with CRCH W5 designs |
| Leaf 14 | **#184** | `fos-demo-literacy-path` | **pending** | |
| Leaf 15 | **#185** | `fos-word-foundation-honesty` | **pending** | Word-as-foundation deep review (2026-07-26); do **not** auto-activate |

**Host sole-active rule:** FOS shares the host with CRCH / AI-SCALE peels. At most **one** delivery writer. AI-SCALE **#167–#169** remain pending and are **not** FOS work.

---

## 6. Relation to other programs

| Program | Relation |
| --- | --- |
| **CRCH** | W0+W1 = upstream rendering/preview slice. **W4 superseded** by FOS W2–W4. W2/W3/W5 remain CRCH-owned. OD-1/OD-2 **resolved** (D8/D9) — CRCH owns seal-validation re-scope (W0-7) and keeps stamping default off. |
| **AI-SCALE** | Leaf 1 Done; peels #167–#169 pending — do not auto-activate; do not fold i18n mega-split into FOS W1 |
| **PQH / SYS-NORM / CDP / LRP waves** | **Done / closed** — do not reopen |
| **CE (#53)** | Registry-only umbrella — do not mark Done; do not treat as delivery leaf |
| **IBL (#106 / #119)** | Outside FOS; #119 stays Blocked; do not invent Word numbers |
| **CE-O02** | Deferred — do not activate |
| **ADR-0071 / 0068 / 0038 / 0070** | Non-goals — do not re-introduce DocumentBrand surfaces, RTL, SYNC_DOWNLOAD_URL, or expand beyond six roles |

---

## 7. Vetoes (hard)

| Veto | Rule |
| --- | --- |
| no-new-features | No new node types, API modes, portals, or CE-O02 |
| checklist-#3b/#5a-GO | Never flip launch checklist **#3b** / **#5a** to GO |
| mark-#53-CE-Done | Never mark umbrella **#53** Done |
| mark-#106-IBL-Done | Never mark umbrella **#106** Done |
| activate-#119-Word-host | Never activate **#119** without licensed Word host |
| invent-P-phase | Formal phase stays **None** |
| parallel-writer | Never second CE/FOS/CRCH writer while another leaf is In Progress |
| claim-go-live | Never claim production go-live / IBL Done / CE Done |
| invent-OD | OD-FOS-1…4 are **closed** (D8–D11). Do not re-open or invent contrary behaviour |
| no-PENDING_RELEASE-to-DRAFT | Per D10 — never add that transition |
| no-absolute-seals | Per D8 — never implement CTAnchor absolute seal placement in FOS/CRCH without a new user decision |
| no-stamp-default-on | Per D9 — never flip PDF page-number stamping default to true without a new user decision |
| duplicate-CRCH-W5 | Do not re-plan LO version pin, default-verify LO smoke, PDF→PNG lane, Word-vs-LO baseline, or conversion soak inside FOS — those stay CRCH W5 |
| ai-scale-peel | Do not perform TemplateImport / en.ts mega-split / mega-fixture peels inside FOS |

---

## 8. Audit evidence index (source of findings)

Findings were produced 2026-07-26 by six read-only audits and parent spot-checks against MAIN.
Ids are stable references used in detail sheets:

| Prefix | Stream | Count (reported) |
| --- | --- | --- |
| **A*** | Authoring journey usability | A1–A27 |
| **B*** | Template-orchestration solidity | B1–B23 |
| **C*** | Publish → API handoff | C1–C22 |
| **D*** | Cross-cutting UI / i18n / a11y / roles | D1–D24 |
| **E*** | Verification solidity / CI truth | E1–E21 |

Spot-checked by parent before plan commit: **A1** (create dialog filters store, no fetch), **A6**
(`insertBlockNode` blockTypes omits `contentModuleRef`), **B1** (`syncStoppedVersionsToPublished`
blanket STOPPED→PUBLISHED), **B2** (`resolvePinnedContentStructures` `ifPresent` drop), **C1**
(copyable curl uses `Authorization: Bearer`), **C2** (`rotateSecret` overwrites single hash),
**D2** (three datetime pickers append literal `Z`), **E1** (Constitution Gates red on `main`).

---

## 9. Exit criteria (program)

Program may move to **Done** only when:

1. Leaves 1–14 are **Done** or explicitly **Deferred** with user confirmation (and OD items resolved or carried).
2. A bank-persona walkthrough on the acceptance stack completes: create → complex author → publish → copy curl → consumer generate succeeds on first try → invocation record shows failure reason when forced to fail.
3. Plan indexes + ledger updated; sole-active cleared; vetoes still held.

---

## 10. Traceability

- CRCH: [core-render-compute-hardening-program.md](./core-render-compute-hardening-program.md)
- Light lane: [lightweight-delivery-lane.md](../behavior/lightweight-delivery-lane.md)
- Docs conventions: [ai-scale-docs-conventions.md](../behavior/ai-scale-docs-conventions.md)
- Module map: [module-map.md](../architecture/module-map.md)
- Credential ADR: [0009-api-credential-lifecycle.md](../adr/api-management/0009-api-credential-lifecycle.md)
- Error model: [0006-api-error-model.md](../adr/api/0006-api-error-model.md)
- Idempotency: [0004-api-idempotency-strategy.md](../adr/api/0004-api-idempotency-strategy.md)
- Compute DSL envelope (do not widen): [0056-whitelist-variable-compute-dsl-bounds.md](../adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md)
