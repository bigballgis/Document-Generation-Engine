# CONTENT-SPOTCHECK — bank-letter-demo-expand (Wave B / #142)

**When:** 2026-07-20 (Stage 5/10 queued deploy + import → publish → generate)  
**Worktree:** `D:/working/DGE-bank-letter-demo-expand` · `feat/bank-letter-demo-expand`  
**Path:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `docker-deploy-queue` → `import-all-demos` → `publish-all-demos` → `generate-all-demos`

## Generate summary

| Result | Count | Notes |
| --- | ---: | --- |
| SUCCESS | **20 / 20** | All manifest content markers + size floors + no placeholder leak |
| FAILED | **0 / 20** | — |

**SQL hotfix:** none. Import fixes landed in worktree (locale column, hex UUIDs, empty-page list helper).

## Spot distinctions (Wave B)

| Check | Result |
| --- | --- |
| Commitment ≠ FOL / `CORP-FOL-OFFER` | **OK** — distinct externalId `DEMO-COMMITMENT-LETTER`, Aurora Industrial Partners markers |
| Formal Demand ≠ retail overdue | **OK** — `DEMO-FORMAL-DEMAND` (Harbour Logistics / Sums Demanded) vs `DEMO-OVERDUE-COLLECTION` |
| Catalogue expand ≠ PRD §6.7 eight-family replacement | **OK** — Wave A 13 retained; +7 Wave B |

## Successful DOCX — sizeBytes + markers

| externalId | sizeBytes | Meridian | Placeholder leak (`{{` / TODO / lorem) |
| --- | ---: | --- | --- |
| CORP-FOL-OFFER | 30162 | yes | no |
| DEMO-FULL-FLOW-LETTER | 4775 | yes | no |
| DEMO-RETAIL-ACCOUNT-OPEN | 6866 | yes | no |
| DEMO-RETAIL-ACCOUNT-BALANCE | 6359 | yes | no |
| DEMO-MORTGAGE-APPROVAL | 9053 | yes | no |
| DEMO-CREDIT-LIMIT-CONFIRM | 9710 | yes | no |
| DEMO-TRADE-LC-NOTICE | 8018 | yes | no |
| DEMO-TRADE-GUARANTEE-NOTICE | 6960 | yes | no |
| DEMO-RATE-CHANGE-NOTICE | 6190 | yes | no |
| DEMO-OVERDUE-COLLECTION | 6485 | yes | no |
| DEMO-ANNUAL-REVIEW | 7218 | yes | no |
| DEMO-FACILITY-RENEWAL | 7057 | yes | no |
| DEMO-WEALTH-STATEMENT | 8733 | yes | no |
| DEMO-FACILITY-AMENDMENT | 7281 | yes | no |
| DEMO-KYC-CDD-NOTICE | 6596 | yes | no |
| DEMO-ACCOUNT-CLOSURE | 6423 | yes | no |
| DEMO-COMMITMENT-LETTER | 7181 | yes | no |
| DEMO-FORMAL-DEMAND | 6592 | yes | no |
| DEMO-COVENANT-WAIVER | 6431 | yes | no |
| DEMO-INSURANCE-ENDORSEMENT | 6679 | yes | no |

Artifacts: `generated_<externalId>.docx` · [generated-docx-manifest.json](./generated-docx-manifest.json) · [all-demos-publish-summary.json](./all-demos-publish-summary.json) (20/20) · [spotcheck-sizes.json](./spotcheck-sizes.json).

## Import / generate fixes applied this Stage 10 (worktree)

| Fix | Why |
| --- | --- |
| Wave B `content_module` SQL `locale='zh-CN'` | NOT NULL column (V69); Wave A skipped insert via `WHERE NOT EXISTS` |
| Invalid SQL UUIDs (`kc`/`cm`/`cw`/`ie` prefixes) → hex | Postgres UUID parse |
| `Get-DemoApiResultItems` empty `content` handling | Empty search returned page object → `$_.name.Trim()` NRE on first Wave B master create |
| `DEMO-FACILITY-AMENDMENT` `minDocxBytes` 7680 → 6144 | Actual durable DOCX 7281 B (peer of commitment / renewal); floor was copied from credit-limit |

## Gates note

- Backend `mvn verify` **GREEN** — Tests run: 2340, Failures: 0, Errors: 0, Skipped: 15 (`-Dsurefire.argLine=-Xmx1536m`).
- Frontend: lint / type-check / build **GREEN**; vitest **RED** on 2 unrelated baseline suites (`apiErrorCatalog` 9 missing keys; `openapiCodegenParity`) — not Wave B registry blockers (documented honestly).
- Generate evidence: **20/20 SUCCESS** on live `:8080` stack after image redeploy.
