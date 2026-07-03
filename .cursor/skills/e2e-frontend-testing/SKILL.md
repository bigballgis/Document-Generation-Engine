---
name: e2e-frontend-testing
description: Automated frontend end-to-end testing workflow with Playwright for the management UI. Use to encode BDD acceptance scenarios as user-journey tests (functional) and to drive UIUX visual/responsive/accessibility evidence for any user-facing slice before Done.
---

# E2E Frontend Testing (Playwright)

Encode user journeys as durable, deterministic tests traceable to BDD acceptance scenarios.

## Assets

- **Dev config**: `frontend/playwright.config.ts` (Chromium, `baseURL` from `FRONTEND_PORT`
  default 5173, web server `pnpm dev`, `trace: on-first-retry`). Auto-switches to docker mode
  when `E2E_TARGET=docker` or `FRONTEND_PORT=4173`.
- **Docker acceptance config**: `frontend/playwright.docker.config.ts` (no webServer, baseURL 4173,
  report `playwright-report/docker`). **Acceptance runs use this** — the stack must already be
  deployed via `.\scripts\docker-deploy.ps1`.
- Specs: `frontend/e2e/*.spec.ts` — 52+ specs named `P<phase>-T<task>-<slug>.spec.ts`,
  `*-uiux-evidence.spec.ts`, plus `role-journeys.spec.ts`, `catalog.spec.ts`, `a11y-smoke.spec.ts`.
- Helpers: `frontend/e2e/helpers/` — `auth.ts` (seeded users, `loginAs`), `nav.ts`, `ui.ts`
  (Element Plus interaction), per-domain API fixture helpers (`masters-api.ts`,
  `template-testing-api.ts`, `content-modules-api.ts`, …), `uiux-evidence.ts` (screenshots,
  1440×900 viewport, `switchBrand`).
- Teardown: `e2e/global-teardown.ts` deletes templates with externalId prefix `E2E-`
  (skip via `E2E_SKIP_CATALOG_CLEANUP=true`).
- Seeded users: `10000001` GLOBAL_ADMIN + role accounts, all password `ChangeMe123!` (see `auth.ts`).

## Commands

```bash
pnpm -C frontend test:e2e                    # dev-mode run (starts pnpm dev @5173)
pnpm -C frontend test:e2e:docker             # docker acceptance smoke set @4173
pnpm -C frontend exec playwright test <spec> --config playwright.docker.config.ts  # targeted docker run
pnpm -C frontend test:e2e:install            # first time / CI: install Chromium + deps
```

## Two complementary tracks

| Track | Owner agent | Asserts |
| --- | --- | --- |
| Functional journeys | `e2e-test-engineer` | login, navigation, role access, workflow completion, forbidden states |
| UIUX evidence | `e2e-uiux-reviewer` | visual quality, responsive, a11y, dual-brand, overflow/overlap, polish |

## Authoring rules

1. Start from the BDD acceptance scenarios (Given/When/Then).
2. Write the failing spec before the app behavior exists; report gaps to the engineers.
3. Seed state via the API fixture helpers (not UI clicking) unless the UI flow itself is
   the behavior under test; prefix fixture externalIds with `E2E-` for teardown cleanup.
4. Extend an existing spec covering the same surface before creating a new file.
5. Use web-first assertions and auto-waiting; no hard `sleep`.
6. Capture evidence: traces, HTML report (CI), screenshots for UIUX (both brands where relevant).
7. Never embed real secrets; seeded test credentials are non-production accounts.
8. Acceptance verdicts (for Done claims) come from docker-config runs, not dev-server runs.

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
