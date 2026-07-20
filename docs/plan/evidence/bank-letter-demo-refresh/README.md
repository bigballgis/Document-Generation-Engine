# Evidence stub — bank-letter-demo-refresh (Wave A / #141)

**Status:** Stub only — **not** Done. Artifacts will be captured after rendering uplift + queued Docker + `import-all` → `publish-all` → `generate-all`.

| Field | Value |
| --- | --- |
| Slice | `bank-letter-demo-refresh` |
| Task Master | **#141** (`demo-refresh-wave-a`) — **in-progress** |
| Branch / worktree | `feat/bank-letter-demo-refresh` · `D:/working/DGE-bank-letter-demo-refresh` |
| BDD | [bank-letter-demo-refresh.md](../../../behavior/bank-letter-demo-refresh.md) (`BDD-DEMO-REFRESH-001`…`014`) |
| Plan | [bank-letter-demo-refresh.md](../../detail/bank-letter-demo-refresh.md) |
| Ops path | [deploy/demo-shared/README.md](../../../../deploy/demo-shared/README.md) — cleanup → import overwrite → publish → generate |
| Wave B | **OUT** — TM **#142** `bank-letter-demo-expand` |

## Expected later artifacts (do not invent)

Place generate / deploy evidence here when available (examples — not yet claimed):

```text
docs/plan/evidence/bank-letter-demo-refresh/
  README.md                          # this stub
  # later:
  # generated-docx-manifest.json     # copy or pointer from .tmp/evidence/
  # all-demos-publish-summary.json
  # spot-check notes (CORP / RETAIL / TRADE / WEALTH)
  # stage5-or-stage10 deploy notes (if queued deploy required)
```

Runtime outputs remain under `.tmp/` per fundraising matrix:

- `.tmp/generated_<externalId>.docx`
- `.tmp/evidence/generated-docx-manifest.json`
- `.tmp/evidence/all-demos-publish-summary.json`
- `.tmp/credentials/<externalId>.json`

Cross-index: [fundraising-demo-summary.md](../../../evidence/fundraising-demo-summary.md) (13-template matrix; Wave A refreshes content, does not add IDs).

## Hard vetoes (evidence discipline)

- Do **not** claim Wave A Done from this stub alone
- Do **not** invent Word-host / pixel evidence
- Do **not** flip checklist **#3b/#5a GO**
- Do **not** archive Wave B new-family artifacts in this leaf
