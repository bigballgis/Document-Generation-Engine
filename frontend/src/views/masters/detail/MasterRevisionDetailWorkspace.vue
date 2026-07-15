<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import MasterAnchorPositionOverview from '@/components/masters/MasterAnchorPositionOverview.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import type { MasterRevisionWorkspaceTab } from '@/views/masters/masterRevisionWorkspaceTabs'
import type { MasterAnchor, MasterReviewDecision } from '@/types/master'

defineProps<{
  workspaceTabs: Array<{ name: MasterRevisionWorkspaceTab; labelKey: string }>
  downloading: boolean
  canSubmitForReview: boolean
  canDecideReview: boolean
  canEditAnchorDisplayLabel: boolean
  changeSummary: string | null | undefined
  filteredAnchors: MasterAnchor[]
  reviewHistory: Array<{
    createdAt: string
    action: string
    changeSummary?: string | null
    commentSummary?: string | null
    actorUsername: string
  }>
  formatReviewAction: (action: string) => string
}>()

const activeWorkspaceTab = defineModel<MasterRevisionWorkspaceTab>({ required: true })
const anchorColumnFilters = defineModel<Record<string, string>>('anchorColumnFilters', {
  required: true,
})

const emit = defineEmits<{
  download: []
  openSubmitReview: []
  openReview: [mode: MasterReviewDecision]
  editDisplayLabel: [anchor: MasterAnchor]
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
</script>

<template>
  <WorkspaceTabShell v-model="activeWorkspaceTab" :tabs="workspaceTabs">
    <template #actions>
      <template v-if="activeWorkspaceTab === 'design'">
        <el-button :loading="downloading" @click="emit('download')">
          {{ t('masters.download.action') }}
        </el-button>
      </template>
      <template v-else-if="activeWorkspaceTab === 'approval'">
        <el-button v-if="canSubmitForReview" type="primary" @click="emit('openSubmitReview')">
          {{ t('masters.submitReview.open') }}
        </el-button>
        <template v-if="canDecideReview">
          <el-button type="success" @click="emit('openReview', 'APPROVED')">
            {{ t('masters.review.approve') }}
          </el-button>
          <el-button type="danger" @click="emit('openReview', 'REJECTED')">
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
            <div v-if="changeSummary">
              <dt>{{ t('masters.revision.changeSummary') }}</dt>
              <dd>{{ changeSummary }}</dd>
            </div>
          </dl>
        </el-card>

        <el-card shadow="never" class="anchors-card">
          <template #header>
            <span>{{ t('masters.revision.anchorsTitle') }}</span>
          </template>
          <MasterAnchorPositionOverview
            :anchors="filteredAnchors"
            :can-edit-display-label="canEditAnchorDisplayLabel"
            :column-filters="anchorColumnFilters"
            @update:column-filters="anchorColumnFilters = $event"
            @edit-display-label="emit('editDisplayLabel', $event)"
          />
        </el-card>
      </section>
    </template>

    <template #approval>
      <p class="approval-hint">{{ t('masters.revisionWorkspace.approvalHint') }}</p>
      <el-card shadow="never" class="history-card">
        <template #header>
          <span>{{ t('masters.revision.reviewHistoryTitle') }}</span>
        </template>
        <el-timeline v-if="reviewHistory.length > 0">
          <el-timeline-item
            v-for="(record, index) in reviewHistory"
            :key="`${record.createdAt}-${index}`"
            :timestamp="formatDateTime(record.createdAt)"
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

<style scoped lang="scss">
.approval-hint {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}

.anchors-card {
  min-width: 0;
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
