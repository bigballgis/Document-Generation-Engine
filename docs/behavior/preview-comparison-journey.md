# BDD：预览与最终产物并排对比

**文件状态:** `ready` | **BDD ID:** `BDD-CDP-CMP-001` | **CDP:** CD-BDD-T07 → CD-E2E-T09

## Actor / Goal

**TEMPLATE_TESTER** 或 **TEMPLATE_APPROVER** 打开并排对比视图，按 warningCode/位置筛选差异。

## Acceptance

### BDD-CDP-CMP-001 — 打开对比

- **Given** 模板有最终路径预览记录 + DOCX/PDF 产物
- **When** 打开 comparison 面板
- **Then** 显示并排视图；可按 `warningCode` 筛选；差异定位到锚点/章节摘要

## 证据

`frontend/e2e/CDP-E2E-T09-preview-comparison.spec.ts`；manifest PASS。
