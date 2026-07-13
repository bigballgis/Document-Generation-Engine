# BDD：API 策略编辑 → 影响预览 → 保存

**文件状态:** `ready` | **BDD ID:** `BDD-CDP-APIPOL-001` / `BDD-CDP-APIPOL-002` | **CDP:** CD-BDD-T06 → CD-E2E-T07

## Actor / Goal

**GROUP_ADMIN** 在模板 API 管理页编辑 **一个配置域**（如 output format），执行影响预览后保存。

## Acceptance

### BDD-CDP-APIPOL-001 — 保存成功

- **Given** 已发布模板 + API 管理入口可见（`?tab=apiAccess`）
- **When** 编辑 **OUTPUT_POLICY** 候选配置 → 影响预览 → 确认警告 → 保存
- **Then** `policyVersion` 递增；审计入口（Activity log）可见

### BDD-CDP-APIPOL-002 — 硬阻断

- **Given** 已发布模板 + API 管理入口可见
- **When** 在 **DEFAULT_ROUTE_TARGET** 将候选 default route 设为**不可调用**的 release version，并执行影响预览（当前平台唯一硬阻断生产者；见 P17 / `ApiPolicyImpactPreviewService`）
- **Then** 保存被阻止：保存按钮不可用，或保存返回 4xx；文案含固定结构（原因 / 影响 / 建议）；`policyVersion` **不**递增

## 证据

`frontend/e2e/CDP-E2E-T07-api-policy-edit-save.spec.ts`；UIUX manifest PASS。

## Traceability

- Plan: [CDP-e2e-full-chain-evidence.md](../plan/detail/CDP-e2e-full-chain-evidence.md) § CD-E2E-T07
- CDP: [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md) CD-BDD-T06
- Confirmed: `docs/security/permission-matrix.md` § API 管理配置动线；`docs/product/PRD.md` API policy impact preview；P17 Done
- Out of scope: T09–T13 full；audit-governance 深查；LR-C5
