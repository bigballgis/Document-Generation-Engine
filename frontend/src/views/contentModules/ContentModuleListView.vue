<script setup lang="ts">
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import CatalogFilterToolbar from '@/components/common/CatalogFilterToolbar.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import ContentModuleCreateDialog from '@/components/contentModules/ContentModuleCreateDialog.vue'
import ContentModuleStatusBadge from '@/components/contentModules/ContentModuleStatusBadge.vue'
import { useContentModuleListView } from '@/views/contentModules/useContentModuleListView'

const {
  t,
  formatDateTime,
  contentModulesStore,
  contentModuleDetailLink,
  createDialogOpen,
  currentPage,
  allModules,
  searchQuery,
  filters,
  activeSortKey,
  hasAnyActive,
  activeFilterChips,
  clearAll,
  removeFilterChip,
  catalogToolbarFilters,
  catalogSortOptions,
  showCatalogChrome,
  canCreate,
  errorMessage,
  reloadModules,
  activateModuleRow,
  handleCreated,
} = useContentModuleListView()
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('contentModules.list.title')"
      :description="t('contentModules.list.description')"
      :help-text="t('packageCatalog.contentModule.noticeDescription')"
    >
      <template #actions>
        <el-button v-if="canCreate" type="primary" @click="createDialogOpen = true">
          {{ t('contentModules.create.open') }}
        </el-button>
      </template>
    </PageHeader>

    <LoadErrorPanel
      v-if="contentModulesStore.lastErrorMessageKey && !contentModulesStore.loadingList"
      :message-key="contentModulesStore.lastErrorMessageKey"
      :retryable="contentModulesStore.lastListErrorRetryable"
      @retry="reloadModules"
    />

    <el-skeleton v-else-if="contentModulesStore.loadingList" :rows="6" animated />

    <template v-else-if="showCatalogChrome">
      <CatalogFilterToolbar
        v-model:search-query="searchQuery"
        v-model:filter-values="filters"
        v-model:active-sort-key="activeSortKey"
        :filters="catalogToolbarFilters"
        :sort-options="catalogSortOptions"
        :active-filter-chips="activeFilterChips"
        :has-any-active="hasAnyActive"
        @clear="clearAll"
        @remove-chip="removeFilterChip"
      />

      <template v-if="allModules.length > 0">
        <AppDataTable activatable :data="allModules" @row-click="activateModuleRow">
          <el-table-column
            prop="groupCode"
            :label="t('contentModules.list.columns.group')"
            width="140"
          />
          <el-table-column
            prop="moduleCode"
            :label="t('contentModules.list.columns.moduleCode')"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column
            :label="t('contentModules.list.columns.name')"
            min-width="220"
          >
            <template #default="{ row }">
              <EntityLinkCell
                :label="row.name"
                :subtitle="row.moduleCode"
                :to="contentModuleDetailLink(row.moduleId)"
              />
            </template>
          </el-table-column>
          <el-table-column
            :label="t('contentModules.list.columns.status')"
            width="160"
          >
            <template #default="{ row }">
              <ContentModuleStatusBadge
                :review-state="row.reviewState"
                :lifecycle-state="row.lifecycleState"
              />
            </template>
          </el-table-column>
          <el-table-column :label="t('contentModules.list.columns.updatedAt')" width="200">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
        </AppDataTable>
        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="contentModulesStore.moduleListSize"
          :total="contentModulesStore.moduleListTotalElements"
        />
      </template>

      <EmptyStatePanel
        v-else
        title-key="contentModules.list.empty"
        description-key="contentModules.list.emptyDescription"
      />
    </template>

    <EmptyStatePanel
      v-else-if="!contentModulesStore.loadingList && !errorMessage"
      title-key="contentModules.list.empty"
      description-key="contentModules.list.emptyDescription"
    >
      <template v-if="canCreate" #actions>
        <el-button type="primary" @click="createDialogOpen = true">
          {{ t('contentModules.create.open') }}
        </el-button>
      </template>
    </EmptyStatePanel>

    <ContentModuleCreateDialog v-model="createDialogOpen" @created="handleCreated" />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}
</style>
