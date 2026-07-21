<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import { masterDesignerJourneySteps, roleJourneyTitleKey } from '@/constants/roleJourneyDefinitions'
import { masterRevisionDetailPath } from '@/routing/routeKeys'
import {
  masterDesignerStepCtaKey,
  resolveMasterDesignerJourneyIndex,
  type MasterDesignerJourneyContext,
} from '@/utils/masterDesignerJourney'
import {
  resolveMasterDesignerWorkspaceNavigation,
  type MasterDesignerWorkspaceNavigation,
} from '@/utils/masterDesignerWorkspaceLink'

const props = withDefaults(
  defineProps<{
    journeyContext: MasterDesignerJourneyContext
    masterId: string
    currentRevisionLineId?: string
    canWrite?: boolean
    isCurrentRevision?: boolean
    showPrimaryCta?: boolean
    enableWorkspaceLink?: boolean
  }>(),
  {
    canWrite: false,
    isCurrentRevision: true,
    showPrimaryCta: true,
    enableWorkspaceLink: false,
  },
)

const emit = defineEmits<{
  upload: []
  submitReview: []
  focusAnchors: []
  openWorkspace: [target: MasterDesignerWorkspaceNavigation]
}>()

const { t } = useI18n()
const router = useRouter()

const resolution = computed(() => resolveMasterDesignerJourneyIndex(props.journeyContext))

const showPrimaryCtaButton = computed(
  () =>
    props.showPrimaryCta &&
    props.canWrite &&
    props.isCurrentRevision &&
    resolution.value.currentStepIndex !== null &&
    resolution.value.activeStepId !== undefined,
)

const ctaKey = computed(() => {
  const stepId = resolution.value.activeStepId
  return stepId ? masterDesignerStepCtaKey(stepId) : ''
})

const workspaceNavigation = computed(() =>
  props.enableWorkspaceLink
    ? resolveMasterDesignerWorkspaceNavigation(resolution.value.activeStepId)
    : null,
)

function handleCtaClick() {
  const stepId = resolution.value.activeStepId
  if (!stepId) {
    return
  }

  switch (stepId) {
    case 'upload':
      emit('upload')
      break
    case 'placeholders':
      if (props.currentRevisionLineId) {
        emit('focusAnchors')
      } else {
        void openCurrentRevision()
      }
      break
    case 'submitReview':
      emit('submitReview')
      break
    case 'rework':
      emit('upload')
      break
    default:
      break
  }
}

function handleWorkspaceLink() {
  const target = workspaceNavigation.value
  if (!target) {
    return
  }
  emit('openWorkspace', target)
}

async function openCurrentRevision() {
  if (props.currentRevisionLineId) {
    router.push(masterRevisionDetailPath(props.masterId, props.currentRevisionLineId))
  }
}
</script>

<template>
  <RoleJourneyTimeline
    :steps="masterDesignerJourneySteps"
    :current-step-index="resolution.currentStepIndex"
    :guidance-key="resolution.guidanceKey"
    :title-key="roleJourneyTitleKey('DOCUMENT_AUTHOR')"
  >
    <template #after>
      <el-button
        v-if="showPrimaryCta && showPrimaryCtaButton && ctaKey"
        type="primary"
        data-master-journey-cta
        @click="handleCtaClick"
      >
        {{ t(ctaKey) }}
      </el-button>
      <el-button
        v-else-if="
          showPrimaryCta &&
          resolution.guidanceKey === 'journey.roles.DOCUMENT_AUTHOR.letterhead.complete.guidance'
        "
        link
        type="primary"
        @click="router.push(masterRevisionDetailPath(masterId, currentRevisionLineId ?? ''))"
      >
        {{ t('journey.roles.DOCUMENT_AUTHOR.letterhead.complete.cta') }}
      </el-button>
      <el-button
        v-else-if="enableWorkspaceLink && workspaceNavigation"
        link
        type="primary"
        data-master-journey-workspace-link
        @click="handleWorkspaceLink"
      >
        {{ t('masters.revisionWorkspace.openWorkspace') }}
      </el-button>
    </template>
  </RoleJourneyTimeline>
</template>
