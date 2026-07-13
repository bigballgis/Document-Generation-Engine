<script setup lang="ts">
import { toRef, useId } from 'vue'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
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
</script>

<template>
  <section v-if="steps.length > 0" class="role-journey-timeline" data-journey-timeline>
    <div v-if="titleKey" class="role-journey-timeline__title-row">
      <h2 class="role-journey-timeline__title" data-journey-title>
        {{ t(titleKey) }}
      </h2>
      <ContextHelpTrigger
        v-if="!inlineHelp && resolvedGuidanceKey"
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
        v-if="inlineHelp && resolvedGuidanceKey"
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

<style scoped lang="scss">
.role-journey-timeline {
  margin-bottom: 1.25rem;
  padding: 1rem 1.25rem;
  background: var(--surface-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
}

.role-journey-timeline__title-row {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-bottom: 0.75rem;
}

.role-journey-timeline__title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
}

.role-journey-timeline__steps {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem 1rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.role-journey-timeline__step-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  flex: 1 1 140px;
  min-width: 0;
}

.role-journey-timeline__step {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  width: 100%;
  min-width: 0;
  padding: 0.5rem 0.75rem;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  text-align: left;
  cursor: default;
  font: inherit;
  color: inherit;
  outline: none;

  &:focus-visible {
    box-shadow: 0 0 0 2px var(--surface-elevated), 0 0 0 4px var(--brand-primary);
  }

  &.is-completed {
    .role-journey-timeline__marker {
      background: var(--brand-accent-soft);
      border-color: var(--brand-primary);
      color: var(--brand-primary);
    }

    .role-journey-timeline__label {
      color: var(--text-primary);
    }
  }

  &.is-current {
    background: var(--brand-accent-soft);

    .role-journey-timeline__marker {
      background: var(--brand-primary);
      border-color: var(--brand-primary);
      color: var(--on-primary);
    }

    .role-journey-timeline__label {
      color: var(--text-primary);
      font-weight: 600;
    }
  }

  &.is-upcoming {
    .role-journey-timeline__marker {
      background: var(--surface-elevated);
      border-color: var(--border-color);
    }

    .role-journey-timeline__label {
      color: var(--text-muted);
    }
  }
}

.role-journey-timeline__marker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
  border: 2px solid var(--border-color);
  border-radius: 50%;
  font-size: 0.75rem;
  font-weight: 700;
}

.role-journey-timeline__dot {
  display: block;
  width: 0.4rem;
  height: 0.4rem;
  border-radius: 50%;
  background: var(--border-color);
}

.role-journey-timeline__content {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.role-journey-timeline__label {
  font-size: 0.9rem;
  line-height: 1.35;
  word-break: break-word;
}

.role-journey-timeline__description {
  font-size: 0.8rem;
  color: var(--text-muted);
  line-height: 1.3;
}

.role-journey-timeline__guidance {
  margin: 1rem 0 0;
  color: var(--text-muted);
  font-size: 0.95rem;
  line-height: 1.45;
}
</style>
