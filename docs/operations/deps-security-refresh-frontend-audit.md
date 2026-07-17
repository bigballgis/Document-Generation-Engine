# deps-security-refresh — Frontend pnpm audit evidence

| Field | Value |
| --- | --- |
| **Slice** | `deps-security-refresh` (Task Master **#49**) |
| **Worktree** | `D:/working/DGE-deps-security-refresh` · `feat/deps-security-refresh` |
| **Recorded** | 2026-07-13 |
| **Scope** | Frontend only (`frontend/package.json` + `frontend/pnpm-lock.yaml`) |
| **Commit** | **no-commit** (implementer handoff) |

---

## Audit commands

```powershell
pnpm -C frontend outdated
pnpm -C frontend audit --prod
pnpm -C frontend audit
```

### Results (after remediation)

| Scope | Result |
| --- | --- |
| `audit --prod` | **No known vulnerabilities found** |
| `audit` (all) | **No known vulnerabilities found** (2026-07-17 — Vitest Critical closed; see exception CLOSED) |
| High / Moderate | **0** after `pnpm.overrides.vite = 6.4.3` |

### Pre-remediation findings (before overrides / floors)

| Severity | Package | Advisory | Notes |
| --- | --- | --- | --- |
| Critical | `vitest` `<3.2.6` | [GHSA-5xrq-8626-4rwp](https://github.com/advisories/GHSA-5xrq-8626-4rwp) | Vitest UI arbitrary file read/exec when UI server listening |
| High | `vite` `<=6.4.2` (via Vitest → `vite@5.4.21`) | [GHSA-fx2h-pf6j-xcff](https://github.com/advisories/GHSA-fx2h-pf6j-xcff) | Windows `server.fs.deny` bypass |
| Moderate | `esbuild` `<=0.24.2` (via nested vite 5) | [GHSA-67mh-4wv8-2f99](https://github.com/advisories/GHSA-67mh-4wv8-2f99) | Dev-server request smuggling |
| Moderate | `vite` path traversal `.map` | [GHSA-4w7w-66w2-5vf9](https://github.com/advisories/GHSA-4w7w-66w2-5vf9) | Nested vite 5 |
| Moderate | `launch-editor` via vite | [GHSA-v6wh-96g9-6wx3](https://github.com/advisories/GHSA-v6wh-96g9-6wx3) | Nested vite 5 |

Direct app `vite@6.4.3` was already on the patched line; High/Moderate came from Vitest 2’s nested `vite@5.4.21`.

---

## Remediation applied

1. Raised caret floors in `frontend/package.json` to current baseline-safe resolved versions.
2. Applied patch/minor bumps within Vue 3 / Vite 6 / Vitest 2 / Element Plus 2 / vue-router 4.
3. Added `pnpm.overrides.vite = "6.4.3"` so Vitest/vite-node/@vitest/mocker resolve the patched Vite 6 line (eliminates High + 3 Moderate).
4. Refreshed `frontend/pnpm-lock.yaml`.

### Notable upgrades (declared / installed)

| Package | Before (package.json floor / lock tip) | After |
| --- | --- | --- |
| `vite` | `^6.0.5` / app tip 6.4.3 + nested 5.4.21 | `^6.4.3` + override **6.4.3 only** |
| `vitest` | `^2.1.8` → 2.1.9 | `^3.2.7` / **3.2.7** (Task Master **#50** — 2026-07-17) |
| `vue` | `^3.5.13` / 3.5.38 | `^3.5.39` / **3.5.39** |
| `element-plus` | `^2.9.1` / 2.14.2 | `^2.14.3` / **2.14.3** |
| `axios` | `^1.7.9` / 1.18.1 | `^1.18.1` / **1.18.1** |
| `pinia` | `^2.3.0` / 2.3.1 | `^2.3.1` / **2.3.1** |
| `vue-router` | `^4.5.0` / 4.6.4 | `^4.6.4` / **4.6.4** |
| `vue-i18n` | `^10.0.5` / 10.0.8 | `^10.0.8` / **10.0.8** |
| `@playwright/test` | `^1.49.1` / 1.61.0 | `^1.61.1` / **1.61.1** |
| `typescript-eslint` | `^8.18.2` / 8.62.0 | `^8.63.0` / **8.63.0** |
| `eslint` / `@eslint/js` | `^9.17.0` | `^9.39.4` / **9.39.4** |

---

## Exception (Critical — GHSA-5xrq-8626-4rwp)

Exception Handling metadata per [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) (reason, risk, owner, expiration date, cleanup task id):

| Field | Value |
| --- | --- |
| Finding | [GHSA-5xrq-8626-4rwp](https://github.com/advisories/GHSA-5xrq-8626-4rwp) — Vitest UI arbitrary file read/exec |
| **Status** | **CLOSED (2026-07-17)** — pins landed on `feat/fe-vitest-3-upgrade` (Task Master **#50**): `vitest@3.2.7` + `@vitest/coverage-v8@3.2.7`; `pnpm -C frontend audit` and `audit --prod` both report **No known vulnerabilities found**; frontend gates green (see Gate evidence below). |
| Resolved versions | `vitest@^3.2.7` → **3.2.7** / `@vitest/coverage-v8@3.2.7` |
| Authorized target | `vitest >= 3.2.6` + aligned `@vitest/coverage-v8 >= 3.2.6` ([ADR-0029](../adr/technology-stack/0029-frontend-application-stack-baseline.md) amendment 2026-07-17) — **met** |
| **Reason (historical #49)** | Session stayed on Vitest **2** pending ADR major confirmation; ADR gate cleared 2026-07-17; pin delivered by **#50**. |
| **Risk (historical)** | Was **dev-only** (Vitest UI server). Production Docker UI / prod deps were never affected. |
| **Owner** | Frontend platform / Task Master **#50** (`fe-vitest-3-upgrade`) |
| **Expiration date** | N/A — **CLOSED** |
| **Cleanup task id** | Task Master **#50** — Vitest 3.2.6+ upgrade (**implemented** 2026-07-17; merge/doc-sync pending orchestrator) |

### Closeout evidence (2026-07-17)

1. `frontend/package.json` (+ lockfile): `vitest` **^3.2.7** / **3.2.7**, `@vitest/coverage-v8` **3.2.7**
2. `pnpm -C frontend audit` — **No known vulnerabilities found** (GHSA-5xrq-8626-4rwp cleared)
3. Frontend gates green (`lint` / `type-check` / `test` / `build`) — see Gate evidence below
4. Status row above stamped **CLOSED (2026-07-17)** with resolved versions

### Deferred majors (ADR-blocked unless separately confirmed)

| Package | Latest | Stay on (until confirmed) | Reason |
| --- | --- | --- | --- |
| `vitest` / `@vitest/coverage-v8` | 4.x | **3.2.7** (pinned #50) | Vitest **3.x** floor authorized by ADR-0029; **4.x** still deferred |
| `vite` | 8.x | **6.4.3** | Vite 6 baseline |
| `pinia` | 3.x | **2.3.1** | Pinia 2 baseline |
| `vue-router` | 5.x | **4.6.4** | Vue Router 4 baseline |
| `vue-i18n` | 11.x | **10.0.8** | vue-i18n 10 baseline (v10 deprecated upstream; migrate needs ADR) |
| `eslint` / `@eslint/js` | 10.x | **9.39.4** | ESLint 9 line |
| `typescript` | 7.x | **~5.7.3** | TS 5.7 baseline |
| `@vitejs/plugin-vue` | 6.x | **5.2.4** | Paired with Vite 6 plugin-vue 5 |
| `vue-tsc` | 3.x | **2.2.12** | Paired with Vue/TS baseline |
| `jsdom` | 29.x | **25.0.1** | Avoid large test-env jump this slice |
| `@cyclonedx/cyclonedx-npm` | 6.x | **5.0.0** | Major tooling jump deferred |
| `adm-zip` | 0.6.x | **0.5.18** | Minor major-ish line; leave until needed |

---

## Gate evidence (GREEN)

### #49 (2026-07-13 — Vitest 2.1.9 era)

```powershell
pnpm -C frontend lint        # ===LINT_OK===
pnpm -C frontend type-check  # ===TYPECHECK_OK===
pnpm -C frontend test        # Test Files 191 passed; Tests 1159 passed
                             # Coverage: stmts/lines 80.23%, branches 80.16%, funcs 64.61%
pnpm -C frontend build       # vite v6.4.3; built in ~17.20s; ===BUILD_OK===
```

### #50 (2026-07-17 — Vitest 3.2.7; GHSA-5xrq-8626-4rwp CLOSED)

```powershell
pnpm -C frontend lint        # ===LINT_OK===
pnpm -C frontend type-check  # ===TYPECHECK_OK===
pnpm -C frontend test        # Test Files 251 passed; Tests 1539 passed
                             # Coverage: stmts/lines 83.08%, branches 83.49%, funcs 61.43%
pnpm -C frontend build       # vite v6.4.3; built in ~24.51s; ===BUILD_OK===
pnpm -C frontend audit       # No known vulnerabilities found
pnpm -C frontend audit --prod # No known vulnerabilities found
```

---

## Behavior / E2E

- **No user-facing product behavior change** — dependency floors, lockfile, and vite override only.
- **E2E / UIUX:** not-applicable for this pure dependency hygiene slice.
