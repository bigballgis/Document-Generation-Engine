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

<style scoped lang="scss">
.onboarding-tour-host {
  position: relative;
}

.onboarding-tour__card {
  position: fixed;
  z-index: 2100;
  left: 50%;
  bottom: var(--space-6, 1.5rem);
  transform: translateX(-50%);
  width: min(28rem, calc(100vw - 2rem));
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background: var(--surface-elevated, #fff);
  border: 1px solid var(--border-color, #e4e7eb);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft, 0 8px 24px rgb(0 0 0 / 12%));
}

.onboarding-tour__title {
  margin: 0 0 0.35rem;
  font-size: 1rem;
  font-weight: 650;
  color: var(--text-primary, #1a1a1a);
}

.onboarding-tour__description {
  margin: 0;
  color: var(--text-muted, #5c6670);
  font-size: 0.9rem;
  line-height: 1.45;
}

.onboarding-tour__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3, 0.75rem);
  margin-top: var(--space-4, 1rem);
}

.onboarding-tour__nav {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2, 0.5rem);
  margin-left: auto;
}

.onboarding-tour__btn {
  min-height: 2rem;
  padding: 0.35rem 0.85rem;
  border-radius: var(--radius-sm);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  outline: none;
  border: 1px solid transparent;
  transition:
    background var(--transition-base, 0.15s ease),
    border-color var(--transition-base, 0.15s ease),
    color var(--transition-base, 0.15s ease);

  &:focus-visible {
    box-shadow:
      0 0 0 2px var(--surface-elevated, #fff),
      0 0 0 4px var(--brand-primary);
  }
}

.onboarding-tour__btn--secondary {
  background: var(--surface-elevated, #fff);
  border-color: var(--border-color, #e4e7eb);
  color: var(--text-primary, #1a1a1a);

  &:hover {
    border-color: var(--brand-primary);
    color: var(--brand-primary);
  }
}

.onboarding-tour__btn--primary {
  background: var(--brand-primary);
  border-color: var(--brand-primary);
  color: var(--on-primary, #fff);

  &:hover {
    background: var(--brand-primary-hover, var(--brand-primary));
    border-color: var(--brand-primary-hover, var(--brand-primary));
  }
}

/* Spotlight only — OA card owns copy + actions (avoids duplicate EP card). */
.onboarding-tour-host :deep(.el-tour__content),
.onboarding-tour-host :deep(.el-tour-content),
.onboarding-tour-host :deep(.el-tour-buttons) {
  display: none !important;
}
</style>
