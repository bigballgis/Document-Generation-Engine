# IBL-E7 / #134 — RTL / bidi spike evidence

Spike (BDD **not-applicable**). Closes finding **F15** by durable **descope** ([ADR-0068 Accepted](../../../adr/rendering-authoring/0068-rtl-bidi-out-of-scope-until-market.md)) — not a product RTL implementation.

| Item | Link |
| --- | --- |
| Spike report (feasibility, LO/POI gaps, verdict) | [SPIKE-REPORT.md](./SPIKE-REPORT.md) |
| Accepted ADR (descope + reopen gates) | [ADR-0068](../../../adr/rendering-authoring/0068-rtl-bidi-out-of-scope-until-market.md) |
| Inventory probe | `backend/src/test/java/com/bank/docgen/rendering/RtlBidiInventoryProbeTest.java` |

**Verdict:** **DESCOPE** RTL/bidi from IBL / v1 go-live; ADR-0068 **Accepted** (2026-07-20). **Accepted ≠ #134 leaf Done.** Do **not** flip **#3b/#5a**; PD-6/7 **OUT**; no Word invent; no full RTL product impl.
