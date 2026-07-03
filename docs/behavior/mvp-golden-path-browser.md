# BDD 行为规格：浏览器 MVP 黄金路径（MVP Golden Path — Browser Only）

**文件状态:** `ready`  
**版本:** 1.0.0  
**编写日期:** 2026-07-04  
**BDD ID 前缀:** `BDD-CDP-MVP-001`  
**交付切片:** CD-BDD-T01 → CD-E2E-T01 / CD-E2E-T01b  
**程序:** [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md)

---

## 1. 概述

本规格定义**唯一**浏览器内完成的 MVP 垂直切片黄金路径，用于证明平台核心竞争力闭环可在管理 UI 中操作完成，**不得**使用 API helper 推进测试/审批/发布状态（setup/teardown 除外）。

**目标链**（与 [master-plan.md](../plan/master-plan.md) § Thin vertical slice 一致）：

```text
Login → approve master → configure template content → test generate (final path)
  → pass test (structured form) → submit for approval → approve (structured form)
  → publish (secondary confirm) → configure minimal API policy → observe runtime success
```

---

## 2. Actor / Role

| 步骤 | 角色 | 测试账户（八位工号） |
| --- | --- | --- |
| 母版审批 | GROUP_ADMIN 或 GLOBAL_ADMIN | `10000002` / `10000001` |
| 模板编排 | TEMPLATE_AUTHOR | `10000004` |
| 测试判定 | TEMPLATE_TESTER | `10000005` |
| 审批判定 | TEMPLATE_APPROVER | `10000006` |
| 发布确认 | GROUP_ADMIN | `10000002` |
| API 策略 | GROUP_ADMIN | `10000002` |
| 运行时验证 | API 调用方凭证（HTTP）或管理端调用摘要 | 见 P12-API 种子 |

---

## 3. Goal（用户目标）

**作为** 银行模板治理团队  
**我希望** 在浏览器中完成从母版到已发布 API 可调用模板的完整路径  
**以便** 向业务与集成方证明平台「敢发布、能调用」，且不依赖隐藏 API 脚本。

---

## 4. Trigger（触发条件）

- Docker 部署栈健康（`:4173` UI，`:8080/healthz`）
- 种子数据包含模板代码 **`CDP-MVP-GOLDEN`**（DRAFT 态，最小绑定 + 1 测试数据集）及可审批母版 **`CDP-MVP-MASTER`**

---

## 5. Preconditions（前置条件）

1. 用户已登录管理 UI（真实登录，非角色模拟）。
2. `CDP-MVP-MASTER` 处于 **待审核** 或 **草稿**（由 seeder 固定，见 CD-E2E-T01b）。
3. `CDP-MVP-GOLDEN` 绑定已批准母版锚点，至少 1 个变量 + 1 段结构化段落。
4. 测试数据集 `CDP-MVP-DATASET-01` 满足提交测试门禁（覆盖率/必选样例 — seeder 保证）。

---

## 6. Primary Journey（主路径）

| # | Actor | UI 动作 | 系统响应 |
| --- | --- | --- | --- |
| 1 | GROUP_ADMIN | 打开母版 hub → 选择 `CDP-MVP-MASTER` → 审批通过 | 母版状态 **审核通过**；审计记录 |
| 2 | TEMPLATE_AUTHOR | 打开模板 dev workspace → 确认绑定/变量 | 绑定校验无阻断项 |
| 3 | TEMPLATE_AUTHOR | 测试 tab → 发起最终路径预览/测试生成 → 等待完成 | SSE/进度完成；可下载 DOCX/PDF |
| 4 | TEMPLATE_TESTER | Dashboard TEST 队列 → Open → Pass test 结构化表单 → 确认 | 模板进入可提交审批状态；TEST 队列项消失 |
| 5 | TEMPLATE_AUTHOR | 提交审批 → 证据清单 → 确认 | `PENDING_DECISION` / APPROVAL 队列 |
| 6 | TEMPLATE_APPROVER | APPROVAL 队列 → Approve 结构化表单 → 确认保真摘要 | 进入 **待发布** |
| 7 | GROUP_ADMIN | 发布摘要 → 二次确认 → Go-live | 发布版本存在且 callable |
| 8 | GROUP_ADMIN | API 管理 → 至少 1 配置域保存（含影响预览） | policyVersion 更新 |
| 9 | （可选 UI 或 API） | 触发同步生成 / 查看最近调用 | 200 + 产物或调用记录 |

---

## 7. Acceptance Scenarios（Given/When/Then）

### BDD-CDP-MVP-001 — 全链浏览器完成

- **Given** Docker 栈就绪且 `CDP-MVP-GOLDEN` 种子存在  
- **When** 按 §6 步骤 1–8 在浏览器中依次操作  
- **Then** 发布版本在模板详情/External access 显示可调用  
- **And** Dashboard 各队列在步骤完成后正确增减  
- **And** 全程无 API lifecycle helper 推进测试/审批/发布状态

### BDD-CDP-MVP-002 — 提交审批门禁阻断

- **Given** 模板缺少必选测试证据（seeder 变体 `CDP-MVP-GOLDEN-BLOCKED`）  
- **When** AUTHOR 点击提交审批  
- **Then** 清单显示阻断项且无法确认提交

### BDD-CDP-MVP-003 — 发布二次确认

- **Given** 模板处于待发布  
- **When** GROUP_ADMIN 打开发布对话框但未确认摘要  
- **Then** 发布按钮不可用或关闭无状态变更

---

## 8. 边界与异常

- LibreOffice 预览超时：显示可重试；E2E 允许 ≤240s 等待（与 P12-T13 一致）。
- 并发预览 429：不在本 spec 范围（见 template-testing-overhaul）。
- 跨组模板：403 统一无权反馈（见 usability-review）。

---

## 9. 可观测证据

| 证据 | 路径 |
| --- | --- |
| Playwright spec | `frontend/e2e/CDP-E2E-T01-mvp-golden-path.spec.ts` |
| UIUX manifest | `frontend/e2e/evidence/CDP-E2E-T01-uiux-manifest.md` |
| 截图 | ≥8 张，1440×900，REDBC + GREENBC |
| Gate | Docker deploy + Playwright green |

---

## 10. 可追溯性

| 来源 | 章节 |
| --- | --- |
| PRD | §6 核心能力；§7 生命周期 |
| master-plan | Thin vertical slice |
| usability-review | §已确认体验基线 L32–52 |
| P12-AUD-B10 | 提交审批门禁 |
| demo-expansion | 渲染保真（步骤 3 最终路径产物） |

---

## 11. BDD 就绪声明

- [x] Actor / Goal / Trigger / Preconditions 已确认  
- [x] Primary journey 无 API 推进歧义  
- [x] Acceptance scenarios ≥3  
- [x] 证据路径已命名  
- **Status:** `ready` — `e2e-test-engineer` 可实施 CD-E2E-T01
