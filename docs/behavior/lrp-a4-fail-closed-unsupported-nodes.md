# BDD 行为规格：LR-A4 — 无 Writer 结构化节点 fail-closed（禁止静默省略）

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-10  
**BDD ID**: `BDD-LRP-A4-FAIL-CLOSED-001`  
**来源任务**: [LRP Wave LR-A § LR-A4 — Unsupported-node fail-closed closure](../plan/detail/LRP-A-rendering-trust-hardening.md)  
**程序发现**: [launch-readiness-program.md](../plan/launch-readiness-program.md) §1 finding 11  
**行业坑**: [CDP-industry-pitfall-registry.md](../plan/detail/CDP-industry-pitfall-registry.md) — **CD-PIT-07**（rich-text / 节点矩阵边界；本切片收口「声明可写但无 DOCX 发射」类静默丢内容）  
**门禁对齐**: [launch-readiness-gate.md](../plan/launch-readiness-gate.md) — LR-A4「unsupported structured node types fail closed; no silent content loss」  
**Task Master / slice**: `lrp-a4-fail-closed-nodes` / plan `LR-A4`  
**上游**: CORE-FORTRESS **F1-A2** 已关闭 `contentModuleRef` 空 pinned 静默 return（[BDD-CORE-FORTRESS-F1-001](./core-fortress-f1-rendering-correctness.md)）；本规格 **不重做** A2，仅回归 + 收口剩余 silent-omit 路径  
**产品锁定（用户 2026-07-10「按你建议来」）**: 对 `qrBarcodeRef` / `attachmentListRef`（及同类无 writer 路径）**在发布门禁硬阻断（fail-closed）**，本切片 **不** 实现完整 QR/附件列表 writer；**禁止** 在 publish/render 静默省略内容；若已有部分 writer 实现则保留，但仍须消除一切 silent-loss 路径

---

## 1. 概述

模板编排人员在锚点绑定中可插入 v1 节点矩阵已声明的引用节点。其中部分类型（当前：`qrBarcodeRef`、`attachmentListRef`）**已进入 schema / UI / `ReferenceNodeService` 校验**，但 **`StructuredContentDocxWriter` 无 DOCX 发射分支**。若仅在运行时偶发失败、或嵌套路径静默跳过，已发布信函会出现 **无痕迹内容丢失**（CD-PIT-07 相邻）。

| 行为域 | 摘要 |
| --- | --- |
| **D1 发布门禁硬阻断** | 任一绑定树含 **writer-unsupported** 节点 → publish checklist **FAIL**；不得进入 `PUBLISHED` |
| **D2 绑定/校验期可见** | 保存或 `validateBindings` 路径将此类节点标为 **blocker**（可翻译 messageKey + location），并汇入锚点完整性 / 阻断项状态 |
| **D3 渲染纵深防御** | 预览 / 测试生成 / 运行时生成遇到此类节点 → **显式失败**（统一信封 + `api.error.rendering.unsupportedNodeType`）；**含嵌套**（condition/loop/module 展开后） |
| **D4 无静默省略** | 禁止「跳过节点继续写其余内容且无 warning/error」；空 `contentModuleRef` pinned 保持 F1 fail-closed |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 实现 `qrBarcodeRef` / `attachmentListRef` 完整 DOCX writer | **Deferred** — 另任务；本切片硬阻断 |
| 病毒扫描、CD-E2E T02–T12、LR-A7 测量、DGE-audit-governance | Out of scope |
| 发明新节点类型或削弱既有 `UNSUPPORTED_NODE`（未知 type）校验 | Forbidden |
| 重开 P18/P22 / F1 阶段状态 | Forbidden |
| 将 writer-unsupported 降级为「仅 fidelity warning、仍可发布」 | Forbidden（与锁定决策冲突） |

---

## 2. Current vs target（gap-close）

| 能力 | 当前（2026-07-10 worktree 勘察） | 目标（本规格） | 缺口 |
| --- | --- | --- | --- |
| 顶层 `qrBarcodeRef` / `attachmentListRef` 渲染 | ✅ `StructuredContentDocxWriteSession` 抛 `DocxAssemblyException` + `api.error.rendering.unsupportedNodeType` | 保持 | — |
| 嵌套路径（`conditionBlock` / `loopBlock` / `contentModuleRef` 展开 / `writeInlineOrBlockChildren` / `writeInlineNode`） | ❌ 未统一走 `isUnsupportedRenderableType`；行内路径可 **静默跳过** | **任何深度** 遇 writer-unsupported → 同 D3 显式失败 | **缺口** |
| `NodeMatrixValidationService` | 仅对 **未知** `type` 发 `UNSUPPORTED_NODE` blocker；矩阵内 qr/attachment **放行** | 对 **writer-unsupported 声明类型** 亦发 blocker（或等价专用校验） | **缺口** |
| `ReferenceNodeService` | 校验 `referenceKey` 存在即视为可引用；**不**标记「无 writer」 | 可继续校验 key；**不得**暗示可发布；阻断由矩阵/门禁承担 | 文档对齐；可选增强 |
| `PublishGateService` | 无专用 unsupported-node 检查项；依赖绑定 blockers → `ANCHOR_INTEGRITY` / `BLOCKER_STATUS`（但绑定当前不拦 qr/attachment） | **硬阻断发布**；checklist 可见原因（专用 check **或** 绑定 blocker 汇入既有项，见 §4 锁定） | **缺口** |
| `contentModuleRef` 空 pinned | ✅ F1：`CONTENT_MODULE_STRUCTURE_MISSING` + 发布门禁 `CONTENT_MODULE_REFERENCES` | 保持；回归 | — |
| 未知节点 type | ✅ 矩阵 blocker + 不得静默写 | 保持 | — |
| 前端 i18n | ✅ `api.error.rendering.unsupportedNodeType`；缺 publishGate 专用文案（若新增 check） | 英基 + zh-CN 对齐 | 视实现选型 |
| 完整 QR/附件 writer | ❌ 无 | **不做** | 仅文档 deferred |

---

## 3. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **模板编排人员** | `TEMPLATE_AUTHOR` / `MASTER_DESIGNER` / 具备模板写权限的 admin | 在绑定中插入结构化节点；查看门禁 checklist |
| **模板测试人员** | `TEMPLATE_TESTER` | 预览/测试生成；应看到显式失败而非「成功但缺段」 |
| **审批 / 发布操作者** | `TEMPLATE_APPROVER` / 具备 publish 权限角色 | 发布前 checklist；存在阻断项时不得发布 |
| **运行时 API 调用方** | API 凭证 | 若错误地发布了含此类节点的版本（防御纵深）→ 生成显式失败 |
| **系统（发布门禁）** | `PublishGateService` | 聚合校验，fail-closed |

授权：跨组 / 无权限 → 既有 fail-closed；本规格不改权限矩阵。

---

## 4. 已确认决策（锁定默认）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **LR-A4-C1** | `qrBarcodeRef`、`attachmentListRef`：**硬阻断发布**，本切片 **不** 实现完整 writer | 用户确认「按你建议来」+ LRP-A 选项 (b) |
| **LR-A4-C2** | **禁止** publish / preview / runtime render **静默省略** 任何 writer-unsupported 或未知节点 | CD-PIT-07；launch-readiness-gate LR-A4 |
| **LR-A4-C3** | 「不支持节点」发布阻断 = **未知 type** ∪ **矩阵已声明但当前无 DOCX writer 的类型**（writer-unsupported set） | 澄清 PRD §6.5 / requirements「结构化富文本包含不支持节点」 |
| **LR-A4-C4** | 当前 writer-unsupported set = `{ qrBarcodeRef, attachmentListRef }`；实现须 **单一权威集合**（校验与 writer 共用或同源），避免漂移 | 本规格 |
| **LR-A4-C5** | 发布门禁表面：**优先** 新增 `PublishGateCheckCode.UNSUPPORTED_STRUCTURED_NODES`（或等价名）+ checklist messageKeys；**可接受等价**：绑定期 blocker → `ANCHOR_INTEGRITY` / `BLOCKER_STATUS` 已阻断且 checklist/详情能展示稳定 messageKey 与节点 location。禁止「仅日志、UI 无原因」 | 计划「extend PublishGateService」+ COR-T01 模式 |
| **LR-A4-C6** | 渲染错误：保持 / 统一 `api.error.rendering.unsupportedNodeType`；category 与既有 `DocxAssemblyException` 映射一致；retryable=`false` | 现有测试与 i18n |
| **LR-A4-C7** | 校验 blocker：可复用 `FidelityWarningCode.UNSUPPORTED_NODE` + `generation.warning.fidelity.unsupportedNode`，**或** 新增专用 messageKey（若新增须 en+zh 同步）；severity 必须为 **BLOCKER**，不得 WARNING | 本规格 |
| **LR-A4-C8** | `contentModuleRef` 空/blank pinned：保持 F1 `CONTENT_MODULE_STRUCTURE_MISSING` + `api.error.validation.contentModuleStructureMissing`；**不得**改回 silent return 或「仅 warning 仍生成」 | F1 Done 回归 |
| **LR-A4-C9** | 若某类型日后补齐 writer：从 writer-unsupported set 移除后，门禁与渲染对该类型恢复放行；**不在本切片做** | Deferred |
| **LR-A4-C10** | 前端作者区可继续展示节点调色板条目（既有 i18n）；插入后须在绑定状态 / 门禁中 **可见阻断**，不得静默成功发布 | UX 诚实 |

---

## 5. Goal

1. 含 writer-unsupported 节点的模板版本 **不能发布**。  
2. 预览 / 测试 / 运行时生成遇此类节点 **显式失败**，用户可见稳定 messageKey。  
3. **任意嵌套深度** 无 silent-omit 路径。  
4. 空 `contentModuleRef` pinned 与未知 type 的既有 fail-closed **不回归**。  
5. Happy path：仅含已支持 writer 节点的模板，发布与生成行为与加固前一致。

---

## 6. Trigger

| # | 触发 | 表面 |
| --- | --- | --- |
| T1 | 保存 / 更新锚点绑定（含 structured content） | Management API + 编排 UI |
| T2 | 查询 / 断言发布门禁 checklist | `PublishGateService.evaluate` / lifecycle publish |
| T3 | 预览或测试生成 DOCX/PDF | Preview / test generation |
| T4 | 运行时同步/异步/批量生成 | Runtime generation API |

---

## 7. Preconditions

- P22-T01/T02 / P18 节点矩阵与 writer 基线 **Done**（LR-A4 依赖已满足）。  
- F1-A2 contentModuleRef fail-closed **Done**。  
- 操作者对目标模板具备相应写 / 测 / 发布权限。  
- 模板版本处于可编辑或可评估门禁的生命周期状态（既有状态机不变）。

---

## 8. Primary journey

### 8.1 成功路径（无 unsupported）

1. 编排人员绑定仅含已支持 writer 的节点（paragraph、list、table、imageRef、sealRef、contentModuleRef+合法 pinned 等）。  
2. 绑定校验无 unsupported blocker。  
3. 其它门禁项通过后，publish checklist 中 unsupported 相关项 **ready**（或无该项且锚点完整性通过）。  
4. 发布成功；运行时生成产出完整 DOCX，无 `unsupportedNodeType`。

### 8.2 失败路径（含 unsupported — 期望）

1. 编排人员在任一锚点树插入 `qrBarcodeRef` 或 `attachmentListRef`（顶层或嵌套）。  
2. 绑定校验出现 **blocker**（messageKey + location）。  
3. 发布门禁对应检查项 **FAIL**；`assertReady` / publish transition **拒绝**。  
4. 若仍触发生成（预览/测试/误发布防御）：返回显式错误信封，**不**产出「缺段成功文档」。

---

## 9. System responses

### 9.1 发布门禁阻断

| 条件 | 行为 |
| --- | --- |
| 版本任一绑定含 writer-unsupported 节点 | Checklist 项失败；`blocker=true`；稳定 messageKey（见 §12） |
| 操作者尝试 publish / 进入 `PUBLISHED` | 拒绝；既有 lifecycle 错误信封（publish gate not ready） |

### 9.2 渲染 / 预览失败

| 条件 | HTTP / 信封 | messageKey |
| --- | --- | --- |
| 生成路径遇到 writer-unsupported（任意深度） | 既有渲染/组装错误映射（通常 422 或既有 DocxAssembly 映射） | **`api.error.rendering.unsupportedNodeType`** |
| 空 contentModuleRef pinned | 既有 | **`api.error.validation.contentModuleStructureMissing`** + code `CONTENT_MODULE_STRUCTURE_MISSING` |

### 9.3 成功

- 无 writer-unsupported、无空 pinned、未知 type 已拦：既有 2xx + 完整产物。

---

## 10. Acceptance scenarios（Given / When / Then）

### A1 — 发布门禁阻断 `qrBarcodeRef`

- **Given** 模板版本某锚点 `structured_content_json` 的 `nodes`（或任意嵌套 `children`）含 `"type":"qrBarcodeRef"` 且 `referenceKey` 非空  
- **When** 评估发布门禁（`evaluate` / publish assert）  
- **Then** 门禁 **未 ready**；checklist 暴露可翻译阻断原因（专用 `UNSUPPORTED_STRUCTURED_NODES` **或** 锚点完整性/阻断项汇入，且可关联到该节点）  
- **And** 生命周期 **不得** transition 到 `PUBLISHED`

### A2 — 发布门禁阻断 `attachmentListRef`

- **Given** 同 A1，节点类型为 `attachmentListRef`  
- **When** 评估发布门禁  
- **Then** 与 A1 相同：硬阻断 + 可见原因；不得发布

### A3 — 绑定校验期 blocker（早发现）

- **Given** 草稿版本绑定含 `qrBarcodeRef` 或 `attachmentListRef`  
- **When** 保存绑定或执行 `validateBindings` / `computeBindingStatus`  
- **Then** 出现 **BLOCKER** 级 fidelity/绑定问题（`UNSUPPORTED_NODE` 或等价）；绑定状态进入会阻断发布的状态（如 `INCOMPATIBLE_CONTENT_TYPE` 或汇总 `blocking=true`）  
- **And** issue 含稳定 `messageKey` 与节点 `location`（无敏感业务明文）

### A4 — 顶层渲染 fail-closed（回归）

- **Given** 结构化 JSON 根 `nodes` 含顶层 `qrBarcodeRef` 或 `attachmentListRef`  
- **When** `StructuredContentDocxWriter` / `DocxAssembler` 生成  
- **Then** 抛出/映射为失败；`messageKey` = `api.error.rendering.unsupportedNodeType`  
- **And** **不**产出省略该节点后的「成功」DOCX

### A5 — 嵌套渲染 fail-closed（缺口收口）

- **Given** `qrBarcodeRef` 或 `attachmentListRef` 位于 `conditionBlock` / `loopBlock` 的 `children`、或 `contentModuleRef` 展开后的 pinned 结构、或经 `writeInlineOrBlockChildren` / `writeInlineNode` 可达路径  
- **When** 生成 DOCX（条件为真 / 循环至少一次 / 模块展开成功）  
- **Then** 与 A4 相同：显式 `api.error.rendering.unsupportedNodeType`（或同义统一 key）  
- **And** **不得** 静默跳过该子节点继续写其余内容

### A6 — 未知节点 type 仍阻断（非回归）

- **Given** 节点 `"type":"rawHtml"`（或不在 `StructuredContentNodeType` 的任意值）  
- **When** 节点矩阵校验 / 发布门禁  
- **Then** `UNSUPPORTED_NODE` blocker；不得发布  
- **And** 渲染路径不得静默省略

### A7 — `contentModuleRef` 空 pinned 仍 fail-closed（F1 回归）

- **Given** `contentModuleRef` 的 pinned structure 为 null/blank  
- **When** 生成 DOCX  
- **Then** `CONTENT_MODULE_STRUCTURE_MISSING` / `api.error.validation.contentModuleStructureMissing`  
- **And** 发布门禁 `CONTENT_MODULE_REFERENCES`（或等价）仍阻断空 pinned  
- **And** **无** silent return

### A8 — Happy path 不受影响

- **Given** 绑定仅含已支持 writer 的节点（含合法 `contentModuleRef`+pinned、`imageRef`/`sealRef` 等）  
- **When** 校验、发布门禁、生成  
- **Then** 无 unsupported 阻断；生成成功；行为与本切片前一致

### A9 — Writer-unsupported set 权威一致

- **Given** 实现中的校验集合与 writer 拒绝集合  
- **When** 审查代码 / 契约测试  
- **Then** 对 `{qrBarcodeRef, attachmentListRef}` 两边一致；不存在「校验放行但 writer 静默跳过」或「校验放行且 writer 抛错但门禁仍 ready」的组合

---

## 11. Boundary / exception

| ID | 边界 | 行为 |
| --- | --- | --- |
| B1 | `referenceKey` 缺失的 qr/attachment | 既有 `MISSING_REFERENCE_KEY` blocker **且/或** writer-unsupported 阻断；不得因缺 key 而静默跳过 |
| B2 | 条件为假导致含 unsupported 的分支未执行 | 生成可不触发 A5；**但** 绑定/门禁仍须按树静态检测阻断发布（不依赖运行时条件求值） |
| B3 | 循环变量为空列表 | 同 B2：静态树检测仍阻断发布 |
| B4 | 仅 UI 调色板展示、未写入 JSON | 无阻断（无节点则无问题） |
| B5 | 跨组访问模板 | 既有 403 fail-closed |
| B6 | 已发布历史版本若含此类节点（数据异常） | 运行时生成仍 D3 fail-closed；修复走新 dev 线（本切片不写迁移清扫，除非实现顺带发现） |
| B7 | 未来补齐 writer | 移出 unsupported set 后 A1–A5 对该类型不再适用（另任务） |

---

## 12. Expected messageKeys

| Key | 用途 | 状态 |
| --- | --- | --- |
| `api.error.rendering.unsupportedNodeType` | 预览/测试/运行时渲染失败 | **已有**（en + frontend catalogs）；保持 |
| `generation.warning.fidelity.unsupportedNode` | 校验 blocker 文案（未知 type 与/或 writer-unsupported） | **已有**；severity=BLOCKER |
| `api.error.validation.contentModuleStructureMissing` | 空 pinned 模块 | **已有**（F1） |
| `api.publishGate.contentModuleReferences.blocked` / `.ready` | 模块引用门禁 | **已有**（F1） |
| `api.publishGate.unsupportedStructuredNodes.blocked` | 专用门禁项失败文案（若选 LR-A4-C5 专用 check） | **新增**（实现时 en properties + frontend 映射 + zh-CN） |
| `api.publishGate.unsupportedStructuredNodes.ready` | 专用门禁项通过 | **新增**（同上） |
| `api.publishGate.anchorIntegrity.blocked` / `api.publishGate.blockerStatus.blocked` | 若采用「绑定 blocker 汇入」等价路径 | **已有**；须确保 UI 仍能展示底层 unsupported messageKey |

错误码（渲染）：与既有 `DocxAssemblyException` / advice 映射一致；不强制新 `ApiErrorCodes` 常量，除非实现需要与 `CONTENT_MODULE_STRUCTURE_MISSING` 同级显式码——若新增，须 OpenAPI/信封一致且 retryable=false。

---

## 13. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 单元测试 | 扩展 `PublishGateServiceTest`（或专用）；`NodeMatrixValidationServiceTest` / 绑定校验；`StructuredContentDocxWriterTest` + `DocxAssemblerTest` 嵌套 A5 |
| API | 门禁 checklist JSON：`checkCode`、`blocker`、`messageKey`；publish 拒绝信封 |
| 生成 | 失败信封含 `api.error.rendering.unsupportedNodeType` + `traceId` |
| 门禁命令 | `mvn -B -ntp -f backend/pom.xml verify` |
| 前端（若新增 gate key） | `apiErrorEn` / `apiErrorZhCn` 或 publishGate i18n；checklist 可见 |
| 手工/Docker（实现后） | 插入 qr 节点 → 门禁红；移除后可发布 |

---

## 14. Traceability

| 文档 | 关系 |
| --- | --- |
| `docs/plan/detail/LRP-A-rendering-trust-hardening.md` § LR-A4 | 任务定义 |
| `docs/plan/launch-readiness-program.md` finding 11 | 问题来源 |
| `docs/plan/detail/CDP-industry-pitfall-registry.md` CD-PIT-07 | 行业坑 |
| `docs/behavior/core-fortress-f1-rendering-correctness.md` F1-A2 | 空 pinned 已关闭；本规格回归 |
| `docs/product/PRD.md` §6.5 | 「结构化富文本包含不支持节点」发布阻断 — 本规格澄清含 writer-unsupported |
| `docs/requirements/requirements-plan.md` | 同上已确认发布阻断项 |
| `docs/domain/domain-model.md` §2.6.1–2.6.5 | 节点矩阵与引用节点 — 本规格补充 writer-unsupported 语义 |
| `docs/security/permission-matrix.md` | 无权限变更 |

---

## 15. Open questions

| ID | 问题 | 状态 |
| --- | --- | --- |
| — | （无）产品路径已锁定为发布硬阻断；实现选型（专用 check vs 绑定汇入）见 **LR-A4-C5**，**不阻塞** `ready` | Resolved by locked defaults |

病毒扫描、完整 QR/附件 writer、CD-E2E 其余旅程、LR-A7：**明确非本切片**，不列入阻塞 OQ。

---

## 16. BDD readiness

```
bdd_readiness: ready
task_ids: [LR-A4, lrp-a4-fail-closed-nodes]
owning_doc: docs/behavior/lrp-a4-fail-closed-unsupported-nodes.md
open_questions: []
locked_decision: publish-gate hard-block for writer-unsupported nodes; no silent omit; writers deferred
writer_unsupported_set: [qrBarcodeRef, attachmentListRef]
```

**交给下一阶段**：`plan-orchestrator` 按 §2 缺口拆 TDD 任务；推荐实现者见 handoff（**backend-engineer** 主责门禁+校验；writer 嵌套 fail-closed 同切片内完成）。
