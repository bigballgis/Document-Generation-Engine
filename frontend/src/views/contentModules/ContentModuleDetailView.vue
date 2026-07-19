<script setup lang="ts">
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import LocaleVariantFamilyNav from '@/components/common/LocaleVariantFamilyNav.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { useContentModuleLocaleVariantSiblings } from '@/composables/useLocaleVariantFamilySiblings'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
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
  settingsDialogOpen,
  activeWorkspaceTab,
  workspaceTabs,
  moduleId,
  detail,
  versions,
  reviewHistory,
  formatReviewAction,
  canConfigureSharedGroups,
  detailSummaryDescription,
  sharedGroupCodes,
  ownerGroupCode,
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
  openSettingsDialog,
  openCreateVersionDialog,
  openEditDraftDialog,
  handleSubmitReview,
  handleApproveReview,
  handleRejectReview,
  openLifecycleImpact,
  confirmLifecycleOperation,
  handleVersionSaved,
} = useContentModuleDetailController()

const { contentModuleDetailLink } = useEntityLinkTargets()
const { siblings: localeVariantSiblings, loading: localeVariantLoading } =
  useContentModuleLocaleVariantSiblings(detail)
</script>

<template>
  <AppPageLayout>
    <PageHeader
      show-back
      :back-label="t('contentModules.detail.backToList')"
      :title="detail?.name ?? t('contentModules.detail.loadingTitle')"
      :description="detailSummaryDescription"
      @back="goBackToList"
    >
      <template v-if="detail && canConfigureSharedGroups" #actions>
        <el-button data-testid="content-module-settings-open" @click="openSettingsDialog">
          {{ t('contentModules.settings.open') }}
        </el-button>
      </template>
    </PageHeader>

    <p v-if="detail?.description" class="header-extra">{{ detail.description }}</p>
    <p
      v-if="detail?.locale"
      class="header-extra locale-line"
      data-testid="content-module-detail-locale"
    >
      {{ t('contentModules.detail.localeLabel', { locale: detail.locale }) }}
    </p>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="contentModulesStore.lastErrorMessageKey ?? 'contentModules.error.loadDetail'"
      @retry="reloadPage"
    />

    <el-skeleton v-else-if="contentModulesStore.loadingDetail" :rows="8" animated />

    <template v-else-if="detail">
      <LocaleVariantFamilyNav
        v-if="detail.locale || detail.localeVariantFamilyId"
        class="family-nav"
        :current-locale="detail.locale"
        :siblings="localeVariantSiblings"
        :loading="localeVariantLoading"
        :sibling-link="contentModuleDetailLink"
      />
      <ContentModuleDetailWorkspace
        v-model="activeWorkspaceTab"
        :workspace-tabs="workspaceTabs"
        :module-id="moduleId"
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
      v-model:settings-dialog-open="settingsDialogOpen"
      :module-id="moduleId"
      :version-dialog-mode="versionDialogMode"
      :version="selectedVersion"
      :loading-impact="contentModulesStore.loadingImpactPreview"
      :impact="contentModulesStore.lifecycleImpactPreview"
      :operation-label-key="lifecycleOperationLabelKey"
      :owner-group-code="ownerGroupCode"
      :shared-group-codes="sharedGroupCodes"
      :can-configure-shared-groups="canConfigureSharedGroups"
      @saved="handleVersionSaved"
      @confirm="confirmLifecycleOperation"
      @settings-saved="reloadPage"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.header-extra {
  margin: calc(-1 * var(--space-4)) 0 var(--space-6);
  color: var(--text-muted);
}

.header-extra.locale-line {
  margin-top: calc(-1 * var(--space-5));
}

.family-nav {
  margin-bottom: var(--space-6);
}
</style>
