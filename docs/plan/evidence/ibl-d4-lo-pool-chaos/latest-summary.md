# IBL-D4 — latest chaos / verify evidence

| Field | Value |
| --- | --- |
| **Recorded** | 2026-07-19 |
| **Worktree tip context** | Merged to MAIN `94cc8eeb` (feature tip `94526674`); worktree removed |
| **Slice status** | **Done** — Stage 12 doc-sync; F22 closed with D3 load suite |

## Chaos suite (`-Plo-pool-chaos,dev-fast`)

| Class | Result |
| --- | --- |
| `PdfConversionPoolChaosTest` | **4** tests, 0 fail / 0 skip |
| `LibreOfficePdfConversionPoolChaosIntegrationTest` | **2** tests, 0 fail, **1 skip** (real `soffice` unavailable — documented optional skip) |

Command:

```powershell
mvn -B -ntp -f backend/pom.xml "-Plo-pool-chaos,dev-fast" test "-Dtest=PdfConversionPoolChaosTest,LibreOfficePdfConversionPoolChaosIntegrationTest"
```

## Default `mvn verify`

| Field | Value |
| --- | --- |
| Command | `mvn -B -ntp -f backend/pom.xml verify` |
| Result | **BUILD SUCCESS** |
| Surefire | **2144** tests, **0** failures, **0** errors, **15** skipped |
| Chaos in suite | `PdfConversionPoolChaosTest` 4/4; LO IT hang path green; real-soffice half among skipped when absent |
| Log | `backend/target/ibl-d4-verify.log` (local worktree; not committed) |

## Metrics exercised

- `docgen.pdf.conversion.pool.active`
- `docgen.pdf.conversion.pool.queue.size`
- `docgen.pdf.conversion.pool.queue.remaining`
- `docgen.pdf.conversion.pool.rejections` (reject path only; timeout path asserts **0**)

## Residuals / honesty

- Real-`soffice` saturation+recovery half **skipped** on this host — use `-Plibreoffice-ci` on a LO-equipped agent for fail-closed proof.
- **Not** claimed: #3b/#5a GO, go-live, Wave D Done, IBL-D5, Word / B7, confirmed NFR SLOs.
