<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import TemplatePublishSummaryDialog from '@/components/templates/TemplatePublishSummaryDialog.vue'
import TemplateSubmitForApprovalSummaryDialog from '@/components/templates/TemplateSubmitForApprovalSummaryDialog.vue'
import TemplateLifecycleDecisionDialog from '@/components/templates/TemplateLifecycleDecisionDialog.vue'
import TemplateMetadataEditDialog from '@/components/templates/TemplateMetadataEditDialog.vue'
import LifecycleCommentDialog from '@/components/templates/LifecycleCommentDialog.vue'
import type {
  ChangeDiffSummary,
  CoverageSummary,
  PreviewComparison,
} from '@/types/template'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'

type LifecycleDecisionDialogMode =
  | 'test-fail'
  | 'test-pass'
  | 'approval-reject'
  | 'approval-approve'

defineProps<{
  templateName: string
  templateDescription: string | null
  templateId: string
  submitting: boolean
  publishVersion: string
  publishGateItems: PublishGateDisplayItem[]
  publishCoverageSummary: CoverageSummary | null
  publishChangeDiffSummary: ChangeDiffSummary | null
  submitGateItems: PublishGateDisplayItem[]
  submitCoverageSummary: CoverageSummary | null
  submitChangeDiffSummary: ChangeDiffSummary | null
  previewComparison: PreviewComparison | null
  decisionDialogMode: LifecycleDecisionDialogMode
  lifecycleComment: string
  credentialSecretExternalId: string
  displayedCredentialSecret: string
  credentialSecretValue: string
}>()

const metadataEditOpen = defineModel<boolean>('metadataEditOpen', { required: true })
const publishSummaryOpen = defineModel<boolean>('publishSummaryOpen', { required: true })
const submitSummaryOpen = defineModel<boolean>('submitSummaryOpen', { required: true })
const decisionDialogOpen = defineModel<boolean>('decisionDialogOpen', { required: true })
const lifecycleCommentDialogOpen = defineModel<boolean>('lifecycleCommentDialogOpen', {
  required: true,
})
const credentialSecretDialogVisible = defineModel<boolean>('credentialSecretDialogVisible', {
  required: true,
})

const emit = defineEmits<{
  metadataSubmit: [payload: { name: string; description: string | null }]
  confirmPublish: [payload: { fidelityViewedConfirmed: boolean }]
  confirmSubmit: []
  submitDecision: [
    payload: {
      reasonCategory?: string
      impactSummary?: string
      commentSummary?: string
      fidelityViewedConfirmed?: boolean
      coverageViewedConfirmed?: boolean
      previewViewedConfirmed?: boolean
      keyEvidenceConfirmed?: boolean
      remediationTestRecordId?: string
      remediationChangeDiffRef?: string
      remediationChecklistCode?: string
      exceptionIntervention?: boolean
      exceptionReason?: string
      secondaryConfirmed?: boolean
    },
  ]
  submitForTest: [comment: string]
}>()

const { t } = useI18n()
</script>

<template>
  <TemplateMetadataEditDialog
    v-model="metadataEditOpen"
    :initial-name="templateName"
    :initial-description="templateDescription"
    :loading="submitting"
    @submit="emit('metadataSubmit', $event)"
  />

  <TemplatePublishSummaryDialog
    v-model="publishSummaryOpen"
    :template-name="templateName"
    :release-version="publishVersion"
    :gate-items="publishGateItems"
    :coverage-summary="publishCoverageSummary"
    :change-diff-summary="publishChangeDiffSummary"
    :preview-comparison="previewComparison"
    :loading="submitting"
    @confirm="emit('confirmPublish', $event)"
  />

  <TemplateSubmitForApprovalSummaryDialog
    v-model="submitSummaryOpen"
    :template-name="templateName"
    :gate-items="submitGateItems"
    :coverage-summary="submitCoverageSummary"
    :change-diff-summary="submitChangeDiffSummary"
    :preview-comparison="previewComparison"
    :loading="submitting"
    @confirm="emit('confirmSubmit')"
  />

  <TemplateLifecycleDecisionDialog
    v-model="decisionDialogOpen"
    :mode="decisionDialogMode"
    :template-id="templateId"
    :loading="submitting"
    :initial-comment="lifecycleComment"
    @submit="emit('submitDecision', $event)"
  />

  <LifecycleCommentDialog
    v-model="lifecycleCommentDialogOpen"
    :loading="submitting"
    @confirm="emit('submitForTest', $event)"
  />

  <el-dialog
    v-model="credentialSecretDialogVisible"
    :title="t('templates.policy.credentialSecretDialogTitle')"
    width="480px"
    :close-on-click-modal="false"
  >
    <p>{{ t('templates.policy.credentialSecretHint') }}</p>
    <p>{{ t('templates.policy.credentialExternalId') }}: {{ credentialSecretExternalId }}</p>
    <el-input
      :model-value="displayedCredentialSecret || credentialSecretValue"
      readonly
      type="textarea"
      :rows="3"
    />
    <template #footer>
      <el-button type="primary" @click="credentialSecretDialogVisible = false">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>
