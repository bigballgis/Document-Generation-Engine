# IBL-D4 / #126 — LibreOffice pool chaos / failover evidence

| Field | Value |
| --- | --- |
| **Slice** | `ibl-d4-lo-pool-chaos` |
| **Task Master** | **#126** |
| **Finding** | **F22** closed (chaos half; with D3 k6 path) — leaf **Done** `94cc8eeb` / `94526674` |
| **BDD** | [ibl-d4-lo-pool-chaos.md](../../../behavior/ibl-d4-lo-pool-chaos.md) (`not-applicable`) |
| **Depends** | IBL-B2 metrics — [pdf-conversion-capacity-plan.md](../../../operations/pdf-conversion-capacity-plan.md) |
| **frontend_ui_in_scope** | false |
| **Go-live / #3b / #5a / Wave D Done** | **not** claimed |

## Suite

| Artifact | Role |
| --- | --- |
| `PdfConversionPoolChaosTest` | Deterministic saturation / timeout / reject / failover recovery + B2 Micrometer gauges/counters |
| `LibreOfficePdfConversionPoolChaosIntegrationTest` | Fake hang process timeout (default verify); real `soffice` saturation+recovery (`@Tag("libreoffice")`) |
| Maven profile **`lo-pool-chaos`** | Focused IT lane: Surefire `groups=lo-pool-chaos` |
| Hang scripts | `backend/src/test/resources/scripts/fake-libreoffice-hang.{cmd,sh}` |

### B2 metrics asserted

| Metric | Chaos coverage |
| --- | --- |
| `docgen.pdf.conversion.pool.active` | Saturation / reject / recovery |
| `docgen.pdf.conversion.pool.queue.size` | Bounded absorb then full |
| `docgen.pdf.conversion.pool.queue.remaining` | Zero under full pressure |
| `docgen.pdf.conversion.pool.rejections` | Increment on capacity reject; **not** on timeout |

## How to reproduce

```powershell
# Focused chaos lane (deterministic + LO hang; real soffice may skip)
mvn -B -ntp -f backend/pom.xml "-Plo-pool-chaos,dev-fast" test

# Default full gate (includes chaos classes)
mvn -B -ntp -f backend/pom.xml verify

# Real-soffice half fail-closed (combine with libreoffice-ci job as needed)
mvn -B -ntp -f backend/pom.xml "-Plibreoffice-ci,dev-fast" test
```

## Honesty

- Real-`soffice` saturation test **skips** when LibreOffice is absent on the host
  (documented optional skip). Under `-Plibreoffice-ci` the same check **fails closed**.
- This leaf does **not** invent confirmed NFR SLOs or change B2 capacity defaults.

## Artifacts

| File | Role |
| --- | --- |
| [latest-summary.md](./latest-summary.md) | Human mirror of last chaos / verify evidence |
| [latest-summary.json](./latest-summary.json) | Machine-readable summary |
