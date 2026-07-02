<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import JourneyWorkspaceLinkButton from '@/components/journey/JourneyWorkspaceLinkButton.vue'
import { roleJourneyTitleKey, templateTesterJourneySteps } from '@/constants/roleJourneyDefinitions'
import {
  resolveTemplateTesterJourneyIndex,
  templateTesterStepCtaKey,
  type TemplateTesterJourneyContext,
} from '@/utils/templateTesterJourney'
import {
  resolveTemplateJourneyWorkspaceQuery,
  type TemplateJourneyWorkspaceQuery,
} from '@/utils/templateJourneyWorkspaceLink'

const props = withDefaults(
  defineProps<{
    journeyContext: TemplateTesterJourneyContext
    canDecide?: boolean
    showPrimaryCta?: boolean
    enableWorkspaceLink?: boolean
  }>(),
  {
    canDecide: false,
    showPrimaryCta: true,
    enableWorkspaceLink: false,
  },
)

const emit = defineEmits<{
  reviewRequest: []
  checkEvidence: []
  recordResult: []
  openWorkspace: [query: TemplateJourneyWorkspaceQuery]
}>()

const { t } = useI18n()

const resolution = computed(() => resolveTemplateTesterJourneyIndex(props.journeyContext))

const showPrimaryCtaButton = computed(
  () =>
    props.showPrimaryCta &&
    props.canDecide &&
    resolution.value.currentStepIndex !== null &&
    resolution.value.activeStepId !== undefined,
)

const ctaKey = computed(() => {
  const stepId = resolution.value.activeStepId
  return stepId ? templateTesterStepCtaKey(stepId) : ''
})

const workspaceQuery = computed(() =>
  props.enableWorkspaceLink
    ? resolveTemplateJourneyWorkspaceQuery('TESTER', resolution.value.activeStepId)
    : null,
)

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
        v-if="showPrimaryCta && showPrimaryCtaButton && ctaKey"
        type="primary"
        data-template-journey-cta
        @click="handleCtaClick"
      >
        {{ t(ctaKey) }}
      </el-button>
      <JourneyWorkspaceLinkButton
        v-else-if="enableWorkspaceLink"
        :workspace-query="workspaceQuery"
        @navigate="emit('openWorkspace', $event)"
      />
    </template>
  </RoleJourneyTimeline>
</template>
