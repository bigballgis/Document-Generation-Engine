# BDD 行为规格：CE-U04 — 站内 PDF 预览（pdf.js）

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U04-IPP`  
**编写日期:** 2026-07-14  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U04  
**Slice:** `ce-u04-inline-pdf-preview`  
**Task Master:** **#67**  
**Formal phase:** **None**

---

## 1. 概述

预览产物目前只能下载后在外部 PDF 阅读器打开。本切片在 `AuthoringSideBySideLayout` 预览面板（及完整预览面板）内嵌 **pdf.js 只读视图**，刷新成功后无需下载即可查看第 1 页并翻页。

| 行为域 | 摘要 |
| --- | --- |
| **IPP-01 站内第 1 页** | 成功 preview run 且 PDF 产物可用时，刷新后预览面板内直接渲染第 1 页 |
| **IPP-02 翻页** | 多页 PDF 提供上一页/下一页控件；只读，无编辑 |
| **IPP-03 侧栏布局** | `AuthoringPreviewPane`（split view）与 Testing 预览 Tab 均展示同一内联视图 |
| **IPP-04 SPECIMEN** | 当 CE-G02 已对 preview/test-generate 路径加水印时，内联视图可见水印（软依赖；本片不实现 G02） |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-G02 水印实现 | Out of scope（#73）；本片消费已有 PDF 字节流 |
| iframe 嵌入 | 禁止；使用 pdf.js canvas 渲染 |
| 正式 runtime 产物预览 | Out of scope；仅 preview/test-generate 管理端路径 |
| DOCX 内联渲染 | Out of scope |

---

## 2. Actor / Role

| Actor | 角色 |
| --- | --- |
| **模板编排/测试人员** | `TEMPLATE_AUTHOR` / `TEMPLATE_TESTER` |
| **系统** | Preview artifact download API + pdf.js 渲染 |

---

## 3. Acceptance scenarios

### BDD-CE-U04-IPP-001 — 刷新后无需下载可见第 1 页

```gherkin
Given 模板在 Authoring 绑定编辑器侧栏有一次 SUCCEEDED 的 preview run，且 PDF 产物可下载
When 用户点击 Refresh now 或预览记录已加载
Then 预览面板展示 data-testid=inline-pdf-preview-viewer
And 第 1 页 canvas 可见（page 1 of N）
And 用户无需点击 Download PDF 即可看到页面内容
```

### BDD-CE-U04-IPP-002 — 多页翻页只读

```gherkin
Given 内联 PDF 预览已加载且总页数 > 1
When 用户点击 Next page
Then 当前页码递增且 canvas 更新
When 用户点击 Previous page
Then 当前页码递减
And 预览区域无文本编辑或表单控件
```

### BDD-CE-U04-IPP-003 — 无 PDF 时展示空态/隐藏

```gherkin
Given preview run 状态非 SUCCEEDED 或 PDF 产物不可用
Then 内联 PDF 预览区域不展示（或展示可理解的空态/加载失败提示）
And Download PDF 按钮行为不变（若产物稍后可用）
```

### BDD-CE-U04-IPP-004 — SPECIMEN 水印（CE-G02 软依赖）

```gherkin
Given CE-G02 已对 preview/test-generate PDF 施加 SPECIMEN 对角水印
When 用户在内联预览查看第 1 页
Then 水印在渲染结果中可见（与下载 PDF 一致）
```

> **注：** CE-G02（#73）未完成时，IPP-004 以文档化软依赖记录；E2E 仅断言页面渲染成功，水印断言在 G02 Done 后由金标/回归补强。

---

## 4. 可观测证据

| 证据 | 路径 |
| --- | --- |
| Vitest | `InlinePdfPreviewViewer.test.ts`, `AuthoringPreviewPane.test.ts` |
| E2E | `frontend/e2e/CE-U04-inline-pdf-preview.spec.ts` |
| UIUX | `frontend/e2e/CE-U04-inline-pdf-preview-uiux-evidence.spec.ts` |

---

## 5. 追溯

| 来源 | 链接 |
| --- | --- |
| CE 程序 §4 CE-U04 | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) |
| F7 侧栏壳 | `AuthoringSideBySideLayout` / `AuthoringPreviewPane` |
| CE-G02 水印 | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §6（软依赖） |
