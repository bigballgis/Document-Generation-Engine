<script setup lang="ts">
import { toRef } from 'vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import TemplateReleaseChangeDiffDialog from '@/components/templates/TemplateReleaseChangeDiffDialog.vue'
import TemplateReleaseVersionHistoryTable from '@/components/templates/TemplateReleaseVersionHistoryTable.vue'
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
  canCompareReleases,
  compareHintKey,
  compareReleaseA,
  compareReleaseB,
  compareDialogVisible,
  onSelectionChange,
  openCompareDialog,
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
      <div class="table-toolbar">
        <el-button
          type="primary"
          size="small"
          data-testid="compare-releases"
          :disabled="!canCompareReleases"
          @click="openCompareDialog"
        >
          {{ t('templates.versions.compare') }}
        </el-button>
        <span class="compare-hint" data-testid="compare-releases-hint">{{ t(compareHintKey) }}</span>
        <el-button v-if="hasActiveFilters" size="small" text @click="clearFilters">
          {{ t('table.clearFilters') }}
        </el-button>
      </div>
      <TemplateReleaseVersionHistoryTable
        v-model:versions-current-page="versionsCurrentPage"
        :paginated-versions="paginatedVersions"
        :column-filters="columnFilters"
        :lifecycle-status-filter-options="lifecycleStatusFilterOptions"
        :default-route-filter-options="defaultRouteFilterOptions"
        :can-manage-versions="canManageVersions"
        :submitting="templatesStore.submitting"
        :page-size="CLIENT_TABLE_PAGE_SIZE"
        :total-version-rows="totalVersionRows"
        :format-date-time="formatDateTime"
        :sort-by-dev-version="sortByDevVersion"
        :sort-by-lifecycle-status="sortByLifecycleStatus"
        :sort-by-updated-at="sortByUpdatedAt"
        :resolve-updated-by-display="resolveUpdatedByDisplay"
        :t="t"
        @version-action="handleVersionAction"
        @selection-change="onSelectionChange"
      />
    </template>

    <TemplateReleaseChangeDiffDialog
      v-model="compareDialogVisible"
      :template-id="templateId"
      :release-version-a="compareReleaseA"
      :release-version-b="compareReleaseB"
    />
  </div>
</template>

<style scoped lang="scss" src="./TemplateReleaseVersionHistoryPanel.scss"></style>
