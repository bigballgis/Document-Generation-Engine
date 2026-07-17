# BDD 行为规格：CE-U19 — 依赖关系只读视图（Dependency read-only view）

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U19-DRV`  
**编写日期:** 2026-07-17  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U19  
**Slice:** `ce-u19-dependency-readonly-view`  
**Task Master:** **#97**  
**Formal phase:** **None**  
**Placement:** ISOLATED `D:/working/DGE-ce-u19-dependency-readonly-view` · `feat/ce-u19-dependency-readonly-view`  
**完成声明约束:** 关闭「模板详情缺少依赖关系只读聚合面」缺口；在 Package Hub 增加 **Dependencies** 只读页，展示母版 revision、锚点、条款版本、release 线；复用既有 template/master/content-module/version-lines 读路径；**不**实现 CE-E03 / CE-G05；**不**改 CE-U17 快捷键；**不**宣称 go-live；**不**激活 CD-3；**leave #50 alone**

---

## 1. 概述

模板 Package Hub（`/templates/:templateId`）今日次级 Tab 仅有 **Overview** 与条件性 **API access**；作者/审批/运维若要回答「这份模板钉了哪版母版、绑了哪些锚点、引用了哪些条款版本、有哪些 release 线」，必须在 Overview 摘要、Design 绑定、Content modules 面板、Version lines 表之间来回跳转。CE 计划卡 CE-U19 要求：**模板详情「依赖」页（只读）**——母版 revision、锚点、条款版本、release 线。

| 缺口（现状证据） | 目标 |
| --- | --- |
| Hub 无 Dependencies 次级 Tab（`HUB_SECONDARY_TABS = overview \| apiAccess`） | Hub 增加 **Dependencies** 只读 Tab + `?tab=dependencies` 深链 |
| Overview 仅 `masterId` 链接 + 当前 `releaseVersion` | 聚合展示 **母版 revision**（发布钉扎优先）+ 绑定锚点 + 条款引用 + release 线摘要 |
| 条款引用仅在 Design `contentModules` 可写面板 | Dependencies 只读列出 `content-module-references`（无 upsert） |
| Version lines 在 Hub 主表（含 clone 等动作） | Dependencies 内 **只读摘要 + 导航**；写动作不放进本 Tab |
| 发布钉扎 `masterRevisionId` / `masterFileHash` 在 DB（CE-K01）但管理读 DTO 未暴露 | 允许对既有 release GET **加法暴露** `masterPin`（复用 E01 形状）；禁止发明全新依赖聚合域 |

| 行为域 | 摘要 |
| --- | --- |
| **DRV-01 Dependencies Tab** | Package Hub 次级 Tab **Dependencies**；English-first；Workspace Tab Shell 只读面 |
| **DRV-02 母版 revision** | 展示绑定母版 + 发布钉扎 revision（有 pin）或 in-flight「尚未钉扎 / 工作母版」说明 |
| **DRV-03 锚点** | 只读列出当前上下文 bindings 的锚点（id + 内容类型；可选 displayLabel） |
| **DRV-04 条款版本** | 只读列出 content-module references（语义版本、locked、outOfDate） |
| **DRV-05 Release 线** | 只读摘要 version-lines（IN_FLIGHT / PUBLISHED）；可导航至 dev / release 详情 |
| **DRV-06 Fail-closed / 空错态** | 组隔离与读授权 fail-closed；加载失败不假 empty；分区空态诚实 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-E03 全库导出 | Out of scope — #80 |
| CE-G05 年检 + 条款 FTS | Out of scope — #77 |
| CE-U17 编辑器快捷键 | 已交付；本片不改 |
| CE-K05 母版反向 impact（master → templates） | 已交付；本片是 **模板 → 依赖** 正向只读，不重做 impact |
| CE-E01 import dry-run `dependencyReport` UI | Out of scope — 非本片「依赖」语义 |
| 在 Dependencies Tab 上编辑绑定 / 升条款 / clone / abandon / publish | **禁止** — 只读；写路径保持既有 Design / Version lines / Lifecycle |
| 新建 `/templates/{id}/dependencies` 聚合 API（除非实现证明 FE 组合不可验收） | 默认 **不需要**；优先组合既有读 API + 可选 release `masterPin` 加法字段 |
| CD-3 / go-live / Task #50 | **禁止** |
| 宣称正式 P-phase | Formal phase 保持 **None** |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **模板编排 / 设计人员** | `authorTemplates` / `manageMasters`（矩阵 §5）+ `route.template-management` | 打开 Dependencies 核对钉扎母版、锚点覆盖、条款 pin、release 线 |
| **测试 / 审批人员** | `decideTests` / `decideApprovals` + 同 route | 只读查看依赖；不得因本 Tab 获得写入口 |
| **组 / 全局管理员** | `GROUP_ADMIN` / `GLOBAL_ADMIN` | 在授权组（或全局）范围内只读查看 |
| **跨组未授权主体** | 无目标组授权 | Fail-closed：不得看到依赖 payload |
| **系统** | 既有 management 读 API + i18n + Hub routing | 解析 `tab=dependencies`；加载并渲染只读分区 |

---

## 3. Goal

1. 具备模板读权限的操作者在 Package Hub 打开 **Dependencies**，**一屏**看到该模板的依赖关系：母版 revision、锚点、条款版本、release 线。  
2. 该页 **全程只读**：无 Save / Upsert / Clone / Publish CTA；导航链接可去既有只读或可写面，但不在本页执行写。  
3. 发布线优先展示 **CE-K01 钉扎**母版 revision（id / sequence / hash 摘要）；in-flight 明确「尚未钉扎」或展示当前工作母版上下文，**不得**把「当前母版 head」伪装成「已发布钉扎」。  
4. 空分区与加载失败可区分；组隔离与 403/404 语义 fail-closed。  
5. E2E + UIUX（双品牌 @1920）验收；formal phase **None**；不宣称 go-live；不激活 CD-3。

---

## 4. 已确认决策 vs 推导假设

### 4.1 已确认（产品 / 计划 / 既有交付）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U19-C1** | 模板详情 **「依赖」页**：母版 revision、锚点、条款版本、release 线。 | CE 计划 §4 CE-U19；Task Master #97 |
| **U19-C2** | **只读**视图（testStrategy：Read-only dependency page + E2E/UIUX）。 | Task Master #97 |
| **U19-C3** | 主表面为现代 **Package Hub**（`/templates/:templateId`），次级 Tab 模式与 Overview / API access 一致。 | [catalog-navigation-ux.md](../product/catalog-navigation-ux.md)；`TemplatePackageHubWorkspace` |
| **U19-C4** | 组隔离 + 模板读权限 fail-closed（`GroupAccessService` / 矩阵 §5、§13）。 | permission-matrix；既有 hub 读路径 |
| **U19-C5** | Journey / 依赖类导向面只读；写动作不在本 Tab action rail。 | Workspace Tab Shell constitution |
| **U19-C6** | English-first（en 基线 + zh-CN）；母版用 **Master / Letterhead** 术语；条款用 **Standard clauses**（L1）/ content-module 技术词仅 L2。 | i18n-english-first；business-terminology-guide |
| **U19-C7** | Formal phase **None**；不宣称 go-live；不激活 CD-3；leave #50；E03/G05/U17 out of scope。 | CE 计划 / handoff |

### 4.2 本片确认的实现决策（计划卡薄 → 仓库事实推导）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **U19-D1** | **Tab 身份：** Hub 次级 Tab 名稳定为 `dependencies`；English 标签 **Dependencies**（zh-CN：**依赖**）。`HUB_SECONDARY_TABS` / `resolveHubSecondaryTab` / `hubWorkspaceTabs` 纳入该名；深链 `?tab=dependencies`。 | U19-C1/C3 |
| **U19-D2** | **主表面：** `/templates/{templateId}` Package Hub 的 Dependencies 槽位（`WorkspaceTabShell` `#dependencies`）。**不是**新路由；**不是** dev workspace 的第五个 top-level workspaceTab。 | Hub 已是「模板详情」聚合面；version-lines 已在 Hub |
| **U19-D3** | **数据上下文默认：** 展示 **当前包活动上下文**——优先 in-flight `GET …/dev/{devVersionId}`（或等价 `getTemplate` 活动详情）的 bindings / 条款引用；release 线来自 `GET …/version-lines`。用户从 Dependencies 内点击某 **PUBLISHED** 行时可导航到 `/releases/{releaseVersion}` 只读详情核对该线快照（含钉扎）。 | 既有 API；与 hub 表一致 |
| **U19-D4** | **母版 revision 分区：** | U19-C1 + CE-K01 |
| | (a) 始终展示绑定 `masterId`（名称链接 → master hub/detail，复用 Overview `EntityLinkCell` 模式）。 | |
| | (b) **若存在已发布线且可读到 pin：** 展示该线（默认：当前默认路由 / 最新 published，实现固定一种并测）的 `masterRevisionId`、可选 `revisionSequence`、`masterFileHash` 截断摘要、`pinOrigin`（若有）。 | |
| | (c) **In-flight 且无 pin：** 明确文案 **Not pinned until publish**（English-first）；可额外展示母版 **current** revision line 摘要（`GET …/masters/{masterId}/revision-lines` 中 `current=true`）并标注为 working context，**不得**标为 Pinned。 | |
| **U19-D5** | **Pin 暴露（真实缺口，允许最小 BE）：** 今日 `TemplateDetailView` / `TemplateReleaseVersionView` **不含** `masterRevisionId`。本片允许对 `GET …/templates/{templateId}/releases/{releaseVersion}`（及必要时 version-line/detail）**加法**可选 `masterPin`（形状对齐已有 `TemplateExportMasterPinView`：`masterRevisionId`、`masterFileHash`、可选 `revisionSequence` / `pinOrigin`）。**禁止**为此新建独立依赖微服务或 CE-E01 导出 UI。若实现能仅用 export 以外的既有读路径验收 D4，可不改 OpenAPI。 | Mandate：不 invent 除非缺口真实；K01 数据已在实体 |
| **U19-D6** | **锚点分区：** 只读表/列表，行至少：`anchorId`、`declaredContentType`；若能从母版 revision anchors 解析则显示 `displayLabel`（CE-U06）。数据源 = 当前上下文 `bindings[]`（detail/dev/release）。空 bindings → 诚实空态（「No anchor bindings」）。**不**提供绑定编辑。 | TemplateDetailView.bindings |
| **U19-D7** | **条款版本分区：** 调用既有 `GET …/templates/{templateId}/content-module-references`；行至少：`referenceKey`、`moduleId`（可链到 content-module 详情）、`semanticVersion`、`locked`、`outOfDate`（及可选 `latestApprovedSemanticVersion` 只读提示）。**不**渲染 upsert/bump 控件（升版仍走 Design / CE-U07 既有入口）。空列表 → 诚实空态。 | `listTemplateContentModuleReferences` |
| **U19-D8** | **Release 线分区：** 消费 `GET …/version-lines`（可与 Hub 主表共享 store/缓存，避免双请求风暴）。Dependencies 内为 **只读摘要**（lineKind、devVersionNumber、releaseVersion、lifecycleStatus、defaultRouteTarget）；行点击导航语义与 Hub 表一致（IN_FLIGHT → dev；PUBLISHED → release detail）。**不**在本分区放 Clone / Abandon / Create from latest 按钮。 | VersionLinesPanel 已有动作面 |
| **U19-D9** | **布局：** 单一 Dependencies 页内分四个只读 section（Master revision / Anchors / Clause versions / Release lines），银行 OA 卡片/分区标题；@1920 一屏可扫读；窄屏可纵向堆叠。稳定 `data-testid`：`template-dependencies-panel`（及各 section 子 testid）。 | OA + E2E |
| **U19-D10** | **加载 / 错误：** 分区或整页 loading skeleton；任一关键读失败 → `LoadErrorPanel`（或等价）+ Retry；**禁止**在失败时渲染「无依赖」成功空态（对齐 CE-K05 空态真值原则）。部分成功允许：失败分区错误、成功分区仍展示。 | UXE3 / K05-C4 |
| **U19-D11** | **权限：** 与打开 Package Hub 相同的模板读边界；403/404 不泄露跨组存在性。本 Tab **不**因 `authorTemplates` 显示写控件。无 `route.template-management` 不可达（既有壳）。 | U19-C4 |
| **U19-D12** | **i18n：** Tab 名、类型标题、空态、Not pinned、错误文案 English-first（`en` + `zh-CN`）。避免对用户暴露内部字段名如 `masterFileHash` 作唯一标签（可作次要 mono 摘要）。 | U19-C6 |
| **U19-D13** | **与 Overview / Version lines 关系：** Overview 保持摘要；Hub 上方 Version lines 主表保持可写动作；Dependencies **不删除**这些面，只增加只读聚合。 | 最小扰动 |
| **U19-D14** | **门禁：** Vitest 覆盖 tab 解析与只读渲染/空错态；E2E 覆盖打开 Dependencies + 四区可见性（或诚实空/钉扎态）；UIUX 双品牌 Critical=0；acceptance 表面变更 → queued docker-deploy。`frontend_ui_in_scope=true`。 | TDD/E2E 宪法 |

### 4.3 非确认假设（不得升格为需求）

| 项 | 状态 |
| --- | --- |
| 四区用 `el-card` 还是单卡多分节 | 实现自选；须满足 D9 可扫读 |
| 默认展示哪条 published 的 pin（defaultRouteTarget vs 最新 published） | 实现固定一种并在测试锁定 |
| Anchors 是否内嵌 CE-U06 位置高亮列表 | 可选；验收只需可读锚点清单 |
| 是否提供「Open in Design → Bindings / Content modules」辅助链接 | 推荐但不强制；不得变成写 CTA 伪装 |
| 是否新增单一 `GET …/dependency-summary` | **默认否**（D5）；仅当 FE 组合无法稳定验收时再开，且须回写本规格 |

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录管理端；具备目标模板组范围读权限（或 GLOBAL_ADMIN）。  
- 模板包存在且未逻辑删除。  
- Docker 验收栈 `http://localhost:4173`（E2E）。  
- 既有 APIs 可用：`getTemplate` / `dev` detail、`version-lines`、`content-module-references`、`getMaster` / `revision-lines`；发布 pin 按 D5 可读。

**Triggers**

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 用户在 Hub 点击 **Dependencies** Tab | 激活次级 Tab |
| T2 | 直接打开 `/templates/{id}?tab=dependencies` | 深链落地 |
| T3 | 用户在 Dependencies 内点击 release 线行或母版/条款链接 | 导航到既有详情（只读或既有可写面） |
| T4 | 加载失败后点击 Retry | 重新拉取失败分区/整页 |

---

## 6. Primary journey（成功路径）

1. 操作者打开模板 Package Hub。  
2. 点击 **Dependencies**（或经 `?tab=dependencies` 进入）。  
3. 系统加载依赖分区：  
   - **Master revision** — 母版名称链接 + 钉扎 revision（若有）或 Not pinned until publish；  
   - **Anchors** — 当前上下文 bindings 列表；  
   - **Clause versions** — content-module references 列表；  
   - **Release lines** — version-lines 只读摘要。  
4. 操作者点击某 published release 行 → 进入 release 只读详情核对快照；面包屑可回 Hub。  
5. 全程无 Dependencies 页内写操作；写需求仍去 Design / Version lines / Lifecycle。

---

## 7. Acceptance scenarios（Given / When / Then）

### BDD-CE-U19-DRV-001 — Hub 暴露 Dependencies Tab 与深链

```gherkin
Given 操作者已登录且可打开模板 Package Hub
When 操作者打开 /templates/{templateId}
Then Hub 次级 Tab 可见 Dependencies（English-first）
When 操作者点击 Dependencies 或打开 ?tab=dependencies
Then 激活 Dependencies 只读面板（data-testid=template-dependencies-panel）
And URL 保留或规范化为 tab=dependencies
```

### BDD-CE-U19-DRV-002 — 母版 revision：已发布钉扎可见

```gherkin
Given 模板存在至少一条 PUBLISHED release 且 CE-K01 已写入 masterRevisionId
And 操作者可读取该 release
When 操作者打开 Dependencies
Then Master revision 分区展示绑定母版标识（名称或 ID 链接）
And 展示该发布上下文的 pinned masterRevisionId（及可获得的 sequence/hash 摘要）
And 文案不将 pin 描述为可编辑字段
```

### BDD-CE-U19-DRV-003 — 母版 revision：in-flight 未钉扎诚实展示

```gherkin
Given 模板仅有 in-flight 开发线（尚无 published pin）或当前上下文无 pin
When 操作者打开 Dependencies
Then Master revision 分区明确表示 Not pinned until publish（或等价 English-first）
And 若展示 working/current 母版 revision，则不得标注为 Pinned
```

### BDD-CE-U19-DRV-004 — 锚点只读列表来自 bindings

```gherkin
Given 当前模板上下文 bindings 含至少一个 anchorId
When 操作者打开 Dependencies
Then Anchors 分区列出每个 binding 的 anchorId 与 declaredContentType
And 不出现绑定 Save / 删除 / 结构化编辑控件
```

### BDD-CE-U19-DRV-005 — 锚点空态

```gherkin
Given 当前模板上下文 bindings 为空且加载成功
When 操作者打开 Dependencies
Then Anchors 分区展示诚实空态（无假数据行）
And 不暗示加载失败
```

### BDD-CE-U19-DRV-006 — 条款版本只读列表

```gherkin
Given GET content-module-references 返回至少一条引用（含 semanticVersion）
When 操作者打开 Dependencies
Then Clause versions 分区展示 referenceKey、moduleId、semanticVersion
And 可见 locked / outOfDate 只读状态（若 API 提供）
And 不出现 upsert / bump 写控件
```

### BDD-CE-U19-DRV-007 — Release 线只读摘要与导航

```gherkin
Given version-lines 含 IN_FLIGHT 与至少一条 PUBLISHED
When 操作者打开 Dependencies
Then Release lines 分区展示两类 lineKind 摘要字段
When 操作者点击 PUBLISHED 行
Then 导航到 /templates/{id}/releases/{releaseVersion} 只读详情
And Dependencies 分区本身无 Clone / Abandon 按钮
```

### BDD-CE-U19-DRV-008 — Fail-closed：跨组不可见

```gherkin
Given 主体仅授权组 A，目标模板在组 B
When 主体尝试打开该模板 Hub / Dependencies 或调用其依赖读 API
Then 返回 403 ACCESS_DENIED（或壳层不可达）且无依赖 payload 泄露
```

### BDD-CE-U19-DRV-009 — 加载失败不假 empty

```gherkin
Given 某一依赖读 API（如 content-module-references 或 version-lines）失败
When 操作者打开 Dependencies
Then 失败分区（或整页）展示错误 + Retry
And 不将该失败渲染为「无条款 / 无 release 线」成功空态
```

### BDD-CE-U19-DRV-010 — 只读：无写 CTA

```gherkin
Given 操作者具备 authorTemplates
When 操作者停留在 Dependencies Tab
Then 页面不提供绑定保存、条款引用写入、clone、publish 等写 CTA
And 可选导航链接可离开本 Tab，但不在本 Tab 提交写请求
```

### BDD-CE-U19-DRV-011 — 与 Overview / Version lines 共存

```gherkin
Given 模板 Hub 已有 Overview 与 Version lines 主表
When 交付 Dependencies 后
Then Overview 与 Version lines 主表仍可用
And Dependencies 为新增只读聚合，不移除既有面
```

### BDD-CE-U19-DRV-012 — E2E + UIUX

```gherkin
Given Docker 验收栈健康且授权用户已登录
When 用户完成 Dependencies 打开与四区可见性（或诚实空/钉扎）旅程
Then 功能 E2E PASS
And UIUX 证据双品牌 @1920 Critical=0（银行 OA；Dependencies 为只读 journey 面）
```

---

## 8. Boundary / exception

| 场景 | 期望 |
| --- | --- |
| 无 published release | Master pin 区走 D4(c)；Release 线可仅有 IN_FLIGHT 或空 published |
| 无 in-flight | Release 线仅 published；锚点/条款上下文取包默认可读快照（实现固定：最新 published release detail 或 getTemplate；须测） |
| 条款 outOfDate=true | 只读提示；不在本页 bump |
| 母版名称加载失败 | 回退显示 masterId；不阻断其它分区 |
| 未知 `tab` | 回落既有默认（overview）；非法值不白屏 |
| `tab=authoring` / lifecycle 深链 | 保持既有 redirect 到 dev（本片不破坏） |
| 未登录 | 既有回登录 |
| DEPRECATED / STOPPED 包 | 仍可读 Dependencies（与 hub 读一致）；无新写入口 |

---

## 9. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | Dependencies Tab；四区只读；空/错/钉扎文案可区分 |
| DOM | `template-dependencies-panel`；无写 CTA testid |
| Network | 组合既有 GET（template/dev、version-lines、content-module-references、master/revision-lines）；可选 release GET 含 `masterPin`；**无** Dependencies 页发起的 PUT/POST/PATCH/DELETE |
| Gates | FE lint/type-check/test/build；若触碰 OpenAPI/BE 则 `mvn verify`；E2E + UIUX；queued deploy |
| 非证据 | 不宣称 go-live；不关闭 E03/G05；不改 #50 |

---

## 10. Traceability

| 项 | 链接 |
| --- | --- |
| Plan | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 **CE-U19** |
| Task Master | **#97** — CE-U19: Dependency read-only view |
| Slice | `ce-u19-dependency-readonly-view` |
| 上游交付 | CE-K01 钉扎；Package Hub / version-lines；content-module-references；CE-U06 锚点标签；CE-U07 outOfDate |
| 导航 / IA | [catalog-navigation-ux.md](../product/catalog-navigation-ux.md) |
| 权限 | [permission-matrix.md](../security/permission-matrix.md) §5、§13 |
| Out of scope | CE-E03 (#80)；CE-G05 (#77)；CE-U17 (#96 Done)；CD-3；go-live；#50 |

---

## 11. BDD readiness

**`ready`** — 规格完整；U19-C1…C7 与 U19-D1…D14 已按计划卡 + Package Hub / 既有依赖读 API / K01 钉扎缺口裁决；验收场景覆盖 Tab/深链、钉扎与未钉扎母版、锚点、条款、release 线导航、fail-closed、错误空态、只读约束、共存、E2E/UIUX。  
**open_questions:** 无（阻塞性问题）。  
**handoff:** `plan-orchestrator` → 任务分解 → frontend-engineer（主）+ 必要时 backend-engineer（D5 `masterPin` 加法暴露）。

**Scenario IDs:** `BDD-CE-U19-DRV-001` … `BDD-CE-U19-DRV-012`
