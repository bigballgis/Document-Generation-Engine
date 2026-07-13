<script setup lang="ts">
import TemplateWorkspaceHeader from '@/components/templates/TemplateWorkspaceHeader.vue'
import TemplateMetadataEditDialog from '@/components/templates/TemplateMetadataEditDialog.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import TemplatePackageHubActions from '@/views/templates/hub/TemplatePackageHubActions.vue'
import TemplatePackageHubWorkspace from '@/views/templates/hub/TemplatePackageHubWorkspace.vue'
import { useTemplatePackageHub } from '@/views/templates/useTemplatePackageHub'

const {
  t,
  formatDateTime,
  templatesStore,
  authorTemplates,
  manageReleaseVersionState,
  metadataEditOpen,
  loadFailed,
  selectedContractEnvironment,
  workspaceRef,
  templateId,
  template,
  showDetailSkeleton,
  showPolicyPanel,
  policyLoadFailed,
  apiPolicy,
  loadingPolicy,
  policySubmitting,
  policyLoadErrorKey,
  credentialColumnFilters,
  credentialsCurrentPage,
  paginatedCredentials,
  credentialStatusFilterOptions,
  totalCredentialRows,
  sortCredentialsByCreatedAt,
  CLIENT_TABLE_PAGE_SIZE,
  showMetadataEdit,
  showDeleteTemplateAction,
  showExportActions,
  hubWorkspaceTabs,
  activeHubTab,
  loadTemplate,
  loadPolicyData,
  backToList,
  handleMetadataUpdate,
  handleDeleteTemplate,
  handleCreateCredential,
  handleRotateCredential,
  handleRevokeCredential,
  handleVersionLinesChanged,
} = useTemplatePackageHub()
</script>

<template>
  <AppPageLayout>
    <TemplateWorkspaceHeader
      :template-name="template?.name ?? t('templates.packageHub.loadingTitle')"
      :group-label="template ? t('templates.packageHub.groupLabel', { groupCode: template.groupCode }) : undefined"
      :status="template?.lifecycleStatus"
      :approval-sub-state="template?.approvalSubState"
      :back-label="t('templates.packageHub.backToList')"
      @back="backToList"
    >
      <template v-if="template" #actions>
        <TemplatePackageHubActions
          :template-id="templateId"
          :external-id="template.externalId"
          :submitting="templatesStore.submitting"
          :show-export-actions="showExportActions"
          :show-delete-template-action="showDeleteTemplateAction"
          :show-metadata-edit="showMetadataEdit"
          @delete="handleDeleteTemplate"
          @edit-metadata="metadataEditOpen = true"
        />
      </template>
    </TemplateWorkspaceHeader>

    <p v-if="template" class="header-extra">
      {{ t('templates.packageHub.externalIdLabel', { externalId: template.externalId }) }}
    </p>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="templatesStore.lastErrorMessageKey ?? 'templates.error.loadDetail'"
      @retry="loadTemplate"
    />

    <el-skeleton v-else-if="showDetailSkeleton" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!template"
      title-key="templates.empty.notFoundTitle"
      description-key="templates.empty.notFoundDescription"
    />

    <TemplatePackageHubWorkspace
      v-else-if="template"
      ref="workspaceRef"
      v-model:active-hub-tab="activeHubTab"
      v-model:credential-column-filters="credentialColumnFilters"
      v-model:credentials-current-page="credentialsCurrentPage"
      v-model:selected-contract-environment="selectedContractEnvironment"
      :template-id="templateId"
      :template="template"
      :can-clone="authorTemplates"
      :can-manage-versions="manageReleaseVersionState"
      :hub-workspace-tabs="hubWorkspaceTabs"
      :show-policy-panel="showPolicyPanel"
      :loading-policy="loadingPolicy"
      :api-policy="apiPolicy"
      :policy-load-failed="policyLoadFailed"
      :policy-load-error-key="policyLoadErrorKey"
      :paginated-credentials="paginatedCredentials"
      :credential-status-filter-options="credentialStatusFilterOptions"
      :page-size="CLIENT_TABLE_PAGE_SIZE"
      :total-credential-rows="totalCredentialRows"
      :submitting="policySubmitting"
      :format-date-time="formatDateTime"
      :sort-credentials-by-created-at="sortCredentialsByCreatedAt"
      @cloned="handleVersionLinesChanged"
      @changed="handleVersionLinesChanged"
      @create-credential="handleCreateCredential"
      @rotate-credential="handleRotateCredential"
      @revoke-credential="handleRevokeCredential"
      @retry-policy-load="loadPolicyData"
    />

    <TemplateMetadataEditDialog
      v-if="template"
      v-model="metadataEditOpen"
      :initial-name="template.name"
      :initial-description="template.description ?? null"
      :loading="templatesStore.submitting"
      @submit="handleMetadataUpdate"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.header-extra {
  margin: calc(-1 * var(--space-4)) 0 var(--space-6);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}
</style>
