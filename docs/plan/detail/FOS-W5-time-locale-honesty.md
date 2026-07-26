# FOS-W5 — Time & locale honesty

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W5 · **Status:** **Not Started**
**Slice id:** `fos-time-locale-honesty` · worktree `../DGE-fos-time-locale-honesty` · branch `feat/fos-time-locale-honesty`
**Task Master:** **#175** · **delivery_lane:** **full**
**Origin:** D2, D3, D9, D15, D17

---

## Before code

```powershell
git worktree add "..\DGE-fos-time-locale-honesty" -b feat/fos-time-locale-honesty origin/main
```

### Tasks

| Id | Sev | What |
| --- | --- | --- |
| **W5-1** | **P0** | Fix datetime pickers that append literal `Z` to local wall-clock |
| **W5-2** | **P0** | `useLocaleFormatters` must show timezone (or explicit UTC) |
| **W5-3** | **P1** | Wire Element Plus `ElConfigProvider` locale to app locale |
| **W5-4** | **P1** | Collaboration / audit server-resolved summaries localisable |
| **W5-5** | **P2** | `CollaborationTimeoutConfigPanel` uses `formatDateTime` |

---

<a id="w5-1"></a>
## W5-1 — Legal-hold / audit / invocations datetime `Z` lie

**Files:**
- `frontend/src/components/legalHold/LegalHoldCreateDialog.vue`
- `frontend/src/components/audit/AuditConsoleFilters.vue`
- `frontend/src/views/api/ApiInvocationsView.vue`

**Evidence:** `value-format="YYYY-MM-DDTHH:mm:ss[Z]"` — Element Plus formats **local** time and
appends a literal `Z`. Labels claim UTC (`effectiveFrom: 'Effective from (UTC)'`).

### Implement

Convert selected local value to true UTC ISO before send (`dayjs(v).utc().toISOString()` or
project-standard dayjs helper — grep existing UTC helpers first). Prefer a shared util used
by all three pickers. Red test: picking a known local instant produces the correct UTC
string (mock timezone if the test harness supports it; otherwise assert the util function).

### Do NOT

- Do not change backend Instant parsing semantics beyond accepting correct ISO UTC.

---

<a id="w5-2"></a>
## W5-2 — Timestamps render without zone

**File:** `frontend/src/composables/useLocaleFormatters.ts`

```ts
return date.toLocaleString(locale.value)
```

### Implement

Include `timeZoneName: 'short'` **or** force `timeZone: 'UTC'` with a visible `UTC` suffix
consistent with the two form labels that already say UTC. Document the chosen convention in
a one-line comment. Update Vitest for the formatter.

Audit console + legal-hold surfaces must match the convention (no mixed local/UTC without labels).

---

<a id="w5-3"></a>
## W5-3 — Element Plus stays English under 中文

**Files:** `frontend/src/main.ts` / `App.vue` / shell root — there is **no** `ElConfigProvider` today.

### Implement

Wrap the app/shell in `<el-config-provider :locale="elementLocale">` driven by the existing
locale store. Import `element-plus/es/locale/lang/zh-cn` and `en`. Cover with a smoke test
or existing i18n brand E2E assertion that pagination/empty text is Chinese when locale is zh.

### Do NOT

- Do not replace app i18n; only sync Element Plus.

---

<a id="w5-4"></a>
## W5-4 — Server-resolved English summaries under 中文

**Surfaces:** Task Hub collaboration `summaryText` (backend resolves
`api.collaboration.workItem.*.summary` via `messages_en.properties` only); similar
`api.audit.lifecycle.*` / `api.contract.adGroup*` patterns.

### Minimal fix (no new feature)

**Preferred:** return `messageKey` (+ args) from the API views that currently return
pre-resolved English, and translate on the client with existing catalogues.

If that requires OpenAPI churn beyond this leaf’s budget, **minimum acceptable fix:**
add `messages_zh_CN.properties` (or Spring basename locale file) with the collaboration +
audit lifecycle keys so server resolution matches Accept-Language / user locale **only if**
the backend already selects locale from the session — verify `MessageResolver` behaviour
first. Do not invent a locale negotiation scheme.

---

<a id="w5-5"></a>
## W5-5 — Raw ISO in timeout config panel

**File:** `frontend/src/components/collaboration/CollaborationTimeoutConfigPanel.vue`

Route `config.updatedAt` through `formatDateTime`.

---

## Exit

Pickers send real UTC; timestamps show zone; Element Plus follows app locale; FE+BE gates
green; E2E zh locale smoke; UIUX datetime fields both brands.

TM **#175** → done on closeout.
