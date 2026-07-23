<script setup lang="ts">
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import CatalogFilterToolbar from '@/components/common/CatalogFilterToolbar.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import TableEditMoreActions from '@/components/common/TableEditMoreActions.vue'
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
  listHydrated,
  listGroupCode,
  showGroupFilter,
  canClearGroupFilter,
  groupCatalogLink,
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

    <el-skeleton v-else-if="libraryAssetsStore.loadingList && !listHydrated" :rows="6" animated />

    <template v-else>
      <div
        v-if="showGroupFilter"
        class="asset-library-group-filter"
        data-testid="asset-library-group-filter"
      >
        <span class="asset-library-group-filter__label">
          {{ t('assetLibrary.list.filters.group') }}
        </span>
        <ScopedGroupSelect
          v-model="listGroupCode"
          class="asset-library-group-filter__control"
          :clearable="canClearGroupFilter"
          :placeholder="t('assetLibrary.list.filters.groupPlaceholder')"
          :aria-label="t('assetLibrary.list.filters.group')"
        />
      </div>

      <template v-if="showCatalogChrome">
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
              :label="t('assetLibrary.list.columns.group')"
              min-width="120"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <EntityLinkCell
                  :label="row.groupCode"
                  :to="groupCatalogLink(row.groupCode)"
                />
              </template>
            </el-table-column>
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
                <TableEditMoreActions
                  v-if="row.status === 'ACTIVE'"
                  :show-edit="false"
                  @command="(command) => command === 'disable' && confirmDisable(row)"
                >
                  <template #more>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        command="disable"
                        class="asset-library-disable-action"
                        data-testid="asset-library-disable"
                      >
                        {{ t('assetLibrary.disable.action') }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </TableEditMoreActions>
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
          data-testid="asset-library-filter-empty"
          title-key="assetLibrary.list.empty"
          :description-key="
            canUpload
              ? 'assetLibrary.list.emptyDescription'
              : 'assetLibrary.list.emptyDescriptionReadOnly'
          "
        />
      </template>

      <EmptyStatePanel
        v-else-if="!libraryAssetsStore.loadingList"
        data-testid="asset-library-honest-empty"
        title-key="assetLibrary.list.empty"
        :description-key="
          canUpload
            ? 'assetLibrary.list.emptyDescription'
            : 'assetLibrary.list.emptyDescriptionReadOnly'
        "
      >
        <template v-if="canUpload" #actions>
          <el-button
            type="primary"
            data-testid="asset-library-upload-open-empty"
            @click="uploadDialogOpen = true"
          >
            {{ t('assetLibrary.upload.open') }}
          </el-button>
        </template>
      </EmptyStatePanel>
    </template>

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

<style scoped lang="scss">
.asset-library-group-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-sm);
}

.asset-library-group-filter__label {
  color: var(--text-muted);
  font-size: 0.875rem;
  font-weight: 600;
}

.asset-library-group-filter__control {
  width: min(100%, 220px);
}
</style>
