# Gates — fos-word-foundation-honesty (#185 / FOS-W15)

**Date:** 2026-07-26  
**Slice:** `fos-word-foundation-honesty`  
**Worktree:** `/home/ubuntu/DGE-fos-word-foundation-honesty` · `feat/fos-word-foundation-honesty`

## Batch recommendation

```
batch_recommendation:
  decision: solo
  member_task_ids: ["185"]
  proposed_slice_id: fos-word-foundation-honesty
  delivery_lane: full
```

## Unit / static gates

| Gate | Result | Notes |
| --- | --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | **PASS** | Includes Checkstyle/PMD/SpotBugs/JaCoCo; NestedTableOrder + GeneratedLetterTypography + FOL logo |
| `pnpm -C frontend lint` | **PASS** | |
| `pnpm -C frontend type-check` | **PASS** | |
| `pnpm -C frontend test` | **PASS** | 1775 tests |
| `pnpm -C frontend build` | **PASS** | |

## Deploy / E2E

| Gate | Result | Notes |
| --- | --- | --- |
| `docker-deploy-queue` / app stack | **BLOCKED** | Docker daemon up; `docker run` container start fails (containerd overlay mount). Honest BLOCKED — no invented greens. |
| Playwright E2E / UIUX | **BLOCKED** | Depends on acceptance stack :8080/:4173 |

## WF disposition (summary)

| Id | Disposition |
| --- | --- |
| WF-1 Nested table/list | Remediated |
| WF-2 Multi-child order | Remediated (post-table cursor advance) |
| WF-3 Clause numbering | Documented (behavior + FE honesty note) |
| WF-4 headerRows | Remediated (overlays + writer + PS repair) |
| WF-5 Style-manifest | Remediated (spacing + eastAsia) |
| WF-6 Generated typography | Remediated (`DemoGeneratedLetterTypographyTest`) |
| WF-7 Money formatters | Documented (caller-owned until CRCH W2 KEEP-8) |
| WF-8 Logo letterhead | Remediated (FOL `word/media/image1.png`) |
