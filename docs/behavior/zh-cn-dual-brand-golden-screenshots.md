# BDD：zh-CN 语言 + 双品牌黄金截图（CD-E2E-T12）

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CDP-I18N`  
**CDP:** CD-2 → **CD-E2E-T12**  
**编写日期:** 2026-07-11（CD-E2E-T12 readiness；对齐 plan acceptance + T01 UIUX 基线）  
**程序:** [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md)  
**计划任务:** [CDP-e2e-full-chain-evidence.md](../plan/detail/CDP-e2e-full-chain-evidence.md) § CD-E2E-T12

---

## 1. 概述

在 Docker 验收栈浏览器（`:4173`）内证明：管理 UI 在 **黄金路径关键表面** 上可：

1. 经壳层 **locale switcher** 切换到 **zh-CN**，并在 **≥3 个关键黄金表面** 上呈现中文 UI（非整页英文回退）；
2. 经壳层 **brand switcher** 在 **REDBC** 与 **GREENBC** 间切换，并在 **1920×1080** 视口下双方均有截图证据。

本切片是 CD-2 **浏览器 UIUX 证据** 扩展（相对 [CDP-E2E-T01-uiux-manifest.md](../../frontend/e2e/evidence/CDP-E2E-T01-uiux-manifest.md) 的英文双品牌基线），**不**重开产品 i18n 词条全量审计、**不**要求修复已知 GREENBC nav active tint 跨相建议项，**不**覆盖 T13 包物化。

**与既有证据关系：**

| 既有 | 覆盖 | 本切片补充 |
| --- | --- | --- |
| CD-E2E-T01 UIUX manifest（15 帧 @1920） | 英文 + REDBC/GREENBC 黄金路径 | **zh-CN** 语言证据；在扩展清单中再次确认双品牌 |
| P20 locale/brand 能力（已交付） | 壳层 locale/brand switcher、`html lang` / `data-brand` | 浏览器黄金表面可观测证明 |
| LRP-B6 / P2-T06 等 | 局部 zh-CN 或双品牌样例 | **CDP 黄金路径关键表面** 的 T12 专用 manifest |

---

## 2. Actor / Role

| Actor | 角色 / 账户 | 权限 |
| --- | --- | --- |
| **证据操作者** | 任一可登录管理 UI 的种子角色（推荐 `GLOBAL_ADMIN` `10000001` 或与所选表面匹配的 Author/Tester/Approver/Group Admin） | 打开关键表面；使用壳层 locale / brand 切换器 |

> E2E 须真实登录；locale/brand 切换须经 UI（`switchLocale` / `switchBrand` 或等价壳层控件），不得仅改 localStorage 后断言。

---

## 3. Goal（用户目标）

**作为** 银行内部部署验收方  
**我希望** 在关键黄金表面上看到 **简体中文** 界面，并确认 **REDBC / GREENBC** 双品牌主题均可切换且可截图留证  
**以便** CD-2 具备 zh-CN + 双品牌的浏览器 UIUX 证据，补齐 T01 英文基线。

---

## 4. Trigger（触发条件）

- Docker 验收栈健康（UI `:4173`，backend `:8080/healthz`）
- 用户已登录管理壳层（可见 `.locale-switcher` 与 `.brand-switcher`）
- 打开至少一个 T01 黄金路径关键表面（见 §5）

---

## 5. Preconditions（前置条件）

1. 用户已真实登录管理 UI（非角色模拟）。
2. 壳层已具备 locale 切换（`en` / `zh-CN`）与 brand 切换（`REDBC` / `GREENBC`）——P20 已交付能力。
3. 可导航到 **≥3 个互不相同的关键黄金表面**。实现方可从下列推荐集中选取（须 ≥3；允许用等价路由/队列态替代，但须在 manifest 中点名）：

   | 推荐表面 | 典型路由 / 状态 | T01 对照帧（英文基线） |
   | --- | --- | --- |
   | Dashboard 队列（如 TEST） | `/dashboard?queue=TEST`（或 APPROVAL / PENDING_RELEASE） | 02 / 04 |
   | 模板测试 / 审批工作区 | 模板详情 Testing 或 Approval 工作区 | 01 / 07 / 08 / 11 |
   | 判定或发布对话框 | Confirm test pass / Confirm approval / Go-live summary | 06 / 09 / 12 |
   | External access / API policy hub | 已发布模板 External access | 13 / 15 |

4. 视口主证据为 **1920×1080**（与 T01 一致；crop 仅可用于品牌 lockup 辅证，不计入「关键表面」三张下限）。
5. Setup 可用既有 golden fixture（如 `prepareCdpMvpGoldenDraft`）到达可读表面；**不得**用 API 伪造 locale/brand 状态。
6. **Out of scope：** T13 包物化；审计治理深查；逐 key 中英词条 diff 全量；修复 GREENBC sidebar active tint（已知跨相 Suggestion，非本切片 Critical）。

---

## 6. Primary Journey（主路径）

| # | Actor | UI 动作 | 系统响应 |
| --- | --- | --- | --- |
| 1 | 操作者 | 登录 → 打开推荐关键表面之一 | 壳层 + 内容区可见；默认可为 `en` + 当前品牌 |
| 2 | 操作者 | 经 `.locale-switcher` 选择 **简体中文 / Chinese (Simplified)** | `html[lang="zh-CN"]`；主壳与当前表面用户可见文案切换为 zh-CN（允许极少数未译 key 回退 en，但 **不得**整页仍为英文） |
| 3 | 操作者 | 在 zh-CN 下导航/停留于 **≥3** 个关键表面并截图 | 每帧可辨中文导航/标题/主 CTA；登记 T12 manifest |
| 4 | 操作者 | 经 `.brand-switcher` 在 **REDBC** ↔ **GREENBC** 间切换 | `html[data-brand]` 更新；logo/主色跟随品牌；布局/IA 不变 |
| 5 | E2E/UIUX | 确保证据集在 **1920×1080** 下同时含 REDBC 与 GREENBC 帧 | 写入 `CDP-E2E-T12-uiux-manifest.md`（扩展/并列于 T01，非覆盖 T01 Verdict） |

---

## 7. System Responses（成功与边界）

对齐已确认产品基线（PRD 双主题 + P20 locale/brand；i18n English-first + zh-CN additive）：

- **Locale：** 切换到 zh-CN 后 `document.documentElement.lang === 'zh-CN'`；壳层与关键表面主文案为中文；切换 **不改变** 信息架构/布局结构。
- **Brand：** 切换后 `data-brand` 为 `REDBC` 或 `GREENBC`；logo 与主色 token 跟随；切换 **不改变** IA/布局。
- **证据：** 主视口 **1920×1080**；T12 产出独立 UIUX manifest + screenshots；Verdict 行必填。
- **Fail-closed（本切片不强制单独场景）：** 未登录不得进入管理壳；无权限表面仍 403/无权反馈（沿用既有矩阵）。
- **已知非阻断：** GREENBC 下部分侧栏 active tint 可能仍偏红（T01 Suggestion）——记录即可，**不**阻塞 T12 PASS。

---

## 8. Acceptance Scenarios（Given / When / Then）

### BDD-CDP-I18N-001 — zh-CN 语言切换覆盖 ≥3 个关键黄金表面

- **Given** Docker 栈就绪，操作者已登录管理 UI，壳层 locale switcher 可用  
- **When** 用户将语言切换为 **zh-CN**，并打开（或停留在）**至少 3 个互不相同的关键黄金表面**（选自 §5 推荐集或其等价态）  
- **Then** 每个表面均可见 **zh-CN** 用户文案（导航、页标题或主 CTA 至少一类为中文；非整页英文）  
- **And** `html` 的 `lang` 为 `zh-CN`  
- **And** 信息架构/布局结构相对切换前无破坏性变化  
- **And** 可观测证据：≥3 张 zh-CN 截图登记于 `CDP-E2E-T12-uiux-manifest.md`（主视口 1920×1080）

> 追溯：CDP § CD-E2E-T12 acceptance「zh-CN locale switch on ≥3 key screens」；P20-T02 locale switcher；T01 关键表面清单。

---

### BDD-CDP-I18N-002 — REDBC + GREENBC 双品牌 @1920

- **Given** Docker 栈就绪，操作者已登录，壳层 brand switcher 可用；至少已处于一个关键黄金表面（可为 zh-CN 或 en；**推荐在 zh-CN 下取证以与 T12 目标一致**）  
- **When** 用户分别切换到 **REDBC** 与 **GREENBC** 并截取关键表面（同一表面双品牌，或不同关键表面各至少一品牌，但证据集须 **两者皆有**）  
- **Then** 证据集在 **1920×1080** 下同时包含 **REDBC** 与 **GREENBC** 帧  
- **And** 各帧 `html[data-brand]` 与所选品牌一致；logo / 主色可辨跟随品牌  
- **And** 切换品牌不改变 IA/布局结构  
- **And** 可观测证据：T12 manifest 明确标注 Brand 列；Verdict 含双品牌项

> 追溯：CDP § CD-E2E-T12 acceptance「REDBC/GREENBC both captured」；PRD 双主题；T01 已有英文双品牌，本场景要求 T12 扩展证据集同样覆盖双方。

---

## 9. Observable Evidence（证明方式）

| 证据 | 期望 |
| --- | --- |
| Playwright UIUX | `frontend/e2e/CDP-E2E-T12-uiux-evidence.spec.ts`（新建）覆盖 **BDD-CDP-I18N-001**、**BDD-CDP-I18N-002** |
| Helpers | 复用 `frontend/e2e/helpers/uiux-evidence.ts` 的 `switchLocale` / `switchBrand` |
| UIUX manifest | `frontend/e2e/evidence/CDP-E2E-T12-uiux-manifest.md` + `CDP-E2E-T12/screenshots/` — Verdict **PASS** |
| 栈 | Docker `:4173` + `:8080/healthz`；Playwright docker config `--workers=1` |
| 与 T01 关系 | **扩展**证据，不修改 T01 已闭合 Verdict；manifest 可交叉引用 T01 英文基线 |

**最低截图数量（确认）：**

- ≥ **3** 帧 zh-CN 关键表面（I18N-001）
- 证据集内 ≥ **1** 帧 REDBC + ≥ **1** 帧 GREENBC @1920（I18N-002；可与 zh-CN 帧重叠计数，例如 2 zh-CN×品牌 + 1 额外 zh-CN 表面）

---

## 10. Traceability

| 来源 | 引用 |
| --- | --- |
| Plan | [CDP-e2e-full-chain-evidence.md](../plan/detail/CDP-e2e-full-chain-evidence.md) § CD-E2E-T12 |
| CDP program | [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md) CD-2 |
| Prior UIUX | [CDP-E2E-T01-uiux-manifest.md](../../frontend/e2e/evidence/CDP-E2E-T01-uiux-manifest.md) |
| Product | [PRD.md](../product/PRD.md) — REDBC/GREENBC 双主题与 logo |
| i18n 能力 | [P20-i18n-ui-upgradeability.md](../plan/detail/P20-i18n-ui-upgradeability.md) P20-T02/T04（已交付） |
| MVP 行为 | [mvp-golden-path-browser.md](./mvp-golden-path-browser.md)（表面语义；本文件不重开生命周期） |

**Task IDs:** `CD-E2E-T12`  
**Formal phase:** None（不改变）  
**Wave:** CD-2 保持 In Progress；本文件不将 T12/wave 标 Done

---

## 11. Confirmed vs pending

### Confirmed（本文件）

- Actor / goal / trigger / preconditions / journey / system responses  
- **BDD-CDP-I18N-001**、**BDD-CDP-I18N-002** Given/When/Then  
- 视口 1920×1080；≥3 zh-CN 关键表面；REDBC+GREENBC 均须出现在 T12 证据集  
- 实现方可从 §5 推荐集选表面；GREENBC nav tint 非阻断  

### Pending questions

_无。_（表面具体三选实现时在 manifest 点名即可；不阻塞 readiness。）

---

## 12. BDD readiness

- **bdd_readiness:** `ready`
- **Scenario IDs:** `BDD-CDP-I18N-001`, `BDD-CDP-I18N-002`
- **open_questions:** 无
- **Next:** `plan-orchestrator` → `e2e-test-engineer` + `e2e-uiux-reviewer` 在 worktree 实现证据；勿将 CD-2 / T12 标 Done 直至 gates + manifest PASS
