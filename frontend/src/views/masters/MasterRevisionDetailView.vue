<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import MasterReviewDialog from '@/components/masters/MasterReviewDialog.vue'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import MasterSubmitReviewDialog from '@/components/masters/MasterSubmitReviewDialog.vue'
import MasterWorkflowBanner from '@/components/masters/MasterWorkflowBanner.vue'
import MasterDesignerJourneyBlock from '@/components/journey/MasterDesignerJourneyBlock.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import { canReviewMasters, sessionContext } from '@/auth/roles'
import { masterDetailPath } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useCapabilities } from '@/composables/useCapabilities'
import { shouldShowMasterDesignerJourney } from '@/utils/masterDesignerJourney'
import type { MasterDocumentDetail, MasterReviewDecision } from '@/types/master'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()
const mastersStore = useMastersStore()
const sessionStore = useSessionStore()
const { manageMasters, reviewMasters } = useCapabilities()

const submitReviewOpen = ref(false)
const reviewDialogOpen = ref(false)
const reviewMode = ref<MasterReviewDecision>('APPROVED')
const loadFailed = ref(false)
const downloading = ref(false)

const masterId = computed(() => String(route.params.masterId ?? ''))
const revisionLineId = computed(() => String(route.params.revisionLineId ?? ''))
const master = computed(() => mastersStore.selectedMaster)
const revisionLine = computed(() => mastersStore.selectedRevisionLine)

const workflowMaster = computed<MasterDocumentDetail | null>(() => {
  if (!master.value || !revisionLine.value) {
    return null
  }
  return {
    ...master.value,
    status: revisionLine.value.status,
    originalFilename: revisionLine.value.originalFilename,
    changeSummary: revisionLine.value.changeSummary,
    anchors: revisionLine.value.anchors,
    reviewHistory: revisionLine.value.reviewHistory,
    updatedAt: revisionLine.value.updatedAt,
    updatedBy: revisionLine.value.updatedBy,
  }
})

const anchorsSource = computed(() => revisionLine.value?.anchors ?? [])
const { filters: anchorColumnFilters, filteredRows: filteredAnchors } = useDataTableFilters(
  anchorsSource,
  [
    { key: 'anchorId', getValue: (row) => row.anchorId },
    { key: 'displayLabel', getValue: (row) => row.displayLabel },
  ],
)

const isCurrentRevision = computed(() => revisionLine.value?.current === true)
const canReview = computed(() => canReviewMasters(sessionContext(sessionStore.session)))
const canSubmitForReview = computed(
  () =>
    isCurrentRevision.value &&
    (revisionLine.value?.status === 'DRAFT' || revisionLine.value?.status === 'REJECTED'),
)
const canDecideReview = computed(
  () => canReview.value && isCurrentRevision.value && revisionLine.value?.status === 'PENDING_REVIEW',
)

const showDesignerJourney = computed(() => {
  if (!workflowMaster.value) {
    return false
  }
  return shouldShowMasterDesignerJourney({
    roles: sessionStore.session?.roles ?? [],
    manageMasters: manageMasters.value,
    reviewMasters: reviewMasters.value,
    status: workflowMaster.value.status,
  })
})

const journeyContext = computed(() => {
  if (!workflowMaster.value) {
    return null
  }
  return {
    status: workflowMaster.value.status,
    originalFilename: workflowMaster.value.originalFilename,
    anchorsLength: workflowMaster.value.anchors.length,
    reviewHistory: workflowMaster.value.reviewHistory,
  }
})

const canWriteJourney = computed(
  () =>
    Boolean(
      manageMasters.value &&
        isCurrentRevision.value &&
        workflowMaster.value &&
        workflowMaster.value.status !== 'PENDING_REVIEW',
    ),
)

const anchorsPanelRef = ref<HTMLElement | null>(null)

const errorMessage = computed(() => {
  const key = mastersStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('masters.error.loadDetail')
})

function formatLineLabel(lineLabel: string | undefined): string {
  if (lineLabel === 'CURRENT') {
    return t('masters.revisionLines.currentLine')
  }
  return lineLabel ?? ''
}

onMounted(async () => {
  await reloadPage()
})

onUnmounted(() => {
  mastersStore.clearSelected()
})

async function reloadPage() {
  loadFailed.value = false
  try {
    await mastersStore.fetchMaster(masterId.value)
    await mastersStore.fetchRevisionLine(masterId.value, revisionLineId.value)
  } catch {
    loadFailed.value = true
  }
}

function goBackToPackage() {
  router.push(masterDetailPath(masterId.value))
}

async function handleSubmitReview(payload: { changeSummary: string }) {
  try {
    await mastersStore.submitReview(masterId.value, payload)
    submitReviewOpen.value = false
    ElMessage.success(t('masters.submitReview.success'))
    await reloadPage()
  } catch {
    ElMessage.error(errorMessage.value || t('masters.error.submitReview'))
  }
}

function openReviewDialog(mode: MasterReviewDecision) {
  reviewMode.value = mode
  reviewDialogOpen.value = true
}

async function handleReviewDecision(payload: {
  decision: MasterReviewDecision
  commentSummary: string
}) {
  try {
    await mastersStore.decideReview(masterId.value, {
      decision: payload.decision,
      commentSummary: payload.commentSummary || undefined,
    })
    reviewDialogOpen.value = false
    ElMessage.success(
      t(payload.decision === 'APPROVED' ? 'masters.review.approveSuccess' : 'masters.review.rejectSuccess'),
    )
    await reloadPage()
  } catch {
    ElMessage.error(errorMessage.value || t('masters.error.decideReview'))
  }
}

async function handleDownload() {
  downloading.value = true
  try {
    await mastersStore.downloadRevisionLineFile(masterId.value, revisionLineId.value)
    ElMessage.success(t('masters.download.success'))
  } catch {
    ElMessage.error(errorMessage.value || t('masters.error.download'))
  } finally {
    downloading.value = false
  }
}

function formatReviewAction(action: string): string {
  const key = `masters.reviewHistory.action.${action}`
  return te(key) ? t(key) : action
}

function handleJourneyFocusAnchors() {
  anchorsPanelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <div class="master-revision-detail-page">
    <header class="page-header">
      <div>
        <el-button link type="primary" @click="goBackToPackage">
          {{ t('masters.revision.backToPackage') }}
        </el-button>
        <h1>{{ formatLineLabel(revisionLine?.lineLabel) }}</h1>
        <p v-if="master && revisionLine" class="meta">
          {{ master.name }}
          · {{ t('masters.hub.groupLabel', { groupCode: master.groupCode }) }}
          · {{ revisionLine.originalFilename }}
        </p>
      </div>
      <div v-if="revisionLine" class="header-actions">
        <MasterStatusBadge :status="revisionLine.status" />
        <el-button :loading="downloading" @click="handleDownload">
          {{ t('masters.download.action') }}
        </el-button>
        <el-button v-if="canSubmitForReview" type="primary" @click="submitReviewOpen = true">
          {{ t('masters.submitReview.open') }}
        </el-button>
        <template v-if="canDecideReview">
          <el-button type="success" @click="openReviewDialog('APPROVED')">
            {{ t('masters.review.approve') }}
          </el-button>
          <el-button type="danger" @click="openReviewDialog('REJECTED')">
            {{ t('masters.review.reject') }}
          </el-button>
        </template>
      </div>
    </header>

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
        @upload="router.push(masterDetailPath(masterId))"
        @submit-review="submitReviewOpen = true"
        @focus-anchors="handleJourneyFocusAnchors"
      />

      <MasterWorkflowBanner v-if="workflowMaster" :master="workflowMaster" />

      <section class="detail-grid">
        <el-card shadow="never">
          <template #header>
            <span>{{ t('masters.revision.summaryTitle') }}</span>
          </template>
          <dl class="summary-list">
            <div v-if="revisionLine.changeSummary">
              <dt>{{ t('masters.revision.changeSummary') }}</dt>
              <dd>{{ revisionLine.changeSummary }}</dd>
            </div>
            <div>
              <dt>{{ t('masters.revision.updatedAt') }}</dt>
              <dd>{{ new Date(revisionLine.updatedAt).toLocaleString() }}</dd>
            </div>
            <div>
              <dt>{{ t('masters.revision.updatedBy') }}</dt>
              <dd>{{ revisionLine.updatedBy }}</dd>
            </div>
          </dl>
        </el-card>

        <section id="anchors-panel" ref="anchorsPanelRef">
        <el-card shadow="never">
          <template #header>
            <span>{{ t('masters.revision.anchorsTitle') }}</span>
          </template>
          <AppDataTable v-if="filteredAnchors.length > 0" :data="filteredAnchors">
            <el-table-column prop="anchorId" sortable min-width="160">
              <template #header>
                <TableColumnHeader
                  :label="t('masters.revision.anchorId')"
                  v-model="anchorColumnFilters.anchorId"
                />
              </template>
            </el-table-column>
            <el-table-column prop="displayLabel" sortable min-width="220">
              <template #header>
                <TableColumnHeader
                  :label="t('masters.revision.anchorLabel')"
                  v-model="anchorColumnFilters.displayLabel"
                />
              </template>
            </el-table-column>
          </AppDataTable>
          <el-empty v-else :description="t('masters.revision.noAnchors')" />
        </el-card>
        </section>
      </section>

      <el-card shadow="never" class="history-card">
        <template #header>
          <span>{{ t('masters.revision.reviewHistoryTitle') }}</span>
        </template>
        <el-timeline v-if="revisionLine.reviewHistory.length > 0">
          <el-timeline-item
            v-for="(record, index) in revisionLine.reviewHistory"
            :key="`${record.createdAt}-${index}`"
            :timestamp="new Date(record.createdAt).toLocaleString()"
          >
            <p class="history-action">{{ formatReviewAction(record.action) }}</p>
            <p v-if="record.changeSummary" class="history-text">
              {{ t('masters.revision.changeSummary') }}: {{ record.changeSummary }}
            </p>
            <p v-if="record.commentSummary" class="history-text">
              {{ t('masters.review.commentSummary') }}: {{ record.commentSummary }}
            </p>
            <p class="history-actor">
              {{ t('masters.revision.actorLabel', { username: record.actorUsername }) }}
            </p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else :description="t('masters.revision.noReviewHistory')" />
      </el-card>
    </template>

    <MasterSubmitReviewDialog v-model="submitReviewOpen" @submit="handleSubmitReview" />
    <MasterReviewDialog
      v-model="reviewDialogOpen"
      :mode="reviewMode"
      @submit="handleReviewDecision"
    />
  </div>
</template>

<style scoped lang="scss">
.master-revision-detail-page {
  min-height: 100vh;
  padding: 2rem;
  background: var(--surface-bg);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;

  h1 {
    margin: 0.5rem 0 0.25rem;
    font-size: 1.75rem;
  }
}

.meta {
  margin: 0;
  color: var(--text-muted);
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.summary-list {
  margin: 0;

  div + div {
    margin-top: 1rem;
  }

  dt {
    margin: 0;
    font-weight: 600;
  }

  dd {
    margin: 0.25rem 0 0;
    color: var(--text-muted);
  }
}

.history-card {
  margin-top: 1rem;
}

.history-action {
  margin: 0;
  font-weight: 600;
}

.history-text,
.history-actor {
  margin: 0.25rem 0 0;
  color: var(--text-muted);
}
</style>
