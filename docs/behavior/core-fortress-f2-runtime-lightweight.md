# BDD 行为规格：CORE-FORTRESS Phase F2 — 运行时轻量化

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-09  
**BDD ID**: `BDD-CORE-FORTRESS-F2-001`

---

## 1. 概述

F2 将不可变模板版本上的重复计算移到发布期固化，并优化生命周期与幂等热路径。

| 工作流 | 要点 |
| --- | --- |
| **F2-B1 保真警告发布缓存** | 发布时计算并持久化 `fidelityWarningCodes`；运行时读取缓存，不再全量重算 |
| **F2-B5 生命周期 bulk update** | `syncPublishedVersionsTo*` 改为单条 bulk SQL |
| **F2-B4 幂等 release 哈希缓存** | 已发布版本的 request hash 索引，避免逐版本 SHA-256 重算 |

---

## 2. 已确认决策

| ID | 决策 |
| --- | --- |
| **F2-C1** | 缓存列：`template_version.fidelity_warning_codes_json`（JSON 字符串数组） |
| **F2-C2** | 在 `publish` transition 成功时写入缓存；已发布版本内容不可变 |
| **F2-C3** | 运行时：`DocumentGenerationEngine` / `RuntimeGenerationService` 优先读缓存；DRAFT/TESTING 仍实时计算 |
| **F2-C4** | bulk update 使用 `@Modifying` repository 方法，同一事务 |
| **F2-C5** | 幂等：在 `IdempotencyService` 或 release 解析层缓存 `(templateId, releaseVersion) → requestHash` |

---

## 3. 验收场景

### BDD-F2-B1-001 — 发布固化警告

**Given** 模板版本含会产生 fidelity warning 的绑定  
**When** 发布成功  
**Then** `fidelity_warning_codes_json` 非空且与发布时 `FidelityValidationService` 结果一致

### BDD-F2-B1-002 — 运行时使用缓存

**Given** 已发布版本且缓存已写入  
**When** 同步生成 DOCX  
**Then** 不调用 `collectWarningCodesForVersion`（可通过 spy/mock 断言）  
**And** 响应 fidelity warnings 与缓存一致

### BDD-F2-B5-001 — bulk stop 已发布版本

**Given** 模板有 N 个 PUBLISHED 版本  
**When** 新发布触发 `syncPublishedVersionsToStopped`  
**Then** 单条 update 语句（repository 测试或 query 计数）

### BDD-F2-B4-001 — 幂等 default route 变更检测

**Given** 多 release 版本  
**When** 幂等 replay 需匹配 release  
**Then** 不逐版本重算 hash（使用预存或索引）
