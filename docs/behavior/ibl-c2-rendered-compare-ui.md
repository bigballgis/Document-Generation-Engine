# BDD 行为规格：IBL-C2 — Side-by-side rendered output compare UI（F18）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-C2` |
| **编写日期** | 2026-07-19 |
| **程序 / 队列** | IBL Wave C · **IBL-C2** / F18 |
| **Slice** | `ibl-c2-rendered-compare-ui` |
| **Branch** | `feat/ibl-c2-rendered-compare-ui` |
| **Worktree** | `D:/working/DGE-ibl-c2-rendered-compare-ui` |
| **Placement** | **ISOLATED** |
| **Task Master** | **#121** IBL-C2 — Batch Recommendation **solo**；`member_task_ids: ["121"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明正式 P-phase；不宣称 go-live） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-c2-rendered-compare-ui`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F18 / IBL-C2 acceptance；边界对照 [preview-comparison-journey.md](./preview-comparison-journey.md)（结构化警告对比 ≠ 本叶）、[core-fortress-f7-authoring-ux.md](./core-fortress-f7-authoring-ux.md)（Authoring 编辑态并排 ≠ 本叶） |
| **Frontend UI** | **`frontend_ui_in_scope=true`** — Playwright **functional + UIUX mandatory** on Docker `:4173` |
| **E2E** | **mandatory** |

**完成声明约束：** 本叶关闭 **F18**——管理 UI 可并排展示两个 **已渲染产物（PDF 页）**，而非仅语义/警告 diff。**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 Wave IBL-C / IBL 程序 Done；**禁止**并入 **IBL-C3** / **IBL-B7**；**禁止**引入 pixel golden / Word host 基线。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["121"]
  proposed_slice_id: ibl-c2-rendered-compare-ui
  shared_acceptance_surface: >
    management UI side-by-side rendered artifact compare journey
    + Playwright E2E on :4173
  vetoes_applied:
    - IBL-C3-locale-golden
    - IBL-B7-Word-blocked
    - umbrella-106-registry-only
    - different-risk-domain-vs-C3
  evidence_amortization: >
    pnpm lint/type-check/test/build + docker-deploy-queue
    + Playwright functional+UIUX
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| Testing workspace 选择两条 SUCCEEDED 预览运行，并排展示两个 **PDF 渲染页** | **IBL-C3** 跨 locale golden / LO 标签诚实化 |
| 复用既有 `downloadPreviewArtifact(..., 'pdf')` + `InlinePdfPreviewViewer`（pdf.js canvas） | 新建后端「双产物 diff API」/ pixel hash / Word COM |
| English-first i18n 文案 + `data-testid` 可测 | 宣称 **#3b GO** / **#5a GO** / go-live |
| Playwright functional + UIUX on Docker 4173 | Authoring F7 编辑态并排、发布版本语义对比、P19 结构化 warning 表冒充本叶 Done |
| Gates：`pnpm -C frontend lint && type-check && test && build` + E2E | Wave C Done / IBL program Done / C1 重开 / B7 Word |

---

## 1. 概述

### 1.1 问题（F18）

| 现状能力 | 为何 **不足以** 关闭 F18 |
| --- | --- |
| `PreviewComparisonService` + `TemplatePreviewPanel` 结构化对比表 / 保真警告 | 展示的是 **warning/blocker 摘要**，不是两个渲染件的视觉并排 |
| `ChangeDiffService` / `SemanticContentDiffEngine` + `TemplateChangeDiffPanel` | **语义**变更维度，不是渲染页 |
| Release version Compare 对话框 | 发布版本间 **语义** diff |
| `TemplatePreviewPanel` 单路 `InlinePdfPreviewViewer` | 一次只看 **一条** 预览的 PDF |
| CORE-FORTRESS F7 / LR-C4 Authoring side-by-side | 编辑器 vs edit-preview，**不是**两条渲染运行对比 |

**F18 缺口：** 管理 UI 无法让作者/测试员在浏览器内 **并排看见两个已渲染输出**（尤其 PDF 页），从而肉眼比对版式/内容差异。

### 1.2 「Rendered artifacts」定义（本叶确认）

| 术语 | 本叶含义 |
| --- | --- |
| **Rendered artifact（主对比面）** | 预览运行成功后的 **PDF 产物**，经既有管理 API `GET /api/management/v1/templates/{templateId}/previews/{previewId}/artifacts/pdf` 拉取 blob，由既有 **`InlinePdfPreviewViewer`（pdf.js → canvas 页）** 在左/右栏各渲染一页视图 |
| **辅佐（非并排主面）** | 每侧可提供 **Download DOCX / Download PDF**（复用既有 artifact 下载）；**不要求**新建 DOCX 内嵌阅读器或 iframe Word |
| **明确不是** | 像素黄金比对、图像差分热力图、同步滚动强制、Word 桌面宿主截图、语义/警告表单独充当「已对比」 |

> **实现偏好：** 前端组合既有 preview run 列表 + 双路 `useInlinePdfPreview` / `InlinePdfPreviewViewer` + 既有 artifact 下载。**默认不新增后端 API**；仅当双路下载在权限/TTL 上现有契约无法支撑时，才 escalate（本 BDD 不预置新 API）。

### 1.3 产品落点（v1）

| 区域 | 落点 |
| --- | --- |
| 入口 | 模板详情 → **Template testing** workspace → **Preview runs** 子 Tab |
| 选择 | 用户选择 **恰好两条** `status === SUCCEEDED` 且 `pdfAvailable`（或等价：有 PDF artifact）的预览运行 |
| 动作 | **Compare rendered outputs**（English-first i18n key；中文 locale 可后补对称文案） |
| 展示 | Dialog 或 Testing Tab 内专用面板：左栏 Run A PDF 预览 + 右栏 Run B PDF 预览，同屏并排可见 |
| 元数据 | 每侧展示可识别身份：`previewId`（必选）+ 可选 `createdAt` / test data set 标签 |
| 可选增强（非验收门槛） | 每侧独立翻页；「同步页码」开关；DOCX 下载按钮 |

---

## 2. Actor / Role

| Actor | 角色 / 账户 | 权限 |
| --- | --- | --- |
| **TEMPLATE_AUTHOR** | 具备模板 Testing workspace 访问与预览产物下载能力的种子用户（主路径） | 打开 Testing → Preview runs；下载/内嵌 PDF artifact；打开并排对比 |
| **TEMPLATE_TESTER** | 等价 Testing 能力用户（可选第二证据角色） | 同上；不强制双角色双 spec |
| **未授权用户** | 无 Testing / 无预览产物读权限 | fail-closed：不可见入口或 403（沿用既有矩阵，本叶不新开 capability） |

> E2E 主路径优先 **TEMPLATE_AUTHOR**（与 CD-E2E-T08 Preview runs / 下载动线一致）。

---

## 3. Goal（用户目标）

**作为** 模板作者或测试员  
**我希望** 在 Testing → Preview runs 中选择两条成功的预览运行，并排查看它们的 **PDF 渲染页**  
**以便** 肉眼比对两次渲染结果的版式与内容差异，而不仅依赖语义 diff 或保真警告列表。

---

## 4. Trigger（触发条件）

- Docker 验收栈健康（UI `:4173`，backend `:8080/healthz`）
- 用户打开模板详情 → **Template testing** → **Preview runs**
- 用户选中恰好两条可对比的 SUCCEEDED 预览运行，并触发 **Compare rendered outputs**

---

## 5. Preconditions（前置条件）

1. 用户已真实登录管理 UI（非角色模拟）。
2. 模板 Testing → Preview runs 至少存在 **两条** `SUCCEEDED` 预览，且均可下载 PDF（`pdfAvailable` / `pdfArtifactStorageKey` 非空，或等价契约）。
3. Artifact 未过期（或 E2E setup 可重新生成两条成功预览）。
4. Setup 可用 API helper 预置预览；**测试本体内**须经 UI 完成选择 → 打开并排对比（不得仅 API 断言）。
5. 既有单路 inline PDF / 结构化对比 / ChangeDiff 可继续存在；本叶验收 **不得**仅断言那些面板。

---

## 6. Primary Journey（主路径）

| # | Actor | UI 动作 | 系统响应 |
| --- | --- | --- | --- |
| 1 | AUTHOR | 打开模板 → **Template testing** → **Preview runs** | 预览运行历史可见 |
| 2 | AUTHOR | 选择恰好两条 SUCCEEDED（且 PDF 可用）的运行 | Compare 动作变为可用；不足两条时 disabled + English-first hint |
| 3 | AUTHOR | 点击 **Compare rendered outputs** | 打开并排对比面（dialog/panel） |
| 4 | 系统 | 左/右栏各加载对应 preview 的 PDF blob 并渲染页 | 同屏可见两个 `InlinePdfPreviewViewer`（或等价 canvas 预览）；每侧带 `previewId` |
| 5 | AUTHOR（可选） | 在一侧翻页 / 下载 DOCX|PDF | 该侧独立响应；另一侧不强制同步（v1） |
| 6 | E2E/UIUX | 截取并排双 PDF 帧 | Manifest 登记 PASS（REDBC @1920 建议；GREENBC 可选） |

---

## 7. System Responses（成功路径）

- Compare 打开后，UI 存在可测的并排容器（建议 `data-testid="rendered-compare-panel"`）。
- 左栏与右栏各有可测 PDF 预览区（建议 `data-testid="rendered-compare-pane-a"` / `rendered-compare-pane-b"`），均展示至少一页已渲染 canvas（或等价可见 PDF 页），而非空态/仅摘要文字。
- 每侧元数据含对应 `previewId`，便于区分 A/B。
- 文案 English-first（`en` 为 base）；中文 locale 对称键允许后补，但不得硬编码仅中文。
- 加载中显示 loading；单侧失败显示该侧错误态，不静默空白冒充成功。
- 不展示模板变量原值、客户明文、完整请求体。
- **不**将「结构化 preview comparison 表」或「ChangeDiff 面板」单独计为本叶成功证据。

---

## 8. Acceptance Scenarios（Given / When / Then）

### BDD-IBL-C2-001 — 并排展示两个渲染 PDF

- **Given** Docker 栈就绪，TEMPLATE_AUTHOR（或等价）已登录，模板 Preview runs 存在至少两条 SUCCEEDED 且 PDF 可用的预览运行  
- **When** 用户恰好选择这两条运行并触发 **Compare rendered outputs**  
- **Then** 并排对比面打开，左栏与右栏均可见 PDF 页渲染（canvas/等价），且同屏并排  
- **And** 两侧分别标识不同的 `previewId`  
- **And** 可观测证据：`data-testid` 容器/双栏 + Playwright 可见性断言

### BDD-IBL-C2-002 — 不得仅用语义/警告 diff 冒充关闭 F18

- **Given** 同 BDD-IBL-C2-001 的前置，且页面上仍可能存在 ChangeDiff / Structured preview comparison / 保真警告  
- **When** 用户完成并排渲染对比打开  
- **Then** 验收断言 **必须**包含双 PDF 预览栏可见  
- **And** **禁止**仅断言 `TemplateChangeDiffPanel`、结构化 comparison 表、或 fidelity warning 列表作为本叶 Done 证据

### BDD-IBL-C2-003 — 选择约束（恰好两条可对比运行）

- **Given** Preview runs 列表已加载  
- **When** 已选 0 / 1 / >2 条，或所选运行非 SUCCEEDED / 无 PDF  
- **Then** **Compare rendered outputs** 不可用（disabled）或点击后显示 English-first 引导（恰好两条 SUCCEEDED + PDF）  
- **And** 当恰好两条合格运行被选中时，动作可用

### BDD-IBL-C2-004 — 单侧产物失败可观测

- **Given** 用户已打开并排对比，其中一侧 PDF artifact 不可用或下载失败（过期/404/网络）  
- **When** 该侧加载结束  
- **Then** 该侧显示明确错误/空态文案（English-first），另一侧若成功仍可显示  
- **And** 不静默两侧皆空却显示「对比成功」

### BDD-IBL-C2-005 — 授权 fail-closed

- **Given** 用户无模板 Testing / 预览产物读权限  
- **When** 尝试打开 Testing Preview runs 或触发对比 / 拉取 artifact  
- **Then** 入口不可见或 API/UI fail-closed（403 / 既有错误信封）  
- **And** 本叶不新增 permission-matrix 行；沿用既有预览产物授权

### BDD-IBL-C2-006 — i18n English-first

- **Given** UI locale 为 `en`（默认）  
- **When** 并排对比面打开  
- **Then** 主标题/动作/hint 使用英文 base catalog（非硬编码中文）  
- **And** 新增键同步 `en`（及项目惯例下的 `zh-CN` 对称，若本叶触及）

### BDD-IBL-C2-007 — Playwright functional + UIUX on Docker 4173

- **Given** `docker-deploy-queue` 验收栈健康（UI `:4173`）  
- **When** E2E functional spec 走完 BDD-IBL-C2-001 主路径，且 UIUX evidence spec 截取并排双 PDF 帧  
- **Then** functional 断言 PASS；UIUX manifest 含至少 1 张 **side-by-side rendered compare** 截图（建议 REDBC @1920）  
- **And** Verdict 可追溯到本文件场景 ID

---

## 9. Boundary / Exception

| 情况 | 期望 | 归属 |
| --- | --- | --- |
| 仅一条 SUCCEEDED | Compare disabled + hint | BDD-IBL-C2-003 |
| SUCCEEDED 但无 PDF（仅 DOCX） | 不可作为对比侧；hint 说明需要 PDF | BDD-IBL-C2-003 |
| Artifact 过期 | 该侧错误态；可提示重新 Run preview | BDD-IBL-C2-004 |
| 结构化 warning / ChangeDiff | 可并存，**不**替代本叶 | BDD-IBL-C2-002 |
| Authoring F7 side-by-side | 编辑态并排 | **Out of scope**（已 Done） |
| CD-E2E-T09 preview comparison | 结构化表 + warningCode 筛选 | **Out of scope**（已有证据；非 F18 视觉并排） |
| 像素差分 / golden PIXEL_* | 禁止 | **IBL-C1 / PD-2**；本叶不做 |
| Word host 对比 | 禁止 | **IBL-B7** Blocked |
| 跨 locale golden 矩阵 | 禁止 | **IBL-C3** |
| 窄视口 | 允许纵向堆叠双栏，但仍须「两份渲染页都可见」；UIUX 可记 note | 实现细节；functional 以桌面宽屏为主 |

---

## 10. Observable Evidence

| 类型 | 证据 |
| --- | --- |
| UI | Preview runs → Compare → 双栏 PDF canvas + previewId |
| Browser | Playwright：双栏 `data-testid` 可见；非仅 semantic 面板 |
| Screenshot | `frontend/e2e/evidence/IBL-C2-*-uiux-manifest.md`（或本切片约定路径）含 side-by-side 帧 |
| Gates | `pnpm -C frontend lint && type-check && test && build` + E2E functional/UIUX GREEN |
| Traceability | Task **#121**；IBL program F18 / IBL-C2 acceptance；ledger 由 stage 12 doc-sync 更新 |

**预期交付物（实现阶段，非本 BDD 写入）：**

- FE：Preview runs 双选 + Compare 动作 + 并排面板（复用 `InlinePdfPreviewViewer`）
- `frontend/e2e/IBL-C2-*-rendered-compare.spec.ts`（functional）
- `frontend/e2e/IBL-C2-*-uiux-evidence.spec.ts` + UIUX manifest
- i18n keys（English-first）

---

## 11. Confirmed vs non-goals

### 11.1 已确认（可驱动 TDD Red）

1. 对比对象 = 两条 **preview run** 的 **PDF 渲染页**（视觉并排）。  
2. 组合既有 artifact 下载 + inline PDF viewer；默认无新后端 API。  
3. `frontend_ui_in_scope=true`；E2E functional + UIUX **mandatory**。  
4. 语义/警告/ChangeDiff **不足以**关闭 F18。  
5. Formal phase **None**；不翻转 #3b/#5a；不宣称 Wave C / IBL Done。

### 11.2 显式 OUT

- Pixel golden / `PIXEL_*` / 图像热力差分  
- Word 宿主 / Path E（IBL-B7）  
- IBL-C3 跨 locale golden / SYNTHETIC→LIBREOFFICE  
- 新建双产物后端 diff 服务（除非实现期证明既有下载契约不够——需 escalate，不在本叶默认范围）  
- 内嵌 DOCX/Word iframe 阅读器  
- Authoring F7 / Release semantic compare / P19 structured comparison 重测冒充 Done  
- Checklist **#3b GO** / **#5a GO**；go-live；Wave IBL-C Done；IBL program Done  

### 11.3 非阻塞实现选择（不挡 ready）

| 问题 | 本叶裁定 |
| --- | --- |
| Dialog vs 内嵌 panel？ | 实现任选；须满足同屏双 PDF + testid |
| 是否强制同步翻页？ | **否**（可选增强） |
| 是否要求两侧 DOCX 下载按钮？ | **建议有**；functional 主断言仍是双 PDF 可见 |
| 两条运行是否必须不同 dataset？ | **否**；允许同 dataset 两次运行 |

---

## 12. Traceability

| 来源 | 关系 |
| --- | --- |
| Task Master **#121** | Owning leaf IBL-C2 |
| IBL program **F18** / **IBL-C2** acceptance | Gap + Done criteria |
| [preview-comparison-journey.md](./preview-comparison-journey.md) | 结构化警告对比 — **边界隔离**（不足关闭 F18） |
| [core-fortress-f7-authoring-ux.md](./core-fortress-f7-authoring-ux.md) | Authoring 并排 — **边界隔离** |
| [preview-success-artifact-download-journey.md](./preview-success-artifact-download-journey.md) | 可复用 SUCCEEDED 预览 / artifact 下载 helper |
| Existing FE | `InlinePdfPreviewViewer` / `useInlinePdfPreview` / `downloadPreviewArtifact` / Preview runs panel |
| Batch / queue | Solo leaf；veto C3 / B7 / #106 |

---

## 13. BDD 就绪声明

**bdd_readiness: ready**

```text
bdd_readiness: ready
owning_doc: docs/behavior/ibl-c2-rendered-compare-ui.md
task_ids: ["121"]
frontend_ui_in_scope: true
e2e_mandatory: true
scenario_ids:
  - BDD-IBL-C2-001
  - BDD-IBL-C2-002
  - BDD-IBL-C2-003
  - BDD-IBL-C2-004
  - BDD-IBL-C2-005
  - BDD-IBL-C2-006
  - BDD-IBL-C2-007
open_questions: []
```

- 场景可直接驱动 Vitest（组件）+ Playwright Red  
- 已持久化本文件；`docs/README.md` behavior 索引须登记本叶（同变更集）  
- 无阻塞性 open questions（「rendered」= 双 PDF 内嵌页；FE 组合既有 API）
