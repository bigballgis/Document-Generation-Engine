<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import ContentModuleStatusBadge from '@/components/contentModules/ContentModuleStatusBadge.vue'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import type {
  ContentModuleLifecycleOperation,
  ContentModuleReviewRecord,
  ContentModuleVersion,
} from '@/types/contentModule'
import type { ContentModuleWorkspaceTab } from '@/views/contentModules/contentModuleWorkspaceTabs'

defineProps<{
  workspaceTabs: Array<{ name: ContentModuleWorkspaceTab; labelKey: string }>
  versions: ContentModuleVersion[]
  reviewHistory: ContentModuleReviewRecord[]
  previewVersion: ContentModuleVersion | null
  previewContentJson: string
  previewVersionLabel: string
  canEditDraft: boolean
  canCreateVersion: boolean
  canSubmitReview: boolean
  canApproveReview: boolean
  canStop: boolean
  canRecover: boolean
  canDeprecate: boolean
  formatReviewAction: (action: string) => string
}>()

const activeWorkspaceTab = defineModel<ContentModuleWorkspaceTab>({ required: true })

const emit = defineEmits<{
  editDraft: []
  createVersion: []
  submitReview: []
  approveReview: []
  rejectReview: []
  lifecycleImpact: [operation: ContentModuleLifecycleOperation]
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
</script>

<template>
  <WorkspaceTabShell v-model="activeWorkspaceTab" :tabs="workspaceTabs">
    <template #actions>
      <template v-if="activeWorkspaceTab === 'versions'">
        <el-button v-if="canEditDraft" @click="emit('editDraft')">
          {{ t('contentModules.version.editDraft') }}
        </el-button>
        <el-button v-if="canCreateVersion" @click="emit('createVersion')">
          {{ t('contentModules.version.create') }}
        </el-button>
      </template>
      <template v-else-if="activeWorkspaceTab === 'content'">
        <el-button v-if="canEditDraft" @click="emit('editDraft')">
          {{ t('contentModules.version.editDraft') }}
        </el-button>
      </template>
      <template v-else-if="activeWorkspaceTab === 'lifecycle'">
        <el-button v-if="canSubmitReview" type="primary" @click="emit('submitReview')">
          {{ t('contentModules.review.submit') }}
        </el-button>
        <template v-if="canApproveReview">
          <el-button type="success" @click="emit('approveReview')">
            {{ t('contentModules.review.approve') }}
          </el-button>
          <el-button type="danger" @click="emit('rejectReview')">
            {{ t('contentModules.review.reject') }}
          </el-button>
        </template>
        <el-button v-if="canStop" type="warning" @click="emit('lifecycleImpact', 'STOP_USE')">
          {{ t('contentModules.lifecycle.stop') }}
        </el-button>
        <el-button v-if="canRecover" @click="emit('lifecycleImpact', 'RECOVER')">
          {{ t('contentModules.lifecycle.recover') }}
        </el-button>
        <el-button v-if="canDeprecate" type="danger" plain @click="emit('lifecycleImpact', 'DEPRECATE')">
          {{ t('contentModules.lifecycle.deprecate') }}
        </el-button>
      </template>
    </template>

    <template #versions>
      <AppDataTable v-if="versions.length > 0" :data="versions">
        <el-table-column prop="semanticVersion" :label="t('contentModules.detail.columns.version')" width="140" />
        <el-table-column :label="t('contentModules.detail.columns.status')" width="180">
          <template #default="{ row }">
            <ContentModuleStatusBadge
              :review-state="row.reviewState"
              :lifecycle-state="row.lifecycleState"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="changeDescription"
          :label="t('contentModules.detail.columns.changeDescription')"
          min-width="200"
        />
        <el-table-column
          :label="t('contentModules.detail.columns.rejectionReason')"
          min-width="200"
        >
          <template #default="{ row }">
            {{ row.rejectionReason?.trim() ? row.rejectionReason : '—' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('contentModules.detail.columns.updatedAt')" width="200">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
      </AppDataTable>
      <EmptyStatePanel
        v-else
        title-key="contentModules.detail.noVersions"
        description-key="contentModules.detail.noVersionsDescription"
      />
    </template>

    <template #content>
      <template v-if="previewVersion">
        <p class="preview-meta">{{ previewVersionLabel }}</p>
        <ControlledStructuredContentEditor
          :model-value="previewContentJson"
          readonly
        />
      </template>
      <EmptyStatePanel
        v-else
        title-key="contentModules.detail.noVersions"
        description-key="contentModules.detail.noVersionsDescription"
      />
    </template>

    <template #lifecycle>
      <p class="lifecycle-hint">{{ t('contentModules.workspace.lifecycleHint') }}</p>
      <el-card shadow="never" class="history-card">
        <template #header>
          <span>{{ t('contentModules.detail.reviewHistoryTitle') }}</span>
        </template>
        <el-timeline v-if="reviewHistory.length > 0">
          <el-timeline-item
            v-for="(record, index) in reviewHistory"
            :key="`${record.createdAt}-${index}`"
            :timestamp="formatDateTime(record.createdAt)"
          >
            <p class="history-action">{{ formatReviewAction(record.action) }}</p>
            <p v-if="record.changeSummary" class="history-text">
              {{ t('contentModules.detail.changeSummary') }}: {{ record.changeSummary }}
            </p>
            <p v-if="record.commentSummary" class="history-text">
              {{ t('contentModules.detail.commentSummary') }}: {{ record.commentSummary }}
            </p>
            <p class="history-actor">
              {{ t('contentModules.detail.actorLabel', { username: record.actorUsername }) }}
            </p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else :description="t('contentModules.detail.noReviewHistory')" />
      </el-card>
    </template>
  </WorkspaceTabShell>
</template>

<style scoped lang="scss">
.lifecycle-hint,
.preview-meta {
  margin: 0 0 var(--space-3);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
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
