# BDD：审批员结构化判定旅程

**文件状态:** `ready` | **BDD ID:** `BDD-CDP-APPR-001` | **CDP:** CD-BDD-T03 → CD-E2E-T04

## Actor / Goal

**TEMPLATE_APPROVER**（`10000006`）在浏览器内 **Approve** 或 **Reject**，确认证据与保真摘要。

## Acceptance

### BDD-CDP-APPR-001 — Approve

- **Given** 模板在 APPROVAL 队列
- **When** Open → Approve → 理由摘要 + 确认关键证据 + 保真摘要已查看
- **Then** 进入 **待发布**；APPROVAL 队列项消失

### BDD-CDP-APPR-002 — Reject

- **When** Reject → 退回原因分类 + 整改要求（必填）
- **Then** 模板回到 DRAFT/修复态；作者收到整改待办

## 证据

`frontend/e2e/CDP-E2E-T04-approver-decision.spec.ts`；manifest PASS。
