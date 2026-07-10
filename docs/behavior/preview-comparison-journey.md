# BDD：预览与最终产物并排对比

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CDP-CMP`  
**CDP:** CD-BDD-T07 → **CD-E2E-T09**  
**编写日期:** 2026-07-11（CD-E2E-T09 readiness confirm；补全 CD-0 必填字段）  
**程序:** [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md)  
**计划任务:** [CDP-e2e-full-chain-evidence.md](../plan/detail/CDP-e2e-full-chain-evidence.md) § CD-E2E-T09

---

## 1. 概述

在浏览器内证明：测试/审批角色可打开 **预览 vs 最终产物** 对比面板，查看结构化差异（定位到锚点/章节等），并在同面板保真警告列表中按 `warningCode` 筛选。

产品行为以 PRD / usability-review 已确认基线 + **P19-T05**（`PreviewComparisonService` + `TemplatePreviewPanel`）为权威。本文件是 CD-2 **浏览器证据** 规格，不重开 P19 全矩阵，也不要求新建双栏 DOCX|PDF 像素级并排阅读器。

**「并排对比」产品落点（v1 已交付）：**

| PRD 表述 | 管理 UI 落点（P19） | 非本切片 |
| --- | --- | --- |
| 预览与最终 DOCX/PDF 并排对比视图 | Testing Tab → **Preview runs** → `TemplatePreviewPanel`：对比摘要 + **Structured preview comparison** 表 + 保真警告列表 | F7 / LR-C4 **Authoring** side-by-side（编辑态 vs edit-preview）— 已 Done，禁止混测 |
| 差异定位到页/锚点/章节/组件 | 对比表列：`locationType`（`PAGE`/`ANCHOR`/`SECTION`/`COMPONENT`）+ `locationRef` + `diffCode`/`summary` | 不要求点击跳转到 Word 光标 |
| 按 `warningCode` 筛选 | 同面板 `FidelityWarningList` 的 `data-testid="filter-warning-code"`（保真警告列表） | 对比表本身无独立 warningCode 筛选项；保真衍生行的 `diffCode`/`locationRef` 常等于 warning code |

---

## 2. Actor / Role

| Actor | 角色 / 账户 | 权限 |
| --- | --- | --- |
| **TEMPLATE_TESTER** | 种子测试员（或具备测试工作区只读/判定能力的等价用户） | 打开模板 Testing workspace；查看预览运行与对比面板 |
| **TEMPLATE_APPROVER** | 种子审批员（可选第二角色证据） | 打开同一模板 Testing / 审批材料中的预览对比摘要；本切片以 Testing Tab 面板为主路径 |

> E2E 主路径优先 **TEMPLATE_TESTER**（与 T08 Testing Tab 动线一致）。APPROVER 若同会话可登录，可复用同一面板断言；不强制双角色双 spec。

---

## 3. Goal（用户目标）

**作为** 测试人员（或审批人员）  
**我希望** 打开预览与最终产物的对比视图，按 `warningCode` 筛选保真警告，并看到差异定位到锚点/章节等位置摘要  
**以便** 在测试/审批材料中快速定位预览对比风险，且 E2E/UIUX 可截图证明。

---

## 4. Trigger（触发条件）

- Docker 验收栈健康（UI `:4173`，backend `:8080/healthz`）
- 用户打开模板详情 → **Template testing** workspace tab → **Preview runs** 子 Tab
- 选中一条 **SUCCEEDED** 的预览运行（`View details` / 行选中），使 `TemplatePreviewPanel` 渲染

---

## 5. Preconditions（前置条件）

1. 用户已真实登录管理 UI（非角色模拟）。
2. 模板存在至少一条 **final-path** 预览记录：`status === SUCCEEDED`，且带 DOCX/PDF 产物引用（可复用 T08 成功预览或 CDP golden / template-testing helpers 预置）。
3. 该预览记录的 `previewComparison` 可供展示（摘要计数 + `items[]`；允许空表，但 **BDD-CDP-CMP-001** 要求至少能证明「面板打开 + 筛选控件可用」；若种子无差异行，应用含 `locationType`/`locationRef` 的非空对比数据，或 API helper 预置后再断言定位列）。
4. 为证明 `warningCode` 筛选：预览的 `fidelityWarnings` 至少 1 条带稳定 `code`（可与对比行同源）。
5. Setup 可用 API helper 预置预览；**测试本体内**须经 UI 打开 Preview runs → 选中预览 → 看见对比面板（不得仅 API 断言）。

---

## 6. Primary Journey（主路径）

| # | Actor | UI 动作 | 系统响应 |
| --- | --- | --- | --- |
| 1 | TESTER | 打开模板 → **Template testing** → **Preview runs** | 预览运行历史可见 |
| 2 | TESTER | 选中一条 SUCCEEDED 预览（View details / 行选中） | `TemplatePreviewPanel` 显示：previewId、status、comparison summary、下载按钮（若有） |
| 3 | 系统 | 渲染结构化对比 | 标题 **Structured preview comparison**；有差异时表格含 `locationType` / `locationRef` / severity / diffCode / summary |
| 4 | TESTER | 在保真警告区使用 **Warning code** 筛选 | 列表按 `warningCode` 过滤；无匹配时显示无匹配空态 |
| 5 | E2E/UIUX | 截取对比面板（含表或摘要 + 筛选控件） | Manifest 登记 PASS |

---

## 7. System Responses（成功路径）

对齐 P19 `PreviewComparisonService` + `TemplatePreviewPanel` + `FidelityWarningList`：

- 预览详情含 `previewComparison`：`totalDiffCount` / `blockerCount` / `warningCount` / `items[]`。
- 对比项：`locationType` ∈ {`PAGE`,`ANCHOR`,`SECTION`,`COMPONENT`}；`locationRef` 为锚点 id / 章节或组件摘要；`severity` ∈ {`WARNING`,`BLOCKER`}；`diffCode` + 非敏感 `summary`。
- 保真警告列表支持按 `warningCode`、location、artifact、viewed 筛选（本切片强制证明 **warningCode**）。
- 不展示模板变量原值、客户数据、完整请求体或完整生成正文。

---

## 8. Acceptance Scenarios（Given / When / Then）

### BDD-CDP-CMP-001 — 打开对比面板 + warningCode 筛选 + 位置定位

- **Given** Docker 栈就绪，TEMPLATE_TESTER（或等价）已登录，模板 Testing → Preview runs 存在至少一条 SUCCEEDED 预览（含 final-path 产物引用），且该预览可展示 `previewComparison` 与至少一条 `fidelityWarnings`  
- **When** 用户打开该预览详情，使对比面板可见，并在保真警告列表输入/选择一个已知 `warningCode` 进行筛选  
- **Then** 面板展示对比摘要（或「无对比项」空态与结构化标题并存的可识别对比区）  
- **And** 当存在对比 `items` 时，至少一行可见 `locationType` 与 `locationRef`（锚点/章节/页/组件级定位摘要）  
- **And** 保真警告列表按该 `warningCode` 过滤后仅保留匹配项（或匹配空态文案）  
- **And** 可观测证据：UI 文案/表格列 + `data-testid="fidelity-warning-list"` / `filter-warning-code`；UIUX 截图写入 `frontend/e2e/evidence/CDP-E2E-T09-uiux-manifest.md`（或本切片约定路径）

> 追溯：PRD「预览与最终 DOCX/PDF 并排对比」+ usability-review 已确认基线；P19-T05 Done。本场景证明 **浏览器可达**，不重测后端 classifier 全部分支。

---

## 9. Boundary / Exception（本切片范围）

| 情况 | 期望 | 归属 |
| --- | --- | --- |
| 无预览 / 未选中 | 面板 empty：引导先 Run preview | 可断言，不阻塞主场景 |
| 对比 items 为空 | 显示 no comparison items；摘要可为 clean | 允许；主场景优先用非空种子 |
| Authoring F7 side-by-side | 编辑态并排预览 | **Out of scope**（CORE-FORTRESS F7 Done） |
| T08 预览成功 + 下载 | 已 Done | 可复用 helper，不重复验收下载 |
| T10 保真「已查看」勾选 | Pass/Approve/Publish 确认 | **CD-E2E-T10** — 禁止本切片扩展 |
| 无权限用户 | fail-closed：不可见 Testing / 403 | 沿用权限矩阵；本切片不新开矩阵行 |
| 敏感明文 | 对比/警告仅非敏感摘要 | 回归：截图与断言不得依赖变量原值 |

### CD-PIT-08（本切片边界）

| 要点 | 本切片要求 |
| --- | --- |
| **Preview ≠ published legal authority** | 对比的是 final-path **预览记录** vs 同链路最终产物引用，用于测试/审批证据 |
| **Mitigation 落点** | 结构化对比表 + 保真 warningCode 筛选即可；不新增「已发布法律件」并排阅读器 |
| **文案** | 保持现有 i18n（Structured preview comparison）；不声称对比通过即等于可发布 |

---

## 10. Observable Evidence

| 类型 | 证据 |
| --- | --- |
| UI | Preview runs → `TemplatePreviewPanel`：comparison summary + structured table + `FidelityWarningList` 筛选 |
| Browser | Playwright 可见 `locationRef` / warning code 过滤结果 |
| Screenshot | CDP-E2E-T09 UIUX manifest（建议 REDBC @1920；GREENBC 可选） |
| Traceability | Plan T09 acceptance；ledger 由实现后 doc-sync 更新 |

**预期交付物（实现阶段，非本 BDD 写入）：**

- `frontend/e2e/CDP-E2E-T09-preview-comparison.spec.ts`（功能）
- `frontend/e2e/CDP-E2E-T09-uiux-evidence.spec.ts` + `frontend/e2e/evidence/CDP-E2E-T09-uiux-manifest.md`
- 复用 T08 / CDP golden / `template-testing-api` helpers 预置 SUCCEEDED 预览

---

## 11. Product vs E2E expectation

| 期望 | 说明 |
| --- | --- |
| **默认：E2E + UIUX only** | P19 已交付 `PreviewComparisonService`、对比表、`FidelityWarningList` warningCode 筛选；本切片补 **浏览器证据 + 截图** |
| **产品变更** | **不预期**。仅当 Docker 下无法打开面板、对比字段缺失、或筛选控件不可用时 escalate 为缺陷修复（非新需求） |
| **种子/数据** | 若默认 golden 预览 `previewComparison.items` 或 `fidelityWarnings` 为空，E2E setup 可用 API helper 预置或选用已知有警告的模板——属实现细节，不阻塞 BDD |

---

## 12. Out of scope（显式）

- CD-E2E-T08 下载成功路径重测、T10 fidelity viewed、T11 audit、T12 zh-CN dual-brand、T13 package materialize
- 新建像素级双栏 DOCX|PDF 阅读器或 PDF.js 同步滚动
- 对比表增加独立 warningCode 列筛选（v1 筛选落在保真警告列表）
- 重开 P19 classifier 全部分支单测矩阵（后端已有 `PreviewComparisonServiceTest`）

---

## 13. Traceability

| 来源 | 关系 |
| --- | --- |
| CD-E2E-T09 plan acceptance | Side-by-side view opens；filter by warningCode；screenshot evidence |
| PRD § 预览与最终产物并排对比 | 产品权威 |
| usability-review 已确认基线 | 并排对比 + warningCode 筛选列表 |
| P19-T05 | `PreviewComparisonService` + preview panel UI Done |
| CD-BDD-T07 | 本文件 |
| CD-E2E-T08 | 上游预览成功/产物；可复用，不扩展 |
| F7 / LR-C4 | Authoring side-by-side — 边界隔离 |

---

## 14. BDD 就绪声明

**bdd_readiness: ready**

- 场景 ID：`BDD-CDP-CMP-001` 可直接驱动 Playwright Red
- 已持久化本文件；`docs/README.md` behavior 索引已登记（ready）
- 无阻塞性 open questions（「并排」= P19 结构化对比面板，非 F7 authoring 布局；warningCode 筛选 = 同面板保真列表）
