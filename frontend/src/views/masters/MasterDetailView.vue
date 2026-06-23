<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import MasterImpactPanel from '@/components/masters/MasterImpactPanel.vue'
import MasterReviewDialog from '@/components/masters/MasterReviewDialog.vue'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import MasterSubmitReviewDialog from '@/components/masters/MasterSubmitReviewDialog.vue'
import MasterMetadataEditDialog from '@/components/masters/MasterMetadataEditDialog.vue'
import MasterWorkflowBanner from '@/components/masters/MasterWorkflowBanner.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import { canReviewMasters, sessionContext } from '@/auth/roles'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useCapabilities } from '@/composables/useCapabilities'
import type { MasterReviewDecision } from '@/types/master'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()
const mastersStore = useMastersStore()
const sessionStore = useSessionStore()
const { manageMasters } = useCapabilities()

const submitReviewOpen = ref(false)
const reviewDialogOpen = ref(false)
const reviewMode = ref<MasterReviewDecision>('APPROVED')
const metadataEditOpen = ref(false)
const loadFailed = ref(false)

const masterId = computed(() => String(route.params.masterId ?? ''))
const master = computed(() => mastersStore.selectedMaster)
const anchorsSource = computed(() => master.value?.anchors ?? [])
const { filters: anchorColumnFilters, filteredRows: filteredAnchors } = useDataTableFilters(
  anchorsSource,
  [
    { key: 'anchorId', getValue: (row) => row.anchorId },
    { key: 'displayLabel', getValue: (row) => row.displayLabel },
  ],
)
const canReview = computed(() => canReviewMasters(sessionContext(sessionStore.session)))
const canSubmitForReview = computed(
  () => master.value?.status === 'DRAFT' || master.value?.status === 'REJECTED',
)
const canDecideReview = computed(
  () => canReview.value && master.value?.status === 'PENDING_REVIEW',
)
const canEditMetadata = computed(() => {
  if (!manageMasters.value || !master.value) {
    return false
  }
  return (
    master.value.status === 'DRAFT' ||
    master.value.status === 'REJECTED' ||
    master.value.status === 'APPROVED'
  )
})
const errorMessage = computed(() => {
  const key = mastersStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('masters.error.loadDetail')
})

onMounted(async () => {
  await reloadMaster()
})

async function reloadMaster() {
  loadFailed.value = false
  try {
    await mastersStore.fetchMaster(masterId.value)
    await mastersStore.fetchImpactAnalysis(masterId.value)
  } catch {
    loadFailed.value = true
  }
}

onUnmounted(() => {
  mastersStore.clearSelected()
})

function goBack() {
  router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.masterManagement])
}

async function handleSubmitReview(payload: { changeSummary: string }) {
  try {
    await mastersStore.submitReview(masterId.value, payload)
    submitReviewOpen.value = false
    ElMessage.success(t('masters.submitReview.success'))
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
  } catch {
    ElMessage.error(errorMessage.value || t('masters.error.decideReview'))
  }
}

async function handleMetadataUpdate(payload: { name: string; description: string | null }) {
  try {
    await mastersStore.updateMasterMetadata(masterId.value, payload)
    metadataEditOpen.value = false
    ElMessage.success(t('masters.metadata.success'))
  } catch {
    ElMessage.error(errorMessage.value || t('masters.error.updateMetadata'))
  }
}

function formatReviewAction(action: string): string {
  const key = `masters.reviewHistory.action.${action}`
  return te(key) ? t(key) : action
}
</script>

<template>
  <div class="master-detail-page">
    <header class="page-header">
      <div>
        <el-button link type="primary" @click="goBack">
          {{ t('masters.detail.backToList') }}
        </el-button>
        <h1>{{ master?.name ?? t('masters.detail.loadingTitle') }}</h1>
        <p v-if="master" class="meta">
          {{ t('masters.detail.groupLabel', { groupCode: master.groupCode }) }}
          · {{ master.originalFilename }}
        </p>
      </div>
      <div v-if="master" class="header-actions">
        <MasterStatusBadge :status="master.status" />
        <el-button v-if="canEditMetadata" @click="metadataEditOpen = true">
          {{ t('masters.metadata.edit') }}
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
      :message-key="mastersStore.lastErrorMessageKey ?? 'masters.error.loadDetail'"
      @retry="reloadMaster"
    />

    <el-skeleton v-else-if="mastersStore.loadingDetail" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!master"
      title-key="masters.empty.notFoundTitle"
      description-key="masters.empty.notFoundDescription"
    />

    <template v-else-if="master">
      <MasterWorkflowBanner :master="master" />

      <section class="detail-grid">
        <el-card shadow="never">
          <template #header>
            <span>{{ t('masters.detail.summaryTitle') }}</span>
          </template>
          <dl class="summary-list">
            <div>
              <dt>{{ t('masters.detail.description') }}</dt>
              <dd>{{ master.description || t('masters.detail.noDescription') }}</dd>
            </div>
            <div v-if="master.changeSummary">
              <dt>{{ t('masters.detail.changeSummary') }}</dt>
              <dd>{{ master.changeSummary }}</dd>
            </div>
            <div>
              <dt>{{ t('masters.detail.updatedAt') }}</dt>
              <dd>{{ new Date(master.updatedAt).toLocaleString() }}</dd>
            </div>
          </dl>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <span>{{ t('masters.detail.anchorsTitle') }}</span>
          </template>
          <AppDataTable v-if="filteredAnchors.length > 0" :data="filteredAnchors">
            <el-table-column prop="anchorId" sortable min-width="160">
              <template #header>
                <TableColumnHeader
                  :label="t('masters.detail.anchorId')"
                  v-model="anchorColumnFilters.anchorId"
                />
              </template>
            </el-table-column>
            <el-table-column prop="displayLabel" sortable min-width="220">
              <template #header>
                <TableColumnHeader
                  :label="t('masters.detail.anchorLabel')"
                  v-model="anchorColumnFilters.displayLabel"
                />
              </template>
            </el-table-column>
          </AppDataTable>
          <el-empty v-else :description="t('masters.detail.noAnchors')" />
        </el-card>
      </section>

      <el-card shadow="never" class="history-card">
        <template #header>
          <span>{{ t('masters.detail.reviewHistoryTitle') }}</span>
        </template>
        <el-timeline v-if="master.reviewHistory.length > 0">
          <el-timeline-item
            v-for="(record, index) in master.reviewHistory"
            :key="`${record.createdAt}-${index}`"
            :timestamp="new Date(record.createdAt).toLocaleString()"
          >
            <p class="history-action">{{ formatReviewAction(record.action) }}</p>
            <p v-if="record.changeSummary" class="history-text">
              {{ t('masters.detail.changeSummary') }}: {{ record.changeSummary }}
            </p>
            <p v-if="record.commentSummary" class="history-text">
              {{ t('masters.review.commentSummary') }}: {{ record.commentSummary }}
            </p>
            <p class="history-actor">
              {{ t('masters.detail.actorLabel', { username: record.actorUsername }) }}
            </p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else :description="t('masters.detail.noReviewHistory')" />
      </el-card>

      <MasterImpactPanel :impact="mastersStore.impactAnalysis" />
    </template>

    <MasterSubmitReviewDialog v-model="submitReviewOpen" @submit="handleSubmitReview" />
    <MasterReviewDialog
      v-model="reviewDialogOpen"
      :mode="reviewMode"
      @submit="handleReviewDecision"
    />
    <MasterMetadataEditDialog
      v-if="master"
      v-model="metadataEditOpen"
      :initial-name="master.name"
      :initial-description="master.description"
      :loading="mastersStore.submitting"
      @submit="handleMetadataUpdate"
    />
  </div>
</template>

<style scoped lang="scss">
.master-detail-page {
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

.page-alert {
  margin-bottom: 1rem;
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
