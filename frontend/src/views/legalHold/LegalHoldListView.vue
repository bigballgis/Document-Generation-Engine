<script setup lang="ts">
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import LegalHoldCreateDialog from '@/components/legalHold/LegalHoldCreateDialog.vue'
import { useLegalHoldListView } from '@/views/legalHold/useLegalHoldListView'

const {
  t,
  formatDateTime,
  legalHoldsStore,
  createDialogOpen,
  statusFilter,
  statusOptions,
  currentPage,
  showListLoadError,
  canManage,
  reloadHolds,
  statusLabel,
  statusTagType,
  scopeLabel,
  scopeSummary,
  templateLinkTo,
  confirmRelease,
  handleCreated,
} = useLegalHoldListView()
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('legalHold.list.title')"
      :description="t('legalHold.list.description')"
      :help-text="t('legalHold.list.help')"
    >
      <template #actions>
        <el-button
          v-if="canManage"
          type="primary"
          data-testid="legal-hold-create-open"
          @click="createDialogOpen = true"
        >
          {{ t('legalHold.create.open') }}
        </el-button>
      </template>
    </PageHeader>

    <LoadErrorPanel
      v-if="showListLoadError"
      :message-key="legalHoldsStore.lastErrorMessageKey || 'legalHold.error.loadList'"
      :retryable="legalHoldsStore.lastListErrorRetryable"
      @retry="reloadHolds"
    />

    <el-skeleton v-else-if="legalHoldsStore.loadingList" :rows="6" animated />

    <template v-else>
      <div class="legal-hold-filters" data-testid="legal-hold-filters">
        <el-select
          v-model="statusFilter"
          class="legal-hold-filters__status"
          data-testid="legal-hold-status-filter"
          :placeholder="t('legalHold.filters.status')"
          :aria-label="t('legalHold.filters.status')"
        >
          <el-option
            v-for="option in statusOptions"
            :key="option.value || 'all'"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>

      <template v-if="legalHoldsStore.holds.length > 0">
        <AppDataTable :data="legalHoldsStore.holds" data-testid="legal-hold-table">
          <el-table-column
            :label="t('legalHold.list.columns.holdId')"
            min-width="160"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <EntityLinkCell :label="row.holdExternalId" />
            </template>
          </el-table-column>
          <el-table-column :label="t('legalHold.list.columns.scope')" width="160">
            <template #default="{ row }">
              {{ scopeLabel(row.scopeType) }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('legalHold.list.columns.summary')"
            min-width="260"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <EntityLinkCell
                v-if="row.scopeType === 'TEMPLATE_WINDOW'"
                :label="row.templateExternalId || row.templateId || '—'"
                :subtitle="scopeSummary(row)"
                :link-to="templateLinkTo(row)"
              />
              <span v-else>{{ scopeSummary(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('legalHold.list.columns.status')" width="120">
            <template #default="{ row }">
              <el-tag
                :type="statusTagType(row.status)"
                effect="light"
                :data-testid="`legal-hold-status-${row.status}`"
              >
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            :label="t('legalHold.list.columns.reason')"
            min-width="160"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.reason || '—' }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('legalHold.list.columns.createdBy')"
            width="140"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.createdByUsername }}
            </template>
          </el-table-column>
          <el-table-column :label="t('legalHold.list.columns.createdAt')" width="200">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="canManage"
            :label="t('common.actions')"
            width="120"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'ACTIVE'"
                link
                type="danger"
                data-testid="legal-hold-release"
                @click.stop="confirmRelease(row)"
              >
                {{ t('legalHold.release.action') }}
              </el-button>
            </template>
          </el-table-column>
        </AppDataTable>
        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="legalHoldsStore.listSize"
          :total="legalHoldsStore.listTotalElements"
        />
      </template>

      <EmptyStatePanel
        v-else
        data-testid="legal-hold-honest-empty"
        title-key="legalHold.list.empty"
        :description-key="
          canManage
            ? 'legalHold.list.emptyDescription'
            : 'legalHold.list.emptyDescriptionReadOnly'
        "
      >
        <template v-if="canManage" #actions>
          <el-button
            type="primary"
            data-testid="legal-hold-create-open-empty"
            @click="createDialogOpen = true"
          >
            {{ t('legalHold.create.open') }}
          </el-button>
        </template>
      </EmptyStatePanel>
    </template>

    <LegalHoldCreateDialog
      v-model="createDialogOpen"
      @created="handleCreated"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.legal-hold-filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.legal-hold-filters__status {
  width: 12rem;
}
</style>
