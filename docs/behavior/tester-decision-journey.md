# BDD：测试员结构化判定旅程

**文件状态:** `ready` | **BDD ID:** `BDD-CDP-TEST-001` | **CDP:** CD-BDD-T02 → CD-E2E-T02/T03

## Actor / Goal

**TEMPLATE_TESTER**（`10000005`）在浏览器内完成 **Pass test** 或 **Fail test**，填写结构化意见并确认证据摘要。

## Acceptance

### BDD-CDP-TEST-001 — Pass test

- **Given** 模板处于 TEST 队列且测试证据齐全
- **When** 测试员 Dashboard → Open → Pass test → 填写表单 → 确认保真摘要
- **Then** TEST 队列项消失；模板进入可提交审批状态

### BDD-CDP-TEST-002 — Fail test

- **Given** 同上
- **When** Fail test → 原因分类 + 影响范围 + 修复建议（必填）
- **Then** 模板回到需作者修复状态；作者 Dashboard 出现整改待办

## 证据

`frontend/e2e/CDP-E2E-T02-tester-pass-decision.spec.ts`；manifest PASS。
