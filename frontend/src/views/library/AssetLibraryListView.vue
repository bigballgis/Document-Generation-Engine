<script setup lang="ts">
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import CatalogFilterToolbar from '@/components/common/CatalogFilterToolbar.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import AssetLibraryUploadDialog from '@/components/library/AssetLibraryUploadDialog.vue'
import { useAssetLibraryListView } from '@/views/library/useAssetLibraryListView'

const {
  t,
  formatDateTime,
  formatSizeBytes,
  libraryAssetsStore,
  uploadDialogOpen,
  currentPage,
  allAssets,
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
  showListLoadError,
  canUpload,
  canDisable,
  uploadImageOrOtherAsset,
  uploadSealAsset,
  reloadAssets,
  handleUpload,
  clearUploadServerError,
  confirmDisable,
  statusLabel,
  statusTagType,
  classLabel,
} = useAssetLibraryListView()
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('assetLibrary.list.title')"
      :description="t('assetLibrary.list.description')"
      :help-text="t('assetLibrary.list.help')"
    >
      <template #actions>
        <el-button
          v-if="canUpload"
          type="primary"
          data-testid="asset-library-upload-open"
          @click="uploadDialogOpen = true"
        >
          {{ t('assetLibrary.upload.open') }}
        </el-button>
      </template>
    </PageHeader>

    <LoadErrorPanel
      v-if="showListLoadError"
      :message-key="libraryAssetsStore.lastErrorMessageKey || 'assetLibrary.error.loadList'"
      :retryable="libraryAssetsStore.lastListErrorRetryable"
      @retry="reloadAssets"
    />

    <el-skeleton v-else-if="libraryAssetsStore.loadingList" :rows="6" animated />

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

      <template v-if="allAssets.length > 0">
        <AppDataTable :data="allAssets" data-testid="asset-library-table">
          <el-table-column
            :label="t('assetLibrary.list.columns.assetKey')"
            min-width="200"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <EntityLinkCell :label="row.assetKey" :subtitle="row.originalFileName" />
            </template>
          </el-table-column>
          <el-table-column
            :label="t('assetLibrary.list.columns.assetClass')"
            width="120"
          >
            <template #default="{ row }">
              {{ classLabel(row.assetClass) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('assetLibrary.list.columns.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            :label="t('assetLibrary.list.columns.contentType')"
            width="130"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.contentType }}
            </template>
          </el-table-column>
          <el-table-column :label="t('assetLibrary.list.columns.size')" width="110">
            <template #default="{ row }">
              {{ formatSizeBytes(row.sizeBytes) }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('assetLibrary.list.columns.uploadedBy')"
            width="140"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.uploadedBy }}
            </template>
          </el-table-column>
          <el-table-column :label="t('assetLibrary.list.columns.uploadedAt')" width="200">
            <template #default="{ row }">
              {{ formatDateTime(row.uploadedAt) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="canDisable"
            :label="t('common.actions')"
            width="120"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'ACTIVE'"
                link
                type="danger"
                data-testid="asset-library-disable"
                @click.stop="confirmDisable(row)"
              >
                {{ t('assetLibrary.disable.action') }}
              </el-button>
            </template>
          </el-table-column>
        </AppDataTable>
        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="libraryAssetsStore.assetListSize"
          :total="libraryAssetsStore.assetListTotalElements"
        />
      </template>

      <EmptyStatePanel
        v-else
        title-key="assetLibrary.list.empty"
        description-key="assetLibrary.list.emptyDescription"
      />
    </template>

    <EmptyStatePanel
      v-else-if="!libraryAssetsStore.loadingList"
      title-key="assetLibrary.list.empty"
      description-key="assetLibrary.list.emptyDescription"
    >
      <template v-if="canUpload" #actions>
        <el-button type="primary" @click="uploadDialogOpen = true">
          {{ t('assetLibrary.upload.open') }}
        </el-button>
      </template>
    </EmptyStatePanel>

    <AssetLibraryUploadDialog
      v-model="uploadDialogOpen"
      :loading="libraryAssetsStore.submitting"
      :server-error-key="libraryAssetsStore.lastMutationErrorMessageKey"
      :can-upload-image-or-other="uploadImageOrOtherAsset"
      :can-upload-seal="uploadSealAsset"
      @submit="handleUpload"
      @clear-server-error="clearUploadServerError"
    />
  </AppPageLayout>
</template>
