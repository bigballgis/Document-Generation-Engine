---
id: BDD-CURSOR-SCAFFOLD-HYGIENE
title: Cursor-only agent/docs scaffold hygiene
status: not-applicable
date: 2026-07-14
bdd_readiness: not-applicable
---

# Cursor scaffold hygiene

## Classification

**bdd_readiness: not-applicable** — ops/docs scaffolding and agent-system hygiene.
No product user-facing behavior, no Playwright/product E2E, no runtime API change.

## Goal

Make the repository **Cursor-canonical**: remove Claude Code / dual-agent leftovers,
reconcile source-of-truth order, align pipeline stage 14, refresh indexes, and add a
lightweight doc-structure validator.

## Actors

- Parent Cursor agent / delivery-orchestrator
- Maintainers editing `.cursor/`, `docs/`, root agent entrypoints

## Out of scope

- Product UI/API behavior
- Formal plan phase activation
- Accepted ADR decision-body rewrites (amend via new ADR only)
- Full ledger historical compaction
