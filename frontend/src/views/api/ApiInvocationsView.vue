<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import InvocationSummaryDrawer from '@/components/templates/InvocationSummaryDrawer.vue'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import type { CrossPackageInvocationRow } from '@/composables/externalServicesOpsCompose'
import { useCrossPackageInvocations } from '@/composables/useCrossPackageInvocations'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { apiPackageSettingsPath } from '@/routing/apiPackageSettings'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { formatDateTime } = useLocaleFormatters()

const {
  pageSize,
  totalElements,
  loading,
  loadFailed,
  compositionLimited,
  rows,
  packageOptions,
  filterDraft,
  drawerVisible,
  selectedRow,
  statusFilterOptions,
  uiPage,
  loadInvocations,
  applyFilters,
  clearFilters,
  openDetail,
  seedTemplateFilter,
} = useCrossPackageInvocations({ autoLoad: false })

const packageSelectOptions = computed(() =>
  packageOptions.value.map((item) => ({
    value: item.id,
    label: item.name,
    subtitle: item.externalId,
  })),
)

const { onRowClick: activateRow } = useActivatableTableRow<CrossPackageInvocationRow>((row) =>
  openDetail(row),
)

function openPackageSettings(templateId: string) {
  void router.push(apiPackageSettingsPath(templateId))
}

function openDashboard() {
  void router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.apiPolicyManagement])
}

onMounted(async () => {
  const templateFromQuery = route.query.templateId
  if (typeof templateFromQuery === 'string' && templateFromQuery.length > 0) {
    seedTemplateFilter(templateFromQuery)
  }
  await loadInvocations()
})

watch(
  () => route.query.templateId,
  (value) => {
    if (typeof value === 'string' && value.length > 0 && value !== filterDraft.templateId) {
      seedTemplateFilter(value)
      void loadInvocations()
    }
  },
)
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('apiPolicy.invocationsPage.title')"
      :description="t('apiPolicy.invocationsPage.description')"
    >
      <template #actions>
        <el-button data-testid="api-invocations-back-dashboard" @click="openDashboard">
          {{ t('apiPolicy.invocationsPage.backToDashboard') }}
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="compositionLimited"
      class="composition-limit"
      type="info"
      show-icon
      :closable="false"
      data-testid="api-invocations-composition-limit"
      :title="t('apiPolicy.invocationsPage.compositionLimitTitle')"
      :description="t('apiPolicy.invocationsPage.compositionLimitDescription')"
    />

    <div class="filters-row" data-testid="api-invocations-filters">
      <el-form-item :label="t('apiPolicy.invocationsPage.filters.status')" class="filter-item">
        <el-select
          v-model="filterDraft.status"
          clearable
          data-testid="api-invocations-filter-status"
          :placeholder="t('apiPolicy.invocationsPage.filters.statusPlaceholder')"
        >
          <el-option
            v-for="option in statusFilterOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('apiPolicy.invocationsPage.filters.package')" class="filter-item">
        <AppSearchSelect
          v-model="filterDraft.templateId"
          clearable
          filterable
          data-testid="api-invocations-filter-package"
          :placeholder="t('apiPolicy.invocationsPage.filters.packagePlaceholder')"
        >
          <el-option
            v-for="option in packageSelectOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          >
            <span>{{ option.label }}</span>
            <span class="option-subtitle">{{ option.subtitle }}</span>
          </el-option>
        </AppSearchSelect>
      </el-form-item>
      <el-form-item :label="t('apiPolicy.invocationsPage.filters.requestId')" class="filter-item">
        <el-input
          v-model="filterDraft.requestId"
          clearable
          data-testid="api-invocations-filter-request-id"
          :placeholder="t('apiPolicy.invocationsPage.filters.requestIdPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('apiPolicy.invocationsPage.filters.createdAfter')" class="filter-item">
        <el-date-picker
          v-model="filterDraft.createdAfter"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss[Z]"
          data-testid="api-invocations-filter-created-after"
        />
      </el-form-item>
      <el-form-item :label="t('apiPolicy.invocationsPage.filters.createdBefore')" class="filter-item">
        <el-date-picker
          v-model="filterDraft.createdBefore"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss[Z]"
          data-testid="api-invocations-filter-created-before"
        />
      </el-form-item>
      <div class="filters-actions">
        <el-button type="primary" data-testid="api-invocations-apply-filters" @click="applyFilters">
          {{ t('apiPolicy.invocationsPage.filters.apply') }}
        </el-button>
        <el-button data-testid="api-invocations-clear-filters" @click="clearFilters">
          {{ t('apiPolicy.invocationsPage.filters.clear') }}
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="section-card" data-testid="api-invocations-table-card">
      <LoadErrorPanel
        v-if="loadFailed && !loading"
        message-key="apiPolicy.invocationsPage.loadFailed"
        :retryable="true"
        @retry="loadInvocations"
      />

      <el-skeleton v-else-if="loading" :rows="6" animated />

      <template v-else>
        <AppDataTable activatable :data="rows" @row-click="activateRow">
          <template #empty>
            <EmptyStatePanel
              title-key="apiPolicy.invocationsPage.emptyTitle"
              description-key="apiPolicy.invocationsPage.emptyDescription"
            />
          </template>
          <el-table-column
            :label="t('apiPolicy.invocationsPage.columns.createdAt')"
            min-width="170"
          >
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('apiPolicy.invocationsPage.columns.package')"
            min-width="220"
          >
            <template #default="{ row }">
              <EntityLinkCell
                :label="row.templateName"
                :subtitle="row.templateExternalId"
                :to="apiPackageSettingsPath(row.templateId)"
              />
            </template>
          </el-table-column>
          <el-table-column
            :label="t('apiPolicy.invocationsPage.columns.status')"
            min-width="120"
          >
            <template #default="{ row }">
              {{ row.status }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('apiPolicy.invocationsPage.columns.requestId')"
            min-width="160"
          >
            <template #default="{ row }">
              {{ row.requestId }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('apiPolicy.invocationsPage.columns.releaseVersion')"
            min-width="120"
          >
            <template #default="{ row }">
              {{ row.resolvedReleaseVersion ?? '—' }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('apiPolicy.invocationsPage.columns.actions')"
            width="200"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button type="primary" link @click.stop="openDetail(row)">
                {{ t('apiPolicy.invocationsPage.openDetail') }}
              </el-button>
              <el-button link @click.stop="openPackageSettings(row.templateId)">
                {{ t('apiPolicy.invocationsPage.openSettings') }}
              </el-button>
            </template>
          </el-table-column>
        </AppDataTable>

        <AppTablePagination
          v-model:current-page="uiPage"
          :page-size="pageSize"
          :total="totalElements"
        />
      </template>
    </el-card>

    <InvocationSummaryDrawer
      v-model:visible="drawerVisible"
      :template-id="selectedRow?.templateId ?? ''"
      :invocation-id="selectedRow?.invocationId ?? null"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.composition-limit {
  margin-bottom: var(--space-4);
}

.filters-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: flex-end;
  margin-bottom: var(--space-4);
}

.filter-item {
  margin-bottom: 0;
  min-width: 12rem;
}

.filters-actions {
  display: flex;
  gap: var(--space-2);
}

.option-subtitle {
  margin-left: var(--space-2);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.section-card {
  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }
}
</style>
