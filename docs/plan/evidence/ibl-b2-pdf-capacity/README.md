# IBL-B2 PDF capacity evidence

| Field | Value |
| --- | --- |
| **Slice** | `ibl-b2-pdf-conversion-capacity` / Task Master **#114** |
| **BDD** | [ibl-b2-pdf-conversion-capacity.md](../../../behavior/ibl-b2-pdf-conversion-capacity.md) |
| **Capacity plan** | [pdf-conversion-capacity-plan.md](../../../operations/pdf-conversion-capacity-plan.md) |
| **DEF** | [TRIAGE-pdf-422.md](../lrp-d6-load-smoke/TRIAGE-pdf-422.md) |

## Stage 4 (code/config) — recorded

| Evidence | Status |
| --- | --- |
| Product defaults pool=4 / queue=8 (`application.yml`, `DocgenRenderingProperties`) | Delivered |
| Binding + executor AbortPolicy tests (`DocgenRenderingPropertiesBindingTest`) | GREEN |
| Bounded-queue absorb then reject (`PdfConversionOffloadSupportTest#absorbsBurstIntoBoundedQueueThenRejectsWhenFull`) | GREEN |
| Capacity plan indexed from ops / docs README | Delivered |
| `mvn -B -ntp -f backend/pom.xml verify` | See Stage 4 gate log on feature branch |

## Stage 5/10 (queued Docker agreed smoke) — pending

Agreed smoke (B2-C7/C8): LR-D6-class concurrent FOL sync PDF; PDF failure count **&lt; 8**; expect
`poolRejectionCount` ≈ 0 under default 4+8. Machine-readable summary will be added here after
`docker-deploy-queue` + harness run (build-deploy-agent).

Do **not** treat this directory as go-live evidence. Do **not** invent NFR SLOs.
