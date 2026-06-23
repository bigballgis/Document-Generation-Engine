<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { isGroupScopedAuditRole } from '@/auth/roles'
import { useAuditStore } from '@/stores/audit'
import { useSessionStore } from '@/stores/session'
import type { LifecycleAuditEvent, ManagementAuditEvent } from '@/types/audit'
import type { TemplateLifecycleStatus } from '@/types/template'
import { downloadJsonExport } from '@/utils/downloadExport'
import { ElMessage } from 'element-plus'

const PAGE_SIZE = 10

const { t, te } = useI18n()
const auditStore = useAuditStore()
const sessionStore = useSessionStore()

const activeTab = ref<'management' | 'lifecycle'>('management')
const managementPage = ref(1)
const lifecyclePage = ref(1)

const errorMessage = computed(() => {
  const key = auditStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('audit.error.loadManagement')
})

const showGroupFilters = computed(() => isGroupScopedAuditRole(auditStore.actorRole))
const groupOptions = computed(() => sessionStore.session?.authorizedGroupCodes ?? [])

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

const paginatedManagementEvents = computed(() => {
  const start = (managementPage.value - 1) * PAGE_SIZE
  return filteredManagementEvents.value.slice(start, start + PAGE_SIZE)
})

const paginatedLifecycleEvents = computed(() => {
  const start = (lifecyclePage.value - 1) * PAGE_SIZE
  return filteredLifecycleEvents.value.slice(start, start + PAGE_SIZE)
})

function formatLifecycleState(state?: string) {
  if (!state) {
    return '—'
  }
  const key = `templates.status.${state as TemplateLifecycleStatus}`
  return te(key) ? t(key) : state
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

onMounted(async () => {
  auditStore.initializeFiltersFromSession()
  await refreshActiveTab()
})

async function refreshActiveTab() {
  try {
    if (activeTab.value === 'management') {
      await auditStore.fetchManagementEvents()
    } else {
      await auditStore.fetchLifecycleEvents()
    }
  } catch {
    // Error surfaced via store message key.
  }
}

async function handleTabChange(tab: string | number | boolean) {
  activeTab.value = tab as 'management' | 'lifecycle'
  await refreshActiveTab()
}

async function applyFilters() {
  managementPage.value = 1
  lifecyclePage.value = 1
  await refreshActiveTab()
}

async function handleExport() {
  try {
    const isManagement = activeTab.value === 'management'
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
        t(activeTab.value === 'management' ? 'audit.error.export' : 'audit.error.exportLifecycle'),
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
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

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
          <AppSearchSelect v-model="auditStore.filters.groupScope" clearable>
            <el-option
              v-for="group in groupOptions"
              :key="group"
              :label="group"
              :value="group"
            />
          </AppSearchSelect>
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
          <AppDataTable :data="paginatedManagementEvents" empty-text="">
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
            v-if="filteredManagementEvents.length > PAGE_SIZE"
            v-model:current-page="managementPage"
            class="table-pagination"
            layout="prev, pager, next"
            :page-size="PAGE_SIZE"
            :total="filteredManagementEvents.length"
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
          <AppDataTable :data="paginatedLifecycleEvents" empty-text="">
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
            v-if="filteredLifecycleEvents.length > PAGE_SIZE"
            v-model:current-page="lifecyclePage"
            class="table-pagination"
            layout="prev, pager, next"
            :page-size="PAGE_SIZE"
            :total="filteredLifecycleEvents.length"
          />
        </template>
      </el-tab-pane>
    </el-tabs>
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
