<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { RouteLocationRaw } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import CatalogFilterToolbar from '@/components/common/CatalogFilterToolbar.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import type { CatalogFilterChip } from '@/composables/useCatalogTableControls'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import type { TableColumnFilterOption } from '@/composables/useTableFilterOptions'
import type { TemplateSummary } from '@/types/template'
import { resolveUpdatedByDisplay } from '@/utils/userDisplay'

defineProps<{
  catalogTemplates: TemplateSummary[]
  catalogToolbarFilters: Array<{
    key: string
    labelKey: string
    type: 'text' | 'select'
    options?: TableColumnFilterOption[]
  }>
  catalogSortOptions: Array<{ key: string; labelKey: string }>
  activeFilterChips: CatalogFilterChip[]
  hasAnyActive: boolean
  pageSize: number
  total: number
  templateDetailLink: (id: string) => RouteLocationRaw | undefined
  groupCatalogLink: (groupCode?: string | null) => RouteLocationRaw | undefined
}>()

const searchQuery = defineModel<string>('searchQuery', { required: true })
const filterValues = defineModel<Record<string, string>>('filterValues', { required: true })
const activeSortKey = defineModel<string>('activeSortKey', { required: true })
const currentPage = defineModel<number>('currentPage', { required: true })

const emit = defineEmits<{
  clear: []
  removeChip: [key: string]
  rowClick: [row: TemplateSummary]
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
</script>

<template>
  <CatalogFilterToolbar
    v-model:search-query="searchQuery"
    v-model:filter-values="filterValues"
    v-model:active-sort-key="activeSortKey"
    :filters="catalogToolbarFilters"
    :sort-options="catalogSortOptions"
    :active-filter-chips="activeFilterChips"
    :has-any-active="hasAnyActive"
    @clear="emit('clear')"
    @remove-chip="emit('removeChip', $event)"
  />

  <template v-if="catalogTemplates.length > 0">
    <AppDataTable activatable :data="catalogTemplates" @row-click="(row: TemplateSummary) => emit('rowClick', row)">
      <el-table-column
        prop="groupCode"
        :label="t('templates.list.columns.group')"
        width="140"
      >
        <template #default="{ row }">
          <EntityLinkCell
            :label="row.groupCode"
            :to="groupCatalogLink(row.groupCode)"
          />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('templates.list.columns.name')"
        min-width="220"
      >
        <template #default="{ row }">
          <EntityLinkCell
            :label="row.name"
            :subtitle="row.externalId"
            :to="templateDetailLink(row.id)"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="externalId"
        :label="t('templates.list.columns.externalId')"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column
        prop="locale"
        :label="t('templates.list.columns.locale')"
        width="120"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ row.locale || '—' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.list.columns.status')" width="160">
        <template #default="{ row }">
          <TemplateStatusBadge
            :status="row.lifecycleStatus"
            :approval-sub-state="row.approvalSubState"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="releaseVersion"
        :label="t('templates.list.columns.releaseVersion')"
        width="140"
      >
        <template #default="{ row }">
          {{ row.releaseVersion ?? t('templates.detail.noReleaseVersion') }}
        </template>
      </el-table-column>
      <el-table-column
        prop="releaseVersionCount"
        :label="t('templates.list.columns.releaseVersionCount')"
        width="120"
      />
      <el-table-column
        :label="t('templates.list.columns.updatedBy')"
        min-width="120"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ resolveUpdatedByDisplay(row.updatedBy, row.updatedByDisplayName) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.list.columns.updatedAt')" min-width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
    </AppDataTable>
    <AppTablePagination
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
    />
  </template>

  <EmptyStatePanel v-else title-key="templates.list.empty" />
</template>
