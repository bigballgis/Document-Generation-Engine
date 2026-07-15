<script setup lang="ts">
import { ref, type ComponentPublicInstance } from 'vue'
import MasterImpactPanel from '@/components/masters/MasterImpactPanel.vue'
import MasterRevisionLinesPanel from '@/components/masters/MasterRevisionLinesPanel.vue'
import MasterWorkflowBanner from '@/components/masters/MasterWorkflowBanner.vue'
import MasterDesignerJourneyBlock from '@/components/journey/MasterDesignerJourneyBlock.vue'
import type { MasterDocumentDetail, MasterImpactAnalysis } from '@/types/master'

defineProps<{
  masterId: string
  master: MasterDocumentDetail
  showDesignerJourney: boolean
  journeyContext: {
    status: MasterDocumentDetail['status']
    originalFilename: string
    anchorCount: number
    reviewHistory: MasterDocumentDetail['reviewHistory']
  } | null
  currentRevisionLineId: string | undefined
  canWriteJourney: boolean
  impact: MasterImpactAnalysis | null
}>()

const emit = defineEmits<{
  upload: []
  submitReview: []
}>()

const revisionLinesPanelRef = ref<ComponentPublicInstance<{ reload: () => Promise<void> }> | null>(
  null,
)

defineExpose({
  reloadRevisionLines: () => revisionLinesPanelRef.value?.reload(),
})
</script>

<template>
  <MasterDesignerJourneyBlock
    v-if="showDesignerJourney && journeyContext"
    :journey-context="journeyContext"
    :master-id="masterId"
    :current-revision-line-id="currentRevisionLineId"
    :can-write="canWriteJourney"
    :show-primary-cta="canWriteJourney"
    :enable-workspace-link="false"
    @upload="emit('upload')"
    @submit-review="emit('submitReview')"
  />

  <MasterWorkflowBanner :master="master" />

  <MasterRevisionLinesPanel ref="revisionLinesPanelRef" :master-id="masterId" />

  <MasterImpactPanel :impact="impact" />
</template>
