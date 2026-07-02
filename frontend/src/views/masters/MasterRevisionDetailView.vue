<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import MasterReviewDialog from '@/components/masters/MasterReviewDialog.vue'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import MasterSubmitReviewDialog from '@/components/masters/MasterSubmitReviewDialog.vue'
import MasterWorkflowBanner from '@/components/masters/MasterWorkflowBanner.vue'
import MasterDesignerJourneyBlock from '@/components/journey/MasterDesignerJourneyBlock.vue'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import { canReviewMasters, sessionContext } from '@/auth/roles'
import { masterDetailPath } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useCapabilities } from '@/composables/useCapabilities'
import { shouldShowMasterDesignerJourney } from '@/utils/masterDesignerJourney'
import {
  MASTER_REVISION_WORKSPACE_TAB_LABEL_KEYS,
  buildMasterRevisionWorkspaceQuery,
  resolveMasterRevisionWorkspaceTabFromQuery,
  type MasterRevisionWorkspaceTab,
} from '@/views/masters/masterRevisionWorkspaceTabs'
import type { MasterDocumentDetail, MasterReviewDecision } from '@/types/master'
import { formatMasterRevisionLineLabel } from '@/utils/masterRevisionLineLabel'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
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
const activeWorkspaceTab = ref<MasterRevisionWorkspaceTab>(
  resolveMasterRevisionWorkspaceTabFromQuery(route.query),
)

const workspaceTabs = computed(() =>
  (['design', 'approval'] as const).map((name) => ({
    name,
    labelKey: MASTER_REVISION_WORKSPACE_TAB_LABEL_KEYS[name],
  })),
)

watch(
  () => route.query.workspaceTab,
  () => {
    activeWorkspaceTab.value = resolveMasterRevisionWorkspaceTabFromQuery(route.query)
  },
)

watch(activeWorkspaceTab, (tab) => {
  if (resolveMasterRevisionWorkspaceTabFromQuery(route.query) === tab) {
    return
  }
  void router.replace({
    query: buildMasterRevisionWorkspaceQuery(route.query, tab),
  })
})

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

const errorMessage = computed(() => {
  const key = mastersStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('masters.error.loadDetail')
})

const revisionLineTitle = computed(() =>
  formatMasterRevisionLineLabel(
    t,
    revisionLine.value?.lineLabel,
    revisionLine.value?.revisionSequence,
  ),
)

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
          {{ formatDateTime(revisionLine.updatedAt) }} · {{ revisionLine.updatedBy }}
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

      <WorkspaceTabShell v-model="activeWorkspaceTab" :tabs="workspaceTabs">
        <template #actions>
          <template v-if="activeWorkspaceTab === 'design'">
            <el-button :loading="downloading" @click="handleDownload">
              {{ t('masters.download.action') }}
            </el-button>
          </template>
          <template v-else-if="activeWorkspaceTab === 'approval'">
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
          </template>
        </template>

        <template #design>
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
              </dl>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <span>{{ t('masters.revision.anchorsTitle') }}</span>
              </template>
              <AppDataTable v-if="filteredAnchors.length > 0" :data="filteredAnchors">
                <el-table-column prop="anchorId" min-width="160">
                  <template #header>
                    <TableColumnHeader
                      :label="t('masters.revision.anchorId')"
                      v-model="anchorColumnFilters.anchorId"
                    />
                  </template>
                </el-table-column>
                <el-table-column prop="displayLabel" min-width="220">
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
        </template>

        <template #approval>
          <p class="approval-hint">{{ t('masters.revisionWorkspace.approvalHint') }}</p>
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
      </WorkspaceTabShell>
    </template>

    <MasterSubmitReviewDialog v-model="submitReviewOpen" @submit="handleSubmitReview" />
    <MasterReviewDialog
      v-model="reviewDialogOpen"
      :mode="reviewMode"
      @submit="handleReviewDecision"
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

.approval-hint {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.875rem;
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
  margin-top: 0;
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
