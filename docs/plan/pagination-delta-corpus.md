# Pagination Delta Corpus — LR-A7 / ADR-0042

**Status:** Docker PDF corpus **measured** (2026-07-10); Word-vs-LO delta **pending** Word-equipped host  
**Last updated:** 2026-07-10  
**Git SHA:** `9a40b48`  
**Tasks:** LR-A7 **Done** (documented exception); CD-HARD-T04 **Done** (executed-by-LR-A7)

## Purpose

Record the Word vs LibreOffice-PDF page-count delta for each demo letter in the corpus. Per
ADR-0042, the v1 **proposed** budget is **±1 page**; entries exceeding the budget would trigger a
fidelity warning, and entries exceeding ±2 a fidelity blocker — **both gated on user confirmation
and a real Word baseline**. ADR-0042 remains **Proposed**.

The corpus is the regression test for layout drift — if a rendering change shifts any entry's
Docker PDF page count, the change must be reviewed.

## Measurement method (2026-07-10)

1. Docker acceptance stack `@ localhost:8080` (healthz UP; LibreOffice in `docgen-backend`).
2. Runtime `POST .../generate` with `format=PDF`, `mode=SYNC_STREAM`.
3. Page counts via host Python `pypdf` (`PdfReader`).
4. **Word baseline:** `method=ms-word-unavailable-on-host` → `wordPages=null`, `delta=null`.
   Do **not** invent Word page counts or treat Docker/LO PDF pages as Microsoft Word baselines.
5. LO-proxy separate soffice pass: skipped (runtime Docker PDF already uses container LibreOffice).

## Baseline measurements

| Demo package | externalId | Word pages (author) | PDF pages (Docker/LO) | Delta | Status |
| --- | --- | --- | --- | --- | --- |
| `deploy/demo-credit-limit` | `DEMO-CREDIT-LIMIT-CONFIRM` | n/a | 6 | n/a | Docker PDF measured; Word pending |
| `deploy/demo-mortgage` | `DEMO-MORTGAGE-APPROVAL` | n/a | 6 | n/a | Docker PDF measured; Word pending |
| `deploy/demo-trade-lc` | `DEMO-TRADE-LC-NOTICE` | n/a | 9 | n/a | Docker PDF measured; Word pending |
| `deploy/demo-collection` | `DEMO-OVERDUE-COLLECTION` | n/a | 8 | n/a | Docker PDF measured; Word pending |
| `deploy/demo-retail-account` | `DEMO-RETAIL-ACCOUNT-OPEN` | n/a | 8 | n/a | Docker PDF measured; Word pending |
| `deploy/demo-fol` (optional) | `CORP-FOL-OFFER` | n/a | 86 | n/a | Docker PDF measured; Word pending |

**Aggregates (required 5):** max PDF pages = **9**; median = **8**; max/median Word delta = **n/a**.

**Evidence:** [`docs/evidence/lrp-a7-pagination/`](../evidence/lrp-a7-pagination/) (README + `measurement-results.json`); full PDFs under worktree `.tmp/evidence/lrp-a7-pagination/` (untracked binaries).

> **Honesty note:** LR-A7 / CD-HARD-T04 close the **Docker PDF measurement gap** (≥5 letters + durable evidence). True Word-vs-LO delta validation remains a **residual follow-up** on a Word-equipped host — not a new In Progress task; do not reopen LR-A6 for this residual. (LR-C9 is a separate usability slice, not Word residual work.)

## Budget enforcement (pending confirmation)

- **±1 page** → no action (within budget).
- **±2 pages** → fidelity warning logged; author should review but may publish.
- **±3+ pages** → fidelity blocker; template must not publish until layout is adjusted.

The enforcement is a **pending proposal** — until the user confirms the budget **and** Word
baselines exist, the system must not treat Docker-only page counts as Word deltas. This file
records the Docker PDF baseline so drift is visible even before Word confirmation.

## Drift detection

When a rendering change (LR-A1 profile isolation, LR-A2 font baseline, POI upgrade,
LibreOffice upgrade) lands, re-measure every required row's Docker PDF pages. Any page-count
change > 0 must be called out in the commit message and reviewed. After Word baselines land,
also re-check deltas against ADR-0042.
