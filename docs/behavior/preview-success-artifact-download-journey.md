# BDD：测试 Tab 预览成功 + 产物下载

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CDP-PREV`  
**CDP:** CD-BDD-T08 → **CD-E2E-T08**  
**编写日期:** 2026-07-10  
**程序:** [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md)  
**计划任务:** [CDP-e2e-full-chain-evidence.md](../plan/detail/CDP-e2e-full-chain-evidence.md) § CD-E2E-T08

---

## 1. 概述

关闭 P12 Template Testing Overhaul **T13 UIUX manifest 缺口**：「preview success frame not captured」（见 `frontend/e2e/evidence/P12-TEMPLATE-TESTING-OVERHAUL-uiux-manifest.md` Note：成功态 Download DOCX/PDF 对话框未截取，仅有失败+重试帧）。

本切片在浏览器内证明：

1. Template **Testing** Tab 单次「Run preview」经 SSE 到达 **成功完成** 态；
2. 成功对话框上的 **Download DOCX / Download PDF** 可触发有效下载；
3. UIUX manifest 含 **preview success** 截图（关闭 T13 缺口）。

产品行为以 P12 [template-testing-overhaul.md](./template-testing-overhaul.md) **SCEN-F1-01** 为权威；本文件是 CD-2 **浏览器证据** 规格，不重开 F1–F6 全矩阵。

---

## 2. Actor / Role

| Actor | 角色 / 账户 | 权限 |
| --- | --- | --- |
| **TEMPLATE_AUTHOR** | `10000004`（或具备 `canAuthorTemplates` 的等价种子用户） | 打开模板 Testing Tab；运行预览；下载临时产物 |

---

## 3. Goal（用户目标）

**作为** 模板作者  
**我希望** 在 Testing Tab 看到预览生成成功，并下载 final-path DOCX/PDF 临时产物  
**以便** 用真实渲染链产物做测试判断，且 E2E/UIUX 证据可证明成功态（非仅失败帧）。

---

## 4. Trigger（触发条件）

- Docker 验收栈健康（UI `:4173`，backend `:8080/healthz`）
- 作者在模板详情 **Template testing** workspace tab → **Test data sets** 子 Tab
- 点击某测试数据集行的 **Run preview**

---

## 5. Preconditions（前置条件）

1. 用户已真实登录管理 UI（非角色模拟）。
2. 模板处于 `DRAFT` 或 `TESTING`（含 in-flight 开发版本），至少 1 个可成功渲染的测试数据集。
3. 系统级并发预览任务数 &lt; 3（避免 SCEN-F1-02 429）。
4. Setup/teardown 可用 API helper（如 `frontend/e2e/helpers/template-testing-api.ts` / CDP golden fixture）；**测试本体内**不得用 API 跳过「Run preview → 成功对话框」UI 步骤。

**种子建议（实现可选，不阻塞 BDD）：** 优先使用已知可成功渲染的数据集（避免 P12 UIUX 当时 FOL 预览在窗口内失败导致只拍到 error 帧）。可复用 CDP MVP / 既有 template-testing helpers。

---

## 6. Primary Journey（主路径）

| # | Actor | UI 动作 | 系统响应 |
| --- | --- | --- | --- |
| 1 | AUTHOR | 打开模板 → **Template testing** tab → Test data sets | 数据集列表可见；行内 **Run preview** 可用 |
| 2 | AUTHOR | 点击目标行 **Run preview** | 弹出 Generating preview 进度对话框；SSE 进度更新 |
| 3 | 系统 | 渲染链完成 DOCX + PDF | 对话框进入 **success**；展示 **Download DOCX**、**Download PDF**；显示 TTL 倒计时（expires in…） |
| 4 | AUTHOR | 点击 **Download DOCX**（及可选 PDF） | 浏览器触发 download（或打开 artifact URL）；文件可取得 |
| 5 | E2E/UIUX | 在 success 态截图并写入 manifest | 关闭 T13「preview success frame not captured」缺口 |

---

## 7. System Responses（成功路径）

对齐 P12 SCEN-F1-01 / `PreviewProgressDialog`：

- `POST …/previews/async-preview`（或现行等价）接受请求后建立 SSE progress 流。
- 完成事件携带 `docxDownloadUrl`、`pdfDownloadUrl`、`expiresAt`（临时产物，默认 24h TTL）。
- 对话框 `phase === 'success'`：两个下载按钮可见；expiry 文案可见。
- 下载走授权 artifact URL；未过期时返回产物字节（非 410）。

---

## 8. Acceptance Scenarios（Given / When / Then）

### BDD-CDP-PREV-001 — 预览成功完成 UI

- **Given** Docker 栈就绪，作者已登录，模板 Testing Tab 有可成功渲染的数据集，并发预览 &lt; 3  
- **When** 作者点击该行 **Run preview** 并等待 SSE 到达终态  
- **Then** 进度对话框进入成功态（非 error/retry）  
- **And** **Download DOCX** 与 **Download PDF** 按钮可见  
- **And** 可见临时有效期提示（`expires in` / 等价 i18n）  
- **And** 可观测证据：对话框 success 区域 +（可选）Network 中 completed SSE

> 追溯：P12 **SCEN-F1-01**（产品行为）；本场景要求 **浏览器成功态**，不得以失败终态冒充通过。

### BDD-CDP-PREV-002 — 产物下载可用

- **Given** BDD-CDP-PREV-001 成功对话框已展示  
- **When** 作者点击 **Download DOCX**（建议再点 **Download PDF** 或至少断言 PDF 按钮可点/href 有效）  
- **Then** Playwright `download` 事件触发，或导航到 artifact URL 且响应成功（非 401/403/410）  
- **And** 建议文件名或 Content-Type 表明为 DOCX（及 PDF，若测）  
- **And** 可观测证据：download 事件 / 响应 status + 非空 body

### BDD-CDP-PREV-003 — UIUX 关闭 T13 preview-success 缺口

- **Given** BDD-CDP-PREV-001 成功对话框可见  
- **When** UIUX evidence spec 在 success 态截取帧并登记 manifest  
- **Then** `frontend/e2e/evidence/CDP-E2E-T08-uiux-manifest.md`（或本切片约定路径）含至少 1 张 **preview success** 截图（含 Download DOCX/PDF）  
- **And** Manifest 明确标注关闭 P12 T13 gap「preview success frame not captured」  
- **And** Verdict 行存在；建议含 REDBC（GREENBC 可选，不阻塞 T08）

---

## 9. Boundary / Exception（本切片范围）

| 情况 | 期望 | 归属 |
| --- | --- | --- |
| 预览失败 + Retry | 已有 P12 SCEN-F1-03 / P12 UIUX frame 05 | **Out of scope**（不重复） |
| 并发 429 | SCEN-F1-02 | Out of scope |
| 过期 410 | SCEN-F1-04 | Out of scope |
| 并排对比 / warningCode 筛选 | [preview-comparison-journey.md](./preview-comparison-journey.md) | **CD-E2E-T09** — 禁止本切片扩展 |
| Pass/Approve/Publish 保真「已查看」勾选 | CD-E2E-T10 | Out of scope |
| Authoring 侧 edit-preview 非权威 banner | F7 BDD-F7-B2-005 | 已 Done；本切片不改 authoring pane |
| 无 `canAuthorTemplates` | fail-closed：不可见 Run preview / 403 | 沿用既有授权；本切片不新开权限矩阵 |

### CD-PIT-08（本切片边界）

| 要点 | 本切片要求 |
| --- | --- |
| **Preview ≠ final artifact authority** | Testing Tab 成功下载证明的是 **final rendering chain 临时预览产物**，用于测试证据；**不是**已发布 release 的法律权威件 |
| **Mitigation 落点** | 成功态必须提供 **final-path Download DOCX/PDF** 证据（BDD-CDP-PREV-001/002）；不在本切片新增 edit-preview banner |
| **文案** | 不声称「下载即等于已发布法律效力」；若 UI 已有 TTL/临时提示则保持；**不**把 T09 对比面板或 T10 勾选并入本切片 |

---

## 10. Observable Evidence

| 类型 | 证据 |
| --- | --- |
| UI | Generating preview 对话框 → success + Download DOCX/PDF + expires copy |
| Browser | Playwright download 事件或 artifact GET 成功 |
| Screenshot | CDP-E2E-T08 UIUX manifest 含 preview success 帧 |
| Traceability | 关闭 P12 T13 manifest Note 所述缺口；ledger/plan 在实现后由 doc-sync 更新 |

**预期交付物（实现阶段，非本 BDD 写入）：**

- `frontend/e2e/CDP-E2E-T08-preview-success-download.spec.ts`（功能）
- `frontend/e2e/CDP-E2E-T08-uiux-evidence.spec.ts` + `frontend/e2e/evidence/CDP-E2E-T08-uiux-manifest.md`
- 复用 `previewProgressDialog` / `runPreviewFromFirstDataSetRow` / `waitForPreviewDialogSuccess`（`frontend/e2e/helpers/template-testing-api.ts`）

---

## 11. Product vs E2E expectation

| 期望 | 说明 |
| --- | --- |
| **默认：E2E + UIUX only** | 产品已具备 SSE 成功态与下载按钮（P12 / `PreviewProgressDialog`）；本切片补 **浏览器成功路径 + 下载 + 成功帧截图** |
| **产品变更** | **不预期**。仅当 Docker 下无法稳定到达 success（种子/渲染回归）时 escalate；修复属缺陷，非本 BDD 新需求 |

---

## 12. Out of scope（显式）

- CD-E2E-T07 API policy、T09 comparison、T10 fidelity viewed、T11 audit、T12 zh-CN dual-brand、T13 package materialize 全矩阵
- audit-governance 程序
- 重跑/重写 P12 F2–F6 全量场景（可引用，不复制）

---

## 13. Traceability

| 来源 | 关系 |
| --- | --- |
| CD-E2E-T08 plan acceptance | Test tab preview complete；download works；screenshot in manifest |
| P12 SCEN-F1-01 | 产品成功路径权威 |
| P12 T13 UIUX Note | 缺口来源（success frame 未捕获） |
| CD-PIT-08 | final-path download 证据；非权威边界 |
| MVP golden §6 step 3 | 相关但更广；T08 专证 preview success + download |
| CD-E2E-T09 | 对比面板 — 不扩展 |

---

## 14. BDD 就绪声明

**bdd_readiness: ready**

- 场景 ID：`BDD-CDP-PREV-001` / `002` / `003` 可直接驱动 TDD Red（Playwright）
- 已持久化本文件；`docs/README.md` behavior 索引已登记
- 无阻塞性 open questions（种子选择为实现细节，不阻塞规格）
