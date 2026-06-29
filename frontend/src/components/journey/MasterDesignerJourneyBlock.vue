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

const props = withDefaults(
  defineProps<{
    journeyContext: MasterDesignerJourneyContext
    masterId: string
    currentRevisionLineId?: string
    canWrite?: boolean
    isCurrentRevision?: boolean
  }>(),
  {
    canWrite: false,
    isCurrentRevision: true,
  },
)

const emit = defineEmits<{
  upload: []
  submitReview: []
  focusAnchors: []
}>()

const { t } = useI18n()
const router = useRouter()

const resolution = computed(() => resolveMasterDesignerJourneyIndex(props.journeyContext))

const showPrimaryCta = computed(
  () =>
    props.canWrite &&
    props.isCurrentRevision &&
    resolution.value.currentStepIndex !== null &&
    resolution.value.activeStepId !== undefined,
)

const ctaKey = computed(() => {
  const stepId = resolution.value.activeStepId
  return stepId ? masterDesignerStepCtaKey(stepId) : ''
})

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
    :title-key="roleJourneyTitleKey('MASTER_DESIGNER')"
  >
    <template #after>
      <el-button
        v-if="showPrimaryCta && ctaKey"
        type="primary"
        data-master-journey-cta
        @click="handleCtaClick"
      >
        {{ t(ctaKey) }}
      </el-button>
      <el-button
        v-else-if="
          resolution.guidanceKey === 'journey.roles.MASTER_DESIGNER.complete.guidance'
        "
        link
        type="primary"
        @click="router.push(masterRevisionDetailPath(masterId, currentRevisionLineId ?? ''))"
      >
        {{ t('journey.roles.MASTER_DESIGNER.complete.cta') }}
      </el-button>
    </template>
  </RoleJourneyTimeline>
</template>
