# BDD 行为规格：CE-U02 — 块排序 / 复制 / 校验定位

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-14  
**BDD ID**: `BDD-CE-U02-BLOCK-SORT-COPY-SCROLL-001`  
**来源**: [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 Wave CE-U · **CE-U02**  
**Slice**: `ce-u02-block-sort-copy-scroll`  
**Task**: CE-U02 块排序/复制/校验定位（Task Master **#65**）  
**bdd_readiness**: **`ready`**  
**Worktree**: `D:/working/DGE-ce-u02-block-sort-copy-scroll` · `feat/ce-u02-block-sort-copy-scroll`  
**Formal phase**: **None**  
**授权依据**: Parent delivery — Batch 3 泳道2；依赖 CE-U01 **Done** (#64)

---

## 1. 概述

结构化编辑器在 CE-U01 嵌套子树基础上，补齐作者日常编排三项能力：**同层拖拽排序**、**块复制**、**绑定结构校验失败时按 blockPath 滚动定位**。

| 行为域 | 摘要 |
| --- | --- |
| **BS-01 同层拖拽排序** | 根层或同一 condition/loop `children` 内可拖拽重排；禁止跨层移动 |
| **BS-02 块复制** | 任意块（含嵌套子树）可复制为同层紧邻副本 |
| **BS-03 校验定位** | 结构校验产出带 `blockPath` 的条目；点击条目 `scrollIntoView` 到对应块卡片 |
| **BS-04 undo/redo** | 排序与复制纳入既有 structure snapshot 历史 |
| **BS-05 只读禁用** | `readonly` 模式隐藏拖拽柄、复制与校验操作 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 跨层拖入拖出 | **禁止** |
| 后端 OpenAPI / 校验 API 变更 | **Out of scope** — 客户端结构校验 |
| 宣称 production go-live | **禁止** |

---

## 2. Actor / Role

| Actor | 角色 |
| --- | --- |
| **模板编排人员** | `TEMPLATE_AUTHOR` / 具备 binding 维护权 |
| **系统（UI）** | `ControlledStructuredContentEditor` + `StructuredContentBlockCard` |

---

## 3. Acceptance scenarios

### BS-01 同层拖拽排序

```gherkin
Given 根层有两个 paragraph 块 A、B（A 在上）
When 作者将 B 拖到 A 上方
Then 根层顺序变为 B、A
And JSON preview 反映新顺序
```

```gherkin
Given condition 的 children 含两个 paragraph
When 作者在嵌套面板内拖拽重排
Then 仅 children 顺序变化，condition 仍在原根层位置
```

### BS-02 块复制

```gherkin
Given 根层有一个 paragraph 含文本 "Clause A"
When 作者点击该块复制
Then 同层紧邻出现副本且文本仍为 "Clause A"
```

### BS-03 校验定位

```gherkin
Given paragraph 内 variable 引用未在 schema 声明的 key
When 作者点击「校验结构」
Then 展示至少一条失败条目含 blockPath
When 作者点击该条目
Then 视口滚动到对应 structured-block-card 且块可见
```

### BS-04 undo/redo

```gherkin
Given 作者复制了一个块
When 作者点击 Undo
Then 副本被移除
When 作者点击 Redo
Then 副本恢复
```

### BS-05 只读

```gherkin
Given 编辑器 readonly=true
Then 不展示拖拽柄、复制按钮与校验结构按钮
```

---

## 4. 可观测证据

| 证据 | 命令 / 产物 |
| --- | --- |
| 单元 | `structuredContentNodePath` + `structuredContentBindingValidation` + `ControlledStructuredContentEditor` Vitest |
| E2E | `e2e/ce-u02-block-sort-copy-scroll.spec.ts` |
| UIUX | `e2e/ce-u02-block-sort-copy-scroll-uiux-evidence.spec.ts` |
| 门禁 | `pnpm -C frontend lint type-check test build` |

---

## 5. Traceability

| 需求 | 本规格 |
| --- | --- |
| CE-U02 plan §4 P1 | BS-01…BS-05 |
| CE-U01 nested paths | BS-01/02 复用 `NodePath` |
| LR-C3 undo/redo | BS-04 |
