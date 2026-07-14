<script setup lang="ts">
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import ContentModuleDetailDialogs from '@/views/contentModules/detail/ContentModuleDetailDialogs.vue'
import ContentModuleDetailWorkspace from '@/views/contentModules/detail/ContentModuleDetailWorkspace.vue'
import { useContentModuleDetailController } from '@/views/contentModules/useContentModuleDetailController'

const {
  t,
  contentModulesStore,
  loadFailed,
  versionDialogOpen,
  versionDialogMode,
  selectedVersion,
  impactDialogOpen,
  activeWorkspaceTab,
  workspaceTabs,
  moduleId,
  detail,
  versions,
  reviewHistory,
  formatReviewAction,
  canSubmitReview,
  canApproveReview,
  canCreateVersion,
  canEditDraft,
  canStop,
  canRecover,
  canDeprecate,
  previewVersion,
  previewContentJson,
  previewVersionLabel,
  lifecycleOperationLabelKey,
  reloadPage,
  goBackToList,
  openCreateVersionDialog,
  openEditDraftDialog,
  handleSubmitReview,
  handleApproveReview,
  handleRejectReview,
  openLifecycleImpact,
  confirmLifecycleOperation,
  handleVersionSaved,
} = useContentModuleDetailController()
</script>

<template>
  <AppPageLayout>
    <PageHeader
      show-back
      :back-label="t('contentModules.detail.backToList')"
      :title="detail?.name ?? t('contentModules.detail.loadingTitle')"
      :description="detail ? `${detail.moduleCode} · ${t('contentModules.detail.groupLabel', { groupCode: detail.groupCode })}` : undefined"
      @back="goBackToList"
    />

    <p v-if="detail?.description" class="header-extra">{{ detail.description }}</p>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="contentModulesStore.lastErrorMessageKey ?? 'contentModules.error.loadDetail'"
      @retry="reloadPage"
    />

    <el-skeleton v-else-if="contentModulesStore.loadingDetail" :rows="8" animated />

    <template v-else-if="detail">
      <ContentModuleDetailWorkspace
        v-model="activeWorkspaceTab"
        :workspace-tabs="workspaceTabs"
        :versions="versions"
        :review-history="reviewHistory"
        :preview-version="previewVersion"
        :preview-content-json="previewContentJson"
        :preview-version-label="previewVersionLabel"
        :can-edit-draft="canEditDraft"
        :can-create-version="canCreateVersion"
        :can-submit-review="canSubmitReview"
        :can-approve-review="canApproveReview"
        :can-stop="canStop"
        :can-recover="canRecover"
        :can-deprecate="canDeprecate"
        :format-review-action="formatReviewAction"
        @edit-draft="openEditDraftDialog"
        @create-version="openCreateVersionDialog"
        @submit-review="handleSubmitReview"
        @approve-review="handleApproveReview"
        @reject-review="handleRejectReview"
        @lifecycle-impact="openLifecycleImpact"
      />
    </template>

    <ContentModuleDetailDialogs
      v-model:version-dialog-open="versionDialogOpen"
      v-model:impact-dialog-open="impactDialogOpen"
      :module-id="moduleId"
      :version-dialog-mode="versionDialogMode"
      :version="selectedVersion"
      :loading-impact="contentModulesStore.loadingImpactPreview"
      :impact="contentModulesStore.lifecycleImpactPreview"
      :operation-label-key="lifecycleOperationLabelKey"
      @saved="handleVersionSaved"
      @confirm="confirmLifecycleOperation"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.header-extra {
  margin: calc(-1 * var(--space-4)) 0 var(--space-6);
  color: var(--text-muted);
}
</style>
