# CE-O02 — addressBlock / 多文档包（BDD readiness stub）

| Field | Value |
| --- | --- |
| **Slice** | `ce-o02-addressblock-package` |
| **bdd_readiness** | **`blocked`** |
| **Program status** | **Deferred** (product decision D5 — not reopened) |
| **Recorded** | 2026-07-20 |
| **Program leaf** | **B3 / CE-O02** — [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §8 Wave CE-O · CE-O02；§10 D5 |
| **Task Master** | **Do not register as In Progress implementation leaf** while blocked. Optional later: registry-only / `blocked` pending product — never sole-active writer until D5 is explicitly reopened and BDD reaches `ready`. |
| **Formal phase** | **None** |
| **Placement** | ISOLATED — `D:/working/DGE-ce-o02-addressblock-package` · `feat/ce-o02-addressblock-package` |
| **Frontend UI** | **Unknown** — not confirmed; do not invent management journeys |

**完成声明约束：** 本文件**不是**可交付 BDD；**不**宣称 CE-O02 Done；**不**发明 `addressBlock` schema / OpenAPI / 节点矩阵；**不**启动 TDD / 实现。

---

## 1. Why blocked (and deferred)

Session menu「做B」将 **B3 CE-O02** 列为剩余工作项，但 **产品历史权威决策尚未被显式推翻**：

| Source | Statement |
| --- | --- |
| CE program **§10 D5** | `addressBlock` 节点 + 多文档包 — **无需求确认**；待产品确认是否覆盖窗口信封/组合包场景 |
| **拍板记录（2026-07-14）** | **D5：按推荐默认（本期不做 addressBlock / 多文档包；后续如有窗口信封/组合包需求再立项）** |
| CE program **§8 CE-O02** | 标题 `addressBlock / 多文档包` · 级/量 P3·— · **待产品拍板后再细化（见 §10）** |
| CE program **§9 队列** | CE P3 queue empty；umbrella **#53** registry only — **do not** claim program Done（**CE-O02 deferred per D5**） |
| Umbrella TM **#53** | Explicit: **Skip CE-O02 (D5 deferred)** |

Attempted extract of minimal confirmed scope from PRD / domain model / requirements / CE program:

| Artifact | Finding |
| --- | --- |
| `docs/product/PRD.md` | **No** `addressBlock` / 窗口信封 / 组合包 confirmed requirement |
| `docs/domain/domain-model.md` | **No** address-block node or multi-doc package type |
| `docs/requirements/requirements-plan.md` | Mentions CE-O02 only as **non-goal** of other slices (e.g. CE-O01 / CE-E03) |
| CE program | Title + D5 deferral only — **no** actor, journey, schema, or acceptance |

**Verdict:** Insufficient confirmed behavior to author Given/When/Then. Inventing schema or journeys would violate document-as-code (assumptions ≠ confirmed requirements).

---

## 2. Confirmed (governance only — not product behavior)

These are **confirmed process facts**, not shippable CE-O02 behavior:

1. D5 recommended default recorded **2026-07-14** = **本期不做**.
2. CE-O02 remains a **placeholder row** until product reopens D5 with concrete scope.
3. Adjacent CE-O01 (PDF/A) is **Done** and explicitly **out of scope** for CE-O02.

---

## 3. Open questions (product — ≤6; blocking)

Answer these before any `bdd_readiness: ready` rewrite or implementation leaf:

1. **Reopen D5?** Is「做B → B3 CE-O02」an explicit product decision to **reopen** addressBlock / multi-doc package this period, or should D5 **2026-07-14「本期不做」** remain in force?
2. **Window envelope?** Must CE-O02 cover **窗口信封** layout (fixed address window positioning on letter stock), or is that out of scope / never?
3. **Multi-document package?** Is the second half a **runtime ZIP/组合包** of multiple generated artifacts (e.g. letter + cover), or something else (library export is already CE-E0x — do **not** conflate)?
4. **Schema / node model?** If addressBlock is in scope: which **fields** (recipient lines, country, postal code, …), structured-content **node type** name, and fail-closed validation — or defer schema until a dedicated product workshop? (**Do not invent until answered.**)
5. **Surface:** Runtime generate API only, management authoring UI, or both? (`frontend_ui_in_scope` unknown.)
6. **Locales / i18n:** Single locale address formatting, multi-locale address rules, or locale-agnostic free-form lines only?

---

## 4. Explicit non-goals (while blocked)

- **No** invented `addressBlock` JSON Schema, OpenAPI properties, or structured-content node matrix rows.
- **No** TDD Red tests, backend/frontend/rendering code, Flyway, or deploy evidence for CE-O02.
- **No** Task Master **In Progress** / sole-active implementation leaf registration for CE-O02.
- **No** claim that CE program / CE-O02 / go-live is Done.
- **No** silent override of D5 without new explicit product confirmation dated after 2026-07-14.

---

## 5. Acceptance scenarios

**None authored.** G/W/T require answers to §3. Placeholder IDs reserved for a future ready spec: `BDD-CE-O02-*` (not allocated).

---

## 6. Worktree / pipeline recommendation

| Option | When |
| --- | --- |
| **Park worktree** (keep `DGE-ce-o02-addressblock-package` idle) | Product will answer §3 soon in this program window |
| **Remove worktree without merge** | Prefer default — D5 still stands; no implementation to merge; avoid idle sole-active confusion |

**Recommended default:** **remove without merge** (or park idle with no gates / no TM activation). Re-provision a fresh worktree only after product reopens D5 and this file is rewritten to `bdd_readiness: ready`.

---

## Traceability

| Artifact | Role |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §8 CE-O02 / §10 D5 | Owning program — deferred placeholder + 2026-07-14拍板 |
| [ce-o01-pdfa-output.md](./ce-o01-pdfa-output.md) | Sibling Wave CE-O leaf (Done); O02 explicitly non-goal |
| PRD / domain / requirements | **No** confirmed addressBlock / multi-doc package scope found (2026-07-20 scan) |
| This file | Open-questions stub only — **blocked** |

---

## Handoff

```
bdd_readiness: blocked
owning_doc: docs/behavior/ce-o02-addressblock-package.md
task_ids: []   # do not activate TM implementation leaf
next: product answers §3 → re-run behavior-spec-author → plan-orchestrator only if ready
```
