# BDD 行为规格：CORE-FORTRESS Phase F1 — 渲染内核正确性

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-08  
**BDD ID**: `BDD-CORE-FORTRESS-F1-001`  
**来源**: 全栈深度审查 + 用户确认 CORE-FORTRESS 纲领，F1 为启动重心

---

## 1. 概述

本规格定义 **CORE-FORTRESS Phase F1** 的三项渲染内核正确性改造。目标：消除双轨渲染、静默内容丢失、Demo 级图片接缝——使结构化内容 → DOCX 保真路径 **单一、可验证、fail-closed**。

| 工作流 | 改造要点 |
| --- | --- |
| **F1-A1 双轨渲染器统一** | 合并 `StructuredContentDocxWriter`（POI 真保真）与 `DocxAssembler` 纯文本降级路径；正文、表格单元格、页眉、页脚使用同一引擎 |
| **F1-A2 contentModuleRef fail-closed** | 空 `pinnedStructure` 不再静默 return；抛出结构化错误；发布门禁提前拦截 |
| **F1-A3 图片/签章解析器生产化** | `StructuredContentImageResolver` 成为 Spring bean，注入 `ObjectStoragePort`（MinIO）；缺失资源 fail-closed |

**与现有代码的关系**

| 现有资产 | 本规格用法 |
| --- | --- |
| `StructuredContentDocxWriter` | **Extend + unify** — 成为唯一权威结构化→DOCX 树遍历引擎 |
| `DocxAssembler` 内 plain-text renderer | **Replace** — 移除独立实现；所有锚点区域调用 writer |
| `expandContentModule` 静默 return | **Replace** — fail-closed 抛错 |
| `StructuredContentImageResolver` classpath-only | **Replace** — MinIO 主路径 + 显式 demo fallback tier |
| `PublishGateService` | **Extend** — 拦截 A2 类错误；A3 缺失资源在发布/预览期可警告或阻断（见 §6） |
| `FidelityValidationService` | **Reuse** — A1 完成后保真校验与 writer 行为一致 |

---

## 2. Actor / Role

| Actor | 说明 | 权限 |
| --- | --- | --- |
| **模板编排人员** | 配置锚点绑定、结构化内容、内容模块引用 | 模板写 + 组范围 |
| **模板测试人员** | 预览/测试生成，验证 DOCX 保真 | 测试决策权限 |
| **运行时 API 调用方** | 同步/异步/批量生成 DOCX/PDF | API 凭证 + AD Group |
| **系统（发布门禁）** | 发布前聚合校验，阻断不可发布版本 | `PublishGateService` |

---

## 3. Goal

1. **A1**：结构化内容在正文、表格单元格、页眉、页脚中 **保真一致**；不存在第二套降级渲染逻辑。
2. **A2**：内容模块引用缺失 pinned 结构时 **显式失败**，发布期阻断，运行时绝不静默丢段。
3. **A3**：图片/签章从对象存储解析；缺失时 **显式失败**（与 `qrBarcodeRef` 同级），不静默跳过。

---

## 4. 已确认决策（2026-07-08）

| ID | 决策 |
| --- | --- |
| **F1-C1** | A1 重构 **必须先** 建立 POI 保真回归断言网（含 writer 专属测试），断言网绿后才允许合并双轨 |
| **F1-C2** | 单一引擎权威输出为 DOCX；若仍需纯文本投影，必须由 DOCX 树 **派生**，不得独立维护条件/循环/表格逻辑 |
| **F1-C3** | A2 错误码：`CONTENT_MODULE_STRUCTURE_MISSING`；category `VALIDATION`；retryable `false` |
| **F1-C4** | A2 发布门禁：绑定校验阶段检测空 pinned → checklist 项 **FAIL**，阻止进入 `PUBLISHED` |
| **F1-C5** | A3 解析顺序：MinIO（按 storage key）→ 显式 demo classpath tier（仅 `rendering/demo-images/` 且须配置 flag 或 profile）→ fail-closed |
| **F1-C6** | A3 错误码：`IMAGE_ASSET_NOT_FOUND` / `SEAL_ASSET_NOT_FOUND`；category `RENDERING`；retryable `false` |
| **F1-C7** | A3 `StructuredContentImageResolver` 必须通过 Spring DI 注入 `DocxAssembler`，禁止 `new` 构造 |
| **F1-C8** | 本 Phase **不含** 表达式引擎扩展、LO 池化、运行时保真缓存（属 F2/F3/F4） |

---

## 5. 前置条件

- P23 Done：演示包银行级排版基线已建立。
- P22/P18 Done：`StructuredContentDocxWriter` 节点矩阵与 style catalog 已存在。
- MinIO 对象存储端口（`ObjectStoragePort`）已在 infrastructure 层可用。

---

## 6. 验收场景（Given / When / Then）

### F1-A1 — 双轨渲染器统一

#### BDD-F1-A1-001 — 正文锚点保真（基线回归）

**Given** 已发布模板版本，锚点绑定含 `styleRef` + `emphasis` + `conditionBlock` 结构化 JSON  
**When** 运行时同步生成 DOCX  
**Then** 输出 DOCX 中对应 run 含正确 `w:rPr`（bold/underline）与 style 引用  
**And** POI 断言通过（与 P23 回归套件一致）

#### BDD-F1-A1-002 — 表格单元格内结构化内容保真

**Given** 母版 DOCX 含表格，某单元格内锚点绑定 rich structured content（非纯文本）  
**When** 生成 DOCX  
**Then** 单元格内段落保留 styleRef/emphasis/list/table 节点渲染结果  
**And** **不得** 降级为 plain-text-only 输出（当前 `DocxAssembler` plainTextFallback 行为被消除）

#### BDD-F1-A1-003 — 页眉/页脚锚点保真

**Given** 母版页眉或页脚含锚点，绑定 structured content  
**When** 生成 DOCX  
**Then** 页眉/页脚区域与正文使用同一 writer 路径，保真一致

#### BDD-F1-A1-004 — 安全网先于重构

**Given** F1-A1 重构尚未开始  
**When** 执行 `StructuredContentDocxWriterTest`（新建）+ 扩展 `DocxAssemblerTest`  
**Then** 覆盖 node-type 分支、嵌套 loop numbering、table/list、header/footer/table-cell 场景  
**And** 全部测试在当前双轨代码下 **先红后绿**（TDD 顺序）

#### BDD-F1-A1-005 — 无双轨逻辑漂移

**Given** A1 合并完成  
**When** 静态扫描 `DocxAssembler`  
**Then** 不存在独立的 `renderNode`/`renderLoopBlock`/`SIMPLE_CONDITION_PATTERN` 纯文本渲染副本  
**And** 所有 structured content 注入点调用 `StructuredContentDocxWriter`

---

### F1-A2 — contentModuleRef fail-closed

#### BDD-F1-A2-001 — 运行时空 pinned 抛错

**Given** 绑定含 `contentModuleRef`，引用模块的 `pinnedStructure` 为空或 null  
**When** 生成 DOCX  
**Then** 返回错误 envelope：`error.code = CONTENT_MODULE_STRUCTURE_MISSING`  
**And** 不产出部分 DOCX（fail-closed）

#### BDD-F1-A2-002 — 发布门禁提前拦截

**Given** 模板版本处于 `TESTING` 或 `PENDING_RELEASE`，存在空 pinned 的 contentModuleRef 绑定  
**When** 执行发布门禁 checklist  
**Then** 对应检查项 **FAIL**  
**And** 版本不可 transition 到 `PUBLISHED`

#### BDD-F1-A2-003 — 正常 pinned 不受影响

**Given** contentModuleRef 指向模块且 `pinnedStructure` 非空合法 JSON  
**When** 生成 DOCX  
**Then** 模块内容正确展开嵌入  
**And** 无新增错误

---

### F1-A3 — 图片/签章解析器生产化

#### BDD-F1-A3-001 — MinIO 主路径解析

**Given** 绑定含 `imageRef` 或 `sealRef`，storage key 指向 MinIO 中存在的 PNG/JPEG  
**When** 生成 DOCX  
**Then** 输出 DOCX 含对应 `w:drawing` / inline image  
**And** POI 断言图片 relationship 存在

#### BDD-F1-A3-002 — 缺失资源 fail-closed

**Given** `imageRef`/`sealRef` 指向 MinIO 不存在的 key（且非 demo tier）  
**When** 生成 DOCX  
**Then** 返回 `IMAGE_ASSET_NOT_FOUND` 或 `SEAL_ASSET_NOT_FOUND`  
**And** **不得** 静默跳过（当前 `writeReferenceNode` return-on-null 行为被消除）

#### BDD-F1-A3-003 — Demo classpath 显式 fallback

**Given** profile 启用 demo image tier，key 映射到 `rendering/demo-images/` classpath 资源  
**When** 生成 DOCX  
**Then** 图片正确嵌入  
**And** fallback 路径在日志/诊断中可观测（非 silent）

#### BDD-F1-A3-004 — Spring DI 注入

**Given** 应用上下文启动  
**When** 检查 `DocxAssembler` 依赖  
**Then** `StructuredContentImageResolver` 为 Spring bean，注入 `ObjectStoragePort`  
**And** 不存在 `new StructuredContentImageResolver()` 直接构造

---

## 7. 边界与异常行为

| 场景 | 期望行为 |
| --- | --- |
| 结构化 JSON 语法错误 | 现有 `INVALID_STRUCTURED_CONTENT` 路径不变 |
| 不支持的 node type | 现有 fail-closed（如 `qrBarcodeRef`）不变 |
| 并发 MinIO 读取失败 | 映射为 `RENDERING` category 可重试错误（infra） |
| 预览 vs 最终生成 | A1/A2/A3 行为 **一致**；预览不得走降级路径 |

---

## 8. 可观测证据

| 证据类型 | 路径 / 命令 |
| --- | --- |
| Writer 专属单元测试 | `StructuredContentDocxWriterTest`（新建） |
| 集成测试 | 扩展 `DocxAssemblerTest`, `DocumentGenerationEngineTest` |
| POI 保真断言 | header/footer/table-cell 场景 POI XML 断言 |
| 门禁 | `mvn -B -ntp -f backend/pom.xml verify` |
| 计划同步 | `docs/plan/detail/CORE-FORTRESS-f1-rendering-correctness.md` |

---

## 9. 追溯性

| 文档 | 关联 |
| --- | --- |
| `docs/product/authoring-rendering-first-principles-review.md` | DOCX 保真为一阶原则 |
| `docs/architecture/module-boundaries.md` | rendering 模块隔离 |
| `docs/plan/launch-readiness-program.md` LR-A4 | 节点矩阵闭环（F1-A3 部分） |
| `docs/plan/competitiveness-deepening-program.md` CD-PIT-* | 生产渲染陷阱 |
| P23 demo typography BDD | 回归基线不得回退 |

---

## 10. 待确认问题

| ID | 问题 | 默认（若无异议） |
| --- | --- | --- |
| **F1-Q1** | A3 demo fallback 是否仅 `local`/`demo` profile 启用？ | 是 — 生产 profile 仅 MinIO |
| **F1-Q2** | A2 发布门禁为空 pinned 是 **FAIL** 还是 **WARN**？ | **FAIL** — 金融场景禁止静默丢段 |
