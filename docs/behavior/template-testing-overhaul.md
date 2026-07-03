# BDD 行为规格：模板测试页全量改造

**文件状态**: `ready`
**版本**: 1.0.0
**编写日期**: 2026-07-03
**来源任务**: 模板测试页全量改造（Template Testing Tab Overhaul）

---

## 目录

1. [概述](#1-概述)
2. [Actor / Role](#2-actor--role)
3. [Goal（用户目标）](#3-goal用户目标)
4. [Trigger（触发条件）](#4-trigger触发条件)
5. [Preconditions（前置条件）](#5-preconditions前置条件)
6. [User Journey Steps（用户操作步骤）](#6-user-journey-steps用户操作步骤)
7. [System Responses（系统响应）](#7-system-responses系统响应)
8. [Acceptance Scenarios（Given/When/Then）](#8-acceptance-scenarios-given--when--then)
   - 8.1 单次预览生成（成功路径）
   - 8.2 单次预览生成（并发超限）
   - 8.3 单次预览生成（失败+重试）
   - 8.4 临时预览文件过期访问
   - 8.5 临时预览文件定时清理
   - 8.6 全量测试运行（成功路径）
   - 8.7 全量测试运行（部分失败）
   - 8.8 全量测试结果因模板内容变更失效
   - 8.9 提交测试按钮——所有条件满足
   - 8.10 提交测试按钮——无有效测试结果（置灰）
   - 8.11 提交测试按钮——存在失败样本（置灰）
   - 8.12 提交测试按钮——变量覆盖率未达阈值（置灰）
   - 8.13 提交测试按钮——锚点覆盖率未达阈值（置灰）
   - 8.14 覆盖率面板——正常展示
   - 8.15 覆盖率面板——未覆盖锚点/变量列表
   - 8.16 测试历史记录——最近 5 次展示
   - 8.17 测试历史记录——超过 5 次时淘汰旧记录
9. [边界与异常行为](#9-边界与异常行为)
10. [可观测证据](#10-可观测证据)
11. [待确认的 \[ASSUMED\] 假设项](#11-待确认的-assumed-假设项)
12. [数据模型变更（新增字段）](#12-数据模型变更新增字段)
13. [API 端点清单（新增）](#13-api-端点清单新增)
14. [BDD 就绪声明](#14-bdd-就绪声明)
15. [可追溯性](#15-可追溯性)

---

## 1. 概述

本规格描述模板测试 Tab（`TemplateDetailTestingTab`）的全量改造行为。改造包含六个功能域：

| # | 功能域 | 改造类型 |
|---|-------|---------|
| F1 | 单次预览生成（「运行预览」按钮） | 新增 SSE 进度、临时下载链接、并发上限 |
| F2 | 全量测试运行（「全量测试」按钮） | 改造原「试生成（已选）」；删除「批量试生成全部」 |
| F3 | 全量测试结果持久化与失效 | 新增版本绑定与模板内容变更触发的失效机制 |
| F4 | 「提交测试」按钮门禁 | 新增三条前置检查，未满足则置灰+Tooltip |
| F5 | 覆盖率可见性 | 新增未覆盖列表、阈值对比 |
| F6 | 测试历史记录 | 新增最近 5 次记录展示 |

---

## 2. Actor / Role

| Actor | 角色描述 | 权限要求 |
|-------|---------|---------|
| **模板作者（Template Author）** | 负责编写并测试模板内容的用户 | `canAuthorTemplates` = true（GroupAccessService） |
| **测试决策者（Test Decision Maker）** | 对测试结果作出 PASSED/FAILED 决定 | `canDecideTemplateTests` = true |
| **系统调度器（Scheduler）** | 后台定时任务，清理过期文件、失效测试结果 | 系统内部，无 HTTP 身份 |

> 本规格中的交互主体默认为**模板作者**，除非明确标注为测试决策者或系统调度器。

---

## 3. Goal（用户目标）

1. **F1 目标**：为单个测试数据集运行一次预览，实时看到进度，生成完成后下载 DOCX/PDF 临时文件，24 小时内有效。
2. **F2 目标**：一键对全部测试数据集运行批量测试，实时查看进度，结果持久保存，不随时间过期（除非模板内容变更）。
3. **F4 目标**：在「提交测试」操作前，系统自动验证测试质量门禁条件，未达标时明确提示原因。
4. **F5 目标**：直观了解当前覆盖率状况、未覆盖的具体锚点和变量、与阈值的差距。
5. **F6 目标**：查看最近 5 次全量测试的历史记录，包括状态、覆盖率摘要和是否通过门禁。

---

## 4. Trigger（触发条件）

| 功能域 | 触发者 | 触发事件 |
|-------|-------|---------|
| F1 | 模板作者 | 点击测试数据集行「运行预览」按钮 |
| F2 | 模板作者 | 点击右上角「全量测试」按钮 |
| F3 (失效) | 系统 | 模板内容（版本号）发生变更 |
| F4 (门禁检查) | 系统 | 渲染「提交测试」按钮时、以及用户 hover tooltip 时 |
| F5 | 模板作者 | 进入 Coverage 子 Tab，或全量测试完成后自动刷新 |
| F6 | 模板作者 | 进入 previewRuns 子 Tab，或全量测试完成后自动刷新 |
| 清理定时任务 | 系统调度器 | @Scheduled，每小时执行一次 `[ASSUMED-TTL-SCHEDULE]` |

---

## 5. Preconditions（前置条件）

### F1 单次预览生成
- 用户已登录且持有有效 JWT，`canAuthorTemplates` = true
- 模板处于 `DRAFT` 或 `TESTING` 状态（含开发版本 in-flight）
- 至少存在一个测试数据集
- 用户已选中一个测试数据集行

### F2 全量测试运行
- 用户已登录且持有有效 JWT，`canAuthorTemplates` = true
- 模板处于 `DRAFT` 或 `TESTING` 状态
- 至少存在一个测试数据集（`hasDataSets` = true）

### F4 提交测试按钮门禁
- 用户已登录，`canAuthorTemplates` = true
- 模板处于 `DRAFT` 状态（`showDraftActions` = true）

### F5 覆盖率展示
- 模板存在 in-flight 开发版本
- 已配置覆盖率阈值（或使用全局默认值）

---

## 6. User Journey Steps（用户操作步骤）

### Journey 1：单次预览生成（F1）

1. 用户打开模板详情页，进入 **Testing** Tab
2. 进入 `dataSets` 子 Tab，查看测试数据集列表
3. 在目标测试数据集行点击「**运行预览**」按钮
4. 系统弹出**进度对话框（Modal）**，显示「正在生成...」
5. 对话框内通过 SSE 实时更新进度状态（QUEUED → PROCESSING → 完成/失败）
6. 生成成功：对话框展示 DOCX 和 PDF 下载链接，及剩余有效时间倒计时
7. 生成失败：对话框展示错误详情和「重试」按钮
8. 用户下载文件，或关闭对话框

### Journey 2：全量测试运行（F2）

1. 用户在测试 Tab 右上角点击「**全量测试**」按钮
2. 系统弹出确认提示（`[ASSUMED-CONFIRM-MODAL]`：是否要对全部 N 个数据集运行测试？）
3. 确认后弹出**批量进度对话框**，显示「已完成 0 / 共 N 个」
4. 对话框通过 SSE 实时更新进度：每完成一个样本推送一次事件
5. 全部完成后：对话框显示汇总（成功 X / 失败 Y），Coverage 面板自动刷新
6. 用户关闭对话框，在「测试历史记录」子区域看到新增的历史条目

### Journey 3：提交测试门禁验证（F4）

1. 用户完成全量测试后，尝试点击「**提交测试**」按钮
2. 若按钮为灰色，用户 hover 上面查看 Tooltip 说明
3. Tooltip 列出未满足的具体条件（含未覆盖的锚点/变量名称列表）
4. 用户补充测试数据或重新运行全量测试，直到所有条件满足
5. 按钮变为可点击状态，用户提交测试

---

## 7. System Responses（系统响应）

### SR-F1-1：接受单次预览请求
- 校验并发数：若当前系统级正在处理的预览任务 ≥ 3，返回 HTTP 429（`PREVIEW_CONCURRENCY_LIMIT_EXCEEDED`）
- 创建 `PreviewRecordEntity`，status = `PROCESSING`，设置 `expiresAt = now + 24h`
- 通过 SSE 连接向客户端推送 `{ event: "progress", status: "PROCESSING" }`
- 异步执行文档生成（调用现有 `PreviewGenerationService`）
- 生成完成后将临时文件存入 MinIO（前缀区分临时/持久），更新 `artifactStorageKey`
- 推送 `{ event: "completed", status: "SUCCEEDED", docxUrl, pdfUrl, expiresAt }`

### SR-F1-2：生成失败
- 更新 `PreviewRecordEntity.status = FAILED`，写入 `errorDetails`
- 推送 `{ event: "error", status: "FAILED", errorCode, errorMessage }`

### SR-F2-1：全量测试运行
- 创建新 `BatchTestRunEntity`，记录 `templateVersionId`（当前 in-flight 版本），status = `RUNNING`
- 依次（或并发，`[ASSUMED-BATCH-CONCURRENCY]`）处理所有数据集
- 每个样本完成时通过 SSE 推送 `{ event: "sample_done", sampleIndex, total, dataSetId, status }`
- 全部完成后更新 `BatchTestRunEntity.status = COMPLETED`，记录 `completedAt`
- 文件存入 MinIO 持久存储（无 TTL）；删除旧记录，保留最新 5 条（按 createdAt）
- 推送 `{ event: "batch_completed", succeeded, failed, runId }`
- 通知前端刷新 Coverage 面板和历史记录列表

### SR-F3-1：模板内容变更触发失效
- 当模板开发版本的内容哈希（或版本号递增）发生变更时，系统将同一模板最新 `BatchTestRunEntity` 的 `invalidatedAt` 设置为当前时间
- `[ASSUMED-INVALIDATION-TRIGGER]`：变更侦测时机为「模板版本内容保存成功后」，由后端服务同步完成

### SR-F4-1：提交测试按钮状态计算
- 后端提供 `GET /templates/{id}/test-submit-readiness` 接口
- 返回 `{ eligible: boolean, reasons: string[], uncoveredAnchors: string[], uncoveredVariables: string[] }`
- 前端在组件挂载和全量测试完成后各拉取一次

### SR-F5-1：覆盖率展示（已有 + 增强）
- 现有 `CoverageComputationService` 返回三维覆盖率
- 新增：每个维度的未覆盖项名称列表（变量 key、锚点 id）

### SR-F6-1：测试历史记录
- `GET /templates/{id}/batch-test-runs?limit=5` 返回最近 5 次 `BatchTestRunEntity` 摘要
- 每条记录含：`runId, createdAt, createdBy, status, totalSamples, succeededCount, failedCount, aggregateCoverage, gatePassed, invalidatedAt`

### SR-清理-1：临时文件清理（@Scheduled）
- 查询所有 `PreviewRecordEntity` 中 `expiresAt < now` 且 `batchTestRunId IS NULL`（即临时预览）
- 从 MinIO 删除对应的 `artifactStorageKey` 和 `pdfArtifactStorageKey`
- 更新 `PreviewRecordEntity.status = EXPIRED`

---

## 8. Acceptance Scenarios（Given / When / Then）

---

### Scenario 8.1：单次预览生成——成功路径（含 SSE 进度）

**ID**: SCEN-F1-01  
**功能域**: F1 单次预览生成

```gherkin
Given 用户 alice 已登录，canAuthorTemplates = true
And   模板 T-001 处于 DRAFT 状态，存在 in-flight 开发版本
And   测试数据集 DS-001 已存在
And   当前系统级并发预览任务数 = 0（< 3）
When  alice 在 DS-001 行点击「运行预览」按钮
Then  系统立即弹出进度对话框，显示「正在生成...」
And   前端通过 SSE 连接到 GET /templates/T-001/preview/sse/{previewId}
And   SSE 推送事件 { event: "progress", status: "PROCESSING" }
When  后端生成 DOCX 和 PDF 成功
Then  SSE 推送事件 { event: "completed", status: "SUCCEEDED", docxUrl, pdfUrl, expiresAt }
And   expiresAt = 请求时刻 + 24 小时（精度到分钟）
And   对话框展示「下载 DOCX」「下载 PDF」两个按钮
And   按钮旁显示剩余有效时间，例如「还有 23h 59min 过期」
And   PreviewRecordEntity 的 status = SUCCEEDED，batchTestRunId IS NULL，expiresAt 已设置
```

---

### Scenario 8.2：单次预览生成——并发超限

**ID**: SCEN-F1-02  
**功能域**: F1 单次预览生成

```gherkin
Given 用户 alice 已登录，canAuthorTemplates = true
And   当前系统级正在 PROCESSING 的预览任务数 = 3（已达并发上限）
When  alice 点击某测试数据集行的「运行预览」按钮
Then  系统返回 HTTP 429
And   错误码 = PREVIEW_CONCURRENCY_LIMIT_EXCEEDED
And   前端在对话框中（或行内）展示「当前预览生成任务已达上限（3个），请稍后重试」
And   不创建新的 PreviewRecordEntity
And   不建立 SSE 连接
```

---

### Scenario 8.3：单次预览生成——生成失败+重试

**ID**: SCEN-F1-03  
**功能域**: F1 单次预览生成

```gherkin
Given 用户 alice 已登录，canAuthorTemplates = true
And   模板 T-001 存在测试数据集 DS-002
And   并发任务数 < 3
When  alice 点击 DS-002 行的「运行预览」按钮
And   后端生成过程中 LibreOffice 转换失败
Then  SSE 推送 { event: "error", status: "FAILED", errorCode: "RENDER_FAILED", errorMessage: "..." }
And   对话框展示错误详情文本
And   对话框展示「重试」按钮
And   PreviewRecordEntity.status = FAILED，errorDetails 已写入
When  alice 点击「重试」按钮
Then  系统发起新的预览生成请求（创建新的 PreviewRecordEntity）
And   进度对话框重置为「正在生成...」状态
```

---

### Scenario 8.4：临时预览文件过期访问

**ID**: SCEN-F1-04  
**功能域**: F1 临时文件 TTL

```gherkin
Given 临时预览记录 P-001 的 expiresAt = T（已过期，T < now）
And   PreviewRecordEntity.status 已被定时任务更新为 EXPIRED
When  alice 点击过期下载链接，或前端调用 GET /previews/P-001/artifact
Then  系统返回 HTTP 410（Gone）
And   错误码 = PREVIEW_ARTIFACT_EXPIRED
And   前端提示「该预览文件已过期，请重新生成」
And   「运行预览」按钮保持可用（允许重新生成）
```

---

### Scenario 8.5：临时预览文件定时清理

**ID**: SCEN-F1-05  
**功能域**: F1 临时文件 TTL / 定时任务

```gherkin
Given 系统存在以下临时预览记录：
      P-A: expiresAt = 2 小时前，batchTestRunId IS NULL，status = SUCCEEDED
      P-B: expiresAt = 1 小时后，batchTestRunId IS NULL，status = SUCCEEDED
      P-C: expiresAt = 3 小时前，batchTestRunId = RUN-001（持久记录，无 TTL）
When  @Scheduled 定时任务执行（间隔 [ASSUMED-TTL-SCHEDULE]：每小时）
Then  P-A 对应的 MinIO 对象被删除
And   P-A 的 status 更新为 EXPIRED
And   P-B 不受影响（未过期）
And   P-C 不受影响（为持久记录，batchTestRunId 非空）
And   定时任务执行完成，日志记录清理数量
```

---

### Scenario 8.6：全量测试运行——成功路径（含 SSE 进度）

**ID**: SCEN-F2-01  
**功能域**: F2 全量测试

```gherkin
Given 用户 alice 已登录，canAuthorTemplates = true
And   模板 T-001 处于 DRAFT 状态，in-flight 开发版本为 V-007
And   存在 3 个测试数据集：DS-001, DS-002, DS-003（全部 required = true）
When  alice 点击右上角「全量测试」按钮并确认
Then  系统创建 BatchTestRunEntity：
      templateId = T-001，templateVersionId = V-007，
      totalSamples = 3，status = RUNNING，createdBy = alice
And   前端弹出批量进度对话框，显示「已完成 0 / 共 3 个」
And   前端通过 SSE 连接到 GET /templates/T-001/batch-test/sse/{runId}
When  DS-001 生成成功
Then  SSE 推送 { event: "sample_done", sampleIndex: 1, total: 3, dataSetId: "DS-001", status: "SUCCEEDED" }
And   对话框更新为「已完成 1 / 共 3 个」
When  DS-002 和 DS-003 均生成成功
Then  SSE 推送 { event: "batch_completed", succeeded: 3, failed: 0, runId: "..." }
And   BatchTestRunEntity.status = COMPLETED，succeededCount = 3，failedCount = 0
And   每个数据集的 PreviewRecordEntity：
      batchTestRunId 已设置，artifactStorageKey 为持久路径（非临时），expiresAt IS NULL
And   对话框显示「全量测试完成：3 个成功，0 个失败」
And   Coverage 面板自动刷新，加载最新覆盖率
And   历史记录列表自动刷新，新增一条记录（状态：成功）
```

---

### Scenario 8.7：全量测试运行——部分失败

**ID**: SCEN-F2-02  
**功能域**: F2 全量测试

```gherkin
Given 模板 T-001 存在 3 个数据集：DS-001, DS-002, DS-003
When  alice 执行全量测试
And   DS-001 生成成功，DS-002 生成失败（errorCode: "VARIABLE_BINDING_MISSING"），DS-003 生成成功
Then  BatchTestRunEntity：succeededCount = 2，failedCount = 1，status = COMPLETED
And   DS-002 对应 PreviewRecordEntity.status = FAILED，errorDetails 已填写
And   对话框显示「全量测试完成：2 个成功，1 个失败」
And   对话框内可展开查看 DS-002 的失败错误详情（errorCode + errorMessage）
And   Coverage 面板刷新后，aggregatePercentage 反映实际覆盖情况
And   提交测试按钮保持灰色（原因：存在失败样本）
```

---

### Scenario 8.8：全量测试结果因模板内容变更失效

**ID**: SCEN-F3-01  
**功能域**: F3 结果失效

```gherkin
Given 模板 T-001 存在有效全量测试结果 RUN-001：
      createdAt = 1 小时前，status = COMPLETED，invalidatedAt IS NULL
And   该结果绑定到模板版本 V-007
When  alice 在编辑器中修改了模板内容并保存（生成新版本 V-008）
Then  系统将 RUN-001.invalidatedAt 设置为当前时间
And   RUN-001 的展示状态变为「已失效」（UI 标签：INVALIDATED）
And   提交测试按钮变为灰色（原因：无有效测试结果）
And   Coverage 面板刷新后反映最新版本 V-008 的覆盖率
And   历史记录列表中 RUN-001 显示「已失效」状态标签
```

---

### Scenario 8.9：提交测试按钮——所有条件满足

**ID**: SCEN-F4-01  
**功能域**: F4 提交测试门禁

```gherkin
Given 模板 T-001 处于 DRAFT 状态
And   存在有效全量测试结果 RUN-002（invalidatedAt IS NULL，status = COMPLETED）
And   RUN-002 中所有数据集均成功（failedCount = 0）
And   锚点覆盖率 = 90%（阈值 80%，满足）
And   变量覆盖率 = 85%（阈值 80%，满足）
And   样本覆盖率 = 100%（阈值 100%，满足）
When  alice 查看测试 Tab 的「提交测试」按钮
Then  按钮状态为可点击（enabled）
And   无 Tooltip 提示（或提示为空）
When  alice 点击「提交测试」按钮并确认
Then  模板生命周期变更为 TESTING 状态
And   页面刷新显示 TESTING 状态标签
```

---

### Scenario 8.10：提交测试按钮——无有效测试结果（置灰）

**ID**: SCEN-F4-02  
**功能域**: F4 提交测试门禁

```gherkin
Given 模板 T-001 处于 DRAFT 状态
And   不存在任何有效全量测试结果（首次或均已失效）
When  alice 查看「提交测试」按钮
Then  按钮为灰色（disabled）
And   Hover Tooltip 显示：「尚未完成全量测试，请先运行全量测试」
```

---

### Scenario 8.11：提交测试按钮——存在失败样本（置灰）

**ID**: SCEN-F4-03  
**功能域**: F4 提交测试门禁

```gherkin
Given 模板 T-001 存在有效全量测试结果 RUN-003（invalidatedAt IS NULL）
And   RUN-003 中 failedCount = 2（DS-002, DS-005 失败）
When  alice 查看「提交测试」按钮
Then  按钮为灰色（disabled）
And   Hover Tooltip 显示：「以下数据集生成失败：DS-002, DS-005。请修复后重新运行全量测试」
```

---

### Scenario 8.12：提交测试按钮——变量覆盖率未达阈值（置灰）

**ID**: SCEN-F4-04  
**功能域**: F4 提交测试门禁

```gherkin
Given 模板 T-001 存在有效全量测试结果，所有样本成功
And   变量覆盖率 = 60%（阈值 80%，不满足）
And   锚点覆盖率 = 90%（阈值 80%，满足）
And   样本覆盖率 = 100%（阈值 100%，满足）
When  alice 查看「提交测试」按钮
Then  按钮为灰色（disabled）
And   Hover Tooltip 显示：「变量覆盖率不足（60% < 80%）。未覆盖变量：customerName, accountNumber」
      （列出具体未覆盖的变量 key，最多显示 5 个，超出则显示「...及其他 N 个」）
```

---

### Scenario 8.13：提交测试按钮——锚点覆盖率未达阈值（置灰）

**ID**: SCEN-F4-05  
**功能域**: F4 提交测试门禁

```gherkin
Given 模板 T-001 存在有效全量测试结果，所有样本成功
And   变量覆盖率 = 90%（阈值 80%，满足）
And   锚点覆盖率 = 75%（阈值 80%，不满足）
And   样本覆盖率 = 100%（阈值 100%，满足）
When  alice 查看「提交测试」按钮
Then  按钮为灰色（disabled）
And   Hover Tooltip 显示：「锚点覆盖率不足（75% < 80%）。未有效绑定锚点：ANCHOR_LOGO, ANCHOR_FOOTER_TEXT」
      （列出 validationStatus != VALID 的锚点 ID）
```

---

### Scenario 8.14：覆盖率面板——正常展示

**ID**: SCEN-F5-01  
**功能域**: F5 覆盖率可见性

```gherkin
Given 模板 T-001 存在 in-flight 版本 V-007
And   变量覆盖率 = 85%（已覆盖 17/20 个必填变量），锚点覆盖率 = 100%（3/3），样本覆盖率 = 100%（5/5）
And   应用阈值：变量 80%，锚点 80%，样本 100%
When  alice 进入 Coverage 子 Tab
Then  Coverage 面板展示三行维度表格：
      | 维度           | 已覆盖/总数 | 覆盖率 | 阈值  | 状态      |
      | 必填变量       | 17/20      | 85%   | 80%  | 已达标    |
      | 样本覆盖       | 5/5        | 100%  | 100% | 已达标    |
      | 锚点绑定       | 3/3        | 100%  | 80%  | 已达标    |
And   页面顶部汇总 Alert 为绿色「覆盖率通过（综合 85%）」
And   显示生效阈值来源（GLOBAL 或 GROUP+groupCode）
```

---

### Scenario 8.15：覆盖率面板——未覆盖锚点/变量列表

**ID**: SCEN-F5-02  
**功能域**: F5 覆盖率可见性

```gherkin
Given 变量覆盖率 = 60%，未覆盖变量：customerName, accountNumber, branchCode
And   锚点覆盖率 = 75%，未有效绑定锚点：ANCHOR_SEAL
When  alice 进入 Coverage 子 Tab
Then  「必填变量」维度行可展开，展开后显示未覆盖变量列表：
      customerName, accountNumber, branchCode
And   「锚点绑定」维度行可展开，展开后显示未有效绑定锚点列表：
      ANCHOR_SEAL（含当前 validationStatus：INVALID / UNBOUND）
And   顶部 Alert 为橙色「覆盖率未达标」
```

---

### Scenario 8.16：测试历史记录——最近 5 次展示

**ID**: SCEN-F6-01  
**功能域**: F6 测试历史记录

```gherkin
Given 模板 T-001 存在 3 条 BatchTestRunEntity 历史记录：
      RUN-100（最新，status=COMPLETED，gate=PASSED，invalidatedAt=null）
      RUN-099（status=COMPLETED，gate=FAILED，invalidatedAt=null）
      RUN-098（status=COMPLETED，gate=PASSED，invalidatedAt=设为1小时前后失效）
When  alice 进入测试 Tab 的历史记录区域
Then  展示最多 5 条记录，按 createdAt 倒序
And   每条记录显示：
      - 执行时间（createdAt 格式化）
      - 执行人（createdBy）
      - 状态标签：COMPLETED / INVALIDATED
      - 覆盖率摘要（aggregateCoverage%）
      - 门禁状态（PASSED / FAILED）
And   RUN-098 显示「已失效」状态标签（因 invalidatedAt 非空）
And   RUN-100 可点击展开查看各数据集详细结果（成功/失败+文件下载链接）
```

---

### Scenario 8.17：测试历史记录——超过 5 次时保留最新 5 条

**ID**: SCEN-F6-02  
**功能域**: F6 测试历史记录

```gherkin
Given 模板 T-001 已存在 5 条历史记录（RUN-096 到 RUN-100）
When  alice 执行一次新的全量测试，产生 RUN-101
Then  系统将最旧的 RUN-096 标记为历史淘汰，或从历史展示列表中移出
And   历史列表中仅显示 RUN-097 至 RUN-101 共 5 条
And   [ASSUMED-RETENTION-POLICY]：RUN-096 的数据库记录和关联 MinIO 文件是否物理删除，
      还是仅从前端列表隐藏，需用户确认（默认推断：软删除/隐藏，不物理删除，
      以保留审计可追溯性）
```

---

## 9. 边界与异常行为

| 场景 | 边界/异常 | 系统行为 |
|------|---------|---------|
| 用户关闭进度对话框后生成仍在进行 | SSE 连接断开 | 后台继续生成；用户可从预览历史列表查看结果，不影响生成完成 |
| 全量测试过程中用户离开页面 | SSE 连接断开 | 后台继续生成，结果持久保存；用户重新进入页面后历史记录刷新显示结果 |
| 并发上限恰好满时，用户提交全量测试 | 无专属全量测试并发限制（`[ASSUMED-BATCH-NO-CONCURRENCY-LIMIT]`） | 全量测试自身不受 3 个并发上限约束，但其内部调用的各次单次生成受全局并发控制 `[ASSUMED-BATCH-CONCURRENCY]` |
| 模板不存在或用户无访问权限 | HTTP 403 / 404 | 前端显示通用错误，按钮均禁用 |
| CoverageComputationService 抛出异常 | 服务端 500 | Coverage 面板显示加载失败提示，提供重试按钮；不影响其他 Tab |
| 测试数据集为空（`hasDataSets` = false） | 「全量测试」按钮禁用 | Tooltip 提示「请先添加测试数据集」 |
| SSE 连接超时（生成时间过长） | 前端 EventSource 超时（`[ASSUMED-SSE-TIMEOUT]`：3 分钟）| 前端提示「生成超时，请检查历史记录」，可通过历史列表刷新获取最终状态 |
| 同一数据集并发提交多次单次预览 | 第 1 次占用并发槽 | 后续请求若超过 3 个上限则返回 429；否则均独立生成 |
| MinIO 存储不可用 | 对象存储异常 | 生成失败，errorCode = `ARTIFACT_STORAGE_FAILED`，记录 errorDetails，推送 SSE error 事件 |

---

## 10. 可观测证据

以下证据可证明功能正确运行：

| 证据类型 | 描述 |
|---------|-----|
| **HTTP 响应** | `POST /templates/{id}/preview/sse` 返回 `text/event-stream` Content-Type |
| **SSE 事件序列** | 开发者工具 Network Tab 可见 `progress` → `completed` 事件流 |
| **数据库记录** | `preview_record` 表中新增记录，`expires_at` = 创建时间 + 24h，`batch_test_run_id` IS NULL |
| **数据库记录** | `template_batch_test_run` 中 `template_version_id` 已填写，`status` = COMPLETED |
| **数据库记录** | 全量测试后 `preview_record` 中关联记录的 `expires_at` IS NULL（持久） |
| **MinIO 文件** | 临时文件存于 `preview-temp/` 前缀；持久文件存于 `batch-test/` 前缀 `[ASSUMED-STORAGE-PREFIX]` |
| **UI 状态** | 提交测试按钮在门禁条件满足时可点击；Tooltip 文本与后端返回的 `reasons` 列表一致 |
| **定时任务日志** | 应用日志出现 `[PreviewCleanupScheduler] Cleaned N expired preview records` |
| **失效触发** | 模板内容保存后，`template_batch_test_run.invalidated_at` 自动更新，UI 刷新显示「已失效」 |
| **历史记录** | 历史记录列表展示 ≤ 5 条，最新一条的 `run_id` 与全量测试返回的 `runId` 一致 |

---

## 11. 待确认的 [ASSUMED] 假设项

以下为编写规格时基于合理推断填写的内容，**需要产品/技术负责人确认后方可实施**：

| ID | 假设内容 | 默认推断 | 影响范围 |
|----|---------|---------|---------|
| `[ASSUMED-TTL-SCHEDULE]` | 临时预览文件清理定时任务执行间隔 | 每小时（`@Scheduled(fixedDelay = 3600000)`） | SR-清理-1, SCEN-F1-05 |
| `[ASSUMED-CONFIRM-MODAL]` | 全量测试按钮点击后是否需要确认弹窗 | 需要弹出确认（防止误触） | Journey 2, SCEN-F2-01 |
| `[ASSUMED-BATCH-CONCURRENCY]` | 全量测试内部对每个样本的调用是否并发执行 | 顺序执行（保持现有 `BatchTestGenerationService` 逻辑），SSE 逐样本推送 | SR-F2-1 |
| `[ASSUMED-BATCH-NO-CONCURRENCY-LIMIT]` | 全量测试是否受「3 个并发上限」约束 | 全量测试本身不受限，其内部的每次单次生成调用受限 | SCEN-F2-01, 边界行为 |
| `[ASSUMED-INVALIDATION-TRIGGER]` | 模板版本内容变更如何触发失效 | 模板版本内容保存成功后，在同一事务或事务后同步更新 `invalidated_at` | SR-F3-1, SCEN-F8-8 |
| `[ASSUMED-STORAGE-PREFIX]` | 临时预览文件与持久批测文件的 MinIO 存储路径前缀 | 临时：`preview-temp/{templateId}/{previewId}/`；持久：`batch-test/{templateId}/{runId}/{dataSetId}/` | SR-F1-1, SR-F2-1 |
| `[ASSUMED-SSE-TIMEOUT]` | 前端 SSE 连接超时时间 | 3 分钟 | 边界行为 |
| `[ASSUMED-RETENTION-POLICY]` | 历史记录超过 5 条时旧记录的处理方式 | 软删除（设置 `is_visible = false` 或 `hidden_at`），不物理删除，以保留审计 | SCEN-F6-02 |
| `[ASSUMED-BATCH-SSE-ENDPOINT]` | 全量测试 SSE 端点设计方式 | 先 `POST /templates/{id}/batch-test` 返回 `runId`，再 `GET /templates/{id}/batch-test/{runId}/sse` 建立 SSE 连接 | SR-F2-1, SCEN-F2-01 |
| `[ASSUMED-UNCOVERED-LIST-MAX]` | Tooltip 中未覆盖列表最大展示数量 | 最多 5 个，超出显示「...及其他 N 个」 | SCEN-F4-04, SCEN-F4-05 |
| `[ASSUMED-HISTORY-COVERAGE]` | 历史记录中的覆盖率摘要数据来源 | 全量测试完成后，`CoverageComputationService.compute()` 的结果快照存入 `BatchTestRunEntity.summaryJson` | SCEN-F6-01 |

---

## 12. 数据模型变更（新增字段）

以下为需要新增的数据库字段，Flyway 版本从 **V41** 开始：

### V41：`preview_record` 表新增字段

```sql
-- V41__preview_record_temp_ttl_and_error.sql
ALTER TABLE preview_record
    ADD COLUMN expires_at        TIMESTAMP WITH TIME ZONE,   -- 临时预览 TTL，null = 持久
    ADD COLUMN error_details     TEXT;                        -- 失败时的错误详情 JSON
```

### V42：`template_batch_test_run` 表新增字段

```sql
-- V42__batch_test_run_versioned_status.sql
ALTER TABLE template_batch_test_run
    ADD COLUMN template_version_id UUID,                     -- 绑定版本，用于失效判断
    ADD COLUMN status              VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',  -- RUNNING / COMPLETED / INVALIDATED
    ADD COLUMN completed_at        TIMESTAMP WITH TIME ZONE,
    ADD COLUMN invalidated_at      TIMESTAMP WITH TIME ZONE,
    ADD COLUMN coverage_snapshot_json TEXT;                  -- 测试完成时的覆盖率快照
```

### V43：新建并发控制辅助（可选，基于 Redisson 原子计数则无需）

```
[ASSUMED-CONCURRENCY-IMPL]：并发上限通过 Redisson AtomicLong 实现，无需数据库迁移
```

---

## 13. API 端点清单（新增）

| 方法 | 路径 | 说明 | 身份要求 |
|------|------|------|---------|
| `POST` | `/templates/{id}/preview-sse` | 启动单次预览，返回 `previewId`（非 SSE） | canAuthorTemplates |
| `GET` | `/templates/{id}/preview/{previewId}/progress` | SSE 流，推送单次预览进度 | canAuthorTemplates |
| `POST` | `/templates/{id}/batch-test` | 启动全量测试，返回 `runId` | canAuthorTemplates |
| `GET` | `/templates/{id}/batch-test/{runId}/progress` | SSE 流，推送全量测试进度 | canAuthorTemplates |
| `GET` | `/templates/{id}/batch-test-runs` | 查询最近 5 次历史记录 | canAuthorTemplates |
| `GET` | `/templates/{id}/test-submit-readiness` | 查询提交测试按钮的门禁状态 | canAuthorTemplates |
| `GET` | `/templates/{id}/coverage/detail` | 覆盖率 + 未覆盖项列表（扩展现有接口） | canAuthorTemplates |

> **注**：SSE 端点使用 `text/event-stream` Content-Type；非 SSE 端点遵循现有统一信封（`metadata`/`result`/`error`）。

---

## 14. BDD 就绪声明

**状态**: `ready`

本规格已满足以下条件，可移交 `plan-orchestrator` 进行任务分解并进入 TDD 实现阶段：

- [x] Actor/Role 已明确（模板作者、测试决策者、系统调度器）
- [x] 所有 6 个功能域均有完整 Given/When/Then 场景
- [x] 覆盖成功路径、边界、异常、并发、过期、失效、门禁等全部场景
- [x] 数据模型变更（Flyway V41-V42）已明确
- [x] 新增 API 端点已列出
- [x] 11 个 [ASSUMED] 项已明确标注，不影响核心路径实现
- [x] 可观测证据已定义（支持验收测试）
- [x] 与现有代码结构（BatchTestGenerationService、PreviewGenerationService、CoverageComputationService、CoverageThresholdResolver）的集成点已明确

**前置确认项（实施前请确认）**：
1. `[ASSUMED-INVALIDATION-TRIGGER]`：失效触发时机（同步 vs 异步）
2. `[ASSUMED-RETENTION-POLICY]`：超过 5 条历史的处理方式（软删除 vs 物理删除）
3. `[ASSUMED-BATCH-CONCURRENCY]`：全量测试内部是否需要并发加速

---

## 15. 可追溯性

| 文档 | 路径 | 关联内容 |
|-----|------|---------|
| 产品需求文档 | `docs/product/PRD.md` | 模板测试流程章节 |
| 需求计划 | `docs/requirements/requirements-plan.md` | 测试覆盖率门禁需求 |
| 覆盖率阈值后端 | `backend/src/main/java/com/bank/docgen/template/service/CoverageThresholdResolver.java` | 全局默认：variable=80%, sample=100%, anchor=80% |
| 覆盖率计算后端 | `backend/src/main/java/com/bank/docgen/template/service/CoverageComputationService.java` | 三维覆盖率计算逻辑 |
| 批量测试实体 | `backend/src/main/java/com/bank/docgen/rendering/persistence/BatchTestRunEntity.java` | 现有批量测试数据结构 |
| 预览记录实体 | `backend/src/main/java/com/bank/docgen/rendering/persistence/PreviewRecordEntity.java` | 现有预览记录（含 expiresAt 待新增） |
| 批量测试服务 | `backend/src/main/java/com/bank/docgen/rendering/service/BatchTestGenerationService.java` | 现有同步批量测试逻辑（待改造为 SSE 异步） |
| 生命周期服务 | `backend/src/main/java/com/bank/docgen/template/service/TemplateLifecycleService.java` | `submitForTest()` 方法（门禁前置检查将在此前插入） |
| 测试工作流前端 | `frontend/src/components/templates/TemplateTestPreviewWorkflowPanel.vue` | 现有「运行预览」「试生成（已选）」「批量试生成全部」按钮（待改造） |
| 覆盖率面板前端 | `frontend/src/components/templates/TemplateCoveragePanel.vue` | 现有覆盖率展示（待增强未覆盖列表） |
| 历史记录面板前端 | `frontend/src/components/templates/TemplatePreviewRunHistoryPanel.vue` | 现有单次预览历史（与新全量测试历史区分） |
| 定时任务参考 | `backend/src/main/java/com/bank/docgen/collaboration/scheduler/CollaborationEscalationScheduler.java` | @Scheduled 实现参考 |
| Flyway 当前版本 | `backend/src/main/resources/db/migration/V40__template_version_deleted_at.sql` | 下一个迁移从 V41 开始 |

---

*本文档由 behavior-spec-author 子 Agent 生成于 2026-07-03。所有 `[ASSUMED]` 项须人工确认后方可纳入实施计划。*
