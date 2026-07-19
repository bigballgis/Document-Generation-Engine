# Evidence — IBL-C3 / F19 cross-locale golden matrix

| Field | Value |
| --- | --- |
| Slice | `ibl-c3-cross-locale-golden` |
| Task Master | **#122** |
| Worktree | `D:/working/DGE-ibl-c3-cross-locale-golden` · `feat/ibl-c3-cross-locale-golden` |
| Recorded | 2026-07-19 |

## Themes added

| Theme ID | Locale / currency | `pdfSource` |
| --- | --- | --- |
| `english-locale-letter` | `en-US`; unary `FORMAT_AMOUNT` + `SPELL_AMOUNT(..., 'USD')` | **SYNTHETIC** |
| `multi-currency-amount` | `en-US` body; binary `FORMAT_AMOUNT` EUR / USD / CNY | **SYNTHETIC** |
| `chinese-uppercase-amount` (existing) | `zh-CN`; `SPELL_AMOUNT` Chinese uppercase | **SYNTHETIC** (unchanged) |

## LibreOffice honesty

| Check | Result |
| --- | --- |
| Host `soffice` on PATH | **NO** (see `soffice-availability.txt`) |
| Invented LO PDF binaries under `expected/` | **NO** |
| Existing `LIBREOFFICE` packages | PDF half **SKIP** via `Assumptions` when soffice unavailable (DOCX still green) |
| New IBL-C3 themes | Honest **SYNTHETIC** PDF halves (PDFBox projection) — not relabeled as LIBREOFFICE |
| IBL-D2 mandatory LO CI lane | **Out of scope** for this leaf (dependency noted only) |

## Gates

Record `mvn -B -ntp -f backend/pom.xml verify` result in `mvn-verify-summary.txt` after the gate run.
