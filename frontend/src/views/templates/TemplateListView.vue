<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import VersionCatalogNotice from '@/components/catalog/VersionCatalogNotice.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import TemplateCreateDialog from '@/components/templates/TemplateCreateDialog.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useLifecycleStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { useCapabilities } from '@/composables/useCapabilities'
import { templateDetailPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateSummary } from '@/types/template'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const lifecycleStatusFilterOptions = useLifecycleStatusFilterOptions()
const router = useRouter()
const templatesStore = useTemplatesStore()
const { authorTemplates } = useCapabilities()

const createDialogOpen = ref(false)
const currentPage = ref(1)
const pageSize = 10

const allTemplates = computed(() => templatesStore.templates)
const { filters: columnFilters, filteredRows: filteredTemplates, hasActiveFilters, clearFilters } =
  useDataTableFilters(allTemplates, [
    { key: 'name', getValue: (row) => row.name },
    { key: 'externalId', getValue: (row) => row.externalId },
    { key: 'status', getValue: (row) => row.lifecycleStatus, matchMode: 'exact' },
    { key: 'releaseVersion', getValue: (row) => row.releaseVersion ?? '' },
    { key: 'updatedAt', getValue: (row) => new Date(row.updatedAt).toLocaleString() },
  ])
const paginatedTemplates = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredTemplates.value.slice(start, start + pageSize)
})
const groupedTemplates = computed(() => {
  const grouped = new Map<string, TemplateSummary[]>()
  for (const template of paginatedTemplates.value) {
    const existing = grouped.get(template.groupCode) ?? []
    existing.push(template)
    grouped.set(template.groupCode, existing)
  }
  return [...grouped.entries()]
})
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

function handleCreated(templateId: string) {
  ElMessage.success(t('templates.create.success'))
  router.push(templateDetailPath(templateId))
}

const sortByLifecycleStatus = rowSortMethod<TemplateSummary>((row) => row.lifecycleStatus)
const sortByUpdatedAt = rowSortMethod<TemplateSummary>((row) => row.updatedAt)
</script>

<template>
  <div class="templates-page">
    <header class="page-header">
      <div>
        <h1>{{ t('templates.list.title') }}</h1>
        <p>{{ t('templates.list.description') }}</p>
      </div>
      <el-button v-if="authorTemplates" type="primary" @click="createDialogOpen = true">
        {{ t('templates.create.open') }}
      </el-button>
    </header>

    <VersionCatalogNotice kind="template" />

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="templatesStore.loadingList" :rows="6" animated />

    <template v-else-if="groupedTemplates.length > 0">
      <div v-if="hasActiveFilters" class="table-toolbar">
        <el-button size="small" text @click="clearFilters">{{ t('table.clearFilters') }}</el-button>
      </div>
      <section v-for="[groupCode, items] in groupedTemplates" :key="groupCode" class="group-section">
        <h2>{{ t('templates.list.groupSection', { groupCode }) }}</h2>
        <AppDataTable
          :data="items"
          @row-click="(row: TemplateSummary) => openTemplate(row.id)"
        >
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
          <el-table-column
            sortable
            :sort-method="sortByLifecycleStatus"
            width="160"
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
          <el-table-column
            sortable
            :sort-method="sortByUpdatedAt"
            min-width="180"
          >
            <template #header>
              <TableColumnHeader
                :label="t('templates.list.columns.updatedAt')"
                v-model="columnFilters.updatedAt"
              />
            </template>
            <template #default="{ row }">
              {{ new Date(row.updatedAt).toLocaleString() }}
            </template>
          </el-table-column>
        </AppDataTable>
      </section>
    </template>

    <el-empty v-else :description="t('templates.list.empty')" />

    <el-pagination
      v-if="filteredTemplates.length > pageSize"
      v-model:current-page="currentPage"
      class="list-pagination"
      layout="prev, pager, next"
      :page-size="pageSize"
      :total="filteredTemplates.length"
    />

    <TemplateCreateDialog v-model="createDialogOpen" @created="handleCreated" />
  </div>
</template>

<style scoped lang="scss">
.templates-page {
  padding: 2rem;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
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

.group-section {
  margin-bottom: 2rem;

  h2 {
    margin: 0 0 0.75rem;
    font-size: 1.125rem;
  }
}

:deep(.el-table__row) {
  cursor: pointer;
}

.list-pagination {
  margin-top: 1rem;
  justify-content: flex-end;
}
</style>
