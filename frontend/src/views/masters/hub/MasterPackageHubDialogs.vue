<script setup lang="ts">
import MasterMetadataEditDialog from '@/components/masters/MasterMetadataEditDialog.vue'
import MasterReplaceFileDialog from '@/components/masters/MasterReplaceFileDialog.vue'
import MasterReviewDialog from '@/components/masters/MasterReviewDialog.vue'
import MasterSubmitReviewDialog from '@/components/masters/MasterSubmitReviewDialog.vue'
import type { MasterImpactAnalysis, MasterReviewDecision } from '@/types/master'

defineProps<{
  masterName: string
  masterDescription: string | null
  originalFilename: string
  submitting: boolean
  uploadProgress: number | null
  replaceServerErrorKey: string | null
  reviewMode: MasterReviewDecision
  impact: MasterImpactAnalysis | null
}>()

const metadataEditOpen = defineModel<boolean>('metadataEditOpen', { required: true })
const replaceFileOpen = defineModel<boolean>('replaceFileOpen', { required: true })
const submitReviewOpen = defineModel<boolean>('submitReviewOpen', { required: true })
const reviewDialogOpen = defineModel<boolean>('reviewDialogOpen', { required: true })

const emit = defineEmits<{
  metadataSubmit: [payload: { name: string; description: string | null }]
  replaceSubmit: [file: File]
  clearServerError: []
  submitReview: [payload: { changeSummary: string }]
  decideReview: [payload: { decision: MasterReviewDecision; commentSummary: string }]
}>()
</script>

<template>
  <MasterMetadataEditDialog
    v-model="metadataEditOpen"
    :initial-name="masterName"
    :initial-description="masterDescription"
    :loading="submitting"
    @submit="emit('metadataSubmit', $event)"
  />
  <MasterReplaceFileDialog
    v-model="replaceFileOpen"
    :current-filename="originalFilename"
    :loading="submitting"
    :upload-progress="uploadProgress"
    :server-error-key="replaceServerErrorKey"
    :impact="impact"
    @submit="emit('replaceSubmit', $event)"
    @clear-server-error="emit('clearServerError')"
  />
  <MasterSubmitReviewDialog v-model="submitReviewOpen" @submit="emit('submitReview', $event)" />
  <MasterReviewDialog
    v-model="reviewDialogOpen"
    :mode="reviewMode"
    :submitting="submitting"
    @submit="emit('decideReview', $event)"
  />
</template>
