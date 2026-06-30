<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import { roleJourneyTitleKey, templateTeamLeadJourneySteps } from '@/constants/roleJourneyDefinitions'
import {
  resolveTemplateTeamLeadJourneyIndex,
  templateTeamLeadStepCtaKey,
  type TemplateTeamLeadJourneyContext,
} from '@/utils/templateTeamLeadJourney'

const props = withDefaults(
  defineProps<{
    journeyContext: TemplateTeamLeadJourneyContext
    canPublish?: boolean
  }>(),
  {
    canPublish: false,
  },
)

const emit = defineEmits<{
  reviewLetterhead: []
  reviewGoLiveRequest: []
  runPreReleaseChecks: []
  confirmGoLive: []
}>()

const { t } = useI18n()

const resolution = computed(() => resolveTemplateTeamLeadJourneyIndex(props.journeyContext))

const showPrimaryCta = computed(
  () =>
    props.canPublish &&
    resolution.value.currentStepIndex !== null &&
    resolution.value.activeStepId !== undefined,
)

const ctaKey = computed(() => {
  const stepId = resolution.value.activeStepId
  return stepId ? templateTeamLeadStepCtaKey(stepId) : ''
})

function handleCtaClick() {
  const stepId = resolution.value.activeStepId
  if (!stepId) {
    return
  }

  switch (stepId) {
    case 'reviewLetterhead':
      emit('reviewLetterhead')
      break
    case 'reviewGoLiveRequest':
      emit('reviewGoLiveRequest')
      break
    case 'runPreReleaseChecks':
      emit('runPreReleaseChecks')
      break
    case 'confirmGoLive':
      emit('confirmGoLive')
      break
    default:
      break
  }
}
</script>

<template>
  <RoleJourneyTimeline
    :steps="templateTeamLeadJourneySteps"
    :current-step-index="resolution.currentStepIndex"
    :guidance-key="resolution.guidanceKey"
    :title-key="roleJourneyTitleKey('GROUP_ADMIN')"
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
