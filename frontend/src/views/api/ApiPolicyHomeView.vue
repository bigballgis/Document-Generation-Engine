<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useLifecycleStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { templateDetailPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateSummary } from '@/types/template'

const { t, te } = useI18n()
const lifecycleStatusFilterOptions = useLifecycleStatusFilterOptions()
const router = useRouter()
const templatesStore = useTemplatesStore()

const publishedTemplates = computed(() => templatesStore.publishedTemplates)
const { filters: columnFilters, filteredRows, hasActiveFilters, clearFilters } = useDataTableFilters(
  publishedTemplates,
  [
    { key: 'name', getValue: (row) => row.name },
    { key: 'externalId', getValue: (row) => row.externalId },
    { key: 'groupCode', getValue: (row) => row.groupCode },
    { key: 'status', getValue: (row) => row.lifecycleStatus, matchMode: 'exact' },
    { key: 'releaseVersion', getValue: (row) => row.releaseVersion ?? '' },
  ],
)

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadList')
})

onMounted(async () => {
  try {
    await templatesStore.fetchTemplates()
  } catch {
    // Error surfaced via store message key.
  }
})

function openTemplate(templateId: string) {
  router.push(templateDetailPath(templateId))
}

const { onRowClick: activateTemplateRow } = useActivatableTableRow<TemplateSummary>((row) =>
  openTemplate(row.id),
)

const sortByLifecycleStatus = rowSortMethod<TemplateSummary>((row) => row.lifecycleStatus)
</script>

<template>
  <div class="api-policy-page">
    <header class="page-header">
      <div>
        <h1>{{ t('apiPolicy.home.title') }}</h1>
        <p>{{ t('apiPolicy.home.description') }}</p>
      </div>
    </header>

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="templatesStore.loadingList" :rows="5" animated />

    <template v-else>
      <div v-if="hasActiveFilters" class="table-toolbar">
        <el-button size="small" text @click="clearFilters">{{ t('table.clearFilters') }}</el-button>
      </div>
      <AppDataTable
        activatable
        :data="filteredRows"
        @row-click="activateTemplateRow"
      >
        <template #empty>
          <el-empty :description="t('apiPolicy.home.empty')" />
        </template>
        <el-table-column prop="name" sortable min-width="220">
          <template #header>
            <TableColumnHeader
              :label="t('templates.list.columns.name')"
              v-model="columnFilters.name"
            />
          </template>
        </el-table-column>
        <el-table-column prop="externalId" sortable min-width="180">
          <template #header>
            <TableColumnHeader
              :label="t('templates.list.columns.externalId')"
              v-model="columnFilters.externalId"
            />
          </template>
        </el-table-column>
        <el-table-column prop="groupCode" sortable width="140">
          <template #header>
            <TableColumnHeader
              :label="t('apiPolicy.home.groupCode')"
              v-model="columnFilters.groupCode"
            />
          </template>
        </el-table-column>
        <el-table-column
          sortable
          :sort-method="sortByLifecycleStatus"
          width="140"
        >
          <template #header>
            <TableColumnHeader
              :label="t('templates.list.columns.status')"
              v-model="columnFilters.status"
              filter-type="select"
              :options="lifecycleStatusFilterOptions"
            />
          </template>
          <template #default="{ row }">
            <TemplateStatusBadge :status="row.lifecycleStatus" />
          </template>
        </el-table-column>
        <el-table-column prop="releaseVersion" sortable width="140">
          <template #header>
            <TableColumnHeader
              :label="t('templates.list.columns.releaseVersion')"
              v-model="columnFilters.releaseVersion"
            />
          </template>
        </el-table-column>
      </AppDataTable>
    </template>
  </div>
</template>

<style scoped lang="scss">
.api-policy-page {
  padding: 2rem;
}

.page-header {
  margin-bottom: 1.5rem;

  h1 {
    margin: 0.25rem 0;
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

.table-toolbar {
  margin-bottom: 0.75rem;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
