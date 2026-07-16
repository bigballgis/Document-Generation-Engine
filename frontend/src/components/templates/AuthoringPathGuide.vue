<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  AUTHORING_PATH_GUIDE_LABEL_KEYS,
  AUTHORING_PATH_GUIDE_STEPS,
  nextAuthoringPathGuideStep,
  resolveAuthoringPathGuideNavigateQuery,
  type AuthoringPathGuideNavigateQuery,
  type AuthoringPathGuideStep,
} from '@/utils/templateAuthoringPathGuide'

const props = defineProps<{
  currentStep: AuthoringPathGuideStep
}>()

const emit = defineEmits<{
  navigate: [query: AuthoringPathGuideNavigateQuery]
  dismiss: []
}>()

const { t } = useI18n()

const steps = computed(() =>
  AUTHORING_PATH_GUIDE_STEPS.map((id, index) => {
    const currentIndex = AUTHORING_PATH_GUIDE_STEPS.indexOf(props.currentStep)
    let status: 'completed' | 'current' | 'upcoming' = 'upcoming'
    if (index < currentIndex) {
      status = 'completed'
    } else if (index === currentIndex) {
      status = 'current'
    }
    return {
      id,
      labelKey: AUTHORING_PATH_GUIDE_LABEL_KEYS[id],
      status,
      navigateQuery: resolveAuthoringPathGuideNavigateQuery(id),
    }
  }),
)

const nextStep = computed(() => nextAuthoringPathGuideStep(props.currentStep))

function onStepActivate(query: AuthoringPathGuideNavigateQuery) {
  emit('navigate', query)
}

function onNext() {
  if (!nextStep.value) {
    emit('dismiss')
    return
  }
  emit('navigate', resolveAuthoringPathGuideNavigateQuery(nextStep.value))
}
</script>

<template>
  <section
    class="authoring-path-guide"
    data-testid="authoring-path-guide"
    data-ce-u16-authoring-path
  >
    <div class="authoring-path-guide__header">
      <div class="authoring-path-guide__titles">
        <h2 class="authoring-path-guide__title">{{ t('templates.authoringPathGuide.title') }}</h2>
        <p class="authoring-path-guide__subtitle">{{ t('templates.authoringPathGuide.subtitle') }}</p>
      </div>
      <div class="authoring-path-guide__actions">
        <button
          v-if="nextStep"
          type="button"
          class="authoring-path-guide__action authoring-path-guide__action--primary"
          data-testid="authoring-path-guide-next"
          @click="onNext"
        >
          {{ t('templates.authoringPathGuide.next') }}
        </button>
        <button
          type="button"
          class="authoring-path-guide__action"
          data-testid="authoring-path-guide-skip"
          @click="emit('dismiss')"
        >
          {{ t('templates.authoringPathGuide.skip') }}
        </button>
        <button
          type="button"
          class="authoring-path-guide__action"
          data-testid="authoring-path-guide-dismiss"
          @click="emit('dismiss')"
        >
          {{ t('templates.authoringPathGuide.dismiss') }}
        </button>
      </div>
    </div>

    <nav
      class="authoring-path-guide__nav"
      role="navigation"
      :aria-label="t('templates.authoringPathGuide.ariaLabel')"
    >
      <ol class="authoring-path-guide__steps">
        <li
          v-for="step in steps"
          :key="step.id"
          class="authoring-path-guide__step-item"
        >
          <button
            type="button"
            class="authoring-path-guide__step"
            :class="{
              'is-completed': step.status === 'completed',
              'is-current': step.status === 'current',
              'is-upcoming': step.status === 'upcoming',
            }"
            :data-step-id="step.id"
            :data-testid="`authoring-path-guide-step-${step.id}`"
            :aria-current="step.status === 'current' ? 'step' : undefined"
            @click="onStepActivate(step.navigateQuery)"
          >
            <span class="authoring-path-guide__marker" aria-hidden="true">
              <span v-if="step.status === 'completed'" class="authoring-path-guide__check">✓</span>
              <span v-else class="authoring-path-guide__dot" />
            </span>
            <span class="authoring-path-guide__label">{{ t(step.labelKey) }}</span>
          </button>
        </li>
      </ol>
    </nav>
  </section>
</template>

<style scoped lang="scss" src="./AuthoringPathGuide.scss"></style>
