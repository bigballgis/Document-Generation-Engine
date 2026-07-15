# BDD 行为规格：CE-K08 — 条款治理元数据（legal metadata）

| Field | Value |
| --- | --- |
| **Slice** | `ce-k08-clause-legal-metadata`（plan alias `ce-k08-clause-governance-metadata`） |
| **Plan task** | **CE-K08**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §3 CE-K08） |
| **Task Master** | **#63** |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-15 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k08-clause-legal-metadata` · `feat/ce-k08-clause-legal-metadata` · base `93a14580` |
| **Scope** | (1) `content_module_version` 可选法务元数据：`jurisdiction`、`effectiveFrom`、`effectiveTo`、`legalReviewRef`；(2) 内容模块目录 list API 按这些字段筛选；(3) 模板发布门禁硬阻断「引用条款版本已过 `effectiveTo`」。**不**改 CE-U07 bump / CE-U08 review 闭环语义；**不** go-live；**不**激活 CD-3 |
| **Owning docs** | 本文件（行为 SoT）；[requirements-plan.md](../requirements/requirements-plan.md)；[PRD.md](../product/PRD.md) §6.4.2；[domain-model.md](../domain/domain-model.md) §2.9.2 / §2.9.3；[contract-outline.md](../api/contract-outline.md)；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) |

---

## 1. 概述

条款/内容模块版本缺少法务管辖、生效区间与评审单号，目录无法按法务维度检索；模板发布也不会因引用条款已过期而阻断。本切片补齐版本级可选元数据、目录筛选与发布硬门禁。

| 行为域 | 摘要 |
| --- | --- |
| **LM-01 版本法务元数据** | 草稿版本可写可选 `jurisdiction` / `effectiveFrom` / `effectiveTo` / `legalReviewRef`；读路径回显；已提交/已批准版本不可改 |
| **LM-02 目录筛选** | `GET /api/management/v1/content-modules` 支持按上述字段筛选（见 K08-C7） |
| **LM-03 发布过期阻断** | 发布门禁新增硬项：任一模板引用的内容模块版本 `effectiveTo` 已过（相对 UTC now）→ checklist FAIL，禁止发布 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 强制填写法务字段 | Out of scope — 全部可选；空 `effectiveTo` = 无到期 |
| `effectiveFrom` 未到即阻断发布 | Out of scope — 本片仅阻断 **已过** `effectiveTo` |
| 运行期生成因过期失败 | Out of scope — 与 STOPPED/DEPRECATED 一致：已发布锁定版本继续可生成；阻断只作用于**新发布** |
| 自动停用过期模块 / 自动 bump | Out of scope — CE-U07 升版提醒正交；本片不做自动治理 |
| 新角色或新权限码 | Out of scope — 复用 `authorContentModules` / catalog browse / 既有 publish 能力 |
| 改写 CE-U07 / CE-U08 待办语义 | Out of scope |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 关注点 |
| --- | --- | --- |
| **条款作者** | `authorContentModules`（`TEMPLATE_AUTHOR` / `MASTER_DESIGNER` / `GROUP_ADMIN` / `GLOBAL_ADMIN`） | 在草稿版本维护法务元数据；目录筛选 |
| **模板编排人员** | 模板写权限 + publish 能力（既有矩阵） | 发布前看到过期条款门禁失败并修复引用 |
| **条款审批人 / 浏览者** | `decideContentModuleReviews` / catalog browse | 只读看见版本法务字段（结构权限边界不变） |
| **系统** | Content module CRUD + `PublishGateService` | 持久化、筛选、fail-closed 发布阻断 |

---

## 3. Goal

1. 作者能为内容模块**草稿版本**记录可选法务元数据，并在 detail/list 版本视图中读回。  
2. 运营/作者可在内容模块目录用管辖地、生效区间、法务评审单号缩小结果集。  
3. 模板发布时，若任一引用的内容模块版本已过 `effectiveTo`，门禁硬失败且发布动作被拒绝；原因对操作者可见（checklist + 稳定 messageKey）。

---

## 4. Preconditions / Trigger

**Preconditions**

- 用户已登录管理会话；目标组内可访问内容模块与/或待发布模板。  
- CE-K06 / P14 内容模块与 `CONTENT_MODULE_REFERENCES` 门禁已存在。  
- 模板存在至少一条 content-module reference（pinned semantic version）。

**Triggers**

- 创建/更新内容模块草稿版本（含法务字段）。  
- `GET /content-modules` 带法务筛选查询参数。  
- `GET …/templates/{id}/publish-gate` 评估或执行 publish。

---

## 5. Primary journey

1. 作者创建或编辑草稿版本，填写可选 `jurisdiction`、`effectiveFrom`、`effectiveTo`、`legalReviewRef` 并保存。  
2. 版本经审批成为 `APPROVED` + `ACTIVE` 后，模板引用该版本。  
3. 若随后该版本 `effectiveTo` 已过（或保存时已设为过去），编排人员打开发布门禁 → 新检查项 FAIL → 发布按钮/API 拒绝。  
4. 作者创建新版本（更新生效区间或换引用）后门禁恢复 PASS（在其它门禁亦满足时）。

---

## 6. System responses（success + fail-closed）

| 表面 | 响应 |
| --- | --- |
| **API — Version create/update** | `CreateContentModuleVersionRequest` / `UpdateContentModuleVersionRequest` 接受可选四字段；`ContentModuleVersionView` 回显；仅 `reviewState=DRAFT` 可写 |
| **API — Validation** | 两日期均非空且 `effectiveFrom` > `effectiveTo` → `422` / `CONTENT_MODULE_VALIDATION_FAILED`（或既有校验码）+ 字段级错误；字符串超长 → 422 |
| **API — Catalog** | `listContentModules` 可选 query：`jurisdiction`、`legalReviewRef`、`effectiveFrom`、`effectiveTo`（语义见 K08-C7）；与既有 `search`/`groupCode`/分页组合；越权组仍空页/403 不变 |
| **Publish gate** | 新 `PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_EXPIRED`（硬阻断）；任一引用版本满足 `effectiveTo != null && utcNow.isAfter(effectiveTo)` → item **FAIL**；否则 **PASS**（含 `effectiveTo` 空） |
| **Publish apply** | 门禁存在该 FAIL 时不得发布（与其它硬项一致） |
| **UI（同片）** | 版本对话框可编辑四字段；目录筛选项对齐 API；发布 checklist 展示该门禁项文案（English-first i18n） |

---

## 7. 已确认决策（locks）

| ID | 决策 |
| --- | --- |
| **K08-C1** | 四字段均为**可选**；落在 **version** 行（`content_module_version`），不在 module header。 |
| **K08-C2** | 类型：`jurisdiction` / `legalReviewRef` = 非空时可 trim 的短文本（建议 max 128 / 128）；`effectiveFrom` / `effectiveTo` = `date-time`（Instant，UTC，OpenAPI `format: date-time`）。 |
| **K08-C3** | 仅 `DRAFT` 可写；`SUBMITTED` / `APPROVED` 更新法务字段 → 拒绝（422 或既有状态机拒绝）；变更须新版本。 |
| **K08-C4** | 日期校验：两者皆非空时要求 `effectiveFrom <= effectiveTo`；相等允许（零长度区间仍有效至该 Instant）。 |
| **K08-C5** | 过期判定：`effectiveTo == null` → 永不过期；否则 `Instant.now(UTC).isAfter(effectiveTo)` 为过期。相等时刻**未**过期。 |
| **K08-C6** | **仅** `effectiveTo` 过期进入发布硬门禁；未来 `effectiveFrom`（尚未生效）**不**阻断本片发布。 |
| **K08-C7** | 目录筛选作用于 `GET /content-modules`。匹配版本 = 该模块的 **catalog filter version**：优先最新 `APPROVED`+`ACTIVE`；若无则取最新版本（`createdAt`/`semanticVersion` 与现网「当前版本」展示一致的规则）。筛选语义：`jurisdiction` / `legalReviewRef` = 大小写不敏感 **exact**（trim 后）；`effectiveFrom` query = filterVersion.effectiveFrom >= param（若 filterVersion 该字段 null 则不匹配该条件）；`effectiveTo` query = filterVersion.effectiveTo <= param（null 不匹配）。未传的筛选参数忽略。 |
| **K08-C8** | 新检查码 `CONTENT_MODULE_EFFECTIVE_EXPIRED`（独立于 `CONTENT_MODULE_REFERENCES`）；硬阻断；详情可列 moduleCode + semanticVersion + effectiveTo（非敏感）。 |
| **K08-C9** | 已发布 release 运行期**不**因随后过期而失败；与模块 STOPPED/DEPRECATED「已锁定仍可生成」一致。 |
| **K08-C10** | 无新权限；无权浏览目录仍 403；无权写模块仍 403。 |
| **K08-C11** | OpenAPI / Flyway / i18n messageKey 与本规格同步为同片交付（实现阶段）；正式字段以 OpenAPI 更新为准。 |
| **K08-C12** | 管理 UI：版本创建/编辑暴露四字段；目录工具条增加对应筛选；发布门禁面板消费新 checkCode（English-first）。 |

---

## 8. Acceptance scenarios

### BDD-CE-K08-LM-001 — 草稿版本写入法务元数据

```gherkin
Given 会话持有 authorContentModules 且模块 MOD-A 存在 DRAFT 版本 1.0.0
When 作者 PUT/更新该版本并提交 jurisdiction="England and Wales"、effectiveFrom、effectiveTo、legalReviewRef="LR-2026-001"
Then 响应 ContentModuleVersionView 回显上述四字段
And 持久化后再次 GET detail 仍可见相同值
```

### BDD-CE-K08-LM-002 — 全部法务字段可省略

```gherkin
Given 作者创建新草稿版本且不传四法务字段（或显式 null）
When 保存成功
Then View 中四字段为 null/缺省
And 该版本不因缺少法务字段而校验失败
```

### BDD-CE-K08-LM-003 — effectiveFrom 晚于 effectiveTo 被拒绝

```gherkin
Given 草稿版本更新请求 effectiveFrom 严格晚于 effectiveTo
When 提交保存
Then 返回 422 校验错误（CONTENT_MODULE_VALIDATION_FAILED 或等价）
And 版本数据不被部分更新为非法区间
```

### BDD-CE-K08-LM-004 — 非草稿不可改法务元数据

```gherkin
Given 版本 reviewState 为 SUBMITTED 或 APPROVED
When 调用方尝试更新任一法务字段
Then 请求被拒绝且原字段不变
```

### BDD-CE-K08-LM-005 — 目录按 jurisdiction 筛选

```gherkin
Given 模块 A 的 catalog filter version jurisdiction="England and Wales"
And 模块 B 的 catalog filter version jurisdiction="Hong Kong"
When 调用 GET /content-modules?jurisdiction=England%20and%20Wales
Then 结果仅包含模块 A（及同 jurisdiction 匹配项）
And 不包含模块 B
```

### BDD-CE-K08-LM-006 — 目录按 legalReviewRef 筛选

```gherkin
Given 模块 A filter version legalReviewRef="LR-2026-001"
When GET /content-modules?legalReviewRef=LR-2026-001
Then 结果包含模块 A
And 其它不同 legalReviewRef 的模块不出现
```

### BDD-CE-K08-LM-007 — 目录按生效区间筛选

```gherkin
Given 模块 A filter version effectiveFrom=T1、effectiveTo=T2（均非空）
When GET 带 effectiveFrom/effectiveTo 查询参数（按 K08-C7）
Then 仅返回 filter version 满足区间条件的模块
And effectiveFrom/To 为 null 的版本在对应条件激活时不匹配
```

### BDD-CE-K08-LM-008 — 发布门禁：引用已过 effectiveTo → FAIL

```gherkin
Given 模板引用内容模块版本 V，且 V.effectiveTo 非空且 utcNow.isAfter(V.effectiveTo)
When 评估 GET publish-gate（或等价发布前检查）
Then checklist 存在 checkCode=CONTENT_MODULE_EFFECTIVE_EXPIRED 且状态 FAIL
And 发布 apply 被拒绝
```

### BDD-CE-K08-LM-009 — 发布门禁：无 effectiveTo → PASS（本项）

```gherkin
Given 模板引用版本 W，且 W.effectiveTo 为空
And 其它门禁项均满足
When 评估 publish-gate
Then CONTENT_MODULE_EFFECTIVE_EXPIRED 为 PASS（或不因本规则失败）
```

### BDD-CE-K08-LM-010 — 发布门禁：effectiveTo 等于 now → 未过期 PASS

```gherkin
Given 引用版本 effectiveTo == utcNow（相等）
When 评估 publish-gate
Then 本检查项不因「已过期」失败（K08-C5）
```

### BDD-CE-K08-LM-011 — 未来 effectiveFrom 不阻断发布

```gherkin
Given 引用版本 effectiveFrom 在未来，effectiveTo 为空或仍在未来
When 评估 publish-gate
Then 不因 effectiveFrom 未到而 FAIL CONTENT_MODULE_EFFECTIVE_EXPIRED
```

### BDD-CE-K08-LM-012 — 已发布锁定版本运行期不受事后过期影响

```gherkin
Given 模板已发布并锁定引用版本 V
And 之后 V.effectiveTo 变为过去（或时钟跨越 effectiveTo）
When 运行期按已发布版本生成文档
Then 生成不因本切片过期规则失败（阻断仅针对新发布）
```

### BDD-CE-K08-LM-013 — 与 CONTENT_MODULE_REFERENCES 正交

```gherkin
Given 引用完整且结构可解析，但 effectiveTo 已过期
When 评估 publish-gate
Then CONTENT_MODULE_REFERENCES 可仍为 PASS
And CONTENT_MODULE_EFFECTIVE_EXPIRED 为 FAIL
```

### BDD-CE-K08-LM-014 — 无目录浏览权限 fail-closed

```gherkin
Given 会话无 content-module catalog browse 权限（如 TEMPLATE_TESTER）
When GET /content-modules（含或不含法务筛选）
Then 返回 403
And 不泄露模块法务元数据
```

### BDD-CE-K08-LM-015 — 管理 UI 可维护与筛选（同片）

```gherkin
Given 作者打开内容模块版本创建/编辑对话框
When 填写法务四字段并保存
Then UI 回显成功且与 API 一致
And 目录页可通过 jurisdiction / legalReviewRef / 生效区间筛选看到预期行
And 模板发布 checklist 对过期引用展示 CONTENT_MODULE_EFFECTIVE_EXPIRED 失败文案（en 基线）
```

---

## 9. Boundary / exception

| 场景 | 行为 |
| --- | --- |
| 空白字符串 jurisdiction / legalReviewRef | 归一为 null（trim 后 empty → null） |
| 多引用中任一过期 | 整项 FAIL；详情列出全部过期引用 |
| 引用缺失/空结构 | 既有 `CONTENT_MODULE_REFERENCES` / structure 规则优先或并行；本码只评价「已解析到的版本实体」的 `effectiveTo` |
| 时区 | 一律 UTC Instant；UI 可本地展示但比较用 UTC |
| 筛选 + search/groupCode | AND 组合；分页 totalElements 反映过滤后集合 |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | VersionView 字段；list query 过滤结果；publish-gate item `CONTENT_MODULE_EFFECTIVE_EXPIRED` |
| DB | Flyway 新列于 `content_module_version`（实现阶段，建议 V62+） |
| Tests | TDD：service/controller/publish-gate 单测覆盖 LM-001…014；FE Vitest/E2E 覆盖 LM-015 |
| Audit | 不要求新审计事件类型；版本更新走既有 content-module 更新审计（若已有） |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-K08 plan §3 | 目标行为 |
| Task Master **#63** | 执行任务 |
| P14 content module + `CONTENT_MODULE_REFERENCES` | 既有引用与门禁基线 |
| CE-U07 | 升版提醒正交；本片不替代 |
| permission-matrix §5.1 | 无新权限 |

**bdd_readiness: ready**

**open_questions:** none（locks K08-C1…C12 已由计划手递与本规格确认）
