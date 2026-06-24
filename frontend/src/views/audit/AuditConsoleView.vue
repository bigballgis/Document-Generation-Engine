<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { isGroupScopedAuditRole } from '@/auth/roles'
import { useAuditStore } from '@/stores/audit'
import type { LifecycleAuditEvent, ManagementAuditEvent } from '@/types/audit'
import type { TemplateLifecycleStatus } from '@/types/template'
import { downloadJsonExport } from '@/utils/downloadExport'
import { validateGroupAdminAuditFilters } from '@/views/audit/auditFilterValidation'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const auditStore = useAuditStore()

const activeTab = ref<'management' | 'lifecycle'>('management')
const loadFailed = ref(false)
const filterValidationKey = ref<string | null>(null)

const loadErrorMessageKey = computed(() => {
  if (auditStore.lastErrorMessageKey) {
    return auditStore.lastErrorMessageKey
  }
  return activeTab.value === 'management'
    ? 'audit.error.loadManagement'
    : 'audit.error.loadLifecycle'
})

const errorMessage = computed(() => {
  const key = auditStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('audit.error.loadManagement')
})

const showGroupFilters = computed(() => isGroupScopedAuditRole(auditStore.actorRole))
const { isGroupLocked: isAuditGroupLocked } = useScopedGroupOptions()

const managementSource = computed(() => auditStore.managementEvents)
const {
  filters: managementColumnFilters,
  filteredRows: filteredManagementEvents,
  hasActiveFilters: hasManagementColumnFilters,
  clearFilters: clearManagementColumnFilters,
} = useDataTableFilters(managementSource, [
  { key: 'eventAt', getValue: (row) => formatDate(row.eventAt) },
  { key: 'eventType', getValue: (row) => row.eventType ?? '' },
  { key: 'templateId', getValue: (row) => row.templateId ?? '' },
  { key: 'actorSummary', getValue: (row) => row.actorSummary ?? '' },
  { key: 'statusSummary', getValue: (row) => row.statusSummary ?? '' },
])

const lifecycleSource = computed(() => auditStore.lifecycleEvents)
const {
  filters: lifecycleColumnFilters,
  filteredRows: filteredLifecycleEvents,
  hasActiveFilters: hasLifecycleColumnFilters,
  clearFilters: clearLifecycleColumnFilters,
} = useDataTableFilters(lifecycleSource, [
  { key: 'eventAt', getValue: (row) => formatDate(row.eventAt) },
  { key: 'eventType', getValue: (row) => row.eventType ?? '' },
  { key: 'templateId', getValue: (row) => row.templateId ?? '' },
  { key: 'fromState', getValue: (row) => formatLifecycleState(row.fromState) },
  { key: 'toState', getValue: (row) => formatLifecycleState(row.toState) },
  { key: 'summary', getValue: (row) => row.summary ?? '' },
])

const managementUiPage = computed({
  get: () => auditStore.managementPage + 1,
  set: (page: number) => {
    void auditStore.fetchManagementEvents(page - 1)
  },
})

const lifecycleUiPage = computed({
  get: () => auditStore.lifecyclePage + 1,
  set: (page: number) => {
    void auditStore.fetchLifecycleEvents(page - 1)
  },
})

function formatLifecycleState(state?: string) {
  if (!state) {
    return '—'
  }
  const key = `templates.status.${state as TemplateLifecycleStatus}`
  return te(key) ? t(key) : state
}

function formatDate(value: string) {
  return formatDateTime(value)
}

function exportScopeSummary(): string {
  const parts: string[] = []
  if (auditStore.filters.eventType?.trim()) {
    parts.push(`${t('audit.filters.eventType')}: ${auditStore.filters.eventType.trim()}`)
  }
  if (auditStore.filters.eventAtFrom?.trim()) {
    parts.push(`${t('audit.filters.eventAtFrom')}: ${auditStore.filters.eventAtFrom.trim()}`)
  }
  if (auditStore.filters.eventAtTo?.trim()) {
    parts.push(`${t('audit.filters.eventAtTo')}: ${auditStore.filters.eventAtTo.trim()}`)
  }
  if (auditStore.filters.groupScope?.trim()) {
    parts.push(`${t('audit.filters.groupScope')}: ${auditStore.filters.groupScope.trim()}`)
  }
  if (auditStore.filters.templateId?.trim()) {
    parts.push(`${t('audit.filters.templateId')}: ${auditStore.filters.templateId.trim()}`)
  }
  if (parts.length === 0) {
    return t('audit.export.scopeAll')
  }
  return parts.join('\n')
}

onMounted(async () => {
  auditStore.initializeFiltersFromSession()
  await refreshActiveTab()
})

watch(activeTab, () => {
  void refreshActiveTab()
})

async function refreshActiveTab() {
  if (showGroupFilters.value) {
    filterValidationKey.value = validateGroupAdminAuditFilters(auditStore.filters)
    if (filterValidationKey.value) {
      return
    }
  } else {
    filterValidationKey.value = null
  }

  loadFailed.value = false
  try {
    if (activeTab.value === 'management') {
      await auditStore.fetchManagementEvents(auditStore.managementPage)
    } else {
      await auditStore.fetchLifecycleEvents(auditStore.lifecyclePage)
    }
  } catch {
    loadFailed.value = true
  }
}

async function handleTabChange(tab: string | number | boolean) {
  activeTab.value = tab as 'management' | 'lifecycle'
}

async function applyFilters() {
  if (showGroupFilters.value) {
    filterValidationKey.value = validateGroupAdminAuditFilters(auditStore.filters)
    if (filterValidationKey.value) {
      return
    }
  } else {
    filterValidationKey.value = null
  }

  loadFailed.value = false
  try {
    if (activeTab.value === 'management') {
      await auditStore.fetchManagementEvents(0)
    } else {
      await auditStore.fetchLifecycleEvents(0)
    }
  } catch {
    loadFailed.value = true
  }
}

async function resetFilters() {
  auditStore.resetFilters()
  filterValidationKey.value = null
  await applyFilters()
}

async function handleExport() {
  const isManagement = activeTab.value === 'management'
  try {
    await ElMessageBox.confirm(exportScopeSummary(), t('audit.export.confirmTitle'), {
      type: 'info',
      confirmButtonText: t('audit.export.confirmAction'),
      cancelButtonText: t('audit.export.cancelAction'),
    })
  } catch {
    return
  }
  try {
    const result = isManagement
      ? await auditStore.exportManagementEvents()
      : await auditStore.exportLifecycleEvents()
    downloadJsonExport(
      t(isManagement ? 'audit.export.managementFilename' : 'audit.export.lifecycleFilename'),
      result,
    )
    ElMessage.success(
      t(isManagement ? 'audit.export.success' : 'audit.export.lifecycleSuccess'),
    )
  } catch {
    ElMessage.error(
      errorMessage.value ||
        t(isManagement ? 'audit.error.export' : 'audit.error.exportLifecycle'),
    )
  }
}

const sortManagementByEventAt = rowSortMethod<ManagementAuditEvent>((row) => row.eventAt)
const sortLifecycleByEventAt = rowSortMethod<LifecycleAuditEvent>((row) => row.eventAt)
const sortLifecycleFromState = rowSortMethod<LifecycleAuditEvent>((row) =>
  formatLifecycleState(row.fromState),
)
const sortLifecycleToState = rowSortMethod<LifecycleAuditEvent>((row) =>
  formatLifecycleState(row.toState),
)
</script>

<template>
  <div class="audit-page">
    <header class="page-header">
      <div>
        <h1>{{ t('audit.title') }}</h1>
        <p>{{ t('audit.description') }}</p>
      </div>
      <el-button
        type="primary"
        :loading="auditStore.exporting"
        @click="handleExport"
      >
        {{ t('audit.export.action') }}
      </el-button>
    </header>

    <el-alert
      v-if="filterValidationKey"
      class="page-alert"
      type="warning"
      :title="t(filterValidationKey)"
      show-icon
      :closable="false"
    />

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="loadErrorMessageKey"
      @retry="refreshActiveTab"
    />

    <template v-else>
    <el-card shadow="never" class="filters-card">
      <div class="filters-grid">
        <el-form-item :label="t('audit.filters.eventType')">
          <el-input
            v-model="auditStore.filters.eventType"
            :placeholder="t('audit.filters.eventTypePlaceholder')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('audit.filters.eventAtFrom')">
          <el-date-picker
            v-model="auditStore.filters.eventAtFrom"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss[Z]"
            :placeholder="t('audit.filters.eventAtFrom')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('audit.filters.eventAtTo')">
          <el-date-picker
            v-model="auditStore.filters.eventAtTo"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss[Z]"
            :placeholder="t('audit.filters.eventAtTo')"
            clearable
          />
        </el-form-item>
        <el-form-item v-if="showGroupFilters" :label="t('audit.filters.groupScope')">
          <ScopedGroupSelect
            v-model="auditStore.filters.groupScope"
            :clearable="!isAuditGroupLocked"
          />
        </el-form-item>
        <el-form-item v-if="showGroupFilters" :label="t('audit.filters.templateId')">
          <el-input
            v-model="auditStore.filters.templateId"
            :placeholder="t('audit.filters.templateIdPlaceholder')"
            clearable
          />
        </el-form-item>
        <div class="filters-actions">
          <el-button type="primary" @click="applyFilters">
            {{ t('audit.filters.apply') }}
          </el-button>
          <el-button text @click="resetFilters">
            {{ t('audit.filters.reset') }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-tabs :model-value="activeTab" @tab-change="handleTabChange">
      <el-tab-pane :label="t('audit.tabs.management')" name="management">
        <el-skeleton v-if="auditStore.loadingManagement" :rows="6" animated />
        <template v-else>
          <div v-if="hasManagementColumnFilters" class="table-toolbar">
            <el-button size="small" text @click="clearManagementColumnFilters">
              {{ t('table.clearFilters') }}
            </el-button>
          </div>
          <AppDataTable :data="filteredManagementEvents" empty-text="">
            <template #empty>
              <el-empty :description="t('audit.empty.management')" />
            </template>
            <el-table-column
              sortable
              :sort-method="sortManagementByEventAt"
              min-width="180"
            >
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.eventAt')"
                  v-model="managementColumnFilters.eventAt"
                />
              </template>
              <template #default="{ row }: { row: ManagementAuditEvent }">
                {{ formatDate(row.eventAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="eventType" sortable min-width="160">
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.eventType')"
                  v-model="managementColumnFilters.eventType"
                />
              </template>
            </el-table-column>
            <el-table-column prop="templateId" sortable min-width="200">
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.templateId')"
                  v-model="managementColumnFilters.templateId"
                />
              </template>
            </el-table-column>
            <el-table-column prop="actorSummary" sortable min-width="160">
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.actorSummary')"
                  v-model="managementColumnFilters.actorSummary"
                />
              </template>
            </el-table-column>
            <el-table-column prop="statusSummary" sortable min-width="160">
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.statusSummary')"
                  v-model="managementColumnFilters.statusSummary"
                />
              </template>
            </el-table-column>
          </AppDataTable>
          <el-pagination
            v-if="auditStore.managementTotalElements > auditStore.pageSize"
            v-model:current-page="managementUiPage"
            class="table-pagination"
            layout="total, prev, pager, next"
            :page-size="auditStore.pageSize"
            :total="auditStore.managementTotalElements"
          />
        </template>
      </el-tab-pane>

      <el-tab-pane :label="t('audit.tabs.lifecycle')" name="lifecycle">
        <el-skeleton v-if="auditStore.loadingLifecycle" :rows="6" animated />
        <template v-else>
          <div v-if="hasLifecycleColumnFilters" class="table-toolbar">
            <el-button size="small" text @click="clearLifecycleColumnFilters">
              {{ t('table.clearFilters') }}
            </el-button>
          </div>
          <AppDataTable :data="filteredLifecycleEvents" empty-text="">
            <template #empty>
              <el-empty :description="t('audit.empty.lifecycle')" />
            </template>
            <el-table-column
              sortable
              :sort-method="sortLifecycleByEventAt"
              min-width="180"
            >
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.eventAt')"
                  v-model="lifecycleColumnFilters.eventAt"
                />
              </template>
              <template #default="{ row }: { row: LifecycleAuditEvent }">
                {{ formatDate(row.eventAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="eventType" sortable min-width="160">
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.eventType')"
                  v-model="lifecycleColumnFilters.eventType"
                />
              </template>
            </el-table-column>
            <el-table-column prop="templateId" sortable min-width="200">
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.templateId')"
                  v-model="lifecycleColumnFilters.templateId"
                />
              </template>
            </el-table-column>
            <el-table-column
              sortable
              :sort-method="sortLifecycleFromState"
              width="140"
            >
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.fromState')"
                  v-model="lifecycleColumnFilters.fromState"
                />
              </template>
              <template #default="{ row }: { row: LifecycleAuditEvent }">
                {{ formatLifecycleState(row.fromState) }}
              </template>
            </el-table-column>
            <el-table-column
              sortable
              :sort-method="sortLifecycleToState"
              width="140"
            >
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.toState')"
                  v-model="lifecycleColumnFilters.toState"
                />
              </template>
              <template #default="{ row }: { row: LifecycleAuditEvent }">
                {{ formatLifecycleState(row.toState) }}
              </template>
            </el-table-column>
            <el-table-column prop="summary" sortable min-width="200">
              <template #header>
                <TableColumnHeader
                  :label="t('audit.columns.summary')"
                  v-model="lifecycleColumnFilters.summary"
                />
              </template>
            </el-table-column>
          </AppDataTable>
          <el-pagination
            v-if="auditStore.lifecycleTotalElements > auditStore.pageSize"
            v-model:current-page="lifecycleUiPage"
            class="table-pagination"
            layout="total, prev, pager, next"
            :page-size="auditStore.pageSize"
            :total="auditStore.lifecycleTotalElements"
          />
        </template>
      </el-tab-pane>
    </el-tabs>
    </template>
  </div>
</template>

<style scoped lang="scss">
.audit-page {
  padding: 2rem;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;

  h1 {
    margin: 0 0 0.25rem;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.page-alert {
  margin-bottom: 1rem;
}

.filters-card {
  margin-bottom: 1.5rem;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.75rem 1rem;
  align-items: end;
}

.filters-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}

.table-pagination {
  margin-top: 1rem;
  justify-content: flex-end;
}

.table-toolbar {
  margin-bottom: 0.75rem;
}
</style>
