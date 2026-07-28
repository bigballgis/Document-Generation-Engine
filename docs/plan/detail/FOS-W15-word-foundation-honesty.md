# FOS-W15 — Word foundation honesty (typography / layout / letterhead)

**Status:** Done  
**Behavior:** [fos-word-foundation-honesty.md](../../behavior/fos-word-foundation-honesty.md)  
 
**Program:** [frontline-operability-solidity-program-2026-07.md](../frontline-operability-solidity-program-2026-07.md)  
**Evidence audit:** [fos-word-foundation-deep-review-2026-07.md](fos-word-foundation-deep-review-2026-07.md)  
**Task Master:** `#185`  
**Slice id:** `fos-word-foundation-honesty`  
**Depends on:** FOS W3 (authoring honesty), FOS W14 (demo literacy — extend), CRCH W3 (table geometry — do not duplicate)  
**Blocks:** None  
**Suggested worktree:** `../DGE-fos-w15-word-foundation`  
**Suggested branch:** `feat/fos-w15-word-foundation`

## Goal

Close the honesty + fidelity gaps that make bank letterhead / typography / structured layout feel like Word is optional. Prefer **wiring existing capability + truthful demos** over new product features.

**Non-goals:** Absolute seals (OD-FOS-1); PDF stamp default ON (OD-FOS-2); new visual designer; inventing Chinese fonts in images; duplicating CRCH W3 table geometry / W0-7 seals / CRCH W2 compute DSL implementation.

## Verdict (from audit)

| Question | Answer |
| --- | --- |
| DOCX as **output** necessary? | **Yes** — bank handoff + LibreOffice PDF. |
| DOCX as **layout foundation** earned today? | **Partially** — engine has OOXML path; KEEP-8 demos + style application under-exercise it. |
| User doubt justified? | **Yes** on typography / letterhead / headers — demos are data-rich, craft-poor. |

## Finding → task map

| Finding | Task | Severity |
| --- | --- | --- |
| WF-1 Nested table/list silent empty | W15-1 | P0 |
| WF-2 Multi-child reorder | W15-2 | P0 |
| WF-3 Clause numbering restarts / literal | W15-3 | P0 |
| WF-4 Demo `headerRows` blank | W15-4 | P1 |
| WF-5 Style-manifest dead fields | W15-5 | P1 |
| WF-6 Typography gates assert master shell | W15-6 | P1 |
| WF-7 No KEEP-8 money formatters | W15-7 | P1 |
| WF-8 No live Word-crafted letterhead path | W15-8 | P1 |

## Tasks

### W15-1 — Nested table/list fail-closed (WF-1)

**Problem:** `tableComponentRef` / `list` inside `conditionBlock`/`loopBlock` can silent-empty (no dispatch branch).

**Steps (TDD):**
1. RED: nested table inside true condition → expect table rows **or** structured job error.
2. GREEN: implement render **or** fail-closed with actionable `RENDERING_*` / job error (prefer fail-closed for bank).
3. Document supported nesting matrix (rendering README or behavior note).

**Done when:** Nested table/list either works or fails the job — never silent empty.

### W15-2 — Multi-child block order deterministic (WF-2)

**Problem:** ≥3 block children under condition/loop can reorder (`writeInlineOrBlockChildren`).

**Steps (TDD):**
1. RED: parent with `[paragraph A, table, paragraph B]` → DOCX order A → table → B.
2. GREEN: preserve document order when emitting.

**Done when:** Order-preserving test green.

### W15-3 — Clause numbering honesty (WF-3)

**Problem:** Clause `numbering.level` is a literal prefix that restarts per anchor; Formal Demand can show seven "1." headings; ADR-0019 duplicate-number gate is blind to assembly.

**Steps:**
1. Document: **"Clause numbers are literal prefixes; continuity across anchors is not Word automatic numbering."**
2. UI tooltip / wizard note near clause insert if surface exists (coordinate FOS W3 — do not invent new node types).
3. Optional low-risk hardening: continue counter across anchors in one generation — **not** full Word `numPr` unless CRCH/rendering owns it later.

**Done when:** Authors cannot reasonably believe clauses are Word multilevel lists; no silent seven-"1." demo without a honesty note.

### W15-4 — Demo table `headerRows` actually populated (WF-4)

**Problem:** PowerShell flatten sets `headerRows` but rows are blank.

**Files:** `scripts/New-DemoKeep8.ps1` (or table helpers); CRE/COM sample JSON.

**Steps:**
1. Fix flatten to copy first-row labels into header **or** build explicit header cell text in JSON.
2. Assert generated structured JSON has non-empty `headerRows[0].cells[*].blocks`.
3. Visual: CRE schedule table shows header text in preview/DOCX.

**Done when:** No KEEP-8 demo ships empty `headerRows` while advertising table headers.

### W15-5 — Style-manifest → OOXML or trim dead schema (WF-5)

**Problem:** `spacingAfterTwips` / CJK / line spacing in style-manifest often never reach `w:spacing` / `w:rFonts[@w:eastAsia]`.

**Steps (TDD):**
1. RED: style with spacing + eastAsia → assert OOXML contains those properties.
2. GREEN: wire bank-critical subset (before/after/line + eastAsia) **or** remove unused fields from schema + docs.
3. Prefer honesty over fake richness.

**Done when:** Fields apply in DOCX or schema/docs no longer advertise them.

### W15-6 — Typography gates assert generated letter (WF-6)

**Problem:** Typography regression asserts POI-built master shell, not generated letter.

**Steps:**
1. Change gate fixture to run structured generation (or assemble) then assert styles/spacing on **output** DOCX.
2. Do not claim P23 typography Done on master-only checks.

**Done when:** At least one regression asserts generated artifact typography properties.

### W15-7 — KEEP-8 money formatter honesty (WF-7)

**Problem:** No KEEP-8 package uses `COMPUTED` / `FORMAT_AMOUNT` / `SPELL_AMOUNT` — money is preformatted strings.

**Steps:**
1. Either: one KEEP-8 letter exercises `FORMAT_AMOUNT` and/or `SPELL_AMOUNT` (engine already present — wire demo only), **or**
2. Catalog / FOS W14 literacy: **"Money formatting is caller-owned until CRCH W2 demo path lands."**
3. Do **not** re-implement CRCH W2 DSL here.

**Done when:** Catalog truthful about money formatting ownership.

### W15-8 — One live Word-crafted letterhead demonstration (WF-8)

**Problem:** Author never *sees* Word after upload; KEEP-8 masters are JVM-POI skeletons (~4–6 KB, no logo/`word/media`); hdr/ftr often only on last `sectPr`.

**Steps:**
1. Ship **one** KEEP-8 (prefer BAL or CRE) with a real letterhead package: logo in header, footer confidential line, hdr/ftr on every section (or document single-section contract + publish-gate warn).
2. Prefer upload-through-product path evidence in docs (designer edits in Word → re-upload → anchors refresh → preview shows change) — even if the binary is committed, document the journey.
3. Update demo catalog: Letterhead column `logo | text-only | none`; FOL literacy: volume ≠ layout (extend FOS W14).
4. Optional: thumbnail / style specimen is **out of scope** unless trivial — do not invent a design studio.

**Done when:** At least one KEEP-8 master has `word/media` + logo visible; catalog letterhead column present; FOL not claimed as layout showcase.

## Implementation order (suggested)

```
W15-1 + W15-2 (P0 writer) → W15-3 (honesty) → W15-4 + W15-8 (demo craft)
→ W15-5 + W15-6 (typography truth) → W15-7 (catalog / one formatter demo)
```

May split into 2–3 serial commits under one TM parent `#185`.

## Gate evidence (wave Done)

| Gate | Command / artifact |
| --- | --- |
| Backend | `mvn -B -ntp -f backend/pom.xml verify` when Java touched |
| Frontend | pnpm gates if UI copy touched |
| Demo | Unzip letterhead master; catalog columns updated |
| Deploy | Queued deploy if runtime/render path changed |
| Docs | Deep-review disposition → Remediate/Documented; ledger note |

## Vetoes

- Do not invent absolute-position seals (OD-FOS-1).
- Do not flip PDF page-number stamp default on (OD-FOS-2).
- Do not duplicate CRCH W3 table geometry / W0-7 seal work / CRCH W2 compute implementation.
- Do not claim "Word-class typography" until W15-5/W15-6/W15-8 green.
- Chinese font embedding in Docker images = separate ops decision — document limitation if fonts absent.

## Exit criteria

- [x] W15-1…W15-8 Done or explicitly Deferred with owner (W15-3/W15-7 Documented; others Remediated)
- [x] Deep-review disposition table updated
- [x] Nested table/list no longer silent no-op; child order preserved
- [x] Clause numbering honesty documented (and/or continuity fixed)
- [x] Demo headerRows + at least one logo letterhead truthful
- [x] Style-manifest wired or trimmed; typography gate asserts generated DOCX
- [x] post-task-doc-sync + commit-review
