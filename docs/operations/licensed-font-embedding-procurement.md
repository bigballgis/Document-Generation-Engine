# Licensed font embedding — procurement checklist (PD-7)

## Purpose

Operational checklist for **pursuing** licensed TrueType/OpenType faces (e.g. Microsoft
**Calibri** / **Cambria**) for embedding in backend DOCX→PDF conversion images.

Authority:

- Product: IBL §8 **PD-7** — Confirmed pursue **2026-07-19**
- ADR: [ADR-0069 Accepted](../adr/rendering-authoring/0069-licensed-font-embedding-pursue.md)
  (pursue path + procurement gate)
- Shipped baseline today: [ADR-0041 Accepted](../adr/rendering-authoring/0041-rendering-font-baseline.md)
  (Carlito/Caladea + CJK — **not** licensed Calibri)
- Legal freeze fonts: [ADR-0060](../adr/rendering-authoring/0060-legal-reproducibility-freeze.md)
  still cites ADR-0041
- Behavior readiness: [pd7-licensed-font-embedding.md](../behavior/pd7-licensed-font-embedding.md)
  (`bdd_readiness: not-applicable`)
- Slice: `pd7-licensed-font-embedding` (Task Master **#139** → **Done** pursue docs `b966874a`; sole-active **cleared**; **Accepted ≠ embedding Done**)

**This checklist is not a go-live authorization.** Completing docs here does **not** flip
launch checklist **#3b GO** / **#5a GO**, does **not** mark licensed embedding Done, and
does **not** authorize inventing font binaries in git.

---

## Confirmed vs pending

| Class | Items |
| --- | --- |
| **Confirmed** | Product intent to **pursue** licensed embedding (2026-07-19); ADR-0041 remains shipped substitute baseline; ADR-0069 Accepted for pursue/gate governance; no font binaries in repo; LRP pairing without GO flip |
| **Pending** | All procurement, license, delivery-path, dependency-policy, Dockerfile, and freeze-recut items in §Gates below |

---

## Residual cannot-ship list

Licensed font embedding **cannot ship** while any of these remain open:

1. No company-approved **redistributable** license for server/container embedding.
2. No recorded vendor pack (SKU / version / entitlement) for the required faces.
3. No approved **non-git** delivery path for font files into the image build.
4. Dependency policy has not verified baking the pack into jammy runtime images.
5. Conversion images still only install ADR-0041 substitutes (Carlito/Caladea + CJK).
6. No Accepted ADR-0041 amendment / successor for a **new shipped** package set.
7. No ADR-0060 legal-freeze re-cut if legal baselines must use the licensed set.
8. (Optional follow-on) No fail-closed runtime/mode hooks with `bdd_readiness: ready`.

---

## Gates checklist

Use status vocabulary: `Pending` | `In Progress` | `Blocked` | `Done`.  
**Do not** mark a gate `Done` without durable evidence (license instrument, ticket, or
build artifact path — never a committed font binary in this repo).

### A. Legal / procurement

| ID | Gate | Status | Evidence / notes |
| --- | --- | --- | --- |
| **A1** | Legal confirms license covers **embedding in Docker/server** conversion images (not desktop-only) | **Pending** | — |
| **A2** | Procurement selects vendor / SKU covering Calibri (+ Cambria if required) | **Pending** | Exact SKU **TBD** — do not invent product names beyond confirmed intent |
| **A3** | License instrument + entitlement scope archived in company system of record | **Pending** | Link ticket/path when available; **not** stored as binary in git |
| **A4** | Redistribution / OEM / cloud restrictions reviewed (multi-instance, CI images, air-gapped) | **Pending** | — |

### B. Asset delivery & hygiene

| ID | Gate | Status | Evidence / notes |
| --- | --- | --- | --- |
| **B1** | Secure artifact store or secret volume chosen for font files | **Pending** | Private registry / vault / build secret — **not** public git |
| **B2** | Access control + audit for who may pull the pack into builds | **Pending** | — |
| **B3** | Explicit ban enforced: **no** `.ttf` / `.otf` commit to this repository | **Confirmed policy** (ADR-0069) | Reviewers reject font binary PRs |

### C. Platform / dependency policy

| ID | Gate | Status | Evidence / notes |
| --- | --- | --- | --- |
| **C1** | Company-approved dependency policy allows the licensed pack in runtime images | **Pending** | Coordinate with tech-stack / security owners |
| **C2** | Image size / supply-chain review for adding the pack | **Pending** | — |
| **C3** | Decision recorded: replace Carlito/Caladea **or** dual-install with preference order | **Pending** | Requires ADR-0041 amendment / successor when ready |

### D. Engineering follow-on (OUT of PD-7 docs leaf Done bar for *embedding*)

| ID | Gate | Status | Evidence / notes |
| --- | --- | --- | --- |
| **D1** | Dockerfile install path for licensed pack (jammy runtime stages) | **Pending** | Follow-on leaf after A/B/C |
| **D2** | `fc-cache` / `fc-list` assertions for licensed face names | **Pending** | — |
| **D3** | `RenderingFontSmokeTest` (or successor) updated for licensed faces | **Pending** | — |
| **D4** | ADR-0041 amended or successor Accepted for new **shipped** set | **Pending** | ADR-0069 stays pursue authority until then |
| **D5** | ADR-0060 freeze re-cut + ops freeze doc update if legal baselines move | **Pending** | See [legal-reproducibility-freeze.md](./legal-reproducibility-freeze.md) |
| **D6** | Optional fail-closed “licensed mode” when pack absent | **Pending** | Needs `bdd_readiness: ready` leaf |

---

## LRP pairing (no GO flip)

| Surface | Today | PD-7 pairing |
| --- | --- | --- |
| LR-A2 / LR-A5 | **Done** — substitute font baseline + ADR-0041 Accepted | Remains the **shipped** path; PD-7 does not reopen LR-A as failed |
| Checklist row **#3** (LR-A critical) | **GO** (A1/A2/A3 Done — substitutes) | Stays **GO** for substitutes; licensed pursue is **extra** residual |
| Checklist **#3b** | **CONDITIONAL** (Path X ≠ GO; Path E for GO) | **Unchanged** — PD-7 must **not** flip to GO |
| Checklist **#5a** | **CONDITIONAL** (LDAP/AD residual) | **Unchanged** — unrelated to fonts |
| Overall checklist | **CONDITIONAL** | Remains **CONDITIONAL**; PD-7 docs ≠ production go-live |
| ADR-0060 / F16 freeze | ADR-0041 font set | Continues until D5; licensed faces are **not** the freeze set today |

Cross-link: [launch-readiness-checklist.md](./launch-readiness-checklist.md) ·
[launch-readiness-program.md](../plan/launch-readiness-program.md) (LR-A2 / LR-A5).

---

## Honesty statements (durable)

1. **Carlito ≠ Calibri.** Metric compatibility does not satisfy a “licensed Calibri
   embedding” claim.
2. **Cannot ship licensed embedding without licensed assets.** Docs and ADRs alone never
   constitute embedding Done.
3. **No fake fonts.** Do not substitute renamed free fonts or invent binaries to green
   a checklist.
4. **ADR-0041 remains baseline** until a post-procurement Accepted amendment/successor
   changes the shipped package set.

---

## Change log

| Date | Change |
| --- | --- |
| 2026-07-20 | **#139 pursue docs → Done** (`b966874a`; sole-active **cleared**). Checklist remains operational; license/delivery gates still **Pending**. **Accepted ≠ embedding Done**. No **#3b/#5a GO** flips. |
| 2026-07-20 | Initial checklist (slice `pd7-licensed-font-embedding` / PD-7 pursue; [ADR-0069](../adr/rendering-authoring/0069-licensed-font-embedding-pursue.md)). All procurement gates **Pending**. No GO flips. |
