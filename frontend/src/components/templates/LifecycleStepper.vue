<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TemplateLifecycleStatus } from '@/types/template'
import {
  LIFECYCLE_STEPPER_LABEL_KEYS,
  LIFECYCLE_STEPPER_STEP_IDS,
  resolveLifecycleStepperModel,
  resolveLifecycleStepperStepStatus,
  resolveLifecycleStepperWorkspaceQuery,
  type LifecycleApprovalSubState,
} from '@/utils/templateLifecycleStepper'
import type { TemplateJourneyWorkspaceQuery } from '@/utils/templateJourneyWorkspaceLink'

const props = defineProps<{
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: LifecycleApprovalSubState
}>()

const emit = defineEmits<{
  navigate: [query: TemplateJourneyWorkspaceQuery]
}>()

const { t } = useI18n()

const model = computed(() =>
  resolveLifecycleStepperModel(props.lifecycleStatus, props.approvalSubState),
)

const steps = computed(() =>
  LIFECYCLE_STEPPER_STEP_IDS.map((id, index) => ({
    id,
    labelKey: LIFECYCLE_STEPPER_LABEL_KEYS[id],
    status: resolveLifecycleStepperStepStatus(index, model.value),
    workspaceQuery: resolveLifecycleStepperWorkspaceQuery(id),
  })),
)

function onStepActivate(workspaceQuery: TemplateJourneyWorkspaceQuery | null) {
  if (!workspaceQuery) {
    return
  }
  emit('navigate', workspaceQuery)
}
</script>

<template>
  <section
    class="lifecycle-stepper"
    data-testid="lifecycle-stepper"
    data-ce-u15-stepper
    :data-terminal="model.terminal ? 'true' : 'false'"
  >
    <div class="lifecycle-stepper__header">
      <h2 class="lifecycle-stepper__title">{{ t('templates.lifecycleStepper.title') }}</h2>
      <p v-if="model.terminal" class="lifecycle-stepper__terminal-note" data-testid="lifecycle-stepper-terminal">
        {{ t(`templates.status.${lifecycleStatus}`) }}
      </p>
    </div>

    <nav
      class="lifecycle-stepper__nav"
      role="navigation"
      :aria-label="t('templates.lifecycleStepper.ariaLabel')"
    >
      <ol class="lifecycle-stepper__steps">
        <li
          v-for="step in steps"
          :key="step.id"
          class="lifecycle-stepper__step-item"
        >
          <button
            type="button"
            class="lifecycle-stepper__step"
            :class="{
              'is-completed': step.status === 'completed',
              'is-current': step.status === 'current',
              'is-upcoming': step.status === 'upcoming',
              'is-inactive': step.status === 'inactive',
              'is-navigable': Boolean(step.workspaceQuery),
            }"
            :data-step-id="step.id"
            :data-testid="`lifecycle-stepper-step-${step.id}`"
            :aria-current="step.status === 'current' ? 'step' : undefined"
            :disabled="!step.workspaceQuery"
            @click="onStepActivate(step.workspaceQuery)"
          >
            <span class="lifecycle-stepper__marker" aria-hidden="true">
              <span v-if="step.status === 'completed'" class="lifecycle-stepper__check">✓</span>
              <span v-else class="lifecycle-stepper__dot" />
            </span>
            <span class="lifecycle-stepper__label">{{ t(step.labelKey) }}</span>
          </button>
        </li>
      </ol>
    </nav>
  </section>
</template>

<style scoped lang="scss" src="./LifecycleStepper.scss"></style>
