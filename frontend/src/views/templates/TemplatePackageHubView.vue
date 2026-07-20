<script setup lang="ts">
import TemplateWorkspaceHeader from '@/components/templates/TemplateWorkspaceHeader.vue'
import TemplateMetadataEditDialog from '@/components/templates/TemplateMetadataEditDialog.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
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
  propertiesOpen,
  dependenciesGuidanceVisible,
  loadFailed,
  workspaceRef,
  templateId,
  template,
  showDetailSkeleton,
  showMetadataEdit,
  showDeleteTemplateAction,
  showExportActions,
  showApiSettingsAction,
  loadTemplate,
  backToList,
  handleMetadataUpdate,
  handleDeleteTemplate,
  handleVersionLinesChanged,
  openProperties,
  openApiSettings,
} = useTemplatePackageHub()
</script>

<template>
  <AppPageLayout data-testid="template-package-hub">
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
          :show-api-settings-action="showApiSettingsAction"
          @delete="handleDeleteTemplate"
          @edit-metadata="metadataEditOpen = true"
          @open-properties="openProperties"
          @open-api-settings="openApiSettings"
        />
      </template>
    </TemplateWorkspaceHeader>

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
      v-model:dependencies-guidance-visible="dependenciesGuidanceVisible"
      :template-id="templateId"
      :can-clone="authorTemplates"
      :can-manage-versions="manageReleaseVersionState"
      @cloned="handleVersionLinesChanged"
      @changed="handleVersionLinesChanged"
    />

    <el-drawer
      v-model="propertiesOpen"
      direction="rtl"
      size="480px"
      destroy-on-close
      data-testid="template-properties-drawer"
      :title="t('templates.packageHub.properties')"
    >
      <TemplateDetailOverviewTab
        v-if="template"
        :template="template"
        :format-date-time="formatDateTime"
      />
    </el-drawer>

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
