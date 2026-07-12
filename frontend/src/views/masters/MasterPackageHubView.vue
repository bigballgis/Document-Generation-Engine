<script setup lang="ts">
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import MasterPackageHubActions from '@/views/masters/hub/MasterPackageHubActions.vue'
import MasterPackageHubBody from '@/views/masters/hub/MasterPackageHubBody.vue'
import MasterPackageHubDialogs from '@/views/masters/hub/MasterPackageHubDialogs.vue'
import { useMasterPackageHub } from '@/views/masters/useMasterPackageHub'

const {
  t,
  mastersStore,
  metadataEditOpen,
  replaceFileOpen,
  submitReviewOpen,
  loadFailed,
  downloading,
  currentRevisionLineId,
  bodyRef,
  masterId,
  master,
  canEditMetadata,
  canReplaceFile,
  showDesignerJourney,
  journeyContext,
  canWriteJourney,
  reloadMaster,
  goBack,
  handleDownloadCurrent,
  handleMetadataUpdate,
  handleReplaceFile,
  clearReplaceServerError,
  handleSubmitReview,
} = useMasterPackageHub()
</script>

<template>
  <AppPageLayout>
    <PageHeader
      show-back
      :back-label="t('masters.hub.backToList')"
      :title="master?.name ?? t('masters.hub.loadingTitle')"
      :description="master ? t('masters.hub.groupLabel', { groupCode: master.groupCode }) : undefined"
      @back="goBack"
    >
      <template v-if="master" #actions>
        <MasterPackageHubActions
          :status="master.status"
          :downloading="downloading"
          :can-replace-file="canReplaceFile"
          :can-edit-metadata="canEditMetadata"
          @download="handleDownloadCurrent"
          @replace-file="replaceFileOpen = true"
          @edit-metadata="metadataEditOpen = true"
        />
      </template>
    </PageHeader>

    <p v-if="master?.description" class="header-extra">{{ master.description }}</p>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="mastersStore.lastErrorMessageKey ?? 'masters.error.loadDetail'"
      @retry="reloadMaster"
    />

    <el-skeleton v-else-if="mastersStore.loadingDetail" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!master"
      title-key="masters.hub.notFoundTitle"
      description-key="masters.hub.notFoundDescription"
    />

    <MasterPackageHubBody
      v-else-if="master"
      ref="bodyRef"
      :master-id="masterId"
      :master="master"
      :show-designer-journey="showDesignerJourney"
      :journey-context="journeyContext"
      :current-revision-line-id="currentRevisionLineId"
      :can-write-journey="canWriteJourney"
      :impact="mastersStore.impactAnalysis"
    />

    <MasterPackageHubDialogs
      v-if="master"
      v-model:metadata-edit-open="metadataEditOpen"
      v-model:replace-file-open="replaceFileOpen"
      v-model:submit-review-open="submitReviewOpen"
      :master-name="master.name"
      :master-description="master.description"
      :original-filename="master.originalFilename"
      :submitting="mastersStore.submitting"
      :upload-progress="mastersStore.uploadProgress"
      :replace-server-error-key="replaceFileOpen ? mastersStore.lastErrorMessageKey : null"
      @metadata-submit="handleMetadataUpdate"
      @replace-submit="handleReplaceFile"
      @clear-server-error="clearReplaceServerError"
      @submit-review="handleSubmitReview"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.header-extra {
  margin: calc(-1 * var(--space-4)) 0 var(--space-6);
  color: var(--text-secondary);
}
</style>
