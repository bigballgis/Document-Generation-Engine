<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import { roleJourneyTitleKey, templateAuthorJourneySteps } from '@/constants/roleJourneyDefinitions'
import { templateDetailPath } from '@/routing/routeKeys'
import {
  resolveTemplateAuthorJourneyIndex,
  templateAuthorStepCtaKey,
  type TemplateAuthorJourneyContext,
} from '@/utils/templateAuthorJourney'

const props = withDefaults(
  defineProps<{
    journeyContext: TemplateAuthorJourneyContext
    templateId: string
    canWrite?: boolean
    primaryCtaDisabled?: boolean
  }>(),
  {
    canWrite: false,
    primaryCtaDisabled: false,
  },
)

const emit = defineEmits<{
  create: []
  design: []
  trialGenerate: []
  submitForTest: []
  submitForApproval: []
}>()

const { t } = useI18n()
const router = useRouter()

const resolution = computed(() => resolveTemplateAuthorJourneyIndex(props.journeyContext))

const showPrimaryCta = computed(
  () =>
    props.canWrite &&
    resolution.value.currentStepIndex !== null &&
    resolution.value.activeStepId !== undefined &&
    resolution.value.activeStepId !== 'awaitGoLive',
)

const ctaKey = computed(() => {
  const stepId = resolution.value.activeStepId
  return stepId ? templateAuthorStepCtaKey(stepId) : ''
})

function handleCtaClick() {
  const stepId = resolution.value.activeStepId
  if (!stepId) {
    return
  }

  switch (stepId) {
    case 'create':
      emit('create')
      break
    case 'design':
      emit('design')
      break
    case 'trialGenerate':
      emit('trialGenerate')
      break
    case 'submitTest':
      emit('submitForTest')
      break
    case 'submitApproval':
      emit('submitForApproval')
      break
    default:
      break
  }
}

function openOverviewTab() {
  router.push(templateDetailPath(props.templateId, 'overview'))
}
</script>

<template>
  <RoleJourneyTimeline
    :steps="templateAuthorJourneySteps"
    :current-step-index="resolution.currentStepIndex"
    :guidance-key="resolution.guidanceKey"
    :title-key="roleJourneyTitleKey('TEMPLATE_AUTHOR')"
  >
    <template #after>
      <el-button
        v-if="showPrimaryCta && ctaKey"
        type="primary"
        data-template-journey-cta
        :disabled="primaryCtaDisabled"
        @click="handleCtaClick"
      >
        {{ t(ctaKey) }}
      </el-button>
      <el-button
        v-else-if="
          resolution.guidanceKey === 'journey.roles.TEMPLATE_AUTHOR.complete.guidance'
        "
        link
        type="primary"
        @click="openOverviewTab"
      >
        {{ t('journey.roles.TEMPLATE_AUTHOR.complete.cta') }}
      </el-button>
    </template>
  </RoleJourneyTimeline>
</template>
