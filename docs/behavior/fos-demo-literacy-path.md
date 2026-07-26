# Behavior: FOS-W14 Demo Literacy Path

**Status:** Confirmed for delivery  
**Traceability:** TM #184 · `fos-demo-literacy-path` · W14-1…W14-4

## Goal

Learners can load KEEP-8 demos on Linux, walk import→publish→open→contract, and KEEP-8
runtime generate fails closed when catalog incomplete. One flagship module uses `nodes`.

## Acceptance

- W14-1 `demo-runtime-generate` throws when KEEP-8 missing/not PUBLISHED
- W14-2 `deploy/*-all-demos.sh` wrappers call pwsh scripts
- W14-3 Learner walkthrough linked from docs + deploy README
- W14-4 At least one KEEP-8 content module migrated `blocks` → `nodes` (commitment letter)
