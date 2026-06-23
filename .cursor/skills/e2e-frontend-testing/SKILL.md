---
name: e2e-frontend-testing
description: Automated frontend end-to-end testing workflow with Playwright for the management UI. Use to encode BDD acceptance scenarios as user-journey tests (functional) and to drive UIUX visual/responsive/accessibility evidence for any user-facing slice before Done.
---

# E2E Frontend Testing (Playwright)

Encode user journeys as durable, deterministic tests traceable to BDD acceptance scenarios.

## Assets

- Config: `frontend/playwright.config.ts` (Chromium, `baseURL` from `FRONTEND_PORT` default 5173,
  web server `pnpm dev`, `trace: on-first-retry`).
- Specs: `frontend/e2e/*.spec.ts` (e.g. existing `a11y-smoke.spec.ts`).

## Commands

```bash
pnpm -C frontend test:e2e            # run E2E suite
pnpm -C frontend test:e2e:install    # first time / CI: install Chromium + deps
```

## Two complementary tracks

| Track | Owner agent | Asserts |
| --- | --- | --- |
| Functional journeys | `e2e-test-engineer` | login, navigation, role access, workflow completion, forbidden states |
| UIUX evidence | `e2e-uiux-reviewer` | visual quality, responsive, a11y, dual-brand, overflow/overlap, polish |

## Authoring rules

1. Start from the BDD acceptance scenarios (Given/When/Then).
2. Write the failing spec before the app behavior exists; report gaps to the engineers.
3. Stable, role-driven setup; seeded users/state; no dependence on test order.
4. Use web-first assertions and auto-waiting; no hard `sleep`.
5. Capture evidence: traces, HTML report (CI), screenshots for UIUX (both brands where relevant).
6. Never embed real secrets; use fixtures / env-injected non-production credentials.

## Pipeline position

```
frontend-engineer (unit/component green) → e2e-test-engineer (functional) → e2e-uiux-reviewer (UIUX) → doc sync → commit
```

## Done

- Functional journeys for the slice pass and map to acceptance scenarios.
- UIUX evidence manifest produced; 🔴 findings resolved.

## Related

- `.cursor/agents/e2e-test-engineer.md`, `.cursor/agents/e2e-uiux-reviewer.md`
- `.cursor/skills/frontend-oa-design/SKILL.md`
