# BDD：审计管理员查询与导出（Activity log smoke）

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CDP-AUDIT`  
**CDP:** CD-BDD-T08 → **CD-E2E-T11**  
**编写日期:** 2026-07-11（CD-E2E-T11 readiness；对齐 plan acceptance + 主线 AuditConsole）  
**程序:** [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md)  
**计划任务:** [CDP-e2e-full-chain-evidence.md](../plan/detail/CDP-e2e-full-chain-evidence.md) § CD-E2E-T11

---

## 1. 概述

在 Docker 验收栈浏览器（`:4173`）内证明：**AUDIT_ADMIN** 可在主线 **Activity log**（`/audit` · `AuditConsoleView`）上：

1. 按 **日期范围** 与/或 **事件类型** 筛选并查询 **management 或 lifecycle** 审计列表；
2. 点击 **Export** 经确认后触发 **JSON 下载**（非 403）；
3. 产出 **首份 audit 角色** UIUX manifest（含只读横幅 / 旅程 / 筛选与导出态截图）。

本文件是 CD-2 **浏览器证据** 规格（smoke），不重开审计治理深查、GROUP_ADMIN 范围校验全矩阵，也不依赖 `DGE-audit-governance` 工作树。

**与既有证据关系：**

| 既有 | 覆盖 | 本切片补充 |
| --- | --- | --- |
| `P21-T11-audit-journey.spec.ts` | 五步旅程、View only 横幅、无 My to-dos | **筛选 Apply + 导出下载** 行为证据 |
| permission-matrix § 审计读取/导出 | 角色与脱敏基线 | 浏览器路径可观测证明 |

---

## 2. Actor / Role

| Actor | 角色 / 账户 | 权限 |
| --- | --- | --- |
| **AUDIT_ADMIN** | 种子审计管理员（`10000004` · `E2E_AUDIT_ADMIN`） | `readAudit`；打开 Activity log；筛选；导出脱敏 JSON |

> E2E 须真实登录该角色，不得仅 API 断言列表/导出。

---

## 3. Goal（用户目标）

**作为** 审计管理员  
**我希望** 在 Activity log 中按日期/事件类型筛选管理或生命周期审计，并导出当前筛选范围的脱敏记录  
**以便** 完成只读审计查询与取证，且 CD-2 具备 audit 角色浏览器 + UIUX 证据。

---

## 4. Trigger（触发条件）

- Docker 验收栈健康（UI `:4173`，backend `:8080/healthz`）
- 用户登录后进入 **Activity log**（`/audit`，默认路由或导航）
- 在 **Management** 或 **Lifecycle** 页签上设置筛选并 Apply；或点击页头 **Export**

---

## 5. Preconditions（前置条件）

1. 用户已真实登录管理 UI（`AUDIT_ADMIN`；非角色模拟）。
2. 栈内存在至少可查询的审计数据（种子/既有事件即可；允许筛选后为空列表，但 Apply 须成功完成请求且 UI 非 403）。
3. Activity log 页可见：业务标题 **Activity log**（非「Audit console」L1）、只读提示、筛选表单（event type、eventAtFrom/To）、Export 按钮。
4. **测试本体内**须经 UI 操作筛选/导出；Setup 可用 API 造数，但不得用 API 替代 Apply/Export 点击。
5. 优先主线 `AuditConsoleView`；**禁止**引入或依赖 `DGE-audit-governance` 工作树产物。

---

## 6. Primary Journey（主路径）

| # | Actor | UI 动作 | 系统响应 |
| --- | --- | --- | --- |
| 1 | AUDIT_ADMIN | 登录 → 打开 `/audit` | Activity log 可见；View only 横幅/标签可见；无 My to-dos 行为组；五步旅程（若展示）可见 |
| 2 | AUDIT_ADMIN | 选择 Management **或** Lifecycle 页签 | 对应事件表加载（或空态）；无授权错误 |
| 3 | AUDIT_ADMIN | 设置 **event type** 与/或 **date from/to** → **Apply** | 列表按筛选刷新；请求成功（非 403/401） |
| 4 | AUDIT_ADMIN | 点击 **Export** → 确认对话框 Confirm | 触发 JSON 文件下载（`downloadJsonExport`）；成功 toast；非 403 |
| 5 | E2E/UIUX | 截取筛选态 + 导出相关帧 | 写入 `CDP-E2E-T11-uiux-manifest.md`（首份 audit 角色） |

---

## 7. System Responses（成功与边界）

对齐 permission-matrix 已确认审计读取/导出基线 + 主线控制台：

- **查询：** `AUDIT_ADMIN` 可读取 management / lifecycle 审计；筛选字段含 `eventType`、`eventAtFrom`、`eventAtTo`（ISO-8601）；Apply 触发列表刷新。
- **导出：** 成功返回脱敏 JSON（management：`management-audit-export-v1-json` 或现行等价；lifecycle 走对应导出 API）；浏览器侧 `downloadJsonExport` 触发下载；导出前有确认框（可取消则不下载）。
- **只读：** AUDIT_ADMIN 会话展示 View only；无协作「My to-dos」行为组。
- **脱敏：** 导出/列表不得暴露模板变量原值、凭证 secret、完整请求体等敏感明文。
- **Fail-closed（本 smoke 不强制单独场景，但不得违反）：** 无 `readAudit` 的角色不得进入可用审计控制台；导出不得对未授权主体返回 200 明文。

**Out of scope（本切片）：** GROUP_ADMIN 缺 `groupScope`/`templateId` 的 422 矩阵；审计治理深查 UI；zh-CN/双品牌（T12）；包物化（T13）。

---

## 8. Acceptance Scenarios（Given / When / Then）

### BDD-CDP-AUDIT-001 — 按日期/事件类型筛选查询

- **Given** Docker 栈就绪，`AUDIT_ADMIN`（`10000004`）已登录，打开 Activity log（`/audit`）  
- **When** 用户在 **Management 或 Lifecycle** 页签设置 **事件类型** 与/或 **日期起止** 筛选，并点击 **Apply**（或等价查询控件）  
- **Then** 结果列表更新（刷新完成；可为有数据或合法空态）  
- **And** 只读横幅/标签可见（View only — no actions / 等价 i18n）  
- **And** 导航中无 My to-dos 行为组  
- **And** 可观测证据：筛选请求非 403；表格或空态可见；可选 Network 中 audit query 200；UIUX 截图登记

> 追溯：CDP § CD-E2E-T11 acceptance「Filter by date/event type」；P21-T11 旅程/只读基线；matrix `readAudit`。

---

### BDD-CDP-AUDIT-002 — 导出触发下载

- **Given** Docker 栈就绪，`AUDIT_ADMIN` 已登录并停留在 Activity log（Management 或 Lifecycle 任一页签；可带或不带筛选）  
- **When** 用户点击 **Export**，在确认对话框中确认导出  
- **Then** 浏览器开始下载导出文件，**或** UI 明确显示导出成功且可取得脱敏 JSON 结果（非仅 toast 而无 payload）  
- **And** 响应非 403；无未授权错误阻断  
- **And** 可观测证据：Playwright download / 文件名断言，或成功路径 + payload shape；UIUX 可截取 Export 按钮/确认态

> 追溯：CDP § CD-E2E-T11 acceptance「export button triggers download」；matrix 审计导出权限（AUDIT_ADMIN 可导出全部审计记录）；主线 `handleExport` + `downloadJsonExport`。

---

## 9. Observable Evidence（证明方式）

| 证据 | 期望 |
| --- | --- |
| Playwright 功能 | `frontend/e2e/CDP-E2E-T11-audit-query.spec.ts`（新建；可复用 `P21-T11` 登录/栈 helper）覆盖 **BDD-CDP-AUDIT-001**、**BDD-CDP-AUDIT-002** |
| UIUX | `frontend/e2e/evidence/CDP-E2E-T11-uiux-manifest.md` + screenshots — **首份 audit 角色** Verdict **PASS** |
| 栈 | Docker `:4173` + `:8080/healthz`；`pnpm -C frontend test:e2e:docker`（或切片约定 workers） |
| 回归锚点 | 既有 `P21-T11-audit-journey.spec.ts` 仍绿（旅程/横幅/无 My to-dos） |

---

## 10. Traceability

| 来源 | 引用 |
| --- | --- |
| Plan | [CDP-e2e-full-chain-evidence.md](../plan/detail/CDP-e2e-full-chain-evidence.md) § CD-E2E-T11 |
| CDP program | [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md) CD-BDD-T08 |
| Permission | [permission-matrix.md](../security/permission-matrix.md) — `readAudit`；审计读取/导出接口与脱敏 |
| Prior E2E | `frontend/e2e/P21-T11-audit-journey.spec.ts`（IA/旅程；非筛选导出） |
| Product surface | `frontend/src/views/audit/AuditConsoleView.vue`（主线） |

**Task IDs:** `CD-E2E-T11`  
**Formal phase:** None（不改变）  
**Wave:** CD-2 保持 In Progress；本文件不将 T11/wave 标 Done

---

## 11. BDD readiness

- **bdd_readiness:** `ready`
- **Scenario IDs:** `BDD-CDP-AUDIT-001`, `BDD-CDP-AUDIT-002`
- **open_questions:** 无（management **或** lifecycle 任一页签即可满足 smoke；实现任选其一或两者各一条用例）
- **Next:** `plan-orchestrator` → `e2e-test-engineer`（+ `e2e-uiux-reviewer`）在 worktree 实现证据；勿改 plan Done
