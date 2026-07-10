# BDD 行为规格：LR-A3 — Master DOCX 上传深度校验与体积上限

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-10  
**BDD ID**: `BDD-LRP-A3-UPLOAD-001`  
**来源任务**: [LRP Wave LR-A § LR-A3 — Upload deep validation + size limits](../plan/detail/LRP-A-rendering-trust-hardening.md)  
**程序发现**: [launch-readiness-program.md](../plan/launch-readiness-program.md) §1 finding 6  
**门禁对齐**: [launch-readiness-gate.md](../plan/launch-readiness-gate.md) — LR-A3 勾选「50MB + deep validation」  
**Task Master**: `lrp-a3-upload-validation` / plan `LR-A3`

---

## 1. 概述

母版设计人员在管理端 **创建母版** 或 **替换母版 DOCX** 时，系统必须在持久化与锚点抽取之前，对上传文件做 **后缀 + 体积 + 内容结构** 校验，并在 Spring multipart / nginx 边界施加体积上限，避免伪装扩展名、损坏包或超大文件进入 POI / LibreOffice。

| 行为域 | 摘要 |
| --- | --- |
| **D1 服务端深度校验** | `MasterDocumentService.validateDocxFile`：非空、后缀 `.docx`、可选 Content-Type 白名单、体积 ≤ 配置上限、ZIP magic + OPC 必需条目探测 |
| **D2 多层体积上限** | 前端选文件提示（50MB）→ nginx `client_max_body_size`（60m）→ Spring multipart（50MB/60MB）→ 服务内 `docgen.master.max-docx-upload-bytes`（50MB） |
| **D3 可翻译拒绝** | 校验失败 → **422** + 稳定 `messageKey`；超限在到达服务前被网关/容器拒绝时 → **可读错误**（禁止向用户展示原始 nginx/Tomcat HTML） |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| **病毒 / 恶意软件扫描** | **Pending question** — 见 §13；不阻塞本规格 `ready` |
| 改变上传 API 形状（路径、字段名、multipart part 名） | Out of scope |
| 拒绝 Word / LibreOffice 产出的合法 `.docx` | 禁止回归 |
| LR-C10 上传 UX 抛光（进度条、拖拽文案、内联错误呈现） | 另任务；本规格只锁定拒绝契约与上限数值。**2026-07-11：** LR-C10 BDD readiness **`not-applicable` confirmed**（presentation only；见 [LRP-C § LR-C10](../plan/detail/LRP-C-usability-deepening.md)） |

---

## 2. Current vs target（非 greenfield）

本切片在 worktree 中已有 **部分实现**。规格以 **目标行为** 为准；实现阶段按缺口收口，不得当作从零重写。

| 能力 | 当前（2026-07-10 代码勘察） | 目标（本规格） | 缺口 |
| --- | --- | --- | --- |
| 后缀 `.docx` | ✅ `validateDocxFile` → `api.error.master.docxRequired` | 保持 | — |
| Content-Type 白名单 | ✅ 允许 OOXML Word MIME 或 `application/octet-stream`；其它非空类型 → `docxRequired` | 保持（辅助防伪装；**不以 Content-Type 为唯一真相**） | — |
| 服务内体积上限 | ✅ `docgen.master.max-docx-upload-bytes` 默认 **52428800**（50MB）→ `api.error.master.docxTooLarge` / **422** | 保持默认 **50MB** | — |
| Spring multipart | ✅ `max-file-size: 50MB`，`max-request-size: 60MB`（env 可覆盖） | 保持 | — |
| nginx `client_max_body_size` | ✅ **60m**（与 request-size 对齐） | 保持 | — |
| ZIP magic `PK\x03\x04` | ✅ `assertDocxPackageStructure` 读签名失败 → `api.error.master.docxCorrupt` | 保持 | 缺 **专用** magic-byte 单测（现有 corrupt 测例覆盖「缺 OPC 条目」） |
| OPC / 包结构探测 | ✅ `ZipInputStream` 要求 `[Content_Types].xml` + `_rels/.rels` + `word/document.xml`；失败 → `docxCorrupt` | **接受当前实现为等价目标**（计划文案写 `OPCPackage.open`；以「可打开为合法 OOXML Word 包」为准，不强制换 API） | 计划示例 key `invalidDocxContent` **不采用** — 统一用已有 `docxCorrupt` |
| messageKeys + i18n | ✅ `docxRequired` / `docxTooLarge` / `docxCorrupt`（backend `messages_en.properties` + frontend `apiErrorEn`/`apiErrorZhCn`） | 保持；**不新增** `invalidDocxContent` | — |
| 创建对话框客户端预检 | ✅ `MasterUploadDialog` 50MB + `.docx` + MIME | 保持 | — |
| 替换对话框客户端预检 | ❌ `MasterReplaceFileDialog` 无体积/类型预检 | **目标**：与创建对话框同规则（或共享 composable） | **缺口** |
| Spring 超限 → 信封 | ❌ 无 `MaxUploadSizeExceededException` / `MultipartException` 专用 handler；可能落入通用 500 | **目标**：到达后端且被 multipart 拒绝时，返回统一信封 + `api.error.master.docxTooLarge`（或等价稳定 key），**HTTP 413 或 422 均可，但必须是 JSON 信封且可翻译** | **缺口** |
| nginx 413 → 可读 UI | ❌ 无 `error_page` / 前端 413 映射 | **目标**：经 4173 代理超限时，UI 展示可读文案（可用 `masters.upload.errorTooLarge` 或映射信封），**禁止**把 nginx 默认 HTML 当主错误面 | **缺口**（可与 LR-C10 协同，但 LR-A3 Done 至少要有一条可测路径） |
| 病毒扫描 | ❌ 未实现 | **不做**（pending Q） | 仅文档 |

**计划文案校正（已确认）**

- 计划草稿曾写「例如 20MB/25MB」；**以 launch-readiness-gate 与现网配置为准，确认默认 50MB（文件）/ 60MB（请求与 nginx）**。
- 计划草稿曾建议新 key `api.error.master.invalidDocxContent`；**确认采用已落地的 `api.error.master.docxCorrupt`**，避免双 key。

---

## 3. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **母版设计人员** | `MASTER_DESIGNER`（及具备母版写权限的 `GROUP_ADMIN` / `GLOBAL_ADMIN`） | 上传/替换 DOCX 的主操作者 |
| **管理端 UI** | — | 选文件预检、展示 `messageKey` / 本地 i18n |
| **nginx（4173 前端容器）** | — | 请求体硬上限 |
| **Spring multipart** | — | 容器级文件/请求体上限 |

授权：无写权限 → 既有 fail-closed（403 / 路由阻断）；本规格不改变权限矩阵。

---

## 4. Goal

1. 仅接受 **真实 OOXML Word 包**（合法 `.docx`），拒绝伪装扩展名、错误 magic、损坏/非 OPC 包。  
2. 超过配置体积的文件 **不得入库、不得抽取锚点**。  
3. 拒绝原因对用户 **可翻译、可定位**（稳定 messageKey 或约定本地 key）。  
4. 合法 Word/LibreOffice `.docx` 上传/替换行为与加固前一致（happy path 无回归）。

---

## 5. Trigger

| # | 触发 | API / UI |
| --- | --- | --- |
| T1 | 创建母版并附带 DOCX | `POST /api/management/v1/masters`（multipart）+ `MasterUploadDialog` |
| T2 | 替换母版当前文件 | `PUT /api/management/v1/masters/{id}/file`（multipart）+ `MasterReplaceFileDialog` |

两入口 **共用** `validateDocxFile` 契约。

---

## 6. Preconditions

- 操作者已登录且对目标 `groupCode` / 母版具备写权限。  
- 替换时母版不处于禁止替换的状态（既有 `api.error.master.invalidState`，本规格不改）。  
- Docker 验收栈：前端经 nginx 代理 `/api/`（`client_max_body_size 60m`）。  
- 配置默认值如下（可用 env 覆盖，**BDD 断言默认值**）：

| 层 | 配置键 | 确认默认值 |
| --- | --- | --- |
| 服务业务上限 | `docgen.master.max-docx-upload-bytes` / `DOCGEN_MASTER_MAX_DOCX_UPLOAD_BYTES` | **52428800**（50 MiB） |
| Spring 单文件 | `spring.servlet.multipart.max-file-size` / `MAX_FILE_SIZE` | **50MB** |
| Spring 整请求 | `spring.servlet.multipart.max-request-size` / `MAX_REQUEST_SIZE` | **60MB** |
| nginx | `client_max_body_size` | **60m** |
| 前端预检 | `MAX_UPLOAD_BYTES`（创建对话框；替换应对齐） | **50 × 1024 × 1024** |

---

## 7. Primary journey（成功路径）

1. 母版设计人员选择 Word/LibreOffice 生成的合法 `.docx`（≤ 50MB）。  
2. （可选）前端预检通过。  
3. 请求经 nginx → Spring 接收 multipart → `validateDocxFile` 通过（后缀、体积、magic、OPC 必需条目）。  
4. 系统存储对象、抽取锚点、返回既有成功响应（创建返回详情；替换更新修订线）。  
5. **Then**：对象存储有新对象；无 `docxCorrupt` / `docxTooLarge`；行为与加固前一致。

---

## 8. System responses

### 8.1 成功

- HTTP **2xx**（既有契约）。  
- 文件已存储；锚点列表按既有规则返回。  
- 无校验类 `MasterValidationException`。

### 8.2 校验拒绝（到达服务且被 `validateDocxFile` 拒绝）

| 条件 | HTTP | `error.messageKey` | 副作用 |
| --- | --- | --- | --- |
| 空文件 / 缺文件 / 非 `.docx` 后缀 / 非法 Content-Type | **422** | `api.error.master.docxRequired` | 不存储、不抽锚点 |
| 文件字节数 **>** `max-docx-upload-bytes`（默认 50MB） | **422** | `api.error.master.docxTooLarge` | 同上 |
| 非 ZIP magic，或 ZIP 但缺 OPC 必需条目 / 无法解析为包 | **422** | `api.error.master.docxCorrupt` | 同上 |

`MasterExceptionAdvice` → `MASTER_VALIDATION_FAILED` + `UNPROCESSABLE_ENTITY`（既有）。

### 8.3 边界层超限（未进入或未完成 `validateDocxFile`）

| 层 | 典型条件 | 期望对外行为 |
| --- | --- | --- |
| nginx | 请求体 **> 60m** | **413**（或代理等价）；UI **不得**以原始 HTML 为唯一提示；应展示可读文案（例如 `masters.upload.errorTooLarge` 或后端信封映射） |
| Spring multipart | 单文件 **> 50MB** 或整请求 **> 60MB**，且请求已过 nginx | **JSON ErrorEnvelope**；推荐 messageKey **`api.error.master.docxTooLarge`**；状态码 **413 或 422**（实现选定一种并在测试中固定） |

> 说明：服务内 50MB 校验是 **权威业务上限**。50MB–60MB 窗口可能先被 Spring 拦截；>60MB 可能先被 nginx 拦截。三层均为防御纵深，用户体验上均须「可读、可翻译」。

---

## 9. Acceptance scenarios（Given / When / Then）

### A1 — 错误 magic bytes 拒绝

- **Given** 已认证母版设计人员；文件名为 `evil.docx`，内容为纯文本或非 `PK\x03\x04` 开头字节，且 Content-Type 为 OOXML Word 或 `application/octet-stream`  
- **When** 通过创建或替换 API 上传  
- **Then** HTTP **422**；`error.messageKey` = `api.error.master.docxCorrupt`；对象存储 **无** 新 put；锚点抽取 **未** 调用  

### A2 — 损坏 / 非 OPC 包拒绝

- **Given** 文件为合法 ZIP（含 `PK` magic）但缺少 `[Content_Types].xml` 或 `_rels/.rels` 或 `word/document.xml` 之一（或包解析抛错）  
- **When** 上传/替换  
- **Then** HTTP **422**；`messageKey` = `api.error.master.docxCorrupt`；无存储  

### A3 — 服务内超限拒绝

- **Given** 合法结构的 `.docx` 字节长度 **>** 配置的 `max-docx-upload-bytes`（测试可注入较小上限，如现有 4096 测例）  
- **When** 请求到达 `MasterDocumentService.create` / `replaceFile`  
- **Then** `MasterValidationException` → **422**；`messageKey` = `api.error.master.docxTooLarge`；无存储  

### A4 — 代理 / multipart 超限可读拒绝

- **Given** 验收栈经 `http://localhost:4173` 代理；上传体超过 nginx **60m**，**或** 超过 Spring multipart 上限但未进入业务校验  
- **When** 用户发起上传  
- **Then** 请求被拒绝；用户看到 **可读、本地化** 错误（映射到 `docxTooLarge` 或 `masters.upload.errorTooLarge`）；**不**把 nginx/Tomcat 默认 HTML 错误页当作主反馈  

### A5 — 合法 Word DOCX 成功（happy path）

- **Given** Word 或 LibreOffice 产出的真实 `.docx`，大小 ≤ 50MB，含至少一个可抽取锚点（既有锚点规则）  
- **When** 创建或替换  
- **Then** 成功响应与加固前一致；文件入库；锚点抽取执行；**不**返回 `docxCorrupt` / `docxTooLarge`  

### A6 — 后缀 / MIME 伪装（辅助）

- **Given** 文件名不以 `.docx` 结尾，**或** Content-Type 为 `text/html` 等非白名单且非空  
- **When** 上传  
- **Then** **422**；`messageKey` = `api.error.master.docxRequired`；无存储  

### A7 — 替换入口与创建入口契约一致

- **Given** 可写母版（非 `PENDING_REVIEW`）  
- **When** 对替换 API 重复 A1–A3、A5  
- **Then** 与创建 API **相同** status / messageKey / 无存储语义；替换对话框具备与创建对话框一致的客户端 50MB / `.docx` 预检（目标态）  

---

## 10. Boundary / exception

| ID | 边界 | 行为 |
| --- | --- | --- |
| B1 | 空 Multipart part | `docxRequired` / 422 |
| B2 | `PENDING_REVIEW` 时替换 | 既有 `invalidState`（本规格不改） |
| B3 | 合法 DOCX 但无锚点 | 既有 `anchorIntegrityFailed`（校验通过后的下游规则） |
| B4 | Content-Type 缺失 / null | **不**仅因缺失拒绝；继续 magic + OPC 探测 |
| B5 | 恰好 = 50MB | **接受**（`>` 才拒绝） |
| B6 | 病毒扫描 | **不执行**（§13） |
| B7 | 跨组写母版 | 既有 accessDenied / 403 fail-closed |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | 422 信封含 `messageKey`、`traceId`；成功路径 2xx + 详情 |
| 单测 | `MasterDocumentServiceValidationTest`：超限、后缀、corrupt、MIME；**补** magic-byte 专用用例 |
| 集成 / MockMvc | 可选：multipart 超限 handler 返回信封 |
| 前端 | 创建/替换对话框预检；API 错误经 `errorEnvelope` → `api.error.master.*` |
| Docker 手工 / E2E | 4173 上传合法小文件成功；超限或伪装文件可读失败（至少一条） |
| 配置 | `application.yml` + `nginx.conf` 默认值与 §6 表一致 |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| `docs/plan/detail/LRP-A-rendering-trust-hardening.md` § LR-A3 | 任务定义 |
| `docs/plan/launch-readiness-gate.md` | 50MB + deep validation 门禁项 |
| `docs/plan/launch-readiness-program.md` §1 finding 6 | 问题来源 |
| `docs/behavior/master-designer-lifecycle.md` | 上游生命周期；本规格细化上传校验边界 |
| `docs/requirements/requirements-plan.md` §待确认 | 病毒扫描 pending Q |
| i18n | `api.error.master.docxRequired` / `docxTooLarge` / `docxCorrupt` |

---

## 13. Open questions（不阻塞 ready）

| ID | 问题 | 状态 | Owner | 日期 |
| --- | --- | --- | --- | --- |
| **Q-LR-A3-01** | 是否在母版 DOCX 上传路径引入 **病毒 / 恶意软件扫描**（引擎选型、同步 vs 异步、失败策略）？ | **Pending** — **本切片明确不做** | 安全 / 产品 | 2026-07-10 |

无其它阻塞性问题。体积默认值、messageKey、校验深度（ZIP magic + OPC 必需条目）已在本规格确认。

---

## 14. BDD readiness

```
bdd_readiness: ready
task_ids: [LR-A3, lrp-a3-upload-validation]
owning_doc: docs/behavior/lrp-a3-master-docx-upload-validation.md
open_questions: [Q-LR-A3-01 virus scanning — deferred, non-blocking]
size_defaults: file=50MB; request=60MB; nginx=60m; service_bytes=52428800
```

**交给下一阶段**：`plan-orchestrator` 将缺口（magic 单测、替换对话框预检、multipart/nginx 可读超限）拆为可执行 TDD 任务；`backend-engineer` / `frontend-engineer` 按 §2 缺口表收口。
