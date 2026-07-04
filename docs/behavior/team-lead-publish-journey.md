# BDD：团队负责人发布 / Go-live 旅程

**文件状态:** `ready` | **BDD ID:** `BDD-CDP-PUB-001` | **CDP:** CD-BDD-T04 → CD-E2E-T05

## Actor / Goal

**GROUP_ADMIN**（`10000002`）在浏览器内完成 **发布摘要 + 二次确认**，使发布版本可调用。

## Acceptance

### BDD-CDP-PUB-001 — Go-live

- **Given** 模板 **PENDING_RELEASE**
- **When** 打开发布对话框 → 阅读摘要（含保真/覆盖率摘要）→ 二次确认
- **Then** 发布版本存在；External access / API 视图显示 callable

### BDD-CDP-PUB-002 — 未确认不可发布

- **When** 关闭对话框或未勾选确认
- **Then** 状态不变；无 release version 新增

## 证据

`frontend/e2e/CDP-E2E-T05-team-lead-publish.spec.ts`；manifest PASS。
