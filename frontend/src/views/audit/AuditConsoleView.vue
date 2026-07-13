<script setup lang="ts">
import AuditConsoleFilters from '@/components/audit/AuditConsoleFilters.vue'
import AuditLifecycleEventsTable from '@/components/audit/AuditLifecycleEventsTable.vue'
import AuditManagementEventsTable from '@/components/audit/AuditManagementEventsTable.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { useAuditConsole } from '@/composables/useAuditConsole'

const {
  t,
  auditStore,
  activeTab,
  loadFailed,
  filterValidationKey,
  showAuditAdminJourney,
  loadErrorMessageKey,
  showGroupFilters,
  isAuditGroupLocked,
  auditEventTypeOptions,
  templateOptions,
  loadingTemplates,
  managementSource,
  lifecycleSource,
  managementUiPage,
  lifecycleUiPage,
  formatLifecycleState,
  formatDate,
  formatEventType,
  formatActor,
  resolveTemplateCell,
  handleTemplateFilterSearch,
  refreshActiveTab,
  handleTabChange,
  applyFilters,
  resetFilters,
  handleExport,
  sortManagementByActor,
  sortManagementByTemplate,
  sortLifecycleByActor,
  sortLifecycleByTemplate,
  sortManagementByEventType,
  sortManagementByEventAt,
  sortLifecycleByEventType,
  sortLifecycleByEventAt,
  sortLifecycleFromState,
  sortLifecycleToState,
} = useAuditConsole()
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('audit.title')"
      :description="t('audit.description')"
    >
      <template v-if="showAuditAdminJourney" #meta>
        <el-tag type="info" effect="plain">
          {{ t('audit.viewOnly.banner') }}
        </el-tag>
      </template>
      <template #actions>
        <el-button
          type="primary"
          :loading="auditStore.exporting"
          @click="handleExport"
        >
          {{ t('audit.export.action') }}
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="filterValidationKey"
      class="page-alert"
      type="warning"
      :title="t(filterValidationKey)"
      show-icon
      :closable="false"
    />

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="loadErrorMessageKey"
      :retryable="auditStore.lastListErrorRetryable"
      @retry="refreshActiveTab"
    />

    <template v-else>
      <AuditConsoleFilters
        v-model:event-type="auditStore.filters.eventType"
        v-model:request-id="auditStore.filters.requestId"
        v-model:event-at-from="auditStore.filters.eventAtFrom"
        v-model:event-at-to="auditStore.filters.eventAtTo"
        v-model:group-scope="auditStore.filters.groupScope"
        v-model:template-id="auditStore.filters.templateId"
        :show-group-filters="showGroupFilters"
        :is-audit-group-locked="isAuditGroupLocked"
        :audit-event-type-options="auditEventTypeOptions"
        :template-options="templateOptions"
        :loading-templates="loadingTemplates"
        @apply="applyFilters"
        @reset="resetFilters"
        @template-search="handleTemplateFilterSearch"
      />

      <el-tabs :model-value="activeTab" @tab-change="handleTabChange">
        <el-tab-pane :label="t('audit.tabs.management')" name="management">
          <AuditManagementEventsTable
            v-model:current-page="managementUiPage"
            :loading="auditStore.loadingManagement"
            :rows="managementSource"
            :page-size="auditStore.pageSize"
            :total="auditStore.managementTotalElements"
            :format-actor="formatActor"
            :format-event-type="formatEventType"
            :format-date="formatDate"
            :resolve-template-cell="resolveTemplateCell"
            :sort-by-actor="sortManagementByActor"
            :sort-by-event-type="sortManagementByEventType"
            :sort-by-template="sortManagementByTemplate"
            :sort-by-event-at="sortManagementByEventAt"
          />
        </el-tab-pane>

        <el-tab-pane :label="t('audit.tabs.lifecycle')" name="lifecycle">
          <AuditLifecycleEventsTable
            v-model:current-page="lifecycleUiPage"
            :loading="auditStore.loadingLifecycle"
            :rows="lifecycleSource"
            :page-size="auditStore.pageSize"
            :total="auditStore.lifecycleTotalElements"
            :format-actor="formatActor"
            :format-event-type="formatEventType"
            :format-date="formatDate"
            :format-lifecycle-state="formatLifecycleState"
            :resolve-template-cell="resolveTemplateCell"
            :sort-by-actor="sortLifecycleByActor"
            :sort-by-event-type="sortLifecycleByEventType"
            :sort-by-template="sortLifecycleByTemplate"
            :sort-from-state="sortLifecycleFromState"
            :sort-to-state="sortLifecycleToState"
            :sort-by-event-at="sortLifecycleByEventAt"
          />
        </el-tab-pane>
      </el-tabs>
    </template>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}
</style>
