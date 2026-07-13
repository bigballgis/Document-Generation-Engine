<script setup lang="ts">
import { toRef } from 'vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { useTemplateReleaseVersionHistoryPanel } from '@/components/templates/useTemplateReleaseVersionHistoryPanel'
import type { TemplateLifecycleStatus } from '@/types/template'

const props = defineProps<{
  templateId: string
  templateLifecycleStatus: TemplateLifecycleStatus
}>()

const emit = defineEmits<{
  changed: []
}>()

const {
  t,
  formatDateTime,
  lifecycleStatusFilterOptions,
  defaultRouteFilterOptions,
  templatesStore,
  loadError,
  loading,
  columnFilters,
  hasActiveFilters,
  clearFilters,
  versionsCurrentPage,
  paginatedVersions,
  totalVersionRows,
  canManageVersions,
  showWorkflowHint,
  loadVersions,
  handleVersionAction,
  sortByDevVersion,
  sortByLifecycleStatus,
  sortByUpdatedAt,
  resolveUpdatedByDisplay,
  CLIENT_TABLE_PAGE_SIZE,
} = useTemplateReleaseVersionHistoryPanel({
  templateId: toRef(props, 'templateId'),
  templateLifecycleStatus: toRef(props, 'templateLifecycleStatus'),
  onChanged: () => emit('changed'),
})
</script>

<template>
  <div class="release-version-history">
    <header class="panel-header">
      <h2>{{ t('templates.versions.title') }}</h2>
      <p>{{ t('templates.versions.description') }}</p>
    </header>

    <el-alert
      v-if="showWorkflowHint"
      type="info"
      :title="t('templates.versions.workflowHintTitle')"
      :description="t('templates.versions.workflowHintDescription')"
      show-icon
      :closable="false"
      class="workflow-hint"
    />

    <LoadErrorPanel
      v-if="loadError"
      message-key="templates.versions.loadError"
      @retry="loadVersions"
    />

    <el-skeleton v-else-if="loading" :rows="4" animated />

    <template v-else>
      <div v-if="hasActiveFilters" class="table-toolbar">
        <el-button size="small" text @click="clearFilters">{{ t('table.clearFilters') }}</el-button>
      </div>
      <AppDataTable :data="paginatedVersions">
        <template #empty>
          <el-empty :description="t('templates.versions.empty')" />
        </template>
        <el-table-column prop="releaseVersion" sortable min-width="140">
          <template #header>
            <TableColumnHeader
              :label="t('templates.versions.releaseVersion')"
              v-model="columnFilters.releaseVersion"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="devVersionNumber"
          sortable
          width="120"
          :sort-method="sortByDevVersion"
        >
          <template #header>
            <TableColumnHeader
              :label="t('templates.versions.devVersionNumber')"
              v-model="columnFilters.devVersionNumber"
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
              :label="t('templates.versions.status')"
              v-model="columnFilters.status"
              filter-type="select"
              :options="lifecycleStatusFilterOptions"
            />
          </template>
          <template #default="{ row }">
            <TemplateStatusBadge :status="row.lifecycleStatus" />
          </template>
        </el-table-column>
        <el-table-column width="140">
          <template #header>
            <TableColumnHeader
              :label="t('templates.versions.defaultRoute')"
              v-model="columnFilters.defaultRoute"
              filter-type="select"
              :options="defaultRouteFilterOptions"
            />
          </template>
          <template #default="{ row }">
            <el-tag v-if="row.defaultRouteTarget" type="success" size="small">
              {{ t('templates.versions.defaultRouteYes') }}
            </el-tag>
            <span v-else>{{ t('templates.versions.defaultRouteNo') }}</span>
          </template>
        </el-table-column>
        <el-table-column sortable min-width="120">
          <template #header>
            <TableColumnHeader
              :label="t('templates.versions.updatedBy')"
              v-model="columnFilters.updatedBy"
            />
          </template>
          <template #default="{ row }">
            {{ resolveUpdatedByDisplay(row.updatedBy, row.updatedByDisplayName) }}
          </template>
        </el-table-column>
        <el-table-column
          sortable
          :sort-method="sortByUpdatedAt"
          min-width="180"
        >
          <template #header>
            <TableColumnHeader
              :label="t('templates.versions.updatedAt')"
              v-model="columnFilters.updatedAt"
            />
          </template>
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="canManageVersions"
          :label="t('templates.versions.actions')"
          min-width="220"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.lifecycleStatus === 'PUBLISHED'"
              link
              type="warning"
              :loading="templatesStore.submitting"
              @click="handleVersionAction(row.releaseVersion, 'deactivate')"
            >
              {{ t('templates.versions.deactivate') }}
            </el-button>
            <el-button
              v-if="row.lifecycleStatus === 'STOPPED'"
              link
              type="primary"
              :loading="templatesStore.submitting"
              @click="handleVersionAction(row.releaseVersion, 'restore')"
            >
              {{ t('templates.versions.restore') }}
            </el-button>
          </template>
        </el-table-column>
      </AppDataTable>
      <AppTablePagination
        v-model:current-page="versionsCurrentPage"
        :page-size="CLIENT_TABLE_PAGE_SIZE"
        :total="totalVersionRows"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.release-version-history {
  padding: 0.25rem 0;
}

.panel-header {
  margin-bottom: 1rem;

  h2 {
    margin: 0 0 0.25rem;
    font-size: 1.125rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.workflow-hint {
  margin-bottom: 1rem;
}

.table-toolbar {
  margin-bottom: 0.75rem;
}
</style>
