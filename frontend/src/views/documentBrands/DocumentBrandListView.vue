<script setup lang="ts">
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import DocumentBrandFormDialog from '@/views/documentBrands/DocumentBrandFormDialog.vue'
import { useDocumentBrandListView } from '@/views/documentBrands/useDocumentBrandListView'

const {
  t,
  documentBrandsStore,
  groupCode,
  statusFilter,
  statusOptions,
  formOpen,
  formMode,
  editingBrand,
  showListLoadError,
  canManage,
  reloadBrands,
  statusLabel,
  statusTagType,
  openCreate,
  openEdit,
  handleCreate,
  handleUpdate,
} = useDocumentBrandListView()
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('documentBrands.list.title')"
      :description="t('documentBrands.list.description')"
      :help-text="t('documentBrands.list.help')"
    >
      <template #actions>
        <el-button
          v-if="canManage"
          type="primary"
          data-testid="document-brand-create-open"
          @click="openCreate"
        >
          {{ t('documentBrands.form.createOpen') }}
        </el-button>
      </template>
    </PageHeader>

    <LoadErrorPanel
      v-if="showListLoadError"
      :message-key="documentBrandsStore.lastErrorMessageKey || 'documentBrands.error.loadList'"
      :retryable="documentBrandsStore.lastListErrorRetryable"
      @retry="reloadBrands"
    />

    <el-skeleton v-else-if="documentBrandsStore.loadingList" :rows="6" animated />

    <template v-else>
      <div class="document-brand-filters" data-testid="document-brand-filters">
        <ScopedGroupSelect
          v-model="groupCode"
          class="document-brand-filters__group"
          data-testid="document-brand-group-filter"
          :placeholder="t('documentBrands.filters.group')"
        />
        <el-select
          v-model="statusFilter"
          class="document-brand-filters__status"
          data-testid="document-brand-status-filter"
          :placeholder="t('documentBrands.filters.status')"
          :aria-label="t('documentBrands.filters.status')"
        >
          <el-option
            v-for="option in statusOptions"
            :key="option.value || 'all'"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>

      <template v-if="documentBrandsStore.brands.length > 0">
        <AppDataTable :data="documentBrandsStore.brands" data-testid="document-brand-table">
          <el-table-column
            :label="t('documentBrands.list.columns.code')"
            min-width="180"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <EntityLinkCell :label="row.documentBrandCode" :subtitle="row.displayName" />
            </template>
          </el-table-column>
          <el-table-column :label="t('documentBrands.list.columns.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            :label="t('documentBrands.list.columns.logoObjectRef')"
            min-width="220"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.logoObjectRef }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('documentBrands.list.columns.letterheadLegalName')"
            min-width="180"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.letterheadLegalName || '—' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="canManage"
            :label="t('common.actions')"
            width="100"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                data-testid="document-brand-edit"
                @click.stop="openEdit(row)"
              >
                {{ t('common.edit') }}
              </el-button>
            </template>
          </el-table-column>
        </AppDataTable>
      </template>

      <EmptyStatePanel
        v-else
        title-key="documentBrands.list.empty"
        description-key="documentBrands.list.emptyDescription"
      >
        <template v-if="canManage" #actions>
          <el-button
            type="primary"
            data-testid="document-brand-create-open-empty"
            @click="openCreate"
          >
            {{ t('documentBrands.form.createOpen') }}
          </el-button>
        </template>
      </EmptyStatePanel>
    </template>

    <DocumentBrandFormDialog
      v-model="formOpen"
      :mode="formMode"
      :initial="editingBrand"
      :loading="documentBrandsStore.submitting"
      :default-group-code="groupCode"
      @create="handleCreate"
      @update="handleUpdate"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.document-brand-filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-4);

  &__group,
  &__status {
    min-width: 12rem;
  }
}
</style>
