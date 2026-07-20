# IBL-E7 / #134 — RTL / bidirectional script spike report

| Field | Value |
| --- | --- |
| Slice | `ibl-e7-rtl-bidi-spike` |
| Task Master | **#134** / plan ID **IBL-E7** |
| Finding | **F15** — No RTL/bidirectional script support in rendering package |
| BDD | **not-applicable** (spike) |
| Date | 2026-07-20 |
| Owners | rendering-engineer (+ doc-keeper style docs) |
| Worktree | `D:/working/DGE-ibl-e7-rtl-bidi-spike` · `feat/ibl-e7-rtl-bidi-spike` |

## Recommendation (verdict)

**DESCOPE** full RTL / bidirectional product support from the IBL program and v1 go-live.

Recorded durably via **Accepted** [ADR-0068](../../../adr/rendering-authoring/0068-rtl-bidi-out-of-scope-until-market.md) (doc-keeper stage 3, 2026-07-20). **F15 closed by descope.** Do **not** schedule an implementation leaf until reopen criteria in that ADR are met. **Accepted ≠ #134 leaf Done.**

| Option | Chosen? | Why |
| --- | --- | --- |
| **Descope** (product OUT; reopen gated) | **Yes** | Market still product-gated; gaps span structured model, POI wire-up, fonts, LO fidelity, editor, golden corpus — far beyond a Wave E leaf |
| Full design ADR + implement RTL now | No | Would invent product scope without market confirmation; conflicts with “no full RTL product impl” spike charter |
| Silent drop of F15 | No | Finding must stay honest; descope + ADR is the durable close |

**Does not:** flip checklist **#3b** / **#5a**; remove SPECIMEN (**PD-6 OUT**); embed licensed fonts (**PD-7 OUT**); invent Word / pixel baselines; claim Wave E / IBL program Done / go-live.

## Feasibility summary

| Layer | Feasible today? | Notes |
| --- | --- | --- |
| Unicode Arabic/Hebrew **codepoints** in DOCX runs | Partial | Writer can emit UTF-8 Arabic text runs; storage/encoding is not the blocker |
| **Correct** RTL paragraph layout / mixed bidi | **No** | No `w:bidi` / `w:rtl` / section `bidi` / complex-script run props from structured writer |
| Structured content contract | **No** | ADR-0019 direct-format whitelist is LTR-oriented (`leftIndent`/`rightIndent`); no `writingDirection` / `bidiLevel` / script marks |
| Apache POI 5.5.1 | Partial | High-level `XWPFParagraph` has no first-class RTL API; low-level `CTPPr.addNewBidi()` exists (probe confirms) but is unused |
| LibreOffice PDF | Unknown / high risk | Engine can layout RTL **if** OOXML + fonts are correct; no platform contract, no golden RTL corpus, fidelity vs Word unproven (same class of risk as ADR-0042 pagination delta) |
| Font baseline (ADR-0041) | **Gap** | Jammy image: Noto CJK + Carlito/Caladea + DejaVu — **no** Arabic/Hebrew Noto (or equivalent) package / `fc-list` gate |
| Authoring UI / i18n | **Out of spike** | Management UI English-first; no RTL editor chrome in scope for this leaf |
| Locale-variant model (ADR-0062) | Orthogonal | Locales exist; model does **not** imply script direction or RTL rendering |

**Bottom line:** Emitting Arabic *characters* is cheap; shipping **bank-grade RTL/bidi letters** is a multi-surface program (schema + writer + fonts + LO fidelity + editor + goldens + Word residual honesty). Not feasible as an IBL-E7 product delivery.

## Inventory — rendering package gaps

Codebase search under `backend/.../rendering/**` found **zero** RTL/bidi/complex-script wire-up (`rtl`, `bidi`, `RightToLeft`, `w:bidi`, etc.).

| Surface | Current state | Gap |
| --- | --- | --- |
| `StructuredContentDocxWriter` + style/inline/block supports | LTR paragraph/run emission; indent whitelist only | No paragraph `bidi`, run `rtl`, complex-script fonts (`cs`/`rtl`), mirror indents |
| `DocxAssembler` / master styles | Style catalog + anchors | No RTL section properties / mirror margins |
| `LibreOfficePdfConversionService` | Headless convert | No RTL-specific options; depends on upstream OOXML + fonts |
| `backend/Dockerfile` (+ packaged) | ADR-0041 font set | Missing Arabic/Hebrew font packages and build assertions |
| Fidelity warnings (ADR-0019) | Known warning codes | No RTL/bidi fidelity warning or publish blocker codes |
| Golden corpus / IBL-C3 | en/zh (+ synthetic themes) | No RTL locale theme; F19 closed without RTL |

## LibreOffice gaps

1. **Input quality:** LO cannot invent correct bidi from LTR-marked OOXML with RTL codepoints alone; mirrored punctuation, weak neutrals (digits, punctuation between LTR/RTL), and list numbering direction need proper OOXML + Unicode bidi algorithm behavior.
2. **Fonts:** Without Arabic/Hebrew-capable fonts in the conversion image, expect tofu / fallback reflow (same failure mode as pre-ADR-0041 CJK).
3. **Fidelity:** Platform already refuses Word-identical pagination promises (ADR-0042). RTL would add another unproven LO↔Word delta class; **IBL-B7 Word host remains Blocked** — no Word invent for RTL baselines in this spike.
4. **PDF/A / veraPDF:** Unassessed for RTL glyph embedding / tagging; would need a dedicated leaf if RTL is ever productized.
5. **Repro freeze (ADR-0060):** Any new font packages would touch the legal reproducibility freeze and require an explicit ADR amendment path — not inventable inside E7.

## Apache POI gaps

1. **Platform wire-up:** Structured writer never sets `CTPPr` bidi / `CTRPr` rtl.
2. **API surface:** Product code would need a controlled OOXML helper (not ad-hoc XML) + structured-model fields; high-level POI helpers are incomplete for full complex-script runs.
3. **Tables / lists / headers:** Cell direction, numbering `lvlJc`, header/footer bidi, and mirrored tabs are separate seams — none inventory-clean today.
4. **Probe evidence:** `RtlBidiInventoryProbeTest` — Arabic+Latin structured output contains glyphs but **no** `<w:bidi` / `<w:rtl`; `CTPPr.addNewBidi()` is available at XMLBeans level.

## Scope exclusions (locked for this leaf)

- **PD-6** SPECIMEN / true re-issue — **OUT**
- **PD-7** licensed font embedding / LRP GO — **OUT**
- **#119** Word host / Word invent — **OUT**
- Checklist **#3b** / **#5a** — **do not flip**
- Full RTL product implementation, editor RTL chrome, or market locale commitments — **OUT**

## Reopen criteria (for future program — not IBL-E7)

Only reopen when **all** hold:

1. Explicit product confirmation of RTL/bidi market need (locales/scripts named).
2. Accepted implementation ADR amending ADR-0019 (structured direction marks) + ADR-0041 (font packages) + fidelity policy.
3. Dedicated delivery wave (not folded into go-live checklist as silent scope).
4. Golden corpus + LO CI evidence; Word residual honesty if still Blocked.

## Evidence artifacts

| Artifact | Path |
| --- | --- |
| This report | [SPIKE-REPORT.md](./SPIKE-REPORT.md) |
| Index | [README.md](./README.md) |
| Accepted ADR (descope) | [0068-rtl-bidi-out-of-scope-until-market.md](../../../adr/rendering-authoring/0068-rtl-bidi-out-of-scope-until-market.md) |
| Probe test | `backend/src/test/java/com/bank/docgen/rendering/RtlBidiInventoryProbeTest.java` |

## Gate note

Spike leaf: BDD **not-applicable**. Probe is unit-level (`-Pdev-fast` / `mvn test -Dtest=RtlBidiInventoryProbeTest`). Full `mvn verify` / deploy queue delegated to build-deploy-agent / parent pipeline. No frontend / E2E / Stage 10 required for descope documentation.
