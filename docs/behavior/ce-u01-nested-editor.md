# BDD 行为规格：CE-U01 — 结构化编辑器嵌套子树编辑

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-14  
**BDD ID**: `BDD-CE-U01-NESTED-EDITOR-001`  
**来源**: [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 Wave CE-U · **CE-U01**  
**Slice**: `ce-u01-nested-editor`  
**Task**: CE-U01 结构化编辑器嵌套子树编辑（Task Master **#64**）  
**bdd_readiness**: **`ready`**  
**Worktree**: `D:/working/DGE-ce-u01-nested-editor` · `feat/ce-u01-nested-editor`  
**Formal phase**: **None**  
**授权依据**: Parent delivery — Batch 3 泳道2；依赖 CE-U03 **Done** (#55)

---

## 1. 概述

`conditionBlock` / `loopBlock` 的 `children` 数组已在 schema 中存在，但 `StructuredContentBlockCard` 仅渲染条件表达式/循环变量，**无递归子块 UI**。作者只能导入 HTML 或手改 JSON 编辑嵌套内容。

本切片为 condition/loop 容器增加 **递归块编辑器**（最大嵌套深度 **3 层**），子块可增删改，并与 LR-C3 **undo/redo 历史栈**兼容。

| 行为域 | 摘要 |
| --- | --- |
| **NE-01 递归渲染** | condition/loop 卡片内展示 `children` 子块列表，递归 `StructuredContentBlockCard` |
| **NE-02 子块增删** | 容器内可插入 paragraph/heading/list/condition/loop/table/clause 等块；可删除子块 |
| **NE-03 深度限制** | 嵌套深度 ≤ 3；达限时隐藏「添加子块」并展示提示 |
| **NE-04 undo/redo** | 嵌套增删改纳入既有 structure snapshot 历史；Ctrl+Z/Y 可撤销/重做 |
| **NE-05 预览贯通** | 嵌套段落文本写入 structure JSON；保存绑定后预览可渲染 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-U02 拖拽排序 / 块复制 / 校验 scrollIntoView | **Out of scope** |
| 改变后端 schema / 渲染语义 | **禁止** — 仅 UI 暴露既有 children |
| 宣称 production go-live | **禁止** |

---

## 2. Actor / Role

| Actor | 角色 |
| --- | --- |
| **模板编排人员** | `TEMPLATE_AUTHOR` / 具备 binding 维护权 |
| **系统（UI）** | `ControlledStructuredContentEditor` + 递归 `StructuredContentBlockCard` |

---

## 3. Acceptance scenarios

### NE-01 递归渲染

```gherkin
Given 绑定 structure 含 conditionBlock 且 children 有一条 paragraph
When 作者打开结构化编辑器
Then 条件块下方展示嵌套子块卡片
And 子段落可编辑 textRun
```

### NE-02 在 condition 内新增段落

```gherkin
Given 空文档含一个 conditionBlock（children 为空）
When 作者在嵌套工具栏点击 Paragraph
Then children 增加 paragraph 块
And JSON preview 含嵌套 paragraph
```

### NE-03 深度限制

```gherkin
Given 嵌套已达 3 层（root condition → loop → condition）
When 作者查看最内层 condition
Then 不展示「添加子块」按钮
And 展示最大深度提示
```

### NE-04 undo/redo

```gherkin
Given 作者在 condition 内新增 paragraph
When 作者点击 Undo
Then 嵌套 paragraph 被移除
When 作者点击 Redo
Then 嵌套 paragraph 恢复
```

### NE-05 E2E 预览

```gherkin
Given 作者在 condition 内新增段落并填写文本
When 作者保存绑定并刷新预览
Then 预览请求成功（structure 含嵌套内容）
```

---

## 4. 可观测证据

| 证据 | 命令 / 产物 |
| --- | --- |
| 单元 | `pnpm -C frontend test structuredContentNodePath` + `ControlledStructuredContentEditor` Vitest |
| E2E | `e2e/ce-u01-nested-editor.spec.ts` |
| UIUX | `e2e/ce-u01-nested-editor-uiux-evidence.spec.ts` |
| 门禁 | `pnpm -C frontend lint type-check test build` |

---

## 5. Traceability

| 需求 | 本规格 |
| --- | --- |
| CE-U01 plan §4 P0 | NE-01…NE-05 |
| LR-C3 undo/redo | NE-04 |
| ADR-0019 structured content v1 | NE-01 节点类型子集 |
