# CE-K05 母版影响分析真实化 + revision diff — BDD

| Field | Value |
| --- | --- |
| **Slice** | `ce-k05-master-impact-real` |
| **Plan task** | **CE-K05**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §3 CE-K05） |
| **Task Master** | **#61** |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-15 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k05-master-impact-real` · `feat/ce-k05-master-impact-real` |
| **Scope of this slice** | 真实 `impactAnalysis`（引用模板按名 + 可点链接）；`retestRequired` 由锚点集合 delta 计算；revision 对比（锚点增/删/重命名 + 文件 hash）；替换母版文件前置影响确认。**依赖 K01**（release→revision 钉扎）。**禁止** CE-K04 语义句级 diff 重做；**不** go-live；**不**激活 CD-3 |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；钉扎前提 [ce-k01-release-bundle-pinning.md](./ce-k01-release-bundle-pinning.md)；权限 [permission-matrix.md](../security/permission-matrix.md) |

---

## 1. 概述

现状 `MasterDocumentService.impactAnalysis` 固定返回 `List.of(), false`；`MasterImpactPanel` 渲染「无引用模板」与「无需重测」假象，给出**错误的安全感**。亦无 revision 间锚点/哈希差异面，替换 DOCX 无前置影响确认。

本切片把母版影响分析真化为可审计查询，并补齐 revision diff 与替换确认闭环。

| 行为域 | 摘要 |
| --- | --- |
| **IMP-01 真实 impactAnalysis** | 按 `masterId` 反查引用模板（各生命周期）→ 返回可展示条目（至少 id + **name**），前端按名链接 |
| **IMP-02 retestRequired** | 相对对比基线的**锚点集合 delta**（增/删/重命名）非空 → `retestRequired=true`；否则 false（无引用模板时 false） |
| **IMP-03 Revision diff** | 两 revision 对比：锚点新增/删除/重命名清单 + 双方文件 hash（可观察） |
| **IMP-04 替换前置确认** | 替换母版文件前展示影响确认（引用模板名 + `retestRequired` / 锚点 delta 摘要）；确认后方可提交 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-K04 结构化内容句级语义 diff | Out of scope — 已交付；本片仅母版锚点集合 + 文件 hash |
| CE-U06 锚点可视高亮 / displayLabel 编辑 | Out of scope |
| 像素级 DOCX 并排阅读器 | Out of scope |
| 改变 K01 钉扎语义 / 发布流程 | **禁止** |
| 宣称 go-live / 激活 CD-3 | **禁止** |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 关注点 |
| --- | --- | --- |
| **MASTER_DESIGNER / 母版管理者** | `manageMasters` | Hub impact 面板诚实；替换前知悉影响 |
| **模板编排 / 测试** | 可读母版 + 可跳模板 | 按名打开受影响包 |
| **审计 / 运维** | 具备母版读权限 | revision hash + 锚点 delta 可引用 |
| **系统** | `MasterDocumentService.impactAnalysis` + revision diff + Hub UI | 真实查询；fail-closed 授权 |

---

## 3. Goal

1. `impactAnalysis` **禁止**再返回固定空列表 + `retestRequired=false` 作为「已分析」结果。
2. 返回的引用模板条目含 **人类可读 name**（及 id）；FE `MasterImpactPanel` **按 name 展示**并可 `router-link` 到模板详情（不得仅展示裸 UUID 作为主标签）。
3. `retestRequired` 由**当前 revision 相对对比基线**的锚点集合变化计算（见 K05-C3）。
4. 提供 revision A vs B（或 current vs previous）diff：锚点 added / removed / renamed + 双方 **file hash**。
5. 替换母版 DOCX 前弹出影响确认；用户确认后才执行既有 `replaceFile`。
6. 无母版读权限 → fail-closed。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **K05-C1** | **引用模板查询：** 凡 `template.masterId = targetMasterId` 且未逻辑删除的模板包均计入 impact（含 DRAFT / TESTING / APPROVAL / PENDING_RELEASE / PUBLISHED 等各生命周期），按组范围与既有母版读模型过滤。 | 计划卡 |
| **K05-C2** | **响应条目：** 每条至少 `templateId` + `name`（展示名）；可选 `lifecycleStatus` / `externalId`。FE 主标签用 **name**；链接目标为既有模板详情路径。向后兼容可保留 `referencedTemplateIds`，但**不得**仅靠 UUID 列表满足本片验收。 | 计划卡「按名 + 链接」；用户 scope |
| **K05-C3** | **`retestRequired`：** 对比基线 = 同一 master 的**上一 revision 行**（若无上一行 → false）。对 current vs baseline 计算锚点集合 delta：`businessKey`/`anchorKey` 增、删、或同 id 下稳定键重命名（实现固定一种键：优先 `anchorKey` / 等价稳定标识）。**delta 非空且引用模板数 ≥ 1** → `retestRequired=true`；无引用模板 → `false`（仍可展示空列表，不得假装「已分析且安全」若查询失败）。仅文件 hash 变化但锚点集合不变 → 本片默认 **`retestRequired=false`**（锚点驱动）；hash 仍在 revision diff 中暴露。 | 计划卡「按锚点集合变化」；用户 scope |
| **K05-C4** | **空列表真值：** 查询成功且确实无引用 → 可显示「无引用模板」；**禁止** stub 短路。查询失败 → 错误态 / unavailable，不得显示假 empty + `retestRequired=false` 成功面。 | 痛点关闭 |
| **K05-C5** | **Revision diff：** 管理 API（或等价 Hub/revision 工作区入口）接受同一 master 下两个 `revisionLineId`（默认 current vs previous）。响应含：`addedAnchors[]`、`removedAnchors[]`、`renamedAnchors[]`（from→to）、`baselineFileHash`、`candidateFileHash`（算法与 K01 一致：**SHA-256** 母版 DOCX 字节）。无权限 → 403。 | 计划卡；K01 hash |
| **K05-C6** | **替换前置确认：** Hub / revision 替换 DOCX 流程在实际上传提交前展示确认对话框：引用模板 **name** 列表（可截断 + 计数）、`retestRequired`、锚点 delta 摘要（或「无锚点变化」）。用户 Cancel → 不调用 `replaceFile`；Confirm → 既有 replace 语义不变。 | 计划卡 |
| **K05-C7** | **授权：** 沿用既有 master 读 / `manageMasters` 写；跨组 fail-closed。 | permission-matrix |
| **K05-C8** | **本片禁止：** 改 K01 钉扎；做句级 CONTENT 语义 diff；go-live；CD-3。 | 计划卡 |

---

## 5. Preconditions / Trigger

**Preconditions**

- K01 已交付（钉扎字段可用；本片 revision hash 与 K01 算法一致）。
- 目标 master 至少有 current revision；可选存在 previous revision。
- 会话对目标 master 可读（写操作另需 `manageMasters`）。

**Triggers**

- 打开 Master Package Hub → 加载 impact 面板。
- 调用 `GET …/masters/{masterId}/impact-analysis`（或既有等价路径）。
- 打开 revision diff / Compare revisions。
- 用户选择替换母版文件并进入确认步。

---

## 6. Primary journey

1. 母版 M 被模板「贷款合同」「授信通知」引用；设计师打开 M 的 Hub。
2. Impact 面板列出两模板的**名称**为可点击链接；因相对上一 revision 删除了锚点 `FOOTER`，显示 **retest required**。
3. 设计师打开 current vs previous revision diff → 见 `FOOTER` removed + 双方 SHA-256 hash。
4. 设计师替换 DOCX → 确认对话框再次展示受影响模板名与重测提示 → Confirm → 文件替换成功；Hub impact 刷新为真实结果。

---

## 7. System responses（success / fail-closed）

| 情况 | 系统响应 |
| --- | --- |
| 有引用 + 锚点 delta | `referencedTemplates` 非空（含 name）；`retestRequired=true`；FE 不显示假 empty |
| 有引用 + 无锚点 delta | 列表非空；`retestRequired=false` |
| 无引用 | 真 empty；`retestRequired=false`；文案诚实 |
| 无上一 revision | `retestRequired=false`；diff 对 previous 不可用或空 delta |
| 无读权限 | 403 / access denied；无半残列表 |
| 替换 Cancel | 不写存储、不改 revision |
| 查询失败 | UI unavailable / error；**禁止** stub 成功 empty |

---

## 8. Acceptance scenarios

### BDD-CE-K05-MIR-001 — 真实引用列表含名称

```gherkin
Given master M 被至少两个未删除模板引用，且模板具有不同 name
And 会话可读取该 master
When 调用 impactAnalysis(M) 或打开 Hub impact 面板
Then 结果包含对应 templateId
And 每条含可展示 name（非仅 UUID）
And 前端链接以 name 为可见标签指向模板详情
And 结果不得为硬编码空列表 stub
```

### BDD-CE-K05-MIR-002 — 真 empty 与 stub 区分

```gherkin
Given master M 无任何引用模板
When 加载 impactAnalysis
Then referenced 列表为空
And UI 可显示「无引用模板」
And 该路径仍执行真实查询（单测可证明非 List.of() 短路，或集成测在有引用时不再 empty）
```

### BDD-CE-K05-MIR-003 — retestRequired 由锚点 delta

```gherkin
Given master M 有 previous 与 current 两 revision
And current 相对 previous 删除或新增或重命名至少一个锚点
And 至少存在一个引用模板
When 计算 impactAnalysis
Then retestRequired=true
```

### BDD-CE-K05-MIR-004 — 无锚点 delta 则无需重测

```gherkin
Given current 与 previous 锚点集合（稳定键）完全一致
And 存在引用模板
When 计算 impactAnalysis
Then retestRequired=false
```

### BDD-CE-K05-MIR-005 — 无引用则 retestRequired=false

```gherkin
Given 锚点发生 delta 但无引用模板
When 计算 impactAnalysis
Then retestRequired=false
And 列表为空
```

### BDD-CE-K05-MIR-006 — Revision diff 锚点 + hash

```gherkin
Given 同一 master 下 revision A 与 B，锚点有增/删/重命名差异，文件字节不同
And 会话可读
When 请求 revision diff(A, B)
Then 响应列出 added / removed / renamed（按实现分类）
And 含 A 与 B 的 SHA-256 file hash 且二者可区分
```

### BDD-CE-K05-MIR-007 — Revision diff 授权 fail-closed

```gherkin
Given 会话对 master 无读权限
When 请求 revision diff
Then 拒绝（403 或既有 access-denied）
And 不返回锚点或 hash 正文
```

### BDD-CE-K05-MIR-008 — 替换前置影响确认（E2E）

```gherkin
Given 用户具备 manageMasters，master 有引用模板且 retestRequired 将为 true
When 用户发起替换母版 DOCX 并到达确认步
Then 对话框展示至少一个引用模板 name 与重测提示（或锚点 delta 摘要）
When 用户 Cancel
Then 不执行 replaceFile
When 用户 Confirm
Then 执行既有 replace 成功语义并刷新 impact
```

### BDD-CE-K05-MIR-009 — FE 禁止假 empty 安全感（回归）

```gherkin
Given API 返回非空 referencedTemplates
When Hub 渲染 MasterImpactPanel
Then 不显示「无引用模板」空态
And 显示 name 链接列表
```

---

## 9. Boundary / exception

- 首个 revision（无 previous）：`retestRequired=false`；diff previous 不可用。
- 重命名检测：同一物理锚点 id 改 key，或稳定键映射 — 实现固定一种并单测；不得把纯 displayLabel 变更算作必须重测（除非稳定键变）。
- 大量引用：列表可分页或「显示前 N + 计数」；确认对话框同理；不得截断导致 `retestRequired` 计算错误。
- 逻辑删除模板：不计入引用。
- 跨组母版：GROUP 范围外不可见（fail-closed）。

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | impactAnalysis JSON 含 name 条目 + 正确 `retestRequired`；revision diff 含 anchors + hashes |
| UI | Hub 面板 name 链接；替换确认对话框；无假 empty |
| Tests | 后端取消/替换 stub 单测；FE Vitest 非空渲染；E2E 替换确认 |
| Gates | `mvn verify` + FE lint/type-check/test/build；用户面 E2E+UIUX |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-K05 plan §3 | 目标行为 |
| Task Master **#61** | 执行任务 |
| CE-K01 BDD | hash 算法 + revision 行前提 |
| permission-matrix 母版管理 | 谁可读/可替换 |
| 现状 `MasterDocumentService.impactAnalysis` stub | 待替换行为 |

---

## 12. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-k05-master-impact-real.md
task_ids: ["#61", "CE-K05"]
scenario_ids:
  - BDD-CE-K05-MIR-001
  - BDD-CE-K05-MIR-002
  - BDD-CE-K05-MIR-003
  - BDD-CE-K05-MIR-004
  - BDD-CE-K05-MIR-005
  - BDD-CE-K05-MIR-006
  - BDD-CE-K05-MIR-007
  - BDD-CE-K05-MIR-008
  - BDD-CE-K05-MIR-009
```
