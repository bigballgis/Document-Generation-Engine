# BDD 行为规格：CE-U10 — sharedGroupCodes 配置 UI

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U10-SGC`  
**编写日期:** 2026-07-15  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U10  
**Slice:** `ce-u10-shared-group-codes-ui`  
**Task Master:** **#85**  
**Formal phase:** **None**  
**完成声明约束:** 关闭 R1/R4「sharedGroupCodes 无 UI」；**复用既有** create/read/access 持久化与 `writeSharedGroupCodes`；**不**重做条款生命周期/审核；**不**宣称 go-live

---

## 1. 概述

后端已支持内容模块 `sharedGroupCodes`：

- `CreateContentModuleRequest.sharedGroupCodes`
- `ContentModuleDetailView` / `SummaryView.sharedGroupCodes`
- `ContentModuleAccessService` 按所属组 **或** 共享组授权可读

管理 UI **零入口**：`ContentModuleCreateDialog` 不采集该字段；详情摘要不展示共享范围；无设置对话框可改共享组。

| 行为域 | 摘要 |
| --- | --- |
| **SGC-01 创建多选** | 创建对话框「Share to groups」多选（英文-first） |
| **SGC-02 设置多选** | 详情 Settings 对话框可查看/更新共享组并保存 |
| **SGC-03 摘要展示** | 详情头/摘要展示所属组 + 共享范围（空则「本组织组 only」类文案） |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 重写 access 谓词 / 引用检索算法 | Out of scope — 已存在 |
| 共享变更完整 impact 分析工作流（矩阵「二次确认+影响分析」的重型治理） | **本片最小确认**：设置保存前 `el-dialog`/`ElMessageBox` 二次确认即可；完整 impact 面板属后续增强，不阻塞 P1·S |
| CE-U08/U09 审核可达性 | Out of scope |
| 跨组共享测试数据集库 | Out of scope（PRD 未确认） |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **GLOBAL_ADMIN / GROUP_ADMIN** | 配置共享范围（permission-matrix） | 创建与 Settings 多选主操作者 |
| **条款作者** | `authorContentModules` | 可创建模块；若无「配置共享范围」权则创建时不展示多选（sharedGroupCodes=[]） |
| **其他可读角色** | 模块读权限 | 可见摘要中的共享范围；不可改 |
| **系统** | 既有 CM API + access | 持久化与 fail-closed 读授权 |

---

## 3. Goal

1. 有权配置共享的管理员在**创建**时多选目标组，写入 `sharedGroupCodes`。  
2. 同一批管理员可在详情 **Settings** 中调整共享组并保存。  
3. 任何可打开详情的用户在摘要区看到共享范围（所属 `groupCode` + shared 列表）。  
4. 选项仅含会话可管理/可见的组目录（ScopedGroupSelect 同源）；不得选所属组自身为「共享目标」（所属组已隐含访问）。  
5. 无配置权者不展示写控件（fail-closed）。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U10-C1** | **创建：** `ContentModuleCreateDialog` 增加多选；提交 `createModule` payload 含 `sharedGroupCodes: string[]`（可空）。 | 计划卡；既有 Create API |
| **U10-C2** | **谁可写共享：** UI 写控件仅对 permission-matrix「配置共享范围」角色展示（GLOBAL_ADMIN、GROUP_ADMIN 在被授权组范围）。纯 `TEMPLATE_AUTHOR` 创建不展示多选，请求 `sharedGroupCodes=[]`（或省略，后端当空）。 | permission-matrix § 条款共享 |
| **U10-C3** | **Settings 对话框：** 详情页新增 Settings（或等价）入口；表单含共享多选；保存更新模块级 `sharedGroupCodes`。 | 计划卡 |
| **U10-C4** | **Settings 持久化：** 复用既有 `writeSharedGroupCodes` / 实体字段。若 OpenAPI 尚无模块级 update shared 端点，本片允许补 **最小** `PUT/PATCH …/content-modules/{id}/shared-group-codes`（或 metadata update 含该字段）——属「接线已有持久化」，不另开治理切片。 | 用户「Backend already exists」+ 计划「设置对话框」 |
| **U10-C5** | **摘要：** PageHeader description 或摘要条展示：`Owner: {groupCode}` + `Shared with: {codes}`；空共享 → 明确「Not shared outside owner group」（英文-first key）。 | 计划卡 |
| **U10-C6** | **选项源：** 复用 `useScopedGroupOptions` / 组目录；排除当前 `groupCode`；非法/不可见组不可选。 | 现有 FE 模式 |
| **U10-C7** | **二次确认：** Settings 保存若 shared 集合相对加载值有变更 → 确认对话框后再提交。创建 可跳过确认。 | 矩阵「二次确认」最小落地 |
| **U10-C8** | **只读：** 无配置权用户打开 Settings 时控件 disabled 或入口隐藏；仍可见摘要。 | fail-closed |
| **U10-C9** | **禁止：** 改变默认同组隔离语义；允许无授权跨组读；go-live。 | 计划 / 矩阵 |

---

## 5. Preconditions / Trigger

**Preconditions**

- 内容模块管理路由可用；组目录可加载。  
- Create API 接受 `sharedGroupCodes`；Detail 返回该字段。

**Triggers**

- 打开创建对话框并提交。  
- 打开详情 → Settings → 保存。  
- 打开详情查看摘要。

---

## 6. Primary journey

1. GROUP_ADMIN 创建条款，多选共享到 `RETAIL`（所属组 `HQ`）→ 创建成功。  
2. 详情摘要显示 Owner HQ · Shared with RETAIL。  
3. Admin 打开 Settings，增加 `WEALTH`，确认后保存 → 摘要更新。  
4. RETAIL 组内授权用户可在目录中读到该模块（既有 access）；无共享且非所属组用户不可见。

---

## 7. System responses（success）

| 表面 | 成功响应 |
| --- | --- |
| **Create** | POST body 含所选 codes；Detail 回读一致 |
| **Settings** | 保存后 Detail/摘要刷新；二次确认后才请求 |
| **Summary** | 共享列表可读；空态文案明确 |
| **Access（回归）** | 共享组用户可读；未共享外组不可读（既有后端） |

---

## 8. Acceptance scenarios

### BDD-CE-U10-SGC-001 — 创建多选写入 sharedGroupCodes

```gherkin
Given 会话为 GROUP_ADMIN（或 GLOBAL_ADMIN），可访问所属组 HQ
When 管理员打开创建对话框，填写必填字段，并多选共享组 ["RETAIL"]
And 提交创建
Then create 请求携带 sharedGroupCodes=["RETAIL"]
And 返回的 Detail.sharedGroupCodes 含 RETAIL
```

### BDD-CE-U10-SGC-002 — 作者创建不暴露写控件

```gherkin
Given 会话为 TEMPLATE_AUTHOR（无配置共享范围权）且可 authorContentModules
When 打开创建对话框
Then 不展示 Share to groups 多选（或只读隐藏）
When 提交创建
Then sharedGroupCodes 为空列表（或不传，后端存空）
```

### BDD-CE-U10-SGC-003 — 详情摘要展示共享范围

```gherkin
Given 模块 groupCode=HQ 且 sharedGroupCodes=["RETAIL","WEALTH"]
When 任意有权打开详情的用户查看详情头/摘要
Then 可见所属组 HQ
And 可见共享组 RETAIL 与 WEALTH（英文-first 标签）
Given 模块 sharedGroupCodes=[]
When 查看摘要
Then 展示未对外共享的明确空态文案
```

### BDD-CE-U10-SGC-004 — Settings 更新共享组

```gherkin
Given 管理员可配置共享，模块当前 sharedGroupCodes=["RETAIL"]
When 打开 Settings，将选择改为 ["RETAIL","WEALTH"] 并确认保存
Then 持久化后 Detail.sharedGroupCodes 为 RETAIL 与 WEALTH（顺序可规范为排序后稳定）
And 摘要刷新为新范围
```

### BDD-CE-U10-SGC-005 — Settings 变更二次确认

```gherkin
Given Settings 中共享选择相对打开时有变更
When 管理员点击 Save
Then 先出现确认对话框
When 管理员取消确认
Then 不发送更新请求
When 管理员再次 Save 并确认
Then 发送更新请求
```

### BDD-CE-U10-SGC-006 — 无配置权不可改

```gherkin
Given 会话无配置共享范围权但可打开模块详情
When 查看详情
Then 摘要只读可见
And Settings 写入口隐藏或保存控件 disabled
```

### BDD-CE-U10-SGC-007 — 选项边界

```gherkin
Given 创建或 Settings 多选打开
Then 选项不含当前所属 groupCode
And 仅含会话组目录中可见/可选的组
When 尝试保存含所属组自身的共享列表（若被篡改请求）
Then 后端规范化忽略或校验失败（与 writeSharedGroupCodes 既有行为一致，实现固定并测）
```

---

## 9. Boundary / exception

- API 校验失败：表单展示 messageKey（英文-first）。  
- 组目录加载失败：多选空 + 错误提示；不阻断仅填所属组的创建（若管理员仍可建空共享）。  
- 空多选合法（仅所属组隔离）。

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 组件/store 单测 | create payload；summary 渲染；settings confirm |
| E2E | 管理员创建带共享 → 摘要可见 → Settings 增组 |
| 后端（若补 update） | 契约测 + access 回归 |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-U10 plan §4 | 目标行为 |
| Task Master **#85** | 执行任务 |
| permission-matrix 条款「配置共享范围」 | 谁可写 |
| PRD / domain 默认同组隔离 + 授权共享 | 产品语义 |
| `CreateContentModuleRequest` / AccessService | 既有后端 |

---

## 12. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-u10-shared-group-codes-ui.md
task_ids: ["#85", "CE-U10"]
scenario_ids:
  - BDD-CE-U10-SGC-001
  - BDD-CE-U10-SGC-002
  - BDD-CE-U10-SGC-003
  - BDD-CE-U10-SGC-004
  - BDD-CE-U10-SGC-005
  - BDD-CE-U10-SGC-006
  - BDD-CE-U10-SGC-007
```
