<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { stepGuidanceKeyFromLabel, type RoleJourneyStep } from '@/constants/roleJourneyDefinitions'

const props = defineProps<{
  open: boolean
  current: number
  steps: RoleJourneyStep[]
  targetFor: (index: number) => string | (() => HTMLElement | null)
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  'update:current': [value: number]
  dismiss: []
}>()

const { t } = useI18n()
const hostRef = ref<HTMLElement | null>(null)

const modelOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})

const modelCurrent = computed({
  get: () => props.current,
  set: (value: number) => emit('update:current', value),
})

const currentStep = computed(() => props.steps[props.current] ?? null)

function onClose() {
  emit('dismiss')
}

function onFinish() {
  emit('dismiss')
}

function skip() {
  emit('dismiss')
}

function goNext() {
  if (props.current >= props.steps.length - 1) {
    emit('dismiss')
    return
  }
  emit('update:current', props.current + 1)
}

function goPrev() {
  if (props.current <= 0) {
    return
  }
  emit('update:current', props.current - 1)
}
</script>

<template>
  <div
    ref="hostRef"
    class="onboarding-tour-host"
    data-testid="onboarding-tour"
    :hidden="!open"
  >
    <!-- Element Plus tour: spotlight / mask / Esc close only (C8-C1). -->
    <el-tour
      v-model="modelOpen"
      v-model:current="modelCurrent"
      :append-to="hostRef ?? 'body'"
      :show-close="false"
      :close-on-press-escape="true"
      :mask="true"
      type="primary"
      class="onboarding-tour"
      @close="onClose"
      @finish="onFinish"
    >
      <el-tour-step
        v-for="(step, index) in steps"
        :key="step.id"
        :target="targetFor(index)"
        :title="t(step.labelKey)"
        :description="t(stepGuidanceKeyFromLabel(step.labelKey))"
      />
    </el-tour>

    <!-- Single OA action card with stable testids (C8-C14). -->
    <div
      v-if="open && currentStep"
      class="onboarding-tour__card"
      role="dialog"
      :aria-label="t(currentStep.labelKey)"
      aria-modal="true"
    >
      <p class="onboarding-tour__title">{{ t(currentStep.labelKey) }}</p>
      <p class="onboarding-tour__description">
        {{ t(stepGuidanceKeyFromLabel(currentStep.labelKey)) }}
      </p>
      <div
        class="onboarding-tour__actions"
        role="group"
        :aria-label="t('onboardingTour.actionsLabel')"
      >
        <button
          type="button"
          class="onboarding-tour__btn onboarding-tour__btn--secondary"
          data-testid="onboarding-tour-skip"
          @click="skip"
        >
          {{ t('onboardingTour.skip') }}
        </button>
        <div class="onboarding-tour__nav">
          <button
            v-if="current > 0"
            type="button"
            class="onboarding-tour__btn onboarding-tour__btn--secondary"
            data-testid="onboarding-tour-prev"
            @click="goPrev"
          >
            {{ t('onboardingTour.prev') }}
          </button>
          <button
            v-if="current < steps.length - 1"
            type="button"
            class="onboarding-tour__btn onboarding-tour__btn--primary"
            data-testid="onboarding-tour-next"
            @click="goNext"
          >
            {{ t('onboardingTour.next') }}
          </button>
          <button
            v-else
            type="button"
            class="onboarding-tour__btn onboarding-tour__btn--primary"
            data-testid="onboarding-tour-finish"
            @click="skip"
          >
            {{ t('onboardingTour.finish') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss" src="./OnboardingTour.scss"></style>
