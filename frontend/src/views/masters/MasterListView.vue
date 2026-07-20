<script setup lang="ts">
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import CatalogFilterToolbar from '@/components/common/CatalogFilterToolbar.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import MasterUploadDialog from '@/components/masters/MasterUploadDialog.vue'
import { useMasterListView } from '@/views/masters/useMasterListView'

const {
  t,
  formatDateTime,
  mastersStore,
  masterDetailLink,
  groupCatalogLink,
  uploadDialogOpen,
  currentPage,
  allMasters,
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
  canUpload,
  errorMessage,
  showListLoadError,
  reloadMasters,
  activateMasterRow,
  handleUpload,
  clearUploadServerError,
  resolveUpdatedByDisplay,
} = useMasterListView()
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('masters.list.title')"
      :description="t('masters.list.description')"
      :help-text="t('packageCatalog.master.noticeDescription')"
    >
      <template #actions>
        <el-button v-if="canUpload" type="primary" @click="uploadDialogOpen = true">
          {{ t('masters.upload.open') }}
        </el-button>
      </template>
    </PageHeader>

    <LoadErrorPanel
      v-if="showListLoadError"
      :message-key="mastersStore.lastErrorMessageKey || 'masters.error.loadList'"
      :retryable="mastersStore.lastListErrorRetryable"
      @retry="reloadMasters"
    />

    <el-skeleton v-else-if="mastersStore.loadingList" :rows="6" animated />

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

      <template v-if="allMasters.length > 0">
        <AppDataTable activatable :data="allMasters" @row-click="activateMasterRow">
          <el-table-column
            prop="groupCode"
            :label="t('masters.list.columns.group')"
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
            :label="t('masters.list.columns.name')"
            min-width="220"
          >
            <template #default="{ row }">
              <EntityLinkCell
                :label="row.name"
                :to="masterDetailLink(row.id)"
              />
            </template>
          </el-table-column>
          <el-table-column :label="t('masters.list.columns.status')" width="160">
            <template #default="{ row }">
              <MasterStatusBadge :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column
            prop="anchorCount"
            :label="t('masters.list.columns.anchors')"
            width="100"
          />
          <el-table-column
            :label="t('masters.list.columns.updatedBy')"
            min-width="120"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ resolveUpdatedByDisplay(row.updatedBy, row.updatedByDisplayName) }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('masters.list.columns.updatedAt')"
            min-width="180"
          >
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
        </AppDataTable>
        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="mastersStore.masterListSize"
          :total="mastersStore.masterListTotalElements"
        />
      </template>

      <EmptyStatePanel
        v-else
        title-key="masters.list.empty"
      />
    </template>

    <EmptyStatePanel
      v-else-if="!errorMessage"
      title-key="masters.list.empty"
    >
      <template v-if="canUpload" #actions>
        <el-button type="primary" @click="uploadDialogOpen = true">
          {{ t('masters.upload.open') }}
        </el-button>
      </template>
    </EmptyStatePanel>

    <MasterUploadDialog
      v-model="uploadDialogOpen"
      :loading="mastersStore.submitting"
      :upload-progress="mastersStore.uploadProgress"
      :server-error-key="uploadDialogOpen ? mastersStore.lastErrorMessageKey : null"
      @submit="handleUpload"
      @clear-server-error="clearUploadServerError"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}
</style>
