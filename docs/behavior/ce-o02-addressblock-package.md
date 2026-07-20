# CE-O02 — addressBlock / 多文档包（BDD readiness stub）

| Field | Value |
| --- | --- |
| **Slice** | `ce-o02-addressblock-package` |
| **bdd_readiness** | **`blocked`** |
| **Program status** | **Deferred** (product decision D5 — **not** reopened) |
| **Recorded** | 2026-07-20 (initial stub); **product Q&A confirmed 2026-07-20** |
| **Program leaf** | **B3 / CE-O02** — [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §8 Wave CE-O · CE-O02；§10 D5 |
| **Task Master** | **Do not register as In Progress implementation leaf** while deferred. Optional later: registry-only / `blocked` pending product reopen — never sole-active writer until D5 is explicitly reopened **and** remaining open questions are answered **and** BDD reaches `ready`. |
| **Formal phase** | **None** |
| **Placement** | ISOLATED — `D:/working/DGE-ce-o02-addressblock-package` · `feat/ce-o02-addressblock-package` (park / remove without merge — no implementation) |
| **Frontend UI** | **Unknown** — still open; do not invent management journeys |

**完成声明约束：** 本文件**不是**可交付 BDD；**不**宣称 CE-O02 Done；**不**发明 `addressBlock` schema / OpenAPI / 节点矩阵；**不**启动 TDD / 实现；**不**翻转 checklist **#3b/#5a**；**不**宣称 go-live。

---

## 1. Why blocked (and deferred)

Session menu「做B」曾将 **B3 CE-O02** 列为剩余工作项，但 **产品权威决策仍为延期**：

| Source | Statement |
| --- | --- |
| CE program **§10 D5** | `addressBlock` 节点 + 多文档包 |
| **拍板记录（2026-07-14）** | **D5：按推荐默认（本期不做 addressBlock / 多文档包；后续如有窗口信封/组合包需求再立项）** |
| **产品确认（2026-07-20）** | **「暂时不做」** — D5 deferral **仍有效**；**不**激活实现叶；**不**将 `bdd_readiness` 改为 `ready` |
| CE program **§8 CE-O02** | 标题 `addressBlock / 多文档包` · 级/量 P3·— · **Deferred**（见 §10） |
| CE program **§9 队列** | CE P3 queue empty；umbrella **#53** registry only — **do not** claim program Done（**CE-O02 deferred per D5**） |
| Umbrella TM **#53** | Explicit: **Skip CE-O02 (D5 deferred)** |

Attempted extract of minimal confirmed scope from PRD / domain model / requirements / CE program (2026-07-20 scan):

| Artifact | Finding |
| --- | --- |
| `docs/product/PRD.md` | **No** `addressBlock` / 窗口信封 / 组合包 confirmed requirement |
| `docs/domain/domain-model.md` | **No** address-block node or multi-doc package type |
| `docs/requirements/requirements-plan.md` | Mentions CE-O02 only as **non-goal** of other slices (e.g. CE-O01 / CE-E03) |
| CE program | Title + D5 deferral + 2026-07-20 future preferences (below) — **no** ready actor/journey/schema/acceptance |

**Verdict:** Deferral stands. Partial future preferences exist (§2) but are **not** sufficient for Given/When/Then while D5 remains「暂时不做」and while §3 open questions remain. Inventing schema or journeys would violate document-as-code.

---

## 2. Confirmed

### 2.1 Governance / schedule (binding now)

1. **D5 (2026-07-14)** = **本期不做** — still authoritative.
2. **Product reconfirmation (2026-07-20):** **「暂时不做」** — keep park; **do not** activate CE-O02 implementation leaf; **do not** flip `bdd_readiness` to `ready`.
3. CE-O02 remains a **placeholder row** until product **explicitly reopens** D5 with concrete scope **and** remaining §3 questions are answered.
4. Adjacent CE-O01 (PDF/A) is **Done** and explicitly **out of scope** for CE-O02.
5. Adjacent CE-E0x library export ZIPs are **Done** and must **not** be conflated with CE-O02 runtime package (see §2.2).

### 2.2 Future preferences (when CE-O02 eventually reopens — **not** implementation authorization)

Recorded from user Q&A **2026-07-20**. These bind **direction if/when** D5 is reopened; they do **not** reopen D5 or authorize BDD `ready` / TDD / TM In Progress:

| # | Topic | Confirmed preference | Honesty bound |
| --- | --- | --- | --- |
| 3 | Multi-document package | **Runtime multi-artifact ZIP** (e.g. letter + companions) | **Not** library export CE-E0x |
| 4 | addressBlock fields | Follow **common / general address standards** (「按通用标准」) | **Do not** invent concrete JSON schema / node matrix yet; a **named** standard or product workshop is still required before ready BDD |
| 5 | Address formatting | **Single format only** (one address formatting rule — locale-agnostic or single-locale) | **Do not** invent multi-locale address rules |

---

## 3. Still open (product — non-blocking while deferred; **blocking** before `bdd_readiness: ready`)

While D5「暂时不做」stands, these do **not** force an implementation leaf. They **must** be answered before any `ready` rewrite:

1. **Window envelope（窗口信封）?** — **Not clearly answered** on 2026-07-20. If「2.暂时不做」applied only to the whole CE-O02 leaf, envelope coverage remains **unknown** (in / out / never).
2. **Surface?** — Runtime generate API only, management authoring UI, or both? (`frontend_ui_in_scope` unknown.)
3. **Named address standard / workshop?** — 「通用标准」is directionally confirmed (§2.2 #4) but **not** a named ISO/UPU/national standard or workshop outcome; required before schema authorship.
4. **Explicit D5 reopen?** — A future product decision must **explicitly reopen** addressBlock / multi-doc package (dated after 2026-07-20) before scheduling implementation. 「暂时不做」is **not** a reopen.

---

## 4. Explicit non-goals (while deferred / blocked)

- **No** invented `addressBlock` JSON Schema, OpenAPI properties, or structured-content node matrix rows.
- **No** TDD Red tests, backend/frontend/rendering code, Flyway, or deploy evidence for CE-O02.
- **No** Task Master **In Progress** / sole-active implementation leaf registration for CE-O02.
- **No** claim that CE program / CE-O02 / go-live is Done.
- **No** silent override of D5 without new explicit product confirmation that **reopens** the leaf (dated after 2026-07-20).
- **No** conflation of CE-O02 runtime multi-artifact ZIP with CE-E0x library export packages.
- **No** multi-locale address formatting rules invented from 「只按一种格式」.

---

## 5. Acceptance scenarios

**None authored.** G/W/T require D5 reopen + answers to §3 (and concrete schema after named standard/workshop). Placeholder IDs reserved for a future ready spec: `BDD-CE-O02-*` (not allocated).

---

## 6. Worktree / pipeline recommendation

| Option | When |
| --- | --- |
| **Park worktree** (keep `DGE-ce-o02-addressblock-package` idle) | Only if product will reopen D5 imminently |
| **Remove worktree without merge** | **Recommended default** — D5「暂时不做」stands; no implementation to merge; avoid idle sole-active confusion |

**Recommended default:** **remove without merge** (or park idle with no gates / no TM activation). Re-provision a fresh worktree only after product **explicitly reopens** D5 and this file is rewritten to `bdd_readiness: ready`.

---

## Traceability

| Artifact | Role |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §8 CE-O02 / §10 D5 | Owning program — deferred + 2026-07-14拍板 + **2026-07-20 产品确认** |
| [ce-o01-pdfa-output.md](./ce-o01-pdfa-output.md) | Sibling Wave CE-O leaf (Done); O02 explicitly non-goal |
| CE-E0x export behaviors | Library / per-template ZIP — **not** CE-O02 runtime package |
| PRD / domain / requirements | **No** confirmed addressBlock / multi-doc package shippable scope (2026-07-20) |
| This file | Deferred stub + future preferences + open questions — **`blocked`** |

---

## Handoff

```
bdd_readiness: blocked
program_status: Deferred
owning_doc: docs/behavior/ce-o02-addressblock-package.md
task_ids: []   # do not activate TM implementation leaf
d5_deferral: in_force (2026-07-14; reconfirmed 2026-07-20 暂时不做)
future_prefs: runtime_multi_artifact_zip | general_address_standards_unnamed | single_format
still_open: window_envelope | surface_api_vs_ui | named_standard_or_workshop | explicit_d5_reopen
next: product explicitly reopens D5 + answers §3 → re-run behavior-spec-author → plan-orchestrator only if ready
```
