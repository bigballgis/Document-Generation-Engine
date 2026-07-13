import { computed, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  emptyGuidanceKeyFromStepLabel,
  stepGuidanceKeyFromLabel,
  type RoleJourneyStep,
} from '@/constants/roleJourneyDefinitions'

export type StepVisualStatus = 'completed' | 'current' | 'upcoming'

export function useRoleJourneyTimeline(options: {
  steps: Ref<RoleJourneyStep[]>
  currentStepIndex: Ref<number | null>
  guidanceKey: Ref<string | undefined>
}) {
  const { t } = useI18n()
  const stepRefs = ref<(HTMLElement | null)[]>([])

  const effectiveCurrentIndex = computed<number | null>(() => {
    if (options.steps.value.length === 0) {
      return null
    }
    if (options.currentStepIndex.value === null) {
      return null
    }
    if (
      options.currentStepIndex.value < 0 ||
      options.currentStepIndex.value >= options.steps.value.length
    ) {
      if (import.meta.env.DEV) {
        console.warn(
          `[RoleJourneyTimeline] currentStepIndex ${options.currentStepIndex.value} out of range; clamped.`,
        )
      }
      return Math.min(
        Math.max(options.currentStepIndex.value, 0),
        options.steps.value.length - 1,
      )
    }
    return options.currentStepIndex.value
  })

  function stepStatus(index: number): StepVisualStatus {
    const current = effectiveCurrentIndex.value
    if (current === null) {
      return 'upcoming'
    }
    if (index < current) {
      return 'completed'
    }
    if (index === current) {
      return 'current'
    }
    return 'upcoming'
  }

  const resolvedGuidanceKey = computed(() => {
    if (options.guidanceKey.value) {
      return options.guidanceKey.value
    }
    const current = effectiveCurrentIndex.value
    if (current !== null) {
      const currentStep = options.steps.value[current]
      if (currentStep) {
        return stepGuidanceKeyFromLabel(currentStep.labelKey)
      }
    }
    const firstStep = options.steps.value[0]
    if (firstStep) {
      return emptyGuidanceKeyFromStepLabel(firstStep.labelKey)
    }
    return ''
  })

  function focusStep(index: number) {
    stepRefs.value[index]?.focus()
  }

  function onStepKeydown(event: KeyboardEvent, index: number) {
    let targetIndex: number | null = null
    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      targetIndex = Math.min(index + 1, options.steps.value.length - 1)
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      targetIndex = Math.max(index - 1, 0)
    } else if (event.key === 'Home') {
      targetIndex = 0
    } else if (event.key === 'End') {
      targetIndex = options.steps.value.length - 1
    }

    if (targetIndex === null || targetIndex === index) {
      return
    }

    event.preventDefault()
    focusStep(targetIndex)
  }

  return {
    t,
    stepRefs,
    stepStatus,
    resolvedGuidanceKey,
    onStepKeydown,
  }
}
