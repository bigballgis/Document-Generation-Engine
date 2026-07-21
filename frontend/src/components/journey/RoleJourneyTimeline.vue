<script setup lang="ts">
import { computed, toRef, useId } from 'vue'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import { type RoleJourneyStep } from '@/constants/roleJourneyDefinitions'
import { useRoleJourneyTimeline } from '@/components/journey/useRoleJourneyTimeline'

const props = withDefaults(
  defineProps<{
    steps: RoleJourneyStep[]
    currentStepIndex: number | null
    guidanceKey?: string
    ariaLabelKey?: string
    titleKey?: string
    /** When false, step descriptions and guidance paragraph are hidden; use the ? help trigger instead. */
    inlineHelp?: boolean
  }>(),
  {
    ariaLabelKey: 'journey.timeline.ariaLabel',
    inlineHelp: false,
  },
)

const guidanceId = useId()
const { t, stepRefs, stepStatus, resolvedGuidanceKey, onStepKeydown } = useRoleJourneyTimeline({
  steps: toRef(props, 'steps'),
  currentStepIndex: toRef(props, 'currentStepIndex'),
  guidanceKey: toRef(props, 'guidanceKey'),
})

/** Empty work set (no current step) must surface guidance visibly — not only behind ?. */
const showInlineGuidance = computed(
  () =>
    Boolean(resolvedGuidanceKey.value) &&
    (props.inlineHelp || props.currentStepIndex === null),
)

const showContextHelp = computed(
  () =>
    Boolean(props.titleKey && resolvedGuidanceKey.value) &&
    !props.inlineHelp &&
    props.currentStepIndex !== null,
)
</script>

<template>
  <EmptyStatePanel
    v-if="steps.length === 0"
    data-testid="journey-timeline-honest-empty"
    :title-key="titleKey || 'journey.timeline.emptyTitle'"
    description-key="journey.timeline.empty.guidance"
  />

  <section v-else class="role-journey-timeline" data-journey-timeline>
    <div v-if="titleKey" class="role-journey-timeline__title-row">
      <h2 class="role-journey-timeline__title" data-journey-title>
        {{ t(titleKey) }}
      </h2>
      <ContextHelpTrigger
        v-if="showContextHelp"
        :title="t(titleKey)"
        :content="t(resolvedGuidanceKey)"
      />
    </div>

    <nav
      class="role-journey-timeline__nav"
      role="navigation"
      :aria-label="t(ariaLabelKey)"
      :aria-describedby="guidanceId"
    >
      <ol class="role-journey-timeline__steps">
        <li
          v-for="(step, index) in steps"
          :key="step.id"
          class="role-journey-timeline__step-item"
        >
          <button
            :ref="(el) => { stepRefs[index] = el as HTMLElement | null }"
            type="button"
            class="role-journey-timeline__step"
            :class="{
              'is-completed': stepStatus(index) === 'completed',
              'is-current': stepStatus(index) === 'current',
              'is-upcoming': stepStatus(index) === 'upcoming',
            }"
            data-journey-step
            :data-tour-anchor="step.id"
            :aria-current="stepStatus(index) === 'current' ? 'step' : undefined"
            @keydown="onStepKeydown($event, index)"
          >
            <span class="role-journey-timeline__marker" aria-hidden="true">
              <span v-if="stepStatus(index) === 'completed'" class="role-journey-timeline__check">✓</span>
              <span v-else class="role-journey-timeline__dot" />
            </span>
            <span class="role-journey-timeline__content">
              <span class="role-journey-timeline__label">{{ t(step.labelKey) }}</span>
              <span
                v-if="inlineHelp && step.descriptionKey"
                class="role-journey-timeline__description"
              >
                {{ t(step.descriptionKey) }}
              </span>
            </span>
          </button>
          <slot name="step-extra" :step="step" :index="index" :status="stepStatus(index)" />
        </li>
      </ol>
    </nav>

    <slot name="guidance">
      <p
        v-if="showInlineGuidance"
        :id="guidanceId"
        class="role-journey-timeline__guidance"
        data-journey-guidance
      >
        {{ t(resolvedGuidanceKey) }}
      </p>
    </slot>

    <slot name="after" />
  </section>
</template>

<style scoped lang="scss" src="./RoleJourneyTimeline.scss"></style>
