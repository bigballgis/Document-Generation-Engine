# BDD 行为规格：IBL-B5 — Seal geometry validation（授权区几何校验）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-B5` |
| **编写日期** | 2026-07-19 |
| **程序 / 队列** | IBL Wave B · **IBL-B5** / F14（`ibl-b5-seal-geometry`） |
| **Slice** | `ibl-b5-seal-geometry` |
| **Branch** | `feat/ibl-b5-seal-geometry` |
| **Worktree** | `D:/working/DGE-ibl-b5-seal-geometry` |
| **Base** | `fc16d508`（`origin/main` / local main tip at provision） |
| **Placement** | ISOLATED |
| **Task Master** | **#117** IBL-B5 — Batch Recommendation **solo**（预期）；`member_task_ids: ["117"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-b5-seal-geometry`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F14 / IBL-B5；ADR [0019](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md)（seal 须在 authorized area）；domain §2.6.5（实现/doc-sync 须同步几何契约，替换 boolean-only 描述） |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（无 Vue / Playwright / UIUX 义务） |

**完成声明约束：** 本叶关闭 F14——`sealRef` 授权区校验从 **信任布尔** 升级为 **几何包含判定**；区外 → fail-closed blocker。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 IBL Wave B / 程序 Done；**禁止**把 IBL-B6（reproducibility freeze）/ B7（Word Path E）并入本叶；**禁止**本叶交付绝对定位 DOCX 发射或母版画区 UI。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["117"]
  proposed_slice_id: ibl-b5-seal-geometry
  shared_acceptance_surface: >
    Geometric containment validation for sealRef placement vs authorized area;
    out-of-area fail-closed SEAL_OUTSIDE_AUTHORIZED_AREA; in/out fixtures
  vetoes_applied:
    - b6-reproducibility-freeze
    - b7-word-path-e
    - umbrella-106-registry-only
    - absolute-position-docx-writer
    - master-seal-zone-authoring-ui
  evidence_amortization: mvn verify (backend only)
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| `ReferenceNodeService.validateSealRef`（或同源抽取）对 **几何授权区** 做包含判定 | IBL-B6 legal-reproducibility freeze |
| 区外 → `SEAL_OUTSIDE_AUTHORIZED_AREA` blocker（fail-closed；发布闸门既有汇入） | IBL-B7 Word Path E / #3b GO |
| in-area / out-of-area 夹具单测 | 绝对定位 writer（按 `sealBox` 写 floating drawing） |
| 废弃 `withinAuthorizedArea` 作为通过权威 | 母版画区管理 UI / FE |
| Gates：`mvn -B -ntp -f backend/pom.xml verify` | 多边形/椭圆区；像素金标；加密电子签章；go-live |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| `placement.withinAuthorizedArea` 为作者/载荷布尔；`false` → blocker，`true`/缺省 → 通过 | `ReferenceNodeService.validateSealRef` L91–101 |
| 无坐标、无区矩形、无印章足迹包含运算 | 同文件；无几何 helper |
| 测试仅断言布尔 `false` 路径 | `ReferenceNodeServiceTest.sealOutsideAuthorizedArea_isBlocker` |
| `authorizedAreaId` 出现在夹具但未解析几何 | 测试 JSON；实现未读该字段做几何 |
| ADR-0019 要求 seal 留在 authorized area、不得裁切/不可见 | ADR-0019 seal 段 |
| Writer 今日仅 inline 嵌入固定 **48pt×48pt** 图 | `StructuredContentDocxInlineSupport.writeReferenceNode` |
| Domain §2.6.5 仍写 boolean-only | `domain-model.md` |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **B5-S1 Geometry model** | 轴对齐矩形 + pt + pageIndex；印章足迹完全落在授权区内 |
| **B5-S2 Catalog supply** | 授权区由文档级 `authorizedSealAreas[]` 目录供给；`authorizedAreaId` 解析 |
| **B5-S3 Validation** | `placement` 存在时几何强制；布尔非权威 |
| **B5-S4 Fail-closed** | 区外 / 未知区 / 非法几何 → blocker；不静默放行 |
| **B5-S5 Fixtures** | in-area 与 out-of-area 自动化夹具 |
| **B5-S6 Honesty bounds** | 不宣称 go-live；不翻转 #3b/#5a；OUT B6/B7；不要求本叶绝对定位写出 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **模板作者 / 绑定配置方** | 在结构化内容中声明签章区目录 + `sealRef.placement` | 期望区外被硬阻断，而非自报布尔 |
| **绑定校验 / 发布闸门** | 消费 `ReferenceNodeService` fidelity blockers | 区外不可发布 |
| **系统（校验器）** | 几何包含判定 | 不信任 `withinAuthorizedArea` |
| **CI / `mvn verify`** | 单元夹具 | in/out 失败即红 |
| **（非本片）管理 UI 用户** | — | `frontend_ui_in_scope=false` |

---

## 3. Goal

1. 关闭 F14：签章授权校验为 **真实几何**，非 boolean-only。  
2. 区外 → **fail-closed** `SEAL_OUTSIDE_AUTHORIZED_AREA`（及几何非法的稳定 blocker，见 §4）。  
3. 提供 in-area / out-of-area 夹具证明。  
4. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；OUT B6/B7。

---

## 4. 已确认决策 vs 非确认 — **几何模型（本叶钉死）**

### 4.1 坐标单位与坐标系（CONFIRMED）

| 项 | 裁决 |
| --- | --- |
| **单位** | JSON 数值单位 = **point（pt）**。与 IBL-B1 direct-format、现行 seal 默认渲染 **48pt** 对齐。实现内部可换算 twips（×20）或 EMU，但 **作者/夹具契约面只用 pt**。 |
| **原点** | 页局部坐标系：原点为该页 **页面框左上角**（top-left of page media box），**+x 向右，+y 向下**。 |
| **页面索引** | `pageIndex`：**0-based** 整数（`≥ 0`）。印章与授权区必须 **同一 `pageIndex`** 才可判定包含；跨页视为区外（blocker）。 |
| **本叶不做** | 不跑完整分页布局引擎；`pageIndex` 为声明式相等约束，不从 DOCX 回流测量。 |

### 4.2 区域形状与印章足迹（CONFIRMED）

| 项 | 裁决 |
| --- | --- |
| **授权区形状** | **轴对齐矩形（AABB） only**。字段：`xPt`、`yPt`、`widthPt`、`heightPt`（均为 number；`widthPt`/`heightPt` **必须 > 0**；`xPt`/`yPt` **≥ 0**）。 |
| **印章足迹** | 同为 AABB：`sealBox.{xPt,yPt,widthPt,heightPt}`。 |
| **缺省足迹尺寸** | 若 `sealBox` 省略 `widthPt`/`heightPt`（但提供了位置），默认 **48 × 48 pt**（对齐今日 writer）。若连 `xPt`/`yPt` 也缺 → 非法几何（见 B5-C7）。 |
| **包含规则** | **完全包含（closed on edges）**：  
  `seal.xPt ≥ area.xPt`  
  `AND seal.yPt ≥ area.yPt`  
  `AND seal.xPt + seal.widthPt ≤ area.xPt + area.widthPt`  
  `AND seal.yPt + seal.heightPt ≤ area.yPt + area.heightPt`  
  `AND seal.pageIndex == area.pageIndex`  
  任一不满足 → **区外**。中心点落在区内但边角越界 → **区外**（对齐 ADR「不得裁切」）。 |
| **v1 否决** | 多边形、圆/椭圆、旋转矩形、百分比坐标、mm/inch 混用、CSS px。 |

### 4.3 授权区如何供给（CONFIRMED）

| 项 | 裁决 |
| --- | --- |
| **供给载体（本叶）** | 结构化内容 JSON **根对象**数组 **`authorizedSealAreas`**（可与 `nodes` 同级）。每项： |
| | `{ "id": "<string>", "pageIndex": <int≥0>, "xPt": <number≥0>, "yPt": <number≥0>, "widthPt": <number>0>, "heightPt": <number>0> }` |
| **引用** | `sealRef.placement.authorizedAreaId` **必须**等于某目录项的 `id`（大小写敏感、trim 后非空）。 |
| **解析失败** | 未知 / 空白 `authorizedAreaId`，或目录项几何非法 → **blocker**（稳定码优先：`SEAL_AUTHORIZED_AREA_UNKNOWN` **或**实现文档化的 fidelity 族等价码；**禁止**静默当区内）。 |
| **重复 id** | 目录内重复 `id` → blocker（`SEAL_AUTHORIZED_AREA_INVALID` 或等价）；fail-closed。 |
| **母版目录未来接线** | ADR 意图区定义在母版；本叶 **允许** 日后由母版/锚点目录注入**同一形状**的 catalog。本叶验收以 **文档根 `authorizedSealAreas` + 校验器可读** 为准；**不**要求本叶交付母版解析/UI。 |
| **禁止** | 仅靠 `withinAuthorizedArea: true` 宣称「在区内」而无几何。 |

### 4.4 `placement` 模式与布尔废弃（CONFIRMED）

| 模式 | 行为 |
| --- | --- |
| **A — 无 `placement`** | 允许（兼容今日 demo inline `sealRef`）。**不**产生 `SEAL_OUTSIDE_AUTHORIZED_AREA`。仍校验 `referenceKey` / `applyScaling` 既有规则。 |
| **B — 有 `placement`** | **强制几何路径**：必须提供非空 `authorizedAreaId` + 可解析目录项 + `sealBox`（至少 `xPt`/`yPt`；宽高可默认 48）。按 §4.2 判定；区外 → `SEAL_OUTSIDE_AUTHORIZED_AREA`。 |
| **布尔 `withinAuthorizedArea`** | **非权威（DEPRECATED）**。存在时 **不得**单独决定通过/阻断。几何区内即使布尔为 `false` → **通过几何门**（可选保留布尔仅作遗留字段，实现可忽略）。几何区外即使布尔为 `true` → **仍 blocker**。 |
| **仅布尔、无几何字段的遗留 `placement`** | 视为 **非法几何** → blocker（`SEAL_PLACEMENT_GEOMETRY_INVALID` 或等价），**不得**再靠 `withinAuthorizedArea: false` 假「几何校验」。实现须迁移既有单测夹具到几何模型。 |

### 4.5 本叶确认决策明细

| ID | 决策 | 依据 |
| --- | --- | --- |
| **B5-C1** | 几何模型 = §4.1–§4.3（pt、页左上原点、AABB、完全包含、根目录 `authorizedSealAreas`）。 | F14；ADR-0019；可测夹具 |
| **B5-C2** | `placement` 存在 → 几何强制；布尔非权威。 | 关闭 boolean-only 谎言 |
| **B5-C3** | 无 `placement` → 不跑授权区几何门（inline 兼容）。 | 保护既有 demo/bindings |
| **B5-C4** | 区外稳定码 **`SEAL_OUTSIDE_AUTHORIZED_AREA`** + 既有 messageKey；非法/未知区用文档化稳定码（可新增）。 | 既有 fidelity 族 |
| **B5-C5** | 校验汇入既有绑定/发布 blocker 路径（`TemplateService` / PublishGate 既有接线保持）。 | P18-T05 |
| **B5-C6** | **本叶不改** seal 绝对定位写出；writer 可继续 inline 48pt。几何契约是 **校验诚实**，不假装坐标已落到 DOCX floating。 | 验收面 = validate + fixtures |
| **B5-C7** | 非法值（NaN、负宽高、非 number、缺 x/y、缺目录）→ fail-closed blocker，禁半忽略。 | 与 B1-C7 同族 |
| **B5-C8** | FE：`frontend_ui_in_scope=false`。 | handoff |
| **B5-C9** | 门禁：`mvn -B -ntp -f backend/pom.xml verify`；E2E/UIUX N/A。行为变更若管线要求 Stage 5/10 queued Docker，按交付宪章执行，但本叶验收不依赖 FE。 | delivery constitution |
| **B5-C10** | 完成边界：关闭 F14 ≠ Wave B Done ≠ go-live；#3b/#5a 不翻转；OUT B6/B7。 | 队列政策 |
| **B5-C11** | 多章重叠 / 可见性裁切相对正文流：**OUT**（本叶不检测 seal↔seal 重叠；不检测与段落碰撞）。ADR 完整视觉承诺留残差，诚实记录。 | 范围控制 |

### 4.6 示例夹具（规范形状 — 非实现代码）

**目录 + 区内（应通过几何门）：**

```json
{
  "authorizedSealAreas": [
    {
      "id": "SEAL_ZONE_A",
      "pageIndex": 0,
      "xPt": 400,
      "yPt": 600,
      "widthPt": 120,
      "heightPt": 120
    }
  ],
  "nodes": [
    {
      "type": "sealRef",
      "referenceKey": "SEAL-1",
      "placement": {
        "authorizedAreaId": "SEAL_ZONE_A",
        "sealBox": { "xPt": 420, "yPt": 620, "widthPt": 48, "heightPt": 48 }
      }
    }
  ]
}
```

**区外（应 blocker `SEAL_OUTSIDE_AUTHORIZED_AREA`）：** 同上目录，但 `sealBox.xPt = 500`（右缘 548 > 区右缘 520）。

### 4.7 Open questions

**无阻塞项。** 几何模型、供给方式、包含规则、布尔废弃与 inline 兼容路径已由本 BDD 确认，供 TDD Red 使用。

```text
open_questions: []
```

---

## 5. Trigger / Preconditions

### Trigger

- 结构化内容校验调用 `ReferenceNodeService.validateStructuredContent`（或同源）。  
- 内容树含 `sealRef`；可选根级 `authorizedSealAreas`。  
- 绑定保存 / validateBindings / 发布闸门汇入 fidelity blockers。

### Preconditions

- 结构化节点矩阵已含 `sealRef`（P18-T05）。  
- 作者/调用方已通过既有授权（本叶不放宽）。  
- 本叶不依赖 LibreOffice / Word host。

---

## 6. Primary journey

1. 作者在结构化 JSON 根声明 `authorizedSealAreas`（如 `SEAL_ZONE_A` 矩形）。  
2. `sealRef` 带 `placement.authorizedAreaId` + `sealBox`（区内坐标）。  
3. 校验：解析目录 → 完全包含 → **无** `SEAL_OUTSIDE_AUTHORIZED_AREA`。  
4. 发布闸门不因签章几何阻断（无其它 blocker 时）。  
5. 负向：将 `sealBox` 移出矩形 → 校验产生 blocker → 发布 fail-closed。

---

## 7. System responses

### 7.1 Success

| 形态 | 响应 |
| --- | --- |
| 几何区内 `placement` | 无 `SEAL_OUTSIDE_AUTHORIZED_AREA`；无几何非法 blocker |
| 无 `placement` 的 inline seal | 几何门跳过；既有 referenceKey/scaling 规则不变 |
| 绑定/发布 | 无签章几何 blocker 时不因此失败 |

### 7.2 Fail-closed

| 条件 | 行为 |
| --- | --- |
| 足迹不完全在区内 / 跨 pageIndex | `SEAL_OUTSIDE_AUTHORIZED_AREA` blocker |
| 未知 `authorizedAreaId` / 非法目录几何 / 非法 sealBox / 仅布尔遗留 placement | 稳定几何/区 blocker（见 B5-C4/C7）；**禁止**静默通过 |
| `applyScaling: true` | 既有 `SEAL_SCALING_NOT_ALLOWED`（不变） |

---

## 8. Acceptance scenarios（Given / When / Then）

### BDD-IBL-B5-001 — 几何模型书面确认（F14）

**Given** IBL-B5 / F14 要求授权区为真实几何而非 boolean-only  
**When** 读取本 BDD §4  
**Then** 单位 **pt**、页左上原点、AABB、完全包含、`authorizedSealAreas` 目录供给均已确认  
**And** `withinAuthorizedArea` 被标为非权威

### BDD-IBL-B5-002 — in-area 夹具通过

**Given** §4.6 区内夹具（目录 + `sealBox` 完全在区内）  
**When** `validateStructuredContent`  
**Then** blockers **不含** `SEAL_OUTSIDE_AUTHORIZED_AREA`  
**And** 不含几何非法类 blocker

### BDD-IBL-B5-003 — out-of-area 夹具阻断

**Given** 同目录但 `sealBox` 越出授权矩形（含仅角点越界）  
**When** `validateStructuredContent`  
**Then** 存在 blocker `SEAL_OUTSIDE_AUTHORIZED_AREA`  
**And** messageKey 为既有 `generation.warning.fidelity.sealOutsideAuthorizedArea`（或实现保持的同一键）

### BDD-IBL-B5-004 — 布尔 true 不能掩盖区外

**Given** 几何区外的 `sealBox`  
**And** `placement.withinAuthorizedArea = true`  
**When** 校验  
**Then** 仍产生 `SEAL_OUTSIDE_AUTHORIZED_AREA`  
**And** **不**因布尔放行

### BDD-IBL-B5-005 — 布尔 false 不能单独充当「几何校验」

**Given** 几何区内的 `sealBox`  
**And** `placement.withinAuthorizedArea = false`  
**When** 校验  
**Then** **不**因布尔单独产生区外 blocker（几何权威）  
**And** 无其它非法时几何门通过

### BDD-IBL-B5-006 — 未知 authorizedAreaId fail-closed

**Given** `placement.authorizedAreaId = "MISSING_ZONE"` 且目录无此项  
**When** 校验  
**Then** 产生稳定 blocker（未知区）  
**And** **不**静默当区内

### BDD-IBL-B5-007 — 非法几何 fail-closed

**Given** `placement` 存在但缺少 `sealBox.xPt`/`yPt`，或 `widthPt ≤ 0`，或目录 `heightPt ≤ 0`  
**When** 校验  
**Then** 产生稳定几何非法 blocker  
**And** **不**回退到布尔权威

### BDD-IBL-B5-008 — 无 placement 兼容 inline

**Given** `sealRef` 仅有 `referenceKey`、无 `placement`  
**When** 校验  
**Then** **不**产生 `SEAL_OUTSIDE_AUTHORIZED_AREA`  
**And** 空 `referenceKey` / `applyScaling` 既有规则仍适用

### BDD-IBL-B5-009 — 跨 pageIndex 视为区外

**Given** 目录区 `pageIndex=0`，`sealBox.pageIndex=1`（或 seal 声明页与区不一致）  
**When** 校验  
**Then** `SEAL_OUTSIDE_AUTHORIZED_AREA`（或等价「页不匹配」归入区外）

### BDD-IBL-B5-010 — 发布闸门汇入

**Given** 绑定内容触发 BDD-IBL-B5-003 区外 blocker  
**When** 发布闸门评估绑定保真 blockers  
**Then** 发布 **fail-closed**（既有汇入路径；不得忽略 SEAL blocker）

### BDD-IBL-B5-011 — 完成边界：非 go-live / 非 #3b/#5a / 非 B6–B7

**Given** 本叶测试与文档更新完成  
**When** 声称切片状态  
**Then** 可关闭 **IBL-B5 / F14** 行为缺口  
**And** **不**宣称 IBL 程序 Done 或 Wave B 全部 Done  
**And** **不**翻转 checklist **#3b** / **#5a**  
**And** **不**交付 B6/B7 验收面  
**And** **不**宣称 seal 坐标已绝对定位写入 DOCX（B5-C6）

---

## 9. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 足迹边与区边重合 | **允许**（闭区间包含） |
| 默认 48×48 宽高 | 仅当提供了 x/y 且省略宽高 |
| 目录为空且存在 `placement` | 未知区 → fail-closed |
| 多个 `sealRef` 共享同一区 | 各自独立包含判定；不检重叠（B5-C11） |
| Schema 扩展 | 实现须让根 `authorizedSealAreas` 与 `placement.sealBox` 可被解析；未知字段策略遵循既有 schema 纪律 |
| FE 未改 | 允许 |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 本文件 §4 | 几何模型 + 供给 + 布尔废弃书面确认 |
| `ReferenceNodeServiceTest`（或继任） | in-area / out-of-area / 布尔非权威 / 未知区 / 非法几何 / 无 placement |
| Fidelity blocker code | `SEAL_OUTSIDE_AUTHORIZED_AREA` 等 |
| Gates | `mvn -B -ntp -f backend/pom.xml verify` GREEN |
| Checklist #3b/#5a | **不**翻转 |
| Domain §2.6.5 | 实现或 post-task doc-sync 同步几何契约（替换 boolean-only） |

---

## 11. Traceability

| 源 | 关系 |
| --- | --- |
| IBL program F14 / IBL-B5 | 本叶关闭 |
| Task Master **#117** | 交付叶 |
| ADR-0019 seal authorized area | 产品依据 |
| P18-T05 / `ReferenceNodeService` | 实现落点 |
| domain §2.6.5 | 须随本叶更新 |
| Checklist #3b/#5a | **不**由本叶翻转 |
| IBL-B6 / B7 | **OUT** |

---

## 12. BDD readiness

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-b5-seal-geometry.md
task_ids: ["117", "IBL-B5"]
frontend_ui_in_scope: false
geometry_units: pt
geometry_shape: axis_aligned_rect_aabb
coordinate_origin: page_media_top_left_x_right_y_down
containment: closed_full_footprint
authorized_area_supply: structured_content_root.authorizedSealAreas[]
placement_boolean_authority: deprecated_non_authoritative
writer_absolute_positioning: out_of_scope_this_leaf
next: plan-orchestrator → rendering-engineer (+ doc-keeper for domain §2.6.5 sync)
```

**Handoff：** Spec `ready`。实现须先红：几何 in/out 夹具 + 布尔非权威 + 未知/非法区 fail-closed；再改 `validateSealRef`；**禁止**继续信任 `withinAuthorizedArea`；**禁止**翻转 #3b/#5a；**禁止**把 B6/B7 并入本叶。
