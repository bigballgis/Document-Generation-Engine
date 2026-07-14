# BDD 行为规格：CE-U05 — Fidelity viewed 持久化 + 修复动线

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U05-FVP`  
**编写日期:** 2026-07-14  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U05  
**Slice:** `ce-u05-fidelity-viewed-persist`  
**Task Master:** **#66**  
**Formal phase:** **None**

---

## 1. 概述

`markViewed` 目前仅更新前端本地 state，父组件未监听，后端无 API——发布勾选「已查看 fidelity」与真实浏览完全脱钩。本切片将 **per warning per preview run** 的 viewed 状态落库，发布门禁校验未 viewed 数量，并改进警告展示（人话文案 + 技术码折叠 + 绑定编辑深链）。

| 行为域 | 摘要 |
| --- | --- |
| **FVP-01 viewed 落库** | 用户在预览保真列表点击「Mark viewed」→ 后端持久化该 preview run 内对应 warning 的 `viewed=true` |
| **FVP-02 发布门禁** | 最新成功 preview run 存在未 viewed 警告时，publish gate `FIDELITY_WARNINGS_VIEWED` 为 blocker |
| **FVP-03 人话文案** | 警告行默认展示可读摘要；技术码收进可展开区域 |
| **FVP-04 修复深链** | 警告行提供「Edit binding」链接，跳转 design/bindings 并打开对应 anchor 编辑 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 逐条 warning「已解决」工作流 | Out of scope |
| 重开 CD-E2E-T10 全矩阵 | 可回归 smoke，不扩展 |
| 站内 PDF 预览（CE-U04） | Out of scope |

---

## 2. Actor / Role

| Actor | 角色 |
| --- | --- |
| **模板编排/测试人员** | `TEMPLATE_AUTHOR` / `TEMPLATE_TESTER` |
| **发布负责人** | `GROUP_ADMIN` |
| **系统** | Preview API + Publish gate + 管理 UI |

---

## 3. Acceptance scenarios

### BDD-CE-U05-FVP-001 — Mark viewed 持久化

```gherkin
Given 模板有一次成功的 preview run，且 fidelityWarnings 含至少一条 viewed=false
When 用户在预览面板对该 warning 点击 Mark viewed
Then 后端返回更新后的 preview record，该 warning viewed=true
And 刷新 preview 后 viewed 状态仍为 true
```

### BDD-CE-U05-FVP-002 — 发布门禁未 viewed 阻断

```gherkin
Given 最新成功 preview run 仍有 unviewed fidelity warnings
When 用户请求 publish gate checklist
Then FIDELITY_WARNINGS_VIEWED 项为 blocker（ready=false）
And publish summary Confirm 因 blocker 不可用
```

### BDD-CE-U05-FVP-003 — 全部 viewed 后门禁就绪

```gherkin
Given 最新成功 preview run 的所有 fidelity warnings 均已 viewed
When 用户请求 publish gate checklist
Then FIDELITY_WARNINGS_VIEWED 项 ready=true
And 用户勾选 fidelity 确认后可提交发布（与其他门禁项满足时）
```

### BDD-CE-U05-FVP-004 — 人话文案与修复深链

```gherkin
Given 预览面板展示 fidelity warning 列表
Then 每行默认展示可读摘要（i18n messageKey 或 generation.warning.fidelity.*）
And 技术 warning code 在可展开区域
And 当 warning 含 anchorId（artifact）时，展示 Edit binding 链接
When 用户点击 Edit binding
Then 导航至 design/bindings 并打开对应 anchor 编辑
```

---

## 4. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-U05 plan §4 | 目标行为 |
| [fidelity-viewed-confirmation-journey.md](./fidelity-viewed-confirmation-journey.md) | 发布勾选 complement；本切片补持久化与 gate |
| domain §2.9 | Test/Approval/Release Summary 保真已查看 |

**bdd_readiness: ready**
