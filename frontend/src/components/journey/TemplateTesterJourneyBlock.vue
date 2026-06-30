<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import { roleJourneyTitleKey, templateTesterJourneySteps } from '@/constants/roleJourneyDefinitions'
import {
  resolveTemplateTesterJourneyIndex,
  templateTesterStepCtaKey,
  type TemplateTesterJourneyContext,
} from '@/utils/templateTesterJourney'

const props = withDefaults(
  defineProps<{
    journeyContext: TemplateTesterJourneyContext
    canDecide?: boolean
  }>(),
  {
    canDecide: false,
  },
)

const emit = defineEmits<{
  reviewRequest: []
  checkEvidence: []
  recordResult: []
}>()

const { t } = useI18n()

const resolution = computed(() => resolveTemplateTesterJourneyIndex(props.journeyContext))

const showPrimaryCta = computed(
  () =>
    props.canDecide &&
    resolution.value.currentStepIndex !== null &&
    resolution.value.activeStepId !== undefined,
)

const ctaKey = computed(() => {
  const stepId = resolution.value.activeStepId
  return stepId ? templateTesterStepCtaKey(stepId) : ''
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
    case 'checkEvidence':
      emit('checkEvidence')
      break
    case 'recordResult':
      emit('recordResult')
      break
    default:
      break
  }
}
</script>

<template>
  <RoleJourneyTimeline
    :steps="templateTesterJourneySteps"
    :current-step-index="resolution.currentStepIndex"
    :guidance-key="resolution.guidanceKey"
    :title-key="roleJourneyTitleKey('TEMPLATE_TESTER')"
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
