# LR-A7 Pagination Measurement Evidence (durable summary)

**measuredAt:** 2026-07-10  
**gitSha:** `9a40b48`  
**tasks:** LR-A7 Done (documented exception); CD-HARD-T04 Done (executed-by-LR-A7)  
**stack:** Docker `docgen-backend` @ `http://localhost:8080` (healthz UP; LibreOffice in container)

## Method

1. Runtime `POST /api/dev/v1/templates/<externalId>/default/generate` with `format=PDF`, `mode=SYNC_STREAM`.
2. Page counts via host Python `pypdf`.
3. **Word baseline:** `method=ms-word-unavailable-on-host` — `wordPages=null`, `delta=null`. No invented numbers.
4. Full PDF binaries live under worktree `.tmp/evidence/lrp-a7-pagination/` (untracked; not committed).

## Results

| externalId | required | Docker PDF pages | Word pages | Delta | PDF bytes |
| --- | --- | --- | --- | --- | --- |
| `DEMO-CREDIT-LIMIT-CONFIRM` | true | 6 | n/a | n/a | 42074 |
| `DEMO-MORTGAGE-APPROVAL` | true | 6 | n/a | n/a | 41283 |
| `DEMO-TRADE-LC-NOTICE` | true | 9 | n/a | n/a | 40404 |
| `DEMO-OVERDUE-COLLECTION` | true | 8 | n/a | n/a | 35744 |
| `DEMO-RETAIL-ACCOUNT-OPEN` | true | 8 | n/a | n/a | 35499 |
| `CORP-FOL-OFFER` | false | 86 | n/a | n/a | 371174 |

**Aggregates (required):** max **9** / median **8** Docker PDF pages; Word delta **n/a**.

## Machine-readable

See [`measurement-results.json`](./measurement-results.json) in this directory.

## Cross-links

- NFR corpus: [`docs/requirements/non-functional-requirements.md`](../../requirements/non-functional-requirements.md)
- Corpus plan: [`docs/plan/pagination-delta-corpus.md`](../../plan/pagination-delta-corpus.md)
- ADR-0042 (remains Proposed): [`docs/adr/rendering-authoring/0042-pagination-delta-budget.md`](../../adr/rendering-authoring/0042-pagination-delta-budget.md)
