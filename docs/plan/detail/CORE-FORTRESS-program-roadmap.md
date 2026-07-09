# CORE-FORTRESS Program Roadmap (F1–F8)

**Program:** Bank-grade core hardening — rendering kernel → runtime → production → frontend → evidence.

| Phase | Name | Status | Detail |
| --- | --- | --- | --- |
| F1 | Rendering core correctness | **Done** (2026-07-09) | [CORE-FORTRESS-f1-rendering-correctness.md](./CORE-FORTRESS-f1-rendering-correctness.md) |
| F2 | Runtime lightweight | **Done** (2026-07-09) | [CORE-FORTRESS-f2-runtime-lightweight.md](./CORE-FORTRESS-f2-runtime-lightweight.md) |
| F3 | Node matrix + expression engine | **Done** (2026-07-09) | [CORE-FORTRESS-f3-node-matrix-expression.md](./CORE-FORTRESS-f3-node-matrix-expression.md) |
| F4 | Production rendering hardening (LO pool, fonts, pagination) | **Done** (2026-07-09; code complete; `mvn verify` env caveat) | [CORE-FORTRESS-f4-production-rendering-hardening.md](./CORE-FORTRESS-f4-production-rendering-hardening.md) |
| F5 | Async durability + security depth | **Done** (2026-07-09; F5 targeted **31/31**; full `mvn verify` Windows env caveat) | [CORE-FORTRESS-f5-async-durability-security.md](./CORE-FORTRESS-f5-async-durability-security.md) |
| F6 | Frontend kernel refactor | **Done** (2026-07-09; F6-T08 E2E env blocker documented) | [CORE-FORTRESS-f6-frontend-kernel-refactor.md](./CORE-FORTRESS-f6-frontend-kernel-refactor.md) |
| F7 | Authoring UX (dirty guard, side-by-side preview) | **Done** (2026-07-09; Vitest **894**; E2E **12/12**) | [CORE-FORTRESS-f7-authoring-ux.md](./CORE-FORTRESS-f7-authoring-ux.md) |
| F8 | Observability, SLO, DR, evidence bundle | **Done** (2026-07-09; `mvn verify` **1154** tests; arch review PASS) | [CORE-FORTRESS-f8-observability-slo-dr.md](./CORE-FORTRESS-f8-observability-slo-dr.md) |

**Program status:** **CORE-FORTRESS Done** (2026-07-09) — F1–F8 complete.

**Autonomous execution order:** F2 → F3 → F4 → F5 → (F6∥F7 after F5) → F8.
