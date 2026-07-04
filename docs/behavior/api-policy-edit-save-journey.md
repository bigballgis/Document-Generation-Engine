# BDD：API 策略编辑 → 影响预览 → 保存

**文件状态:** `ready` | **BDD ID:** `BDD-CDP-APIPOL-001` | **CDP:** CD-BDD-T06 → CD-E2E-T07

## Actor / Goal

**GROUP_ADMIN** 在模板 API 管理页编辑 **一个配置域**（如 output format），执行影响预览后保存。

## Acceptance

### BDD-CDP-APIPOL-001 — 保存成功

- **Given** 已发布模板 + API 管理入口可见
- **When** 编辑候选配置 → 影响预览 → 确认警告/处理硬阻断 → 保存
- **Then** `policyVersion` 递增；审计入口可见

### BDD-CDP-APIPOL-002 — 硬阻断

- **Given** 影响预览返回 hard-block
- **Then** 保存按钮不可用或保存返回 4xx + 固定结构文案（原因/影响/建议）

## 证据

`frontend/e2e/CDP-E2E-T07-api-policy-edit-save.spec.ts`；manifest PASS。
