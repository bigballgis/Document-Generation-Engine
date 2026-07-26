# Gates — fos-credential-lifecycle (#180 / FOS-W10)

**Date:** 2026-07-26  
**Branch:** `feat/fos-credential-lifecycle`  
**Scope:** W10-1…W10-5

## Backend

| Gate | Result |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | **PASS** |

## Frontend

| Gate | Result |
| --- | --- |
| `pnpm -C frontend lint` | **PASS** |
| `pnpm -C frontend type-check` | **PASS** |
| `pnpm -C frontend test` | **PASS** (1772) |
| `pnpm -C frontend build` | **PASS** |

## Deploy / E2E

| Gate | Result |
| --- | --- |
| Docker deploy queue / E2E | **BLOCKED** — daemon up, **0 images**; no greenwash |

## Implemented

- W10-1 Previous secret hash + 28-day `rotation_grace_period_ends_at`; auth accepts prior hash in grace
- W10-2 Rotate gated on effective ACTIVE/EXPIRING_SOON; rebases `expiresAt`
- W10-3 `expiresAt` / grace on create/rotate/summary + credentials UI column + secret dialog
- W10-4 Optional `expiryDays` on create; expiry alerts INFO (30d) → WARNING (≤7d)
- W10-5 Copy secret; rotate confirm mentions 28-day grace (parent confirm already present)
