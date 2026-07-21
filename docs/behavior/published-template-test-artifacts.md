# BDD 行为规格：已发布模板 — 测试产物可查看 / 可下载

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-PTA`  
**编写日期:** 2026-07-21  
**Slice:** `published-template-test-artifacts`  
**Task ids:** `["published-template-test-artifacts"]` · Task Master **#144** (**Done** — MAIN `ac36ecbc` / feature `6bc74ff1`; sole-active **cleared**)  
**Formal phase:** **None**  
**Placement:** merged to MAIN; worktree **REMOVED**  
**Plan detail:** [published-template-test-artifacts.md](../plan/detail/published-template-test-artifacts.md)  
**SYS-NORM Wave 4 cross-ref:** program §5.1 product evidence for TM **#148** docs-close — [sys-norm-test-artifacts.md](./sys-norm-test-artifacts.md) (**BDD-SYS-NORM-W4-001…010** → **BDD-PTA-***; plan [detail](../plan/detail/sys-norm-test-artifacts.md))  
**完成声明约束:** 关闭「已发布模板无法查看测试时生成的文件」缺口 — **已交付**；**不**翻转 checklist **#3b** / **#5a**；**不** reopen RTL；CE-O02 **保持 Deferred**；**不**宣称 go-live

---

## 1. 概述

用户确认问题：「已经发布的模板，没办法查看测试时候生成的文件」。

| 缺口（现状证据） | 目标 |
| --- | --- |
| `TemplateReleaseDetailView` Testing 页仅挂载 `BatchTestHistoryPanel` 只读摘要；**缺少** `TemplatePreviewRunHistoryPanel`（SUCCEEDED 行 DOCX/PDF 下载） | 发布线（及优选 STOPPED / DEPRECATED 发布线）Testing 页可列出既有 preview runs，并对 SUCCEEDED 产物下载 |
| `@open-preview` / `@open-data-set` **未接线** | 批量历史展开后，样本含 `previewId` 时可 **Open preview** 落到可下载/选中的 preview 历史；Open data set 见边界 |
| 开发工作区 `showAuthoringSection === false`（`PUBLISHED` / `STOPPED` / `DEPRECATED`）隐藏完整 Testing 工作区 | **不**为已发布状态重新打开可变编 Testing；在 **release detail** Testing 页补齐只读产物回顾 |
| `AsyncBatchTestExecutionSupport` 持久化 `sampleResults` 时丢弃 `previewId` / `docxKey` / `pdfKey`（成功路径写 `null`） | 异步批量完成后样本结果保留可钻取标识，使 Open preview 可用 |
| 后端 preview 下载 API **本就不**按 `PUBLISHED` 生命周期拦截 | **禁止**为本片新增不必要的 PUBLISHED 阻断 |

| 行为域 | 摘要 |
| --- | --- |
| **PTA-01 Preview 历史可读** | Release detail → Testing：挂载与 authoring 同等授权门禁的 preview run 历史列表 |
| **PTA-02 产物下载** | SUCCEEDED 行可下载 DOCX / PDF（fail-closed 与 authoring 一致） |
| **PTA-03 批量 Open preview** | BE 持久化 `previewId`（及既有契约字段 `docxKey`/`pdfKey`）；FE 接线后 Open preview 可用 |
| **PTA-04 Fail-closed** | 无读权限不可见历史/不可下载；**不**放宽权限矩阵 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 将 `showAuthoringSection` 对 `PUBLISHED`/`STOPPED`/`DEPRECATED` 改为 `true` | **禁止** — 不重开可变编 Testing / Run preview / Run full test |
| 在 release detail 新增完整 Data sets / Coverage / Change diff 工作区 | **Out of scope**（除非已有只读面可复用；本片不强制新建） |
| 为 download API 增加「仅 DRAFT/TESTING」生命周期闸 | **禁止** — 用户确认后端下载本就不 lifecycle-gate PUBLISHED |
| 翻转 #3b / #5a；reopen RTL；激活 CE-O02 | **禁止** |
| 宣称 go-live / 激活 CD-3 / 正式 P3 | **禁止** |
| 扩展 CE-U18 路径统一语义以外的批量运行模型 | Out of scope — 复用既有异步历史 + CE-U18 钻取 |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **模板作者 / 编排人员** | `authorTemplates`（可读模板快照） | 打开已发布（或 STOPPED/DEPRECATED）release detail → Testing；查看 preview 历史；下载 SUCCEEDED DOCX/PDF；从批量历史 Open preview |
| **测试决策者** | `decideTests`（可读快照时） | 只读回顾测试产物；不因本片获得新写能力 |
| **只读 / 无组授权** | 无模板可读权限 | 历史/下载 fail-closed（403/404）；不泄露他组产物 |
| **系统** | Preview 历史 API + 下载 API + `AsyncBatchTestExecutionSupport` | 列表既有 runs；按既有授权返回字节；异步批量样本 JSON 含钻取字段 |

---

## 3. Goal

1. 在 **PUBLISHED**（优选同页覆盖 **STOPPED** / **DEPRECATED** 发布线）release detail 的 **Testing** 页，用户能列出该模板既有 **preview runs**，并对 **SUCCEEDED** 记录下载 DOCX / PDF。  
2. 批量测试历史展开后，当样本结果含 **`previewId`** 时，**Open preview** 可用并定位到对应 preview（历史选中 / 下载面）。  
3. 授权保持 fail-closed，与 authoring Testing 下载门禁一致 — **不**扩大 capability / 组范围。  
4. Formal phase 保持 **None**；不翻转 #3b/#5a；不 reopen RTL；CE-O02 保持 deferred。

---

## 4. 已确认决策 vs 推导假设

### 4.1 已确认（用户根因 + 验收）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **PTA-C1** | 已发布模板须能查看测试期生成文件（preview 历史 + SUCCEEDED DOCX/PDF 下载）。 | 用户确认；验收 #1 |
| **PTA-C2** | FE 主因：release Testing 缺 `TemplatePreviewRunHistoryPanel`；`@open-preview` / `@open-data-set` 未接线；`showAuthoringSection` 对 PUBLISHED 为 false 导致完整 Testing 被藏。 | 用户确认根因 |
| **PTA-C3** | BE 次因：异步批量 `sampleResults` 持久化丢弃 `previewId`/`docxKey`/`pdfKey`，导致 Open preview 钻取失效。 | 用户确认根因；代码：`AsyncBatchTestExecutionSupport` 成功/失败路径均写 `null` keys |
| **PTA-C4** | 后端下载 API **不要**新增 PUBLISHED lifecycle 阻断。 | 用户确认 |
| **PTA-C5** | Fail-closed 授权不变 — 无权限放宽。 | 验收 #3 |
| **PTA-C6** | **不**翻转 #3b/#5a；**无** RTL；CE-O02 **deferred**。 | 验收 #4 |
| **PTA-C7** | 批量历史 **Open preview** 在 `previewId` 存在时可用（BE 持久化修复后）。 | 验收 #2 |

### 4.2 本片确认的实现边界（仓库事实推导，非新产品范围）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **PTA-D1** | **表面：** 在 `TemplateReleaseDetailView` Testing 槽位挂载 `TemplatePreviewRunHistoryPanel`（只读列表 + SUCCEEDED 下载），保留既有 `BatchTestHistoryPanel`。 | PTA-C1/C2；与 `TemplateDetailTestingTab` previewRuns 子区对齐的只读子集 |
| **PTA-D2** | **禁止**通过把 `showAuthoringSection` 对 PUBLISHED 置 true 来「顺带」恢复 Run preview / Run full test / Data sets 编辑。Release Testing 保持 **只读回顾**。 | PTA-C2；catalog release detail read-only 语义 |
| **PTA-D3** | **Open preview：** 接线 `@open-preview` → 选中对应 `previewId` 的 preview 历史行（或等价可见选中态），使用户可下载该次产物。 | PTA-C7；CE-U18 BTH-009 / U18-D4 |
| **PTA-D4** | **Open data set：** release detail **无** Data sets 子 Tab。本片不强制新建。若用户激活 Open data set：须有 **非静默** 反馈（English-first toast/提示），且 **不得** 打开可变编路径。若未来/既有只读数据集面可复用则可选接线 — **非**本片 Done 门槛。 | PTA-C2 缺口陈述 vs 验收仅强制 Open preview |
| **PTA-D5** | **BE 持久化：** 异步批量成功样本须把 `PreviewRecordView.previewId` 与产物键（`artifactStorageKey` / `pdfArtifactStorageKey` 映射为契约字段 `docxKey`/`pdfKey`，或等价 JSON 字段）写入 `sampleResultsJson`；失败样本可继续无 keys。`SampleResult` 形状可扩展 `previewId` 以匹配 `BatchTestHistorySampleResultView`。 | PTA-C3；CE-U18 U18-D2 |
| **PTA-D6** | **生命周期覆盖：** 必达 **PUBLISHED**；**STOPPED** / **DEPRECATED** 发布线若使用同一 `TemplateReleaseDetailView` Testing 槽，行为相同（只读历史 + 下载）。 | 验收 #1「preferably」；catalog STOPPED release read-only |
| **PTA-D7** | **授权：** 列表/下载沿用 `requireReadableSnapshot`（或现行等价）；403/404 不泄露他组。无新 capability bit；不改 permission-matrix 赋权面。 | PTA-C5 |
| **PTA-D8** | **i18n：** English-first（en + zh-CN）；复用既有 preview history / batch history 文案键优先。 | i18n-english-first |
| **PTA-D9** | **frontend_ui_in_scope:** `true` — 须 E2E + UIUX（银行 OA）。 | delivery constitutions |

---

## 5. Trigger

| 触发 | 事件 |
| --- | --- |
| 打开发布 Testing | 用户进入 `/templates/{templateId}/releases/{releaseVersion}` → **Testing** 工作区 Tab |
| 下载产物 | 用户在 preview 历史对 SUCCEEDED 行点击 Download DOCX / Download PDF |
| 批量钻取 | 用户展开 `BatchTestHistoryPanel` 行 → 点击 **Open preview**（样本含 `previewId`） |
| 异步批量完成（写路径） | 系统完成 `batch-tests/run` 样本生成并持久化 `sampleResultsJson`（本片修复写入字段） |

---

## 6. Preconditions

- 用户已登录管理 UI；对目标模板具备 **可读快照** 权限（与 authoring preview 历史一致）。  
- 模板存在发布线 `releaseVersion`，生命周期为 **PUBLISHED**（或 STOPPED / DEPRECATED 且走同一 release detail）。  
- 至少存在一条历史 preview run（下载场景要求至少一条 **SUCCEEDED** 且产物未按既有 TTL/清理策略失效）；或至少一条含可解析 `previewId` 的批量历史样本（Open preview 场景）。  
- 下载失败路径：无权限或产物不可用时沿用既有错误面（不发明新错误码除非实现必须）。

---

## 7. Primary journey

1. 作者从 package hub 打开 **已发布** release → release detail。  
2. 切换到 **Testing** Tab。  
3. 看到只读说明 + **批量测试历史** + **Preview run 历史**（列表可见）。  
4. 对一条 **SUCCEEDED** preview 点击 **Download DOCX**（及可选 PDF）→ 获得文件字节。  
5. （可选）展开一次 COMPLETED 批量历史 → 对含 `previewId` 的成功样本点 **Open preview** → 历史中对应 preview 被选中且可下载。  
6. 全程 **无** 可变编 Testing 控件（无 Run preview / Run full test / 数据集编辑）被本片重新启用。

---

## 8. System responses

| 路径 | 响应 |
| --- | --- |
| Testing Tab 加载（有权限） | Preview 历史列表渲染；批量历史仍可用；只读提示保留 |
| SUCCEEDED 下载（有权限、产物可用） | 返回 DOCX/PDF 字节；UI 触发 download |
| SUCCEEDED 下载（无权限） | API 403/404；UI 既有错误；无他组内容 |
| Open preview（样本有 `previewId`） | 选中对应 preview 历史行；下载控件可用（若仍 SUCCEEDED） |
| Open preview（无 `previewId`） | 不展示 Open preview 或点击无钻取（与 CE-U18 面板既有 `v-if="sample.previewId"` 一致） |
| 异步批量持久化（修复后） | `sampleResultsJson` 成功样本含 `previewId` 与产物键字段 |
| PUBLISHED 下载 API | **不**因生命周期单独拒绝（保持现状门禁语义） |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-PTA-001 — PUBLISHED release Testing 显示 preview 历史

```gherkin
Given 模板存在已发布 releaseVersion，且至少有一条历史 preview run
And 作者具备该模板可读权限
When 作者打开 /templates/{templateId}/releases/{releaseVersion} 并进入 Testing Tab
Then 页面展示 TemplatePreviewRunHistoryPanel（或等价 preview run 列表）
And 仍展示 BatchTestHistoryPanel
And 不出现 Run preview / Run full test 等可变编 Testing 主入口
```

### BDD-PTA-002 — SUCCEEDED preview 可下载 DOCX/PDF

```gherkin
Given BDD-PTA-001 且存在 status=SUCCEEDED 的 preview run，产物仍可用
When 作者对该行激活 Download DOCX（及 Download PDF）
Then 浏览器获得对应文件（download 事件或授权 artifact GET 成功，非 401/403）
And 授权门禁与 authoring Testing 下载一致（fail-closed）
```

### BDD-PTA-003 — STOPPED/DEPRECATED 发布线同行为（优选）

```gherkin
Given 发布线生命周期为 STOPPED 或 DEPRECATED，且使用同一 TemplateReleaseDetailView Testing 槽
And 作者可读该模板，且存在 SUCCEEDED preview
When 作者打开该 release Testing Tab 并下载产物
Then 行为与 BDD-PTA-001/002 相同（列表可见 + 可下载）
```

### BDD-PTA-004 — 异步批量 sampleResults 持久化 previewId / 产物键

```gherkin
Given 对模板执行异步全量测试且至少一样本 SUCCEEDED
When 系统持久化该次 BatchTestRun 的 sampleResultsJson
Then 成功样本 JSON 含非空 previewId
And 含可用于契约/钻取的 docxKey 与 pdfKey（或与 PreviewRecordView 产物键等价的已文档化字段）
And GET 批量历史（或详情）暴露的 sampleResults 可供 FE Open preview 使用
```

### BDD-PTA-005 — 批量历史 Open preview 接线可用

```gherkin
Given PUBLISHED release Testing 页已挂载 BatchTestHistoryPanel
And 某 COMPLETED 运行的样本含 previewId（BDD-PTA-004 修复后）
When 作者展开该行并激活 Open preview
Then UI 选中对应 previewId 的 preview 历史行（或等价可见选中）
And 作者可对该 preview 执行 SUCCEEDED 下载（若产物仍可用）
```

### BDD-PTA-006 — Open data set 在 release 面不静默、不打开编辑

```gherkin
Given 作者在 release detail Testing 的批量历史展开区看到 Open data set
When 作者激活 Open data set
Then 系统不打开可变编 Data sets / authoring Testing
And 若无只读数据集表面可导航，则显示非阻断 English-first 提示
And 应用不崩溃
```

### BDD-PTA-007 — Fail-closed 授权不变

```gherkin
Given 会话对模板不可读（跨组或无 capability）
When 请求 preview 历史列表或 DOCX/PDF 下载，或批量历史
Then API 拒绝（403/404 既有契约）
And UI 不展示他组 preview / 样本内容
And 本片未引入新的权限放宽
```

### BDD-PTA-008 — 不新增 PUBLISHED 下载生命周期阻断

```gherkin
Given 作者可读已发布模板，且 SUCCEEDED preview 产物可用
When 调用既有 preview 产物下载 API
Then 请求不因 lifecycleStatus=PUBLISHED 被本片新增规则拒绝
And 仅既有授权 / 产物存在性 / TTL 清理等原有规则可导致失败
```

### BDD-PTA-009 — 非目标与完成约束

```gherkin
Given 本片交付完成
Then 未将 showAuthoringSection 对 PUBLISHED/STOPPED/DEPRECATED 改为 true 以恢复可变编 Testing
And 未翻转 checklist #3b / #5a
And 未 reopen RTL
And 未激活或宣称 CE-O02 Done
And 未宣称 go-live
```

---

## 10. Boundary / exception

| 场景 | 期望 |
| --- | --- |
| Preview 历史为空 | 可见 empty 态；不崩溃 |
| Preview FAILED / 非 SUCCEEDED | 不提供成功下载；沿用面板既有禁用/隐藏 |
| 产物已清理 / 410 / 不可用 | 既有错误提示；不假装下载成功 |
| `sampleResults` 无 `previewId`（修复前遗留 run） | 不显示 Open preview 或点击无钻取；摘要/展开仍可用 |
| JSON 损坏 | 批量展开 empty/error；preview 历史面板独立可用 |
| 无 `authorTemplates` 但可读（decideTests） | 只读列表 + 下载按既有可读门禁；无新写入口 |
| 双品牌 / @1920 | UIUX Critical=0；符合 bank OA + English-first |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | Release Testing：preview 历史列表 + 批量历史；SUCCEEDED 下载控件 |
| Network | Preview list + artifact download 成功；无因 PUBLISHED 新增的拒绝 |
| API / DB | 新异步批量 run 的 `sampleResultsJson` 含 `previewId` 与产物键 |
| Vitest | Release detail Testing 挂载历史面板；open-preview 接线；BE 持久化字段 |
| E2E + UIUX | 至少 BDD-PTA-001 + BDD-PTA-002；优选 BDD-PTA-005；双品牌 @1920；Critical=0 |
| 非目标 | 无 #3b/#5a 翻转；无 RTL；CE-O02 仍 deferred |

---

## 12. Traceability

| 来源 | 关系 |
| --- | --- |
| 用户确认根因（2026-07-21） | 「已经发布的模板，没办法查看测试时候生成的文件」 |
| Slice `published-template-test-artifacts` | 本片 task id |
| [ce-u18-batch-test-history.md](./ce-u18-batch-test-history.md) | 批量展开 / Open preview / sampleResults 契约（U18-D2 / **U18-D2′ PTA**；U18-D4；BTH-009） |
| [openapi-v1.yaml](../api/openapi-v1.yaml) `BatchTestHistorySampleResultView` | 异步样本规范含 `previewId` + `docxKey`/`pdfKey`（成功且产生 preview 时）；既有 `downloadTemplatePreviewDocx` / `Pdf` |
| [contract-outline.md](../api/contract-outline.md) «sampleResults（CE-U18 / PTA）」+ «预览产物下载» | 合同叙事；**无** PUBLISHED 下载生命周期闸；**无**新权限 |
| [template-testing-overhaul.md](./template-testing-overhaul.md) | F1 下载 / F6 历史基线 |
| [preview-success-artifact-download-journey.md](./preview-success-artifact-download-journey.md) | Testing 成功态 DOCX/PDF 下载证据模式（dev Testing；本片对齐发布线只读回顾） |
| [catalog-navigation-ux.md](../product/catalog-navigation-ux.md) | Release detail 只读表面；STOPPED 仍可读 |
| `TemplateReleaseDetailView.vue` | 现状仅 `BatchTestHistoryPanel` |
| `TemplateDetailTestingTab.vue` | Authoring 对照：历史面板 + open-preview/open-data-set 接线 |
| `useTemplateDetailVisibility.showAuthoringSection` | PUBLISHED/STOPPED/DEPRECATED → false（本片不翻转） |
| `AsyncBatchTestExecutionSupport` / `AsyncBatchTestOrchestrator.SampleResult` | 异步样本持久化缺口 |
| `BatchTestHistorySampleResultView` | 历史样本契约含 `previewId`/`docxKey`/`pdfKey` |
| Checklist #3b / #5a；CE-O02；RTL | 非目标 |

---

## 13. 现状 → 实现提示（非额外需求）

| 发现 | 路径提示（供 plan / 实现） |
| --- | --- |
| Release Testing 缺 preview 历史 | `TemplateReleaseDetailView` `#testing` 增加 `TemplatePreviewRunHistoryPanel` |
| 事件未接线 | 监听 `BatchTestHistoryPanel` `@open-preview`（必达）；`@open-data-set` 按 PTA-D4 |
| 选中 preview | 本地 `selectedPreviewId` 或复用 panel `@selected`；可选挂载只读 `TemplatePreviewPanel` |
| 成功样本写 null keys | 从 `previewGenerationService.runTestGenerateForBatch` 返回的 `PreviewRecordView` 填入 `previewId` + 产物键后再 `writeSampleResults` |
| `SampleResult` 无 `previewId` 字段 | 扩展 record 以匹配历史 view；或序列化时使用含 `previewId` 的 DTO — 单测锁定 JSON |
| 勿改 `showAuthoringSection` | 保持发布态隐藏 dev Testing；只修 release 面 |

---

## 14. BDD 就绪声明

**bdd_readiness: ready**

- 场景 ID：`BDD-PTA-001` … `BDD-PTA-009` 可驱动 TDD Red（FE Vitest / Playwright + BE 单测）
- 已持久化本文件；`docs/README.md` behavior 索引已登记
- **open_questions: []**（根因与验收已由用户确认；Open data set 在 release 面降级为非静默边界，不阻塞就绪）

**frontend_ui_in_scope:** `true`  
**owning_doc:** `docs/behavior/published-template-test-artifacts.md`  
**task_ids:** `["published-template-test-artifacts"]`
