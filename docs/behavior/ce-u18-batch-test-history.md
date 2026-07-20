# BDD 行为规格：CE-U18 — 批量测试历史钻取 + 双路径统一

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U18-BTH`  
**编写日期:** 2026-07-17  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U18  
**Slice:** `ce-u18-batch-test-history`  
**Task Master:** **#93**  
**Formal phase:** **None**  
**Placement:** ISOLATED `D:/working/DGE-ce-u18-batch-test-history` · `feat/ce-u18-batch-test-history`  
**完成声明约束:** 关闭「历史仅摘要、无法钻取样本结果」与「同步/异步双路径并存」缺口；**不**宣称 go-live；**不**激活 CD-3 / P3；**不**触碰 #50

---

## 1. 概述

模板 Testing 工作区已有 **异步全量测试**（`POST .../batch-tests/run` + SSE 进度对话框）与 **批量测试历史表**（`BatchTestHistoryPanel`，最近 5 次摘要）。后端已将逐样本结果写入 `BatchTestRunEntity.sample_results_json`（`sampleResultsJson`），但：

| 缺口（现状证据） | 目标 |
| --- | --- |
| `GET .../batch-tests` → `BatchTestRunSummaryView` **不含** `sampleResults`；`BatchTestHistoryPanel` 仅展示摘要列 | 历史行可展开；展示并消费 `sampleResultsJson` 解析后的样本明细 |
| P12 SCEN-F6-01 承诺「可点击展开查看各数据集详细结果」未在 UI 落地 | 展开明细 + 跳转到对应测试数据集（及可选预览） |
| 仍保留 **无进度** 同步路径：`POST .../previews/batch-test`（`batchTestGenerate` / `handleBatchTestGenerate`） | 管理 UI **退役**该入口；全量测试只走异步路径 |
| 异步完成后 `onBatchCompleted` → `emit('test-generate-batch')` → 误调同步 `handleBatchTestGenerate` | 完成后只刷新历史 / 覆盖率 / 门禁，**绝不**再打同步批量 |

| 行为域 | 摘要 |
| --- | --- |
| **BTH-01 历史钻取** | 展开历史行；展示每样本成功/失败、标识、错误摘要；可跳转到对应数据集 |
| **BTH-02 契约暴露** | 管理 API 向 FE 暴露可消费的样本结果（来自 `sampleResultsJson`） |
| **BTH-03 路径统一** | UI 唯一全量测试入口 = 异步 run + SSE；退役同步批量 UI 路径 |
| **BTH-04 Fail-closed** | 无读权限不可见历史/明细；无写权限不可启动全量测试 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-U16 Authoring path / CE-U15 Stepper / CE-U14 Tasks | 已交付；本片不改其语义 |
| Runtime sync/async **generation** 批处理（P11 `BatchGenerationService`） | Out of scope — 本片仅 **模板测试** 批量路径 |
| 历史保留策略变更（仍最近 5 条 / soft-hide） | Out of scope — 沿用 P12 |
| 重写 SSE 进度对话框 UX | Out of scope — 复用既有 `BatchTestProgressDialog` |
| Demo seeder 内部直接调 `BatchTestGenerationService` | 允许服务内调用；**不得**再经管理 UI 同步入口 |
| 宣称 go-live / 激活 CD-3 / 正式 P3 / #50 | **禁止** |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **模板作者 / 编排人员** | `authorTemplates`（可读模板快照 + 可启动测试） | 运行全量测试；查看历史；展开样本明细；跳转到失败/成功数据集 |
| **只读 / 无编排权限** | 无 `authorTemplates` 或不可读模板 | 不得启动全量测试；历史/明细按既有授权 fail-closed |
| **测试决策者** | `decideTests` | 可查看历史与明细（只读钻取）；不因本片获得绕过授权的写入口 |
| **系统** | `AsyncBatchTestOrchestrator` + `BatchTestHistoryService` + history API | 持久化 `sampleResultsJson`；列表/明细返回；SSE 进度 |

---

## 3. Goal

1. 作者在 Testing → **Preview runs**（或承载 `BatchTestHistoryPanel` 的等价子 Tab）看到历史时，能 **展开** 任一行，看到该次运行的 **逐样本结果**（来自 `sampleResultsJson`）。  
2. 作者可从样本行 **跳转** 到 Testing → **Data sets**，并选中对应测试数据集，便于修数或单次预览。  
3. 管理 UI 上 **只有一条**「Run full test」路径：异步 `batch-tests/run` + 进度对话框；**无进度的同步** `previews/batch-test` 不再出现在用户旅程中。  
4. 异步批量完成后，刷新历史 / 覆盖率 / submit eligibility，**不会**再触发同步批量 API。  
5. Formal phase 保持 **None**；不宣称 go-live；不激活 CD-3。

---

## 4. 已确认决策 vs 推导假设

### 4.1 已确认（产品 / 计划）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U18-C1** | 前端消费 `sampleResultsJson`：**展开明细 + 跳转**。 | CE 计划 §4 CE-U18；Task Master #93 |
| **U18-C2** | **移除**无进度 legacy 同步批量路径（管理 UI）。 | 同上 |
| **U18-C3** | **统一**批量测试历史双路径（创建 + 回顾同一异步模型）。 | 同上 |
| **U18-C4** | Formal phase **None**；不宣称 go-live；不激活 CD-3；P3 parked；leave #50。 | CE 计划 / Task Master |
| **U18-C5** | 前端用户面变更 → E2E + UIUX 必做；银行 OA + English-first。 | delivery constitutions |
| **U18-C6** | P12 历史仍最近 5 条；INVALIDATED / 覆盖率摘要行为保留。 | [template-testing-overhaul.md](./template-testing-overhaul.md) F6 |

### 4.2 本片确认的实现决策（计划卡薄 → 仓库事实推导）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **U18-D1** | **契约：** 管理侧向 FE 暴露样本结果数组（字段名建议 `sampleResults`），数据来自实体 `sampleResultsJson`。允许：(a) 扩展 `GET .../batch-tests` 摘要项，或 (b) `GET .../batch-tests/{runId}` 详情。实现择一并单测锁定；**不得**要求 FE 直读 DB。 | U18-C1；现状 `BatchTestRunSummaryView` 无样本字段 |
| **U18-D2** | **规范样本形状（异步规范）：** `dataSetExternalId`、`success`、`errorDetail?`；可选 `docxKey` / `pdfKey`（若 JSON 中有）。FE 对偶发旧同步形状（`testDataSetId` / `previewId` / `status`）做 **normalize**，映射到同一展示模型。 | `AsyncBatchTestOrchestrator.SampleResult`；`SubmitTestEligibilityService` 已按 `success` + `dataSetExternalId` 解析 |
| **U18-D2′（PTA 修正，2026-07-21）** | **异步成功样本持久化：** 当样本产生了 preview 时，`sampleResultsJson` **必须**写入非空 `previewId`，并在产物已落库时写入 `docxKey` / `pdfKey`（不再把成功路径键写成 null）。`previewId` 属规范异步形状的一部分（不仅是 legacy sync）。失败样本仍可 null keys。契约见 OpenAPI `BatchTestHistorySampleResultView` 与 [published-template-test-artifacts.md](./published-template-test-artifacts.md) BDD-PTA-004。CE-U18 叶状态仍为 **Done**；本行是后续 bug-fix 合同对齐，不 reopen U18。 | 用户确认根因；PTA / TM #144 |
| **U18-D3** | **展开：** `BatchTestHistoryPanel` 行可 expand；展开区列出样本行：数据集标识、成功/失败标签、失败时 `errorDetail`（截断+可展开全文可选）。空数组 / 解析失败 → 可见 empty / error，不崩溃。 | U18-C1；P12 SCEN-F6-01 |
| **U18-D4** | **跳转：** 点击样本（或「Open data set」）→ `workspaceTab=testing` + `testingTab=dataSets`，并设置 `selectedTestDataSetId` 为解析到的数据集 id（用 `dataSetExternalId` / 名称与当前列表匹配）。匹配失败 → 仍切到 Data sets + 可见 toast/提示，不静默失败。若规范化后存在 `previewId`，可另提供「Open preview」→ `testingTab=previewRuns` + 选中该 preview。 | U18-C1「跳转」；Testing 子 Tab 模型 |
| **U18-D5** | **唯一运行路径：** Testing action rail「Run full test」→ `runBatchTest`（`POST .../batch-tests/run`）+ `BatchTestProgressDialog`。退役 UI 对 `batchTestGenerate` / `handleBatchTestGenerate` / `@test-generate-batch`→同步 handler 的绑定。 | U18-C2/C3；现状双路径 |
| **U18-D6** | **完成回调：** `onBatchCompleted` / `@completed` **只**刷新：`BatchTestHistoryPanel`、`coverageRefreshToken`、submit eligibility；**禁止**调用同步 `previews/batch-test`。 | 现状误连线 |
| **U18-D7** | **同步 API：** 管理 UI 客户端移除对 `POST .../previews/batch-test` 的用户旅程调用。后端 endpoint / `BatchTestGenerationService` 可暂留供 demo/seed，但不得再从 mgmt UI 触发；若实现选择在同一切片废弃 HTTP 映射，须保留 seed 的服务直调或改为异步。 | U18-C2「移除」；frontend-focused |
| **U18-D8** | **RUNNING 行：** 可展示摘要；展开明细若尚无最终 `sampleResults` 则显示进行中空态（或禁用 expand），完成后刷新可见。 | 状态机 RUNNING→COMPLETED/FAILED |
| **U18-D9** | **授权：** 历史与明细沿用 `requireReadableSnapshot`；启动全量测试沿用既有 author 门禁。403/404 → 既有错误面，不泄露他组数据。 | fail-closed |
| **U18-D10** | **i18n：** English-first（en + zh-CN）；控件用语如 **Run full test**、**Sample results**、**Open data set**。 | i18n-english-first |

---

## 5. Trigger

| 触发 | 事件 |
| --- | --- |
| 查看历史 | 进入 Testing → Preview runs（历史面板挂载 / refresh） |
| 钻取 | 用户展开历史行，或点击样本跳转 |
| 运行全量测试 | 用户确认「Run full test」 |
| 完成刷新 | SSE `batch_completed` / 对话框 completed |

---

## 6. Preconditions

- 用户已登录；对模板有可读快照权限（历史/明细）。  
- 启动全量测试：具备既有 author 能力；模板处于允许测试的生命周期；至少 1 个测试数据集。  
- 历史钻取：至少 1 条非 hidden 的 `BatchTestRun`（或 empty 态可验证）。

---

## 7. Primary journey

1. 作者打开模板 **dev workspace → Testing**。  
2. 确认后点击 **Run full test** → 进度对话框（SSE）→ 完成。  
3. 历史表刷新；出现新行（COMPLETED / FAILED + 计数 + 覆盖率 / 门禁摘要）。  
4. 作者 **展开** 该行 → 看到样本明细（成功/失败 + 数据集标识 + 错误）。  
5. 作者对失败样本选择 **Open data set** → 落到 Data sets 子 Tab 且对应数据集被选中。  
6. 全程 **无** 同步 `previews/batch-test` 请求。

---

## 8. System responses

| 路径 | 响应 |
| --- | --- |
| 成功完成异步批量 | 202 + `runId`/`streamUrl`；SSE 进度；历史可钻取；覆盖率/eligibility 刷新 |
| 展开有样本的 COMPLETED 行 | 展示 normalize 后的样本列表 |
| 展开无样本 / 坏 JSON | 可见 empty 或解析错误提示；表格不崩 |
| 跳转匹配成功 | `testingTab=dataSets` + 选中数据集 |
| 跳转匹配失败 | Data sets 可见 + 非阻断提示 |
| 无权限读 | 历史加载失败/空 + 既有错误；不暴露他组 run |
| 无权限跑 | Run full test 不可用或启动失败；不走同步旁路 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-CE-U18-BTH-001 — 历史行展开显示 sampleResults

```gherkin
Given 模板存在一次 COMPLETED 批量测试运行，sampleResultsJson 含至少 2 个样本
And 作者在 Testing → Preview runs 看到该历史行摘要
When 作者展开该行
Then 展开区显示每个样本的数据集标识与成功/失败状态
And 失败样本显示 errorDetail（或等价可读错误摘要）
And 数据来自管理 API 暴露的 sampleResults（源于 sampleResultsJson）
```

### BDD-CE-U18-BTH-002 — 从样本跳转到 Data sets

```gherkin
Given 历史展开区有一条样本，dataSetExternalId 对应模板上已有测试数据集 D
When 作者激活 Open data set（或等价控件）
Then URL/状态为 workspaceTab=testing 且 testingTab=dataSets
And 选中的测试数据集为 D
```

### BDD-CE-U18-BTH-003 — 跳转无法匹配时的可见反馈

```gherkin
Given 展开区样本的 dataSetExternalId 在当前模板数据集列表中不存在（已删）
When 作者激活 Open data set
Then 仍导航到 testingTab=dataSets
And 用户看到非阻断错误/提示（English-first）
And 应用不崩溃
```

### BDD-CE-U18-BTH-004 — 唯一全量测试路径为异步

```gherkin
Given 作者在 Testing 工作区且至少有 1 个测试数据集
When 作者确认 Run full test
Then 系统调用 POST .../batch-tests/run（202）并打开进度对话框
And 浏览器网络中不出现 POST .../previews/batch-test
```

### BDD-CE-U18-BTH-005 — 异步完成后不触发同步批量

```gherkin
Given 一次异步全量测试正在进度对话框中运行
When SSE 报告 batch_completed（或对话框 completed）
Then 历史面板与 coverage / submit eligibility 刷新
And 不调用 POST .../previews/batch-test
And 不出现同步批量成功 toast（templates.testDataSets.batchSuccess）作为完成主反馈
```

### BDD-CE-U18-BTH-006 — 管理 UI 退役同步批量入口

```gherkin
Given 作者使用模板 dev workspace Testing（及 legacy workspace 若仍可达）
When 作者寻找全量/批量测试入口
Then 仅存在异步 Run full test（或等价单一控件）
And 不存在会调用 batchTestGenerate / previews/batch-test 的用户可见入口
```

### BDD-CE-U18-BTH-007 — INVALIDATED / empty / RUNNING 边界

```gherkin
Given 历史含 INVALIDATED 行、空历史、以及可选 RUNNING 行
When 作者查看历史面板
Then INVALIDATED 仍显示失效标签（既有行为）
And 空历史显示 empty
And RUNNING 行不因展开缺失样本而崩溃（禁用展开或进行中空态）
```

### BDD-CE-U18-BTH-008 — Fail-closed 授权

```gherkin
Given 会话对模板不可读
When 请求批量测试历史或样本明细
Then API 拒绝（403/404 既有契约）且 UI 不展示他组样本内容
```

### BDD-CE-U18-BTH-009 — 旧同步 JSON 形状可 normalize（兼容）

```gherkin
Given 某历史 run 的 sampleResultsJson 为旧同步形状（含 testDataSetId / previewId）
When 作者展开该行
Then FE normalize 后仍展示可读样本行（至少数据集标识 + 状态）
And 若存在 previewId，可提供 Open preview 跳到 previewRuns
```

### BDD-CE-U18-BTH-010 — 非目标与完成约束

```gherkin
Given 本片交付完成
Then 未宣称 go-live
And 未激活 CD-3 / 正式 P3
And 未修改 #50 相关依赖策略
And 未将 Runtime P11 generation 批处理与模板测试路径混淆为同一入口
```

---

## 10. Boundary / exception

| 场景 | 期望 |
| --- | --- |
| `sampleResultsJson` null / `[]` | 展开 empty，不报未捕获异常 |
| JSON 损坏 | 展开区错误提示 + Retry（刷新历史）；摘要列仍可用 |
| 大数据集（多样本） | 展开区可滚动；不得撑破 OA 布局 |
| SSE 断开后完成 | 用户刷新历史仍可钻取最终样本结果 |
| Demo/seed 仍用 `BatchTestGenerationService` | 允许；不得恢复 UI 同步入口 |
| 双品牌 / @1920 | UIUX Critical=0；展开明细符合 bank OA |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | History（或 detail）响应含可解析 `sampleResults`；traceId 在 envelope |
| UI | 历史行 expand；样本表；Open data set 深链到 dataSets |
| Network | Run full test / completed 路径无 `previews/batch-test` |
| Vitest | History expand + normalize；completed 回调不调 sync；退役 handler |
| E2E + UIUX | 至少 BTH-001 + BTH-002 + BTH-004；UIUX 双品牌 @1920；Critical=0 |
| 非目标 | 无 go-live / CD-3 / #50 |

---

## 12. Traceability

| 来源 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U18 | 目标行为（P2·M） |
| Task Master **#93** | 执行任务；slice `ce-u18-batch-test-history` |
| [template-testing-overhaul.md](./template-testing-overhaul.md) SCEN-F6-01 | 历史展开承诺（未完全落地） |
| `BatchTestHistoryPanel` / `BatchTestRunSummary` | 现状摘要-only UI |
| `useTemplatePreviewActions.handleBatchTestGenerate` | legacy sync 路径 |
| `useTemplateDetailDevWorkspace.handleRunFullTest` | 规范异步路径 |
| `BatchTestRunEntity.sampleResultsJson` / `AsyncBatchTestExecutionSupport.writeSampleResults` | 持久化来源 |
| `BatchTestHistoryService` / `BatchTestRunSummaryView` | 契约缺口（待暴露样本） |
| CE-U16 / CD-3 / P3 / #50 | 非目标 |

---

## 13. 现状 → 实现提示（非额外需求）

| 发现 | 路径提示（供 plan / 实现） |
| --- | --- |
| `BatchTestRunSummaryView` 无样本字段 | 扩展 view + mapper 读 `getSampleResultsJson()`，或新增 `GET /{runId}` |
| `BatchTestHistoryPanel` 无 expand | `el-table` expand / 嵌套表；i18n Sample results |
| `onBatchCompleted → emit('test-generate-batch') → handleBatchTestGenerate` | 改为 bump `coverageRefreshToken` + reload history/eligibility |
| `templatesApi.batchTestGenerate` | 从用户旅程移除；单测改挂异步路径 |
| 双 JSON 形状 | FE `normalizeSampleResults(raw)` 单一出口 |
| Demo seeder | 保持服务直调或改异步；勿恢复 UI sync |

---

**bdd_readiness: ready**  
**open_questions: []**  
**frontend_ui_in_scope: true**  
**owning_doc:** `docs/behavior/ce-u18-batch-test-history.md`  
**task_ids:** ["93"]
