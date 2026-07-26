# CRCH — Core Rendering & Compute Hardening Program

**Program id:** `CRCH` (Core Rendering & Compute Hardening)
**Type:** Ad-hoc NON-CE remediation program — **not** a formal P-phase, **not** an IBL/CE wave
**Formal plan phase:** **None** (single-active-phase discipline unaffected)
**Status:** **Not Started** (plan only — no code written)
**Created:** 2026-07-26
**Origin:** User-commissioned deep audit of AI-authored core functionality (template orchestration, rendering fidelity, compute formulas, PDF conversion)
**Audit baseline commit:** `df9a5b7d`

---

## 0. How to use this plan (READ THIS FIRST)

This plan is written to be executed by an implementer agent that has **no prior context**.
Everything needed is written down. Do not improvise.

### 0.1 Execution order — strictly sequential

```
W0  detail/CRCH-W0-rendering-correctness.md    ← START HERE (6 tasks)
W1  detail/CRCH-W1-preview-consolidation.md    ← then this (6 tasks)
W2  design in §5 of this file — NOT yet decomposed; needs a detail sheet first
W3  design in §6 — NOT yet decomposed
W4  design in §7 — NOT yet decomposed
W5  design in §8 — NOT yet decomposed
```

W0 and W1 were approved by the user to be delivered as **ONE merged slice**
(Batch Recommendation = `merge`, user-confirmed 2026-07-26).
Do **not** split them into two worktrees. Do **not** start W2–W5 without a new user instruction.

### 0.2 Non-negotiable house rules for the implementer

| Rule | Detail |
| --- | --- |
| **Worktree** | Create `../DGE-render-p0-preview-dedupe` on branch `feat/render-p0-preview-dedupe` from `origin/main`. Never implement on MAIN. |
| **TDD** | For EVERY task: write the failing test FIRST, run it, **confirm it fails for the stated reason**, then implement, then confirm green. |
| **No drive-by edits** | Change only the files each task names. If you believe another file must change, stop and report — do not expand scope silently. |
| **No new dependencies** | Everything below is implementable with libraries already on the classpath (Apache POI, PDFBox, Jackson, `javax.imageio` from the JDK). Adding a dependency requires user approval. |
| **English-first i18n** | Any user-facing string must be added to BOTH `frontend/src/locales/en.ts` and `frontend/src/locales/zh-CN.ts`. Never hardcode display text in a component. |
| **Line numbers are a hint, not an address** | All line numbers below are as of commit `df9a5b7d`. Always locate code by the quoted snippet, not by the line number alone. |

### 0.3 Gate commands (must all pass before Done)

```powershell
# Backend — always
mvn -B -ntp -f backend/pom.xml verify

# Backend — MANDATORY for this program because W0-5 / W0-6 touch LibreOffice.
# The default verify SKIPS real LibreOffice tests, which is exactly how these defects survived.
mvn -B -ntp -f backend/pom.xml verify -Plibreoffice-ci

# Frontend — always, all four
pnpm -C frontend lint
pnpm -C frontend type-check
pnpm -C frontend test
pnpm -C frontend build

# Docker acceptance stack — ALWAYS via the queue script. Never a second compose project.
.\scripts\docker-deploy-queue.ps1
```

### 0.4 If a test you write unexpectedly PASSES before you implement anything

That means the defect is not reproducible as described. **Stop. Do not invent a fix.**
Report which task, what you tried, and what you observed. A wrong "fix" for a
non-existent bug is worse than no fix.

---

## 1. Why this program exists

The user commissioned a deep review because this subsystem was authored primarily by AI and
they did not trust it. Four independent read-only audits were run, and every high-severity
finding below was then **personally re-verified against source** by the reviewing agent.

The verdict was that the **skeleton is good** — clear layering, circuit breakers, bounded
pools, metrics, a genuinely productised fidelity-warning + publish-gate design, and code
comments showing real awareness of LibreOffice concurrency pitfalls.

The failures are concentrated in **seams and boundary conditions**:

- an index computed against one collection and consumed against another,
- a process timeout that never kills the process,
- container paths that are unique for one resource and global for two others,
- a hardcoded constant that silently overrides an author's declared value,
- a warning that is raised but never consumed,
- a component nested inside a component that already contains it.

This is the characteristic failure signature of large-volume AI authorship: every module is
locally plausible, and nobody verified the joins. **That is why W5 (verification harness)
matters more than W0 (bug fixes).** W0 fixes the six defects we found. W5 is what catches
the seventh.

---

## 2. Confirmed product decisions (user, 2026-07-26)

These are **confirmed** per the document-as-code source-of-truth order
(latest explicit user confirmation ranks highest). Do not re-litigate them.

| # | Decision | Consequence |
| --- | --- | --- |
| D1 | Deliver **W0 + W1 merged as one slice** | One worktree, one gate run, one deploy evidence set |
| D2 | Compute DSL gets the **full extension** (arithmetic + row context + aggregation + logic) | Amends **ADR-0056**; scheduled as W2; see §5 |
| D3 | Money rounding becomes **configurable per render profile, default `HALF_UP`** | Part of W2; today `HALF_UP` is hardcoded and `HALF_EVEN` appears nowhere |

**D2 safety constraint (non-negotiable):** the existing security envelope must survive the
extension — whitelist-only evaluation, no scripting engine, and the caps in `ComputeDslLimits`
(expression length 2048, nesting depth 8, path segments 16, dependency depth 8, collection
size 10 000). This envelope is the single best-designed part of the compute subsystem.
Extending capability must not widen the attack surface.

---

## 3. Open decisions — BLOCKED pending user input

Do not implement these. Surface them and wait.

### OD-1 — Seal placement is validated but never rendered (compliance gap)

`ReferenceNodeService.validateSealPlacementGeometry` enforces, as a **BLOCKER**, that a seal's
`placement.sealBox` (`pageIndex`, `xPt`, `yPt`, optional `widthPt`/`heightPt`) lies fully
inside a declared `authorizedSealAreas[]` region. Three distinct fidelity warning codes exist
for violations (`SEAL_OUTSIDE_AUTHORIZED_AREA`, `SEAL_AUTHORIZED_AREA_UNKNOWN`,
`SEAL_AUTHORIZED_AREA_INVALID`).

The rendering package contains **zero** references to `placement`, `CTAnchor`, `positionH`, or
`positionV`. `StructuredContentDocxInlineSupport.writeReferenceNode` writes the seal as an
**inline run** at the anchor's position, wherever that happens to be on the page.

So the "seal must fall inside its authorized area" control is enforced against a coordinate
model that the renderer does not implement. For a bank this is a paper-only control.

**Question for the user:** is inline seal placement acceptable (in which case the authorized-area
validation should be re-scoped or removed so it stops implying a guarantee it does not provide),
or must the renderer implement absolutely-positioned floating seals honouring `sealBox`?

The second option is a substantial piece of OOXML work (`CTAnchor` drawing with
`positionH`/`positionV` relative to page, plus per-page targeting) and must be its own slice.

**W0-3 deliberately does NOT resolve this.** It only stops the renderer from silently ignoring
the author's declared `widthPt`/`heightPt`. Positioning remains inline until OD-1 is answered.

### OD-2 — Should PDF page-number stamping be ON by default?

`docgen.rendering.pdf-page-number-stamping-enabled` defaults to `false`, yet the stamper exists
precisely because headless LibreOffice frequently fails to evaluate Word `PAGE` fields. Bank
letter masters that rely on footer `PAGE` / `SECTIONPAGES` fields may therefore ship PDFs with
missing or wrong page numbers under the default configuration.

Turning it on globally is a behaviour change across every existing render profile, so it needs
an explicit decision. W0-4 makes the stamping **correct or honestly degraded** but does not
change the default.

---

## 4. Wave overview

| Wave | Name | Tasks | Detail sheet | Status |
| --- | --- | --- | --- | --- |
| **W0** | Rendering & conversion correctness | 6 | [CRCH-W0-rendering-correctness.md](detail/CRCH-W0-rendering-correctness.md) | **Not Started** |
| **W1** | Preview consolidation | 6 | [CRCH-W1-preview-consolidation.md](detail/CRCH-W1-preview-consolidation.md) | **Not Started** |
| **W2** | Compute DSL capability | ~6 | not yet written — see §5 | **Not Started** |
| **W3** | DOCX fidelity depth | ~6 | not yet written — see §6 | **Not Started** |
| **W4** | Authoring information architecture | ~4 | not yet written — see §7 | **Not Started** |
| **W5** | Fidelity verification harness | ~5 | not yet written — see §8 | **Not Started** |

Severity legend used throughout: **P0** = produces a wrong document, a wrong recipient, or a
crash. **P1** = capability or usability gap that blocks real work. **P2** = quality debt.

---

## 5. W2 — Compute DSL capability (design; needs a detail sheet before execution)

**Approved by D2. Amends ADR-0056. Do not start without a detail sheet and a fresh BDD spec.**

### 5.1 Current state (verified)

`ComputeExpressionParser` accepts exactly three expression forms: a literal, a `${path}`
variable reference, and a call to one of eight whitelisted functions
(`COALESCE`, `SUM`, `COUNT`, `AVG`, `FILTER`, `FORMAT_AMOUNT`, `FORMAT_DATE`, `SPELL_AMOUNT`).

There is **no infix operator handling anywhere in the parser**. The following cannot be
expressed today:

| Business need | Today |
| --- | --- |
| `amount = quantity × unitPrice` | impossible |
| `tax = amount × taxRate` | impossible |
| `share = item ÷ total` | impossible |
| `total = subtotal + freight` | impossible |
| per-row computed column in a table loop | impossible — compute runs once, before assembly |
| subtotal by category | impossible |
| running total | impossible |

Document-level column aggregation **does** work, but only through a counter-intuitive idiom:
`FILTER` doubles as a projection operator, so summing a column is written
`SUM(FILTER(${items}, amount, GT, 0))`. There is no `MAP`, and `SUM(${items.amount})` fails
because path resolution refuses to walk a field across a list.

### 5.2 Scope

| Id | Item | Notes |
| --- | --- | --- |
| W2-1 | Infix arithmetic `+ - * /` with parentheses and standard precedence | All evaluation in `BigDecimal`. Division by zero must fail closed with a distinct message. |
| W2-2 | Row-level evaluation context | Recompute `COMPUTED` variables per loop-row iteration; expose `rowIndex` (1-based) and `rowCount`. Today compute completes in `DocumentGenerationAssemblySupport` **before** `StructuredContentDocxWriteSession` runs its loop, so this is a change to evaluation *timing*, not just grammar — the riskiest part of W2. |
| W2-3 | Aggregation completion | `MIN`, `MAX`, `MAP` (proper projection, so `FILTER` stops being abused), `RUNNING_TOTAL`, `GROUP_SUM`, `PERCENT_OF` |
| W2-4 | Logic functions | `IF`, `AND`, `OR`, `NOT` |
| W2-5 | Rounding mode configurable per render profile, default `HALF_UP` (**D3**) | Touches `AVG` (currently fixed `HALF_UP` scale 10), `SpellAmountCn`, `SpellAmountEnUsd` |
| W2-6 | Function catalogue API + real editor | Server-published catalogue (name, arity, argument kinds, description, example) consumed by the frontend for autocomplete and inline validation. **Delete** the hardcoded client-side whitelist in `frontend/src/utils/computeExpressionValidate.ts` — it has already drifted from the server and accepts expressions the server rejects. |

### 5.3 Precision defects to fix inside W2

| Defect | Location | Fix |
| --- | --- | --- |
| Inbound `Double`/`Float` money loses precision before reaching `BigDecimal` | `ComputeExpressionEvaluator.toNumber` uses `number.toString()` | Use `BigDecimal.valueOf(double)` semantics deliberately, and document the JSON binding contract for monetary fields |
| `FORMAT_AMOUNT` unary form forces 2 fraction digits regardless of currency | `ComputeExpressionEvaluator` ~L203-207 | Use the ISO 4217 default fraction digits, as the binary form already does (JPY = 0) |
| `AVG` hardcodes scale 10 / `HALF_UP` | `ComputeExpressionEvaluator` ~L146 | Honour the configured rounding mode from W2-5 |

### 5.4 W2 acceptance

- Every new operator and function has unit tests including boundary and failure cases.
- A golden-corpus theme proves a per-row computed column plus a document total in a real DOCX.
- Security tests prove the caps in `ComputeDslLimits` still hold under the new grammar
  (in particular: deep nesting via arithmetic must not bypass the depth cap).
- `ADR-0056` is amended (not replaced) and its status history records the extension.

---

## 6. W3 — DOCX fidelity depth (design)

Framing that must be agreed before work starts: this engine implements
**"write structured content into a master shell"**, not **"mail-merge that preserves whatever
formatting sat on the placeholder"**. That is a coherent choice for bank-controlled typography,
but it is not what most people mean by "format fidelity", and the gap should be an explicit
product position rather than an accident.

| Id | Item | Current state |
| --- | --- | --- |
| W3-1 | Preserve run properties on anchor replacement | `StructuredContentDocxWriter.clearParagraph` deletes all runs; the placeholder's `rPr` is never cloned as a baseline |
| W3-2 | Clone table geometry from a master template row | Tables are built fresh via `insertNewTbl`; `vMerge`, `gridSpan`, borders and column widths are all lost. `PARTIAL_TABLE_LAYOUT_ADJUSTMENT` exists in the enum but the writer never emits it |
| W3-3 | Support SDT content controls, and make bookmarks fillable | `DocxAnchorExtractor` discovers `anchor.*` bookmarks; the fill path matches only `{{anchor:id}}` in paragraph text, so bookmark-only masters are silently skipped |
| W3-4 | Explicit style-collision policy | `DocxMasterStyleRegistry` does `if (styles.styleExist(styleId)) return;` — a same-named master style silently suppresses the catalogue's typography |
| W3-5 | Wire assembly warnings into the generation path | `DocxAssembler.lastAssemblyFidelityWarnings` and `StructuredContentDocxStyleSupport.fidelityWarningCodes` are populated but never read by `DocumentGenerationAssemblySupport` |
| W3-6 | Upgrade output validation | `OoxmlOutputValidator` checks XML well-formedness only, not ECMA-376 schema conformance |

---

## 7. W4 — Authoring information architecture (design)

| Id | Item | Current state |
| --- | --- | --- |
| W4-1 | Flatten navigation | An author simultaneously tracks a lifecycle stepper, an optional authoring-path guide, a workspace tab, and a sub-tab — four concurrent position indicators, up to five levels deep when editing a binding |
| W4-2 | Reconcile guide order with tab order | The path guide sequences `master → bindings → variables → preview` while Design defaults to landing on `bindings` and lists `variables` first |
| W4-3 | Split the test-data dialog | `TemplateTestDataSetEditDialog.vue` is 708 lines and still falls back to hand-written JSON for `LIST`/`OBJECT` fields |
| W4-4 | Variable-path autocomplete | Only `ConditionExpressionInput` offers `${...}` completion; nested JSON paths in test data have none |

---

## 8. W5 — Fidelity verification harness (design) — **highest long-term value**

This wave is the direct answer to "it was written by AI and I do not trust it". W0 fixes six
known defects; W5 is what finds the next six.

| Id | Item | Rationale |
| --- | --- | --- |
| W5-1 | Pin the LibreOffice version and assert the font baseline at image build time | Backend image installs `libreoffice-*` with no version pin; the optional sidecar uses `linuxserver/libreoffice:latest` with **no** Carlito/Caladea/Noto baseline at all |
| W5-2 | Put one real LibreOffice smoke test in the DEFAULT `mvn verify` | Real-LO tests currently live behind the optional `-Plibreoffice-ci` profile, so a green default build says nothing about the conversion path |
| W5-3 | PDF → PNG perceptual diff as a **non-blocking** fidelity dashboard lane | The golden corpus explicitly forbids pixel diffing (`golden-corpus/README.md`), so this must be a separate evidence lane, not a CI gate |
| W5-4 | Word-vs-LibreOffice pagination baseline | A pagination delta budget already exists but the Word-side baseline is incomplete (ADR-0042 is host-blocked) |
| W5-5 | Concurrency soak for the conversion pool | W0-5 and W0-6 are both concurrency/lifecycle defects that no existing test would have caught |

---

## 9. Traceability

- Audit evidence and re-verification: this program document, §1 and the W0/W1 detail sheets
- Compute DSL decision record to amend: `docs/adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md`
- Font policy: ADR-0041 (Carlito/Caladea are metric-compatible substitutes, not Calibri)
- LibreOffice CI lane: `docs/architecture/libreoffice-ci-lane.md`
- Golden corpus rules (including the pixel-diff prohibition): `backend/src/test/resources/golden-corpus/README.md`
- Docker queue: `.cursor/skills/docker-deploy-queue/SKILL.md`
- Worktree discipline: `.cursor/skills/worktree-isolation/SKILL.md`
