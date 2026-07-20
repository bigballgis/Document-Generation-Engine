<script setup lang="ts">
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import LegalEntityFormDialog from '@/views/legalEntities/LegalEntityFormDialog.vue'
import { useLegalEntityListView } from '@/views/legalEntities/useLegalEntityListView'

const {
  t,
  legalEntitiesStore,
  groupCode,
  statusFilter,
  statusOptions,
  draftDefaultCode,
  defaultOptions,
  formOpen,
  formMode,
  editingEntity,
  showListLoadError,
  canManage,
  reloadEntities,
  statusLabel,
  statusTagType,
  openCreate,
  openEdit,
  handleCreate,
  handleUpdate,
  saveDefault,
} = useLegalEntityListView()
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('legalEntities.list.title')"
      :description="t('legalEntities.list.description')"
      :help-text="t('legalEntities.list.help')"
    >
      <template #actions>
        <el-button
          v-if="canManage"
          type="primary"
          data-testid="legal-entity-create-open"
          @click="openCreate"
        >
          {{ t('legalEntities.form.createOpen') }}
        </el-button>
      </template>
    </PageHeader>

    <el-card
      v-if="canManage"
      shadow="never"
      class="legal-entity-default-card"
      data-testid="legal-entity-default-panel"
    >
      <h2>{{ t('legalEntities.default.title') }}</h2>
      <p class="legal-entity-default-card__hint">{{ t('legalEntities.default.help') }}</p>
      <div class="legal-entity-default-card__row">
        <el-select
          v-model="draftDefaultCode"
          clearable
          class="legal-entity-default-card__select"
          data-testid="legal-entity-default-select"
          :placeholder="t('legalEntities.default.placeholder')"
        >
          <el-option
            v-for="option in defaultOptions"
            :key="option.value || 'none'"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button
          type="primary"
          data-testid="legal-entity-default-save"
          :loading="legalEntitiesStore.submitting"
          @click="saveDefault"
        >
          {{ t('legalEntities.default.save') }}
        </el-button>
      </div>
    </el-card>

    <LoadErrorPanel
      v-if="showListLoadError"
      :message-key="legalEntitiesStore.lastErrorMessageKey || 'legalEntities.error.loadList'"
      :retryable="legalEntitiesStore.lastListErrorRetryable"
      @retry="reloadEntities"
    />

    <el-skeleton v-else-if="legalEntitiesStore.loadingList" :rows="6" animated />

    <template v-else>
      <div class="legal-entity-filters" data-testid="legal-entity-filters">
        <ScopedGroupSelect
          v-model="groupCode"
          class="legal-entity-filters__group"
          data-testid="legal-entity-group-filter"
          :placeholder="t('legalEntities.filters.group')"
        />
        <el-select
          v-model="statusFilter"
          class="legal-entity-filters__status"
          data-testid="legal-entity-status-filter"
          :placeholder="t('legalEntities.filters.status')"
          :aria-label="t('legalEntities.filters.status')"
        >
          <el-option
            v-for="option in statusOptions"
            :key="option.value || 'all'"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>

      <template v-if="legalEntitiesStore.entities.length > 0">
        <AppDataTable :data="legalEntitiesStore.entities" data-testid="legal-entity-table">
          <el-table-column
            :label="t('legalEntities.list.columns.code')"
            min-width="180"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <EntityLinkCell :label="row.legalEntityCode" :subtitle="row.displayName" />
            </template>
          </el-table-column>
          <el-table-column
            :label="t('legalEntities.list.columns.documentBrandCode')"
            min-width="180"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <EntityLinkCell :label="row.documentBrandCode" />
            </template>
          </el-table-column>
          <el-table-column :label="t('legalEntities.list.columns.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">
                {{ statusLabel(row.status) }}
              </el-tag>
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
                data-testid="legal-entity-edit"
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
        title-key="legalEntities.list.empty"
        description-key="legalEntities.list.emptyDescription"
      >
        <template v-if="canManage" #actions>
          <el-button
            type="primary"
            data-testid="legal-entity-create-open-empty"
            @click="openCreate"
          >
            {{ t('legalEntities.form.createOpen') }}
          </el-button>
        </template>
      </EmptyStatePanel>
    </template>

    <LegalEntityFormDialog
      v-model="formOpen"
      :mode="formMode"
      :initial="editingEntity"
      :loading="legalEntitiesStore.submitting"
      :default-group-code="groupCode"
      @create="handleCreate"
      @update="handleUpdate"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.legal-entity-default-card {
  margin-bottom: var(--space-4);

  h2 {
    margin: 0 0 var(--space-2);
    font-size: 1.125rem;
  }

  &__hint {
    margin: 0 0 var(--space-3);
    color: var(--text-muted);
    font-size: 0.9rem;
    line-height: 1.4;
  }

  &__row {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    align-items: center;
  }

  &__select {
    min-width: 16rem;
  }
}

.legal-entity-filters {
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
