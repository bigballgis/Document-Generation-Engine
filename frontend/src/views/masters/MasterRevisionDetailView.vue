<script setup lang="ts">
import MasterDesignerJourneyBlock from '@/components/journey/MasterDesignerJourneyBlock.vue'
import MasterReviewDialog from '@/components/masters/MasterReviewDialog.vue'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import MasterSubmitReviewDialog from '@/components/masters/MasterSubmitReviewDialog.vue'
import MasterWorkflowBanner from '@/components/masters/MasterWorkflowBanner.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import MasterRevisionDetailWorkspace from '@/views/masters/detail/MasterRevisionDetailWorkspace.vue'
import MasterAnchorDisplayLabelDialog from '@/components/masters/MasterAnchorDisplayLabelDialog.vue'
import { useMasterRevisionDetailController } from '@/views/masters/useMasterRevisionDetailController'
import { resolveUpdatedByDisplay } from '@/utils/userDisplay'

const {
  t,
  mastersStore,
  submitReviewOpen,
  reviewDialogOpen,
  reviewMode,
  loadFailed,
  downloading,
  activeWorkspaceTab,
  workspaceTabs,
  masterId,
  revisionLineId,
  master,
  revisionLine,
  workflowMaster,
  anchorColumnFilters,
  filteredAnchors,
  isCurrentRevision,
  canSubmitForReview,
  canDecideReview,
  showDesignerJourney,
  journeyContext,
  canWriteJourney,
  canEditAnchorDisplayLabel,
  revisionLineTitle,
  reloadPage,
  goBackToPackage,
  handleSubmitReview,
  openReviewDialog,
  handleReviewDecision,
  handleDownload,
  formatReviewAction,
  editLabelOpen,
  editingAnchor,
  savingAnchorLabel,
  openEditAnchorLabel,
  handleSaveAnchorDisplayLabel,
} = useMasterRevisionDetailController()

const { formatDateTime } = useLocaleFormatters()
</script>

<template>
  <AppPageLayout>
    <PageHeader
      show-back
      :back-label="t('masters.revision.backToPackage')"
      :title="revisionLineTitle"
      :description="master && revisionLine ? `${master.name} · ${t('masters.hub.groupLabel', { groupCode: master.groupCode })} · ${revisionLine.originalFilename}` : undefined"
      @back="goBackToPackage"
    >
      <template v-if="revisionLine" #meta>
        <MasterStatusBadge :status="revisionLine.status" />
        <el-tag v-if="isCurrentRevision" size="small" type="success">
          {{ t('masters.revisionLines.currentBadge') }}
        </el-tag>
        <el-tag v-else size="small" type="info">
          {{ t('masters.revisionLines.historicalBadge') }}
        </el-tag>
        <span class="meta-updated">
          {{ formatDateTime(revisionLine.updatedAt) }} ·
          {{ resolveUpdatedByDisplay(revisionLine.updatedBy, revisionLine.updatedByDisplayName) }}
        </span>
      </template>
    </PageHeader>

    <p v-if="revisionLine && !isCurrentRevision" class="historical-hint">
      {{ t('masters.revision.historicalReadOnlyHint') }}
    </p>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="mastersStore.lastErrorMessageKey ?? 'masters.revision.loadError'"
      @retry="reloadPage"
    />

    <el-skeleton v-else-if="mastersStore.loadingRevisionLine || mastersStore.loadingDetail" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!revisionLine || !master"
      title-key="masters.revision.notFoundTitle"
      description-key="masters.revision.notFoundDescription"
    />

    <template v-else>
      <MasterDesignerJourneyBlock
        v-if="showDesignerJourney && journeyContext"
        :journey-context="journeyContext"
        :master-id="masterId"
        :current-revision-line-id="revisionLineId"
        :can-write="canWriteJourney"
        :is-current-revision="isCurrentRevision"
        :show-primary-cta="false"
        :enable-workspace-link="false"
      />

      <MasterWorkflowBanner v-if="workflowMaster" :master="workflowMaster" />

      <MasterRevisionDetailWorkspace
        v-model="activeWorkspaceTab"
        v-model:anchor-column-filters="anchorColumnFilters"
        :workspace-tabs="workspaceTabs"
        :downloading="downloading"
        :can-submit-for-review="canSubmitForReview"
        :can-decide-review="canDecideReview"
        :can-edit-anchor-display-label="canEditAnchorDisplayLabel"
        :change-summary="revisionLine.changeSummary"
        :filtered-anchors="filteredAnchors"
        :review-history="revisionLine.reviewHistory"
        :format-review-action="formatReviewAction"
        @download="handleDownload"
        @open-submit-review="submitReviewOpen = true"
        @open-review="openReviewDialog"
        @edit-display-label="openEditAnchorLabel"
      />
    </template>

    <MasterSubmitReviewDialog v-model="submitReviewOpen" @submit="handleSubmitReview" />
    <MasterReviewDialog
      v-model="reviewDialogOpen"
      :mode="reviewMode"
      @submit="handleReviewDecision"
    />
    <MasterAnchorDisplayLabelDialog
      v-if="editingAnchor"
      v-model="editLabelOpen"
      :anchor-id="editingAnchor.anchorId"
      :initial-display-label="editingAnchor.displayLabel"
      :loading="savingAnchorLabel"
      @submit="handleSaveAnchorDisplayLabel"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.meta-updated {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.historical-hint {
  margin: calc(-1 * var(--space-4)) 0 var(--space-6);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}
</style>
