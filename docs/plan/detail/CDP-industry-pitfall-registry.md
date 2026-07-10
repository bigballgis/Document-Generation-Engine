# CDP Industry Pitfall Registry

**Program:** [competitiveness-deepening-program.md](../competitiveness-deepening-program.md)  
**Wave CD-0:** Spec + ADR drafts (non-code)  
**Wave CD-1/3:** Implementation mitigations  
**Sources:** Industry literature (LibreOffice/PDF conversion, enterprise CCMS, bank doc automation), codebase audit (P22 gap spec, OPT seams), LRP launch-readiness audit (2026-07-03)

---

## 1. Registry format

| Column | Meaning |
| --- | --- |
| **ID** | CD-PIT-NN |
| **Symptom** | What users/integration partners see |
| **Root cause** | Technical why |
| **Detection** | Automated test or manual check |
| **Mitigation** | Concrete fix |
| **Owner wave** | CD-0 spec / P22 / CD-HARD |
| **Doc anchor** | ADR or NFR section to create/update |

---

## 2. Pitfalls

### CD-PIT-01 — Font substitution in headless conversion

| Field | Content |
| --- | --- |
| **Symptom** | PDF page count differs from author Word; table columns clip; footer wraps |
| **Root cause** | Calibri/Cambria absent in Linux container; LibreOffice substitutes Liberation/Carlito with different metrics |
| **Detection** | `RenderingFontSmokeTest`: convert fixed-width sample DOCX; assert page count + golden PDF hash tolerance; CI fails if core fonts missing |
| **Mitigation** | Bake `ttf-mscorefonts-installer` or licensed fonts into `backend/Dockerfile` / LibreOffice sidecar; document in `deploy/README.md` |
| **Owner wave** | CD-HARD-T01 |
| **Doc anchor** | NFR §production rendering; new ADR `0041-rendering-font-baseline.md` (draft in CD-0) |

### CD-PIT-02 — Word vs LibreOffice layout engine divergence

| Field | Content |
| --- | --- |
| **Symptom** | Legally paginated documents shift page breaks in PDF |
| **Root cause** | No OSS JS engine reproduces Word layout; LO ≠ Word |
| **Detection** | Corpus of ≥5 demo letters: compare page count Word (manual baseline) vs LO PDF; record delta budget |
| **Mitigation** | (1) Author defensively: explicit table widths, avoid floating objects; (2) Publish **pagination delta budget** in NFR; (3) Fidelity warnings for page-count mismatch vs baseline |
| **Owner wave** | CD-0 doc + CD-HARD-T04 |
| **Doc anchor** | NFR §compatibility; PRD §6.5 footnote |

### CD-PIT-03 — OOXML strictness / escaping (LO 24+ «corrupt» DOCX)

| Field | Content |
| --- | --- |
| **Symptom** | Generated DOCX fails in LibreOffice 24+ until resaved; variables/fields missing |
| **Root cause** | Invalid XML characters, unescaped ampersands, malformed relationships |
| **Detection** | Open output with LO 24 headless; POI `OPCPackage.open` + schema validation; regression from PHPWord LO24 issue class |
| **Mitigation** | Central XML escaping in `StructuredContentDocxWriter`; never write raw user text into XML without escape |
| **Owner wave** | P22-T01, CD-HARD-T03 |
| **Doc anchor** | P22-T01 acceptance; ADR rendering appendix |

### CD-PIT-04 — Word list numbering (`numId` / `ilvl`) corruption

| Field | Content |
| --- | --- |
| **Symptom** | Nested lists restart at 1; mixed bullet/decimal wrong; section restart breaks |
| **Root cause** | POI numbering definitions not aligned with master `numbering.xml`; multiple abstract nums |
| **Detection** | `DocxAssemblerTest` extracts list labels from generated DOCX; FOL demo list anchors |
| **Mitigation** | `DocxListNumberingSupport` single registry; master generators seed numbering.xml; condition/loop re-sequence policy documented |
| **Owner wave** | P22-T01 |
| **Doc anchor** | demo-expansion BDD-001 list scenarios |

### CD-PIT-05 — Dual page number fields

| Field | Content |
| --- | --- |
| **Symptom** | Only global «Page N of M»; missing section «Page X of Y» |
| **Root cause** | Footer uses `PAGE` only; no `SECTIONPAGES`; PDF stamper overwrites section semantics |
| **Detection** | POI field code inspection + PDF text regex for both patterns |
| **Mitigation** | P22-T03 master footers; P22-T04 stamper respects `pageNumberingProfile` |
| **Owner wave** | P22-T03, P22-T04 |
| **Doc anchor** | demo-expansion BDD-005, BDD-006 |

### CD-PIT-06 — Synchronous LibreOffice on request thread

| Field | Content |
| --- | --- |
| **Symptom** | Timeouts under concurrent PDF requests; thread pool exhaustion |
| **Root cause** | `RuntimeGenerationService` inline conversion (OPT-F6) |
| **Detection** | Load test: N concurrent PDF generations; p95 latency |
| **Mitigation** | Bounded async pool + job status OR queue; timeout with retryable error |
| **Owner wave** | CD-HARD-T02 |
| **Doc anchor** | execution-sync-ledger seam; OPT-F6 |

### CD-PIT-07 — Rich-text boundary creep

| Field | Content |
| --- | --- |
| **Symptom** | Authors embed tables/layout in prose; paste brings unsupported HTML |
| **Root cause** | Rich text field asked to carry structure (headless CMS anti-pattern) |
| **Detection** | Paste E2E + binding validator flags; count of `CONTROLLED_STYLE_FALLBACK` |
| **Mitigation** | Enforce node matrix in UI; paste cleaning summary; block publish on disallowed nodes |
| **Owner wave** | P18 (Done UI) + CD-HARD-T05 binding wire |
| **Doc anchor** | authoring-first-principles §4; usability-review L82 |

### CD-PIT-08 — Edit preview mistaken for legal evidence

| Field | Content |
| --- | --- |
| **Symptom** | Testers approve based on fast preview; final DOCX differs |
| **Root cause** | UX does not distinguish preview modes |
| **Detection** | CD-E2E-T10; copy audit in `TemplateTestDataSetPanel` |
| **Mitigation** | Banner on edit preview; test/approve flows require final-path artifact download |
| **Owner wave** | CD-E2E-T08, CD-E2E-T10 |
| **Doc anchor** | usability-review L38–39 |

### CD-PIT-09 — Programmatic template fill fragility at scale

| Field | Content |
| --- | --- |
| **Symptom** | Batch generation drift; table column resize over hundreds of docs |
| **Root cause** | Auto-fit tables; implicit styles |
| **Detection** | Batch test on 50+ samples; byte-size and layout hash variance |
| **Mitigation** | Fixed table widths in `tableComponent`; styleRef mandatory for headings |
| **Owner wave** | P22-T01, demo packages |
| **Doc anchor** | PRD §6.5.1 |

### CD-PIT-10 — Two-UI review fragmentation

| Field | Content |
| --- | --- |
| **Symptom** | SMEs review PDF/email; authors edit in app — feedback loop breaks |
| **Root cause** | No in-app suggest/comment on full publication view |
| **Detection** | UX review checklist; user interviews post-launch |
| **Mitigation** | **v1:** Dashboard queues + inline open to exact anchor; **post-v1:** comment threads (defer — document in usability-review 待确认) |
| **Owner wave** | CD-0 doc decision |
| **Doc anchor** | usability-review §待确认 |

### CD-PIT-11 — LibreOffice shared-profile concurrency failures *(added 2026-07-03, LRP audit)*

| Field | Content |
| --- | --- |
| **Symptom** | Concurrent PDF conversions fail intermittently, output goes missing, or `soffice` crashes silently (industry-frequent: shared user-profile lock) |
| **Root cause** | `LibreOfficePdfConversionService` `ProcessBuilder` passes no `-env:UserInstallation`; the conversion pool (default size 2) lets concurrent `soffice` processes contend for the same HOME profile lock |
| **Detection** | Integration/smoke test running ≥4 conversions in parallel |
| **Mitigation** | Unique temp profile per invocation + `--norestore --nolockcheck --nodefault --nologo` + cleanup in `finally` |
| **Owner wave** | **LR-A1** |
| **Doc anchor** | [docs/plan/detail/LRP-A-rendering-trust-hardening.md](./LRP-A-rendering-trust-hardening.md) |

### CD-PIT-12 — SSE buffered/broken behind reverse proxy *(added 2026-07-03, LRP audit)*

| Field | Content |
| --- | --- |
| **Symptom** | On Docker 4173, preview/batch-test progress does not arrive incrementally (whole batch bursts at once) or the stream drops silently |
| **Root cause** | `frontend/nginx.conf` has no SSE location (nginx defaults `proxy_buffering on`, `proxy_read_timeout 60s`); backend sends no `X-Accel-Buffering: no` / `Cache-Control: no-cache`; `SseEmitterRegistry` 3-min timeout without heartbeat |
| **Detection** | LR-E1 incremental-arrival E2E (4173) |
| **Mitigation** | Heartbeat comment ~20s + response headers + nginx SSE location (buffering off, raised read_timeout) (LR-B3 landed 2026-07-04 — heartbeat + anti-buffering headers + nginx SSE location + Docker curl smoke; browser-level incremental proof → LR-E1) |
| **Owner wave** | **LR-B3 / LR-E1** |
| **Doc anchor** | [docs/plan/detail/LRP-B-runtime-scaleout-session.md](./LRP-B-runtime-scaleout-session.md) |

### CD-PIT-13 — Hard JWT expiry loses authoring work *(added 2026-07-03, LRP audit)*

| Field | Content |
| --- | --- |
| **Symptom** | Any request 401s once an author has been editing for 30 minutes; unsaved form work is lost |
| **Root cause** | `PT30M` hard expiry with no renewal/revocation (logout is log-only) |
| **Detection** | Session-expiry scenario E2E + dirty-form guard tests |
| **Mitigation** | Sliding renewal/refresh + Redis revocation list (LR-B6) + dirty-form guard and local drafts (LR-C1/C2) (LR-B6 landed 2026-07-04 — sliding renewal + revocation; 8 h absolute cap, fail-closed Redis check, silent frontend renewal + reminder banner; LR-C1/C2 still open) |
| **Owner wave** | **LR-B6 + LR-C1/C2** |
| **Doc anchor** | [docs/plan/detail/LRP-B-runtime-scaleout-session.md](./LRP-B-runtime-scaleout-session.md) + [docs/plan/detail/LRP-C-usability-deepening.md](./LRP-C-usability-deepening.md) |

### CD-PIT-14 — Duplicate scheduled jobs on scale-out *(added 2026-07-03, LRP audit)*

| Field | Content |
| --- | --- |
| **Symptom** | With multiple replicas, cleanup/escalation jobs execute repeatedly (duplicate escalation to-dos, racing deletes) |
| **Root cause** | 3 `@Scheduled` jobs (invocation retention / collaboration escalation / preview temp cleanup) have no distributed mutex; P15 delivered HPA, contradicting ADR-0039's single-instance assumption |
| **Detection** | Two-instance compose smoke observing duplicate execution logs |
| **Mitigation** | Topology decision ADR-0044 (LR-B1) + ShedLock-style DB mutex (LR-B2; dependency requires dependency-policy check) (ADR-0044 Accepted 2026-07-04 — decision convergence; LR-B2 ShedLock landed) |
| **Owner wave** | **LR-B1 / LR-B2** |
| **Doc anchor** | [docs/plan/detail/LRP-B-runtime-scaleout-session.md](./LRP-B-runtime-scaleout-session.md) |

### CD-PIT-15 — Unbounded audit table growth *(added 2026-07-03, LRP audit)*

| Field | Content |
| --- | --- |
| **Symptom** | Management/runtime audit tables grow without bound, degrading queries and backups |
| **Root cause** | V9/V17 only create tables + indexes; no retention/archival (contrast: invocation records already have V43/V44 + a cleanup scheduler) |
| **Detection** | Capacity reports / slow-query monitoring |
| **Mitigation** | Retention config + scheduled cleanup/archival (mirror the invocation pattern; record the retention baseline in an ADR) |
| **Owner wave** | **LR-D1** |
| **Doc anchor** | [docs/plan/detail/LRP-D-ops-observability.md](./LRP-D-ops-observability.md) |

---

## 3. P22 task ↔ pitfall mapping

| P22 task | Must mitigate pits |
| --- | --- |
| P22-T01 | CD-PIT-03, 04, 07, 09 |
| P22-T02 | CD-PIT-04, 09 |
| P22-T03 | CD-PIT-05 |
| P22-T04 | CD-PIT-02, 05 |
| P22-T15 | All P22 pits + regression suite |

---

## 4. CD-0 ADR drafts to create (doc-keeper)

| ADR | Title | Decision outline |
| --- | --- | --- |
| `docs/adr/rendering-authoring/0041-rendering-font-baseline.md` | Font baseline for DOCX/PDF | Required font bundle in deploy images |
| `docs/adr/rendering-authoring/0042-pagination-delta-budget.md` | Word vs LO pagination | Acceptable page-count delta for v1 |
| `docs/adr/rendering-authoring/0043-ooxml-output-validation.md` | OOXML output gate | LO 24 open required in CI |

Status: LR-A5 Done 2026-07-10. [ADR-0041](../../adr/rendering-authoring/0041-rendering-font-baseline.md) **Accepted** (architecture-reviewer PASS_WITH_NOTES 2026-07-10). ADR-0042/0043 remain **Proposed** (0042 blocked on Word-equipped host; 0043 blocked on slice B XSD/LO24).

---

## 5. Exit gate (CD-0 pitfall wave)

- [ ] All CD-PIT-01…10 rows present (this file)
- [ ] Three ADR drafts created with Proposed status
- [ ] NFR §production rendering subsection added (font + pagination reference)
- [ ] P22 task table updated with pitfall column (CD-P22-PLAN)
