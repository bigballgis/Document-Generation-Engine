# BDD 行为规格：CE-U07 — 条款升版提醒 + 一键 bump

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U07-COB`  
**编写日期:** 2026-07-14  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U07  
**Slice:** `ce-u07-clause-outdated-bump`  
**Task Master:** **#82**  
**Formal phase:** **None**

---

## 1. 概述

模板详情条款面板仅展示 pinned 版本；条款模块升版并批准后，作者无感知。本切片在引用存在**更新已批准版本**时展示 out-of-date 徽标，提供一键 bump（走既有 `upsertReference`），支持批量升级确认；Dashboard 作者待办增加「引用条款有新版」项。

| 行为域 | 摘要 |
| --- | --- |
| **COB-01 out-of-date 检测** | 列表 API 返回 `outOfDate` + `latestApprovedSemanticVersion`（当存在更新 referencable 版本时） |
| **COB-02 一键 bump** | 条款面板对未锁定引用展示 Bump 操作，调用 `PUT …/content-module-references/{key}` 升至最新已批准版本 |
| **COB-03 批量升级** | 面板提供「升级全部过期引用」+ 确认对话框，逐条 upsert |
| **COB-04 Dashboard 待办** | `TEMPLATE_AUTHOR` 在 Dashboard Tasks 看到「引用条款有新版」待办，深链至 design/contentModules |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 锁定引用自动升 pin | Out of scope — 仅展示徽标，禁用 bump |
| 条款审核待办（CE-U08） | Out of scope |
| 发布门禁新增 blocker | Out of scope — 提醒为主 |

---

## 2. Actor / Role

| Actor | 角色 |
| --- | --- |
| **模板作者** | `TEMPLATE_AUTHOR` |
| **系统** | Content module reference API + Dashboard task hub |

---

## 3. Acceptance scenarios

### BDD-CE-U07-COB-001 — out-of-date 徽标

```gherkin
Given 草稿模板引用条款 MOD-A 的 1.0.0（已批准）
And MOD-A 存在更新的已批准 active 版本 1.1.0
When 作者打开模板详情 design/contentModules 条款面板
Then 该引用行展示 out-of-date 徽标
And 列表 API 返回 outOfDate=true 与 latestApprovedSemanticVersion=1.1.0
```

### BDD-CE-U07-COB-002 — 一键 bump

```gherkin
Given 引用行 outOfDate=true 且 locked=false
When 作者点击 Bump to latest
Then 系统调用 upsertReference 将 pin 升至 latestApprovedSemanticVersion
And 引用行不再展示 out-of-date 徽标
```

### BDD-CE-U07-COB-003 — 批量升级确认

```gherkin
Given 条款面板存在 2 条及以上未锁定的 out-of-date 引用
When 作者点击 Bump all outdated 并确认
Then 所有可升级引用均升至各自 latestApprovedSemanticVersion
And 面板无剩余 out-of-date 未锁定引用
```

### BDD-CE-U07-COB-004 — Dashboard 作者待办

```gherkin
Given 作者可写的草稿模板至少有一条 out-of-date 条款引用
When 作者打开 Dashboard Tasks
Then 出现「引用条款有新版」待办项，含模板名称与过期引用数量
When 作者打开该待办
Then 导航至模板 design/contentModules 工作区
```

### BDD-CE-U07-COB-005 — 锁定引用不可 bump

```gherkin
Given 已发布模板锁定引用 pinned 于旧版，且模块有更新已批准版本
When 作者在只读条款面板查看
Then 可展示 out-of-date 徽标（若 API 返回 outOfDate）
And 不展示 Bump 操作
```

---

## 4. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-U07 plan §4 | 目标行为 |
| `TemplateContentModuleReferenceService.upsertReference` | bump 复用既有写路径 |
| CE-U08 | 条款审核待办 — 独立切片 |

**bdd_readiness: ready**
