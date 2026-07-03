# BDD：审计管理员查询与导出

**文件状态:** `ready` | **BDD ID:** `BDD-CDP-AUDIT-001` | **CDP:** CD-BDD-T08 → CD-E2E-T11

## Actor / Goal

**AUDIT_ADMIN**（`10000007`）在活动日志中筛选事件并触发导出。

## Acceptance

### BDD-CDP-AUDIT-001 — 筛选

- **Given** 已登录审计管理员
- **When** 设置日期/事件类型筛选 → 查询
- **Then** 结果列表更新；只读横幅可见；无 My to-dos 行为组

### BDD-CDP-AUDIT-002 — 导出

- **When** 点击导出
- **Then** 下载开始或显示导出结果（非 403）

## 证据

`frontend/e2e/CDP-E2E-T11-audit-query.spec.ts`；首份 audit 角色 UIUX manifest PASS。
