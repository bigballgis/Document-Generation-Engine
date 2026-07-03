# Coverage Ratchet Plan (SOR-C02)

**Status:** Active ratchet toward constitution targets (ADR-0035).  
**Bundle floors (enforced in CI):** backend JaCoCo LINE ≥ 0.70 / BRANCH ≥ 0.45; frontend Vitest lines 22 / functions 32 / branches 55.  
**Constitution targets:** changed lines ≥ 85%; security-critical / core domain ≥ 90%.

## Per-package backend floors (next ratchet)

| Package | Current bundle share | Target floor | Cadence |
| --- | --- | --- | --- |
| `com.bank.docgen.authorization` | security-critical | 0.90 line | +0.05 per SOR slice |
| `com.bank.docgen.runtime` | security-critical | 0.90 line | +0.05 per SOR slice |
| `com.bank.docgen.rendering` | core domain | 0.85 line | +0.05 per SOR slice |
| Remaining modules | standard | 0.85 line | bundle ratchet +0.02/quarter |

## Diff-coverage (PR gate — incremental)

1. PR workflow runs full `mvn verify` + `pnpm test --coverage`.
2. Maintainers review JaCoCo HTML report for touched packages before merge.
3. Next increment: wire `jacoco:report` diff against `origin/main` (blocked on bank runner mirror per SOR Q2).

## Evidence

- CI: `.github/workflows/coverage-ratchet.yml`
- Bundle enforcement: `backend/pom.xml`, `frontend/vitest.config.ts`
