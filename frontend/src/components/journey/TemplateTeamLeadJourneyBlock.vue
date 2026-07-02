<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import JourneyWorkspaceLinkButton from '@/components/journey/JourneyWorkspaceLinkButton.vue'
import { roleJourneyTitleKey, templateTeamLeadJourneySteps } from '@/constants/roleJourneyDefinitions'
import {
  resolveTemplateTeamLeadJourneyIndex,
  templateTeamLeadStepCtaKey,
  type TemplateTeamLeadJourneyContext,
} from '@/utils/templateTeamLeadJourney'
import {
  resolveTemplateJourneyWorkspaceQuery,
  type TemplateJourneyWorkspaceQuery,
} from '@/utils/templateJourneyWorkspaceLink'

const props = withDefaults(
  defineProps<{
    journeyContext: TemplateTeamLeadJourneyContext
    canPublish?: boolean
    showPrimaryCta?: boolean
    enableWorkspaceLink?: boolean
  }>(),
  {
    canPublish: false,
    showPrimaryCta: true,
    enableWorkspaceLink: false,
  },
)

const emit = defineEmits<{
  reviewLetterhead: []
  reviewGoLiveRequest: []
  runPreReleaseChecks: []
  confirmGoLive: []
  openWorkspace: [query: TemplateJourneyWorkspaceQuery]
}>()

const { t } = useI18n()

const resolution = computed(() => resolveTemplateTeamLeadJourneyIndex(props.journeyContext))

const showPrimaryCtaButton = computed(
  () =>
    props.showPrimaryCta &&
    props.canPublish &&
    resolution.value.currentStepIndex !== null &&
    resolution.value.activeStepId !== undefined,
)

const ctaKey = computed(() => {
  const stepId = resolution.value.activeStepId
  return stepId ? templateTeamLeadStepCtaKey(stepId) : ''
})

const workspaceQuery = computed(() =>
  props.enableWorkspaceLink
    ? resolveTemplateJourneyWorkspaceQuery('TEAM_LEAD', resolution.value.activeStepId)
    : null,
)

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
