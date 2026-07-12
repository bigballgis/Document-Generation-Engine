# Frontend

Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router + vue-i18n (English base).

## Commands

```powershell
pnpm install
pnpm dev
pnpm lint
pnpm type-check
pnpm knip          # unused files / exports / deps (Knip)
pnpm knip:prod     # production-entry scan only
pnpm test
pnpm build
```

Dead-code scan evidence: [`docs/evidence/slim-knip-scan/`](../docs/evidence/slim-knip-scan/README.md).
Repo helper: `.\scripts\knip-scan.ps1`.

## P0 scope

- Login route shell (no backend auth wiring yet — P1)
- REDBC / GREENBC theme tokens via CSS variables
- English-first i18n message keys
