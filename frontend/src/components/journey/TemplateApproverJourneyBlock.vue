<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import { roleJourneyTitleKey, templateApproverJourneySteps } from '@/constants/roleJourneyDefinitions'
import {
  resolveTemplateApproverJourneyIndex,
  templateApproverStepCtaKey,
  type TemplateApproverJourneyContext,
} from '@/utils/templateApproverJourney'

const props = withDefaults(
  defineProps<{
    journeyContext: TemplateApproverJourneyContext
    canDecide?: boolean
  }>(),
  {
    canDecide: false,
  },
)

const emit = defineEmits<{
  reviewRequest: []
  reviewSubmission: []
  recordDecision: []
}>()

const { t } = useI18n()

const resolution = computed(() => resolveTemplateApproverJourneyIndex(props.journeyContext))

const showPrimaryCta = computed(
  () =>
    props.canDecide &&
    resolution.value.currentStepIndex !== null &&
    resolution.value.activeStepId !== undefined,
)

const ctaKey = computed(() => {
  const stepId = resolution.value.activeStepId
  return stepId ? templateApproverStepCtaKey(stepId) : ''
})

function handleCtaClick() {
  const stepId = resolution.value.activeStepId
  if (!stepId) {
    return
  }

  switch (stepId) {
    case 'reviewRequest':
      emit('reviewRequest')
      break
    case 'reviewSubmission':
      emit('reviewSubmission')
      break
    case 'recordDecision':
      emit('recordDecision')
      break
    default:
      break
  }
}
</script>

<template>
  <RoleJourneyTimeline
    :steps="templateApproverJourneySteps"
    :current-step-index="resolution.currentStepIndex"
    :guidance-key="resolution.guidanceKey"
    :title-key="roleJourneyTitleKey('TEMPLATE_APPROVER')"
  >
    <template #after>
      <el-button
        v-if="showPrimaryCta && ctaKey"
        type="primary"
        data-template-journey-cta
        @click="handleCtaClick"
      >
        {{ t(ctaKey) }}
      </el-button>
    </template>
  </RoleJourneyTimeline>
</template>
