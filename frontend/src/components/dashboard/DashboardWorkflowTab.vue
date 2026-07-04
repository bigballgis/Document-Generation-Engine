<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import type { RoleJourneyStep } from '@/constants/roleJourneyDefinitions'

defineProps<{
  journeySteps: RoleJourneyStep[]
  journeyCurrentStepIndex: number | null
  journeyGuidanceKey: string | undefined
  journeyTitleKey: string | undefined
  dashboardJourneyPath: string | null
}>()

const emit = defineEmits<{
  openJourney: []
}>()

const { t } = useI18n()
</script>

<template>
  <section id="journey-section" class="journey-section">
    <RoleJourneyTimeline
      :steps="journeySteps"
      :current-step-index="journeyCurrentStepIndex"
      :guidance-key="journeyGuidanceKey"
      :title-key="journeyTitleKey"
    >
      <template v-if="dashboardJourneyPath" #after>
        <el-button
          link
          type="primary"
          data-dashboard-journey-link
          @click="emit('openJourney')"
        >
          {{ t('dashboard.journey.openWorkspace') }}
        </el-button>
      </template>
    </RoleJourneyTimeline>
  </section>
</template>

<style scoped lang="scss">
.journey-section {
  margin-bottom: var(--space-6);
}
</style>
