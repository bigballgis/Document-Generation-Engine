<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import type { RouteLocationRaw } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { rowSortMethod } from '@/composables/useDataTableFilters'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useAuditEventTypeOptions } from '@/composables/useAuditEventTypeOptions'
import { useAuditTemplateFilterOptions } from '@/composables/useAuditTemplateFilterOptions'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { isGroupScopedAuditRole } from '@/auth/roles'
import { ROUTE_KEYS, templatePackageHubPath } from '@/routing/routeKeys'
import { useAuditStore } from '@/stores/audit'
import { useSessionStore } from '@/stores/session'
import type { LifecycleAuditEvent, ManagementAuditEvent } from '@/types/audit'
import type { TemplateLifecycleStatus } from '@/types/template'
import { resolveAuditActorDisplay, resolveAuditTemplateDisplay } from '@/utils/auditEntityDisplay'
import type { AuditActorDisplayFields } from '@/utils/auditEntityDisplay'
import { downloadJsonExport } from '@/utils/downloadExport'
import { shouldShowAuditAdminJourney } from '@/utils/auditAdminJourney'
import { formatAuditEventType } from '@/utils/auditEventLabels'
import { validateGroupAdminAuditFilters } from '@/views/audit/auditFilterValidation'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t, te } = useI18n()
const route = useRoute()
const { formatDateTime } = useLocaleFormatters()
const auditStore = useAuditStore()
const sessionStore = useSessionStore()

const activeTab = ref<'management' | 'lifecycle'>('management')
const loadFailed = ref(false)
const filterValidationKey = ref<string | null>(null)

const { reload: reloadActiveTab, signal: auditAbortSignal } = useAbortableCatalogLoader(async (signal) => {
  if (activeTab.value === 'management') {
    await auditStore.fetchManagementEvents(auditStore.managementPage, { signal })
    return
  }
  await auditStore.fetchLifecycleEvents(auditStore.lifecyclePage, { signal })
})

const showAuditAdminJourney = computed(() =>
  shouldShowAuditAdminJourney({ roles: sessionStore.session?.roles ?? [] }),
)

const eventLabelTranslator = computed(() => ({
  translate: t,
  hasKey: te,
}))

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
const auditEventTypeOptions = useAuditEventTypeOptions()
const { templateOptions, loadingTemplates, searchTemplates } = useAuditTemplateFilterOptions()
const canLinkTemplates = computed(() => sessionStore.canAccessRoute(ROUTE_KEYS.templateManagement))

const managementSource = computed(() => auditStore.managementEvents)
const lifecycleSource = computed(() => auditStore.lifecycleEvents)

const managementUiPage = computed({
  get: () => auditStore.managementPage + 1,
  set: (page: number) => {
    void auditStore.fetchManagementEvents(page - 1, { signal: auditAbortSignal.value })
  },
})

const lifecycleUiPage = computed({
  get: () => auditStore.lifecyclePage + 1,
  set: (page: number) => {
    void auditStore.fetchLifecycleEvents(page - 1, { signal: auditAbortSignal.value })
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

function formatEventType(eventType?: string) {
  if (!eventType) {
    return '—'
  }
  return formatAuditEventType(eventType, eventLabelTranslator.value)
}

function formatActor(event: AuditActorDisplayFields) {
  return resolveAuditActorDisplay(event)
}

function resolveTemplateCell(
  event: Pick<
    ManagementAuditEvent,
    'templateId' | 'templateDisplayName' | 'templateExternalId'
  >,
) {
  const display = resolveAuditTemplateDisplay(event)
  const to: RouteLocationRaw | undefined =
    event.templateId && canLinkTemplates.value
      ? templatePackageHubPath(event.templateId)
      : undefined
  return { ...display, to }
}

function handleTemplateFilterSearch(query: string) {
  void searchTemplates(query)
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
  if (auditStore.filters.requestId?.trim()) {
    parts.push(`${t('audit.filters.requestId')}: ${auditStore.filters.requestId.trim()}`)
  }
  if (parts.length === 0) {
    return t('audit.export.scopeAll')
  }
  return parts.join('\n')
}

function applyRequestIdFromRouteQuery() {
  const raw = route.query.requestId
  const requestId = Array.isArray(raw) ? raw[0] : raw
  if (typeof requestId === 'string' && requestId.trim().length > 0) {
    auditStore.filters.requestId = requestId.trim()
  }
}

onMounted(async () => {
  auditStore.initializeFiltersFromSession()
  applyRequestIdFromRouteQuery()
  if (showGroupFilters.value) {
    await searchTemplates('')
  }
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
    await reloadActiveTab()
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
      await auditStore.fetchManagementEvents(0, { signal: auditAbortSignal.value })
    } else {
      await auditStore.fetchLifecycleEvents(0, { signal: auditAbortSignal.value })
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

const sortManagementByActor = rowSortMethod<ManagementAuditEvent>((row) => formatActor(row))
const sortManagementByTemplate = rowSortMethod<ManagementAuditEvent>(
  (row) => resolveAuditTemplateDisplay(row).label,
)
const sortLifecycleByActor = rowSortMethod<LifecycleAuditEvent>((row) => formatActor(row))
const sortLifecycleByTemplate = rowSortMethod<LifecycleAuditEvent>(
  (row) => resolveAuditTemplateDisplay(row).label,
)
const sortManagementByEventType = rowSortMethod<ManagementAuditEvent>((row) =>
  formatEventType(row.eventType),
)
const sortManagementByEventAt = rowSortMethod<ManagementAuditEvent>((row) => row.eventAt)
const sortLifecycleByEventType = rowSortMethod<LifecycleAuditEvent>((row) =>
  formatEventType(row.eventType),
)
const sortLifecycleByEventAt = rowSortMethod<LifecycleAuditEvent>((row) => row.eventAt)
const sortLifecycleFromState = rowSortMethod<LifecycleAuditEvent>((row) =>
  formatLifecycleState(row.fromState),
)
const sortLifecycleToState = rowSortMethod<LifecycleAuditEvent>((row) =>
  formatLifecycleState(row.toState),
)
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('audit.title')"
      :description="t('audit.description')"
    >
      <template v-if="showAuditAdminJourney" #meta>
        <el-tag type="info" effect="plain">
          {{ t('audit.viewOnly.banner') }}
        </el-tag>
      </template>
      <template #actions>
        <el-button
          type="primary"
          :loading="auditStore.exporting"
          @click="handleExport"
        >
          {{ t('audit.export.action') }}
        </el-button>
      </template>
    </PageHeader>

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
      :retryable="auditStore.lastListErrorRetryable"
      @retry="refreshActiveTab"
    />

    <template v-else>
    <el-card shadow="never" class="filters-card">
      <div class="filters-grid">
        <el-form-item :label="t('audit.filters.eventType')">
          <AppSearchSelect
            v-model="auditStore.filters.eventType"
            clearable
            :placeholder="t('audit.filters.eventTypePlaceholder')"
          >
            <el-option
              v-for="option in auditEventTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </AppSearchSelect>
        </el-form-item>
        <el-form-item :label="t('audit.filters.requestId')">
          <el-input
            v-model="auditStore.filters.requestId"
            clearable
            :placeholder="t('audit.filters.requestIdPlaceholder')"
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
          <AppSearchSelect
            v-model="auditStore.filters.templateId"
            clearable
            filterable
            remote
            :remote-method="handleTemplateFilterSearch"
            :loading="loadingTemplates"
            :placeholder="t('audit.filters.templateIdPlaceholder')"
          >
            <el-option
              v-for="option in templateOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </AppSearchSelect>
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
          <AppDataTable :data="managementSource" empty-text="">
            <template #empty>
              <el-empty :description="t('audit.empty.management')" />
            </template>
            <el-table-column
              prop="actorSummary"
              sortable
              :sort-method="sortManagementByActor"
              min-width="160"
              :label="t('audit.columns.actorSummary')"
            >
              <template #default="{ row }: { row: ManagementAuditEvent }">
                {{ formatActor(row) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="eventType"
              sortable
              :sort-method="sortManagementByEventType"
              min-width="180"
              :label="t('audit.columns.eventType')"
            >
              <template #default="{ row }: { row: ManagementAuditEvent }">
                {{ formatEventType(row.eventType) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="templateId"
              sortable
              :sort-method="sortManagementByTemplate"
              min-width="200"
              :label="t('audit.columns.templateId')"
            >
              <template #default="{ row }: { row: ManagementAuditEvent }">
                <EntityLinkCell
                  v-bind="resolveTemplateCell(row)"
                />
              </template>
            </el-table-column>
            <el-table-column
              sortable
              :sort-method="sortManagementByEventAt"
              min-width="180"
              :label="t('audit.columns.eventAt')"
            >
              <template #default="{ row }: { row: ManagementAuditEvent }">
                {{ formatDate(row.eventAt) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="statusSummary"
              sortable
              min-width="160"
              :label="t('audit.columns.statusSummary')"
            />
          </AppDataTable>
          <AppTablePagination
            v-model:current-page="managementUiPage"
            :page-size="auditStore.pageSize"
            :total="auditStore.managementTotalElements"
          />
        </template>
      </el-tab-pane>

      <el-tab-pane :label="t('audit.tabs.lifecycle')" name="lifecycle">
        <el-skeleton v-if="auditStore.loadingLifecycle" :rows="6" animated />
        <template v-else>
          <AppDataTable :data="lifecycleSource" empty-text="">
            <template #empty>
              <el-empty :description="t('audit.empty.lifecycle')" />
            </template>
            <el-table-column
              prop="actorId"
              sortable
              :sort-method="sortLifecycleByActor"
              min-width="140"
              :label="t('audit.columns.actorSummary')"
            >
              <template #default="{ row }: { row: LifecycleAuditEvent }">
                {{ formatActor(row) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="eventType"
              sortable
              :sort-method="sortLifecycleByEventType"
              min-width="180"
              :label="t('audit.columns.eventType')"
            >
              <template #default="{ row }: { row: LifecycleAuditEvent }">
                {{ formatEventType(row.eventType) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="templateId"
              sortable
              :sort-method="sortLifecycleByTemplate"
              min-width="200"
              :label="t('audit.columns.templateId')"
            >
              <template #default="{ row }: { row: LifecycleAuditEvent }">
                <EntityLinkCell
                  v-bind="resolveTemplateCell(row)"
                />
              </template>
            </el-table-column>
            <el-table-column
              sortable
              :sort-method="sortLifecycleFromState"
              width="140"
              :label="t('audit.columns.fromState')"
            >
              <template #default="{ row }: { row: LifecycleAuditEvent }">
                {{ formatLifecycleState(row.fromState) }}
              </template>
            </el-table-column>
            <el-table-column
              sortable
              :sort-method="sortLifecycleToState"
              width="140"
              :label="t('audit.columns.toState')"
            >
              <template #default="{ row }: { row: LifecycleAuditEvent }">
                {{ formatLifecycleState(row.toState) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="summary"
              sortable
              min-width="200"
              :label="t('audit.columns.summary')"
            />
            <el-table-column
              sortable
              :sort-method="sortLifecycleByEventAt"
              min-width="180"
              :label="t('audit.columns.eventAt')"
            >
              <template #default="{ row }: { row: LifecycleAuditEvent }">
                {{ formatDate(row.eventAt) }}
              </template>
            </el-table-column>
          </AppDataTable>
          <AppTablePagination
            v-model:current-page="lifecycleUiPage"
            :page-size="auditStore.pageSize"
            :total="auditStore.lifecycleTotalElements"
          />
        </template>
      </el-tab-pane>
    </el-tabs>
    </template>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}

.filters-card {
  margin-bottom: var(--space-6);
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-3) var(--space-4);
  align-items: end;
}

.filters-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}
</style>
