# BDD：母版设计员上传至审核通过

**文件状态:** `ready` | **BDD ID:** `BDD-CDP-MASTER-001` | **CDP:** CD-BDD-T05 → CD-E2E-T06

## Actor / Goal

**MASTER_DESIGNER** 在浏览器内：上传/替换 DOCX → 锚点校验 → 提交审核 → 审批通过。

## Acceptance

### BDD-CDP-MASTER-001 — 完整母版链

- **Given** 母版 DRAFT 或待审核
- **When** 上传 DOCX → 查看锚点完整性 → 提交审核 → GROUP_ADMIN 批准
- **Then** 母版 **审核通过**；下游模板可绑定

## 边界

- 替换已批准母版 → 回到 draft（已有 `master-replace-docx.spec.ts` 可复用片段）
- **上传深度校验与体积上限**（magic / 损坏包 / 超限 / 合法 DOCX）→ 见 [LR-A3 upload validation](./lrp-a3-master-docx-upload-validation.md)（`BDD-LRP-A3-UPLOAD-001`）

## 证据

`frontend/e2e/CDP-E2E-T06-master-lifecycle.spec.ts`；manifest PASS。
