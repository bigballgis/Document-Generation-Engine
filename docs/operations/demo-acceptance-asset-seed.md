# Demo / 验收 managed-asset seed (SYS-NORM Wave 8)

**Status:** Confirmed ops contract — **implemented** (TM **#152** Done; MAIN merge `8aca145b` / feature `7df6c563`); not production-default.  
**Program:** [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) Wave **8 Done**  
**Behavior SoT:** [sys-norm-demo-seed-terms.md](../behavior/sys-norm-demo-seed-terms.md) (**BDD-SYS-NORM-W8-001…018**; W8-C1…C3)  
**Related:** [ce-e02-asset-library.md](../behavior/ce-e02-asset-library.md) · [core-fortress-f1-rendering-correctness.md](../behavior/core-fortress-f1-rendering-correctness.md) F1-C5 · [prod-true-prod-contract.md](../behavior/prod-true-prod-contract.md) TPC-C7

---

## 1. Product default — honest empty

| Environment | Asset Library when zero managed `library_asset` |
| --- | --- |
| **Production / true-prod defaults** | **Honest empty** (title + next-step copy + Upload CTA when permitted). **No** fabricated sample rows. |
| **Demo / 验收 (optional)** | May enable a **managed-asset seed** profile/Flyway path so catalogs are non-empty for walkthroughs. |

Honest empty remains correct whenever the seed path is **off** or absent. Seed is **not** required for production empty correctness.

---

## 2. Optional demo/验收 seed path

**Confirmed intent (Wave 8):**

- A property-gated ApplicationRunner seed inserts minimal managed `library_asset` rows
  (`IMG-1` IMAGE, `SEAL-1` SEAL) aligned with demo bindings.
- Seed inserts **managed catalog rows** (listable via Asset Library API/UI) — not classpath-only ghosts.
- Source bytes are copied from classpath `rendering/demo-images/` into MinIO + `library_asset`
  via `AssetLibraryService.upload` — classpath alone never appears as catalog rows (N23).
- **Off by default** on production / true-prod claiming profiles.

**Mechanism (landed):**

| Item | Value |
| --- | --- |
| Property | `docgen.demo-asset-library.seed-enabled` |
| Env | `DOCGEN_SEED_DEMO_ASSET_LIBRARY` (default `false`) |
| Component | `com.bank.docgen.demo.DemoAssetLibrarySeeder` (`@ConditionalOnProperty` … `havingValue=true`) |
| Seeded keys | `IMG-1` (IMAGE), `SEAL-1` (SEAL) |
| Compose | `docker-compose.prod.yml` → `${DOCGEN_SEED_DEMO_ASSET_LIBRARY:-false}` |

**Enable (demo/验收 only):**

```bash
DOCGEN_SEED_DEMO_ASSET_LIBRARY=true
```

**Forbidden:**

- Turning managed-asset seed **on** as the production default.
- Documenting or UX-implying that empty catalogs are “broken” without seed.
- Changing F1 `StructuredContentImageResolver` signature or MinIO → demo classpath → fail-closed order (CE-E02 E02-C13).

---

## 3. N23 — `demo-images` bypass ≠ Asset Library

| Tier | What it is | What it is **not** |
| --- | --- | --- |
| **Managed Asset Library** | Durable `library_asset` rows + MinIO object bytes; listed on `/library/assets` | — |
| **Classpath `rendering/demo-images/`** | LAB/test **rendering fallback** when object storage miss and demo classpath tier is **explicitly** enabled (`DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED` / profile) | Asset Library catalog content |

**Rules:**

1. Management Asset Library must **not** imply classpath-only keys are “in the library.”
2. Production / true-prod keeps demo classpath tier **off** by default (TPC-C7).
3. A LAB render may still resolve a demo classpath image while Asset Library list remains empty — that is expected under N23, not a catalog seed.

---

## 4. Operator checklist (acceptance)

1. Confirm production-shaped stack: seed **off**, demo classpath tier **off**, Asset Library honest empty when zero rows.
2. For demo/验收 walkthroughs only: enable the Wave 8 seed path → list shows seeded managed keys → bindings can resolve via MinIO/managed path without relying on classpath alone.
3. Do **not** flip launch checklist **#3b** / **#5a** from this seed work.

---

## 5. Explicitly out of scope (post-program parked queue)

Not Wave 8 substitutes — see plan §4a:

- Reminder timing  
- Asset library group isolation  
- Binding editor re-layout  
- Auto `referenceKey`  

**N18** Legal hold actor EntityLink remains **deferred** (not claimed Done by Wave 8).
