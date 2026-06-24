<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export interface PublishGateSummaryItem {
  key: string
  label: string
  ready: boolean
  informational?: boolean
}

const props = defineProps<{
  modelValue: boolean
  templateName: string
  releaseVersion: string
  gateItems: PublishGateSummaryItem[]
  bindingsReady: boolean
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const readyCount = computed(
  () => props.gateItems.filter((item) => !item.informational && item.ready).length,
)
const requiredCount = computed(
  () => props.gateItems.filter((item) => !item.informational).length,
)

function close() {
  visible.value = false
}

function confirm() {
  emit('confirm')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.publishSummary.title')"
    width="520px"
    :close-on-click-modal="false"
    @close="close"
  >
    <p class="publish-summary-intro">
      {{ t('templates.publishSummary.description', { name: templateName }) }}
    </p>

    <dl class="publish-summary-release">
      <dt>{{ t('templates.publishSummary.releaseVersion') }}</dt>
      <dd>{{ releaseVersion }}</dd>
    </dl>

    <section class="publish-summary-section">
      <h3>{{ t('templates.publishSummary.checklistTitle') }}</h3>
      <p class="publish-summary-progress">
        {{ t('templates.publishSummary.checklistProgress', { ready: readyCount, total: requiredCount }) }}
      </p>
      <ul class="publish-summary-list">
        <li v-for="item in gateItems" :key="item.key">
          <span>{{ item.label }}</span>
          <el-tag v-if="item.informational" type="info" size="small">
            {{ t('templates.publishGate.informational') }}
          </el-tag>
          <el-tag v-else :type="item.ready ? 'success' : 'warning'" size="small">
            {{ item.ready ? t('templates.publishGate.ready') : t('templates.publishGate.pending') }}
          </el-tag>
        </li>
      </ul>
    </section>

    <section class="publish-summary-section">
      <h3>{{ t('templates.publishSummary.validationTitle') }}</h3>
      <p>
        {{
          bindingsReady
            ? t('templates.publishSummary.bindingsReady')
            : t('templates.publishSummary.bindingsPending')
        }}
      </p>
      <p class="publish-summary-note">{{ t('templates.publishSummary.testCoverageNote') }}</p>
      <p class="publish-summary-note">{{ t('templates.publishSummary.changeDiffNote') }}</p>
    </section>

    <template #footer>
      <el-button @click="close">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="confirm">
        {{ t('templates.publishSummary.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.publish-summary-intro {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.publish-summary-release {
  display: grid;
  gap: 0.25rem;
  margin: 0 0 1rem;

  dt {
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0;
    font-size: 1.125rem;
    font-weight: 600;
    font-family: monospace;
  }
}

.publish-summary-section {
  margin-bottom: 1rem;

  h3 {
    margin: 0 0 0.5rem;
    font-size: 0.95rem;
  }

  p {
    margin: 0;
  }
}

.publish-summary-progress {
  margin-bottom: 0.5rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.publish-summary-list {
  margin: 0;
  padding-left: 1.25rem;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    margin-bottom: 0.35rem;
  }
}

.publish-summary-note {
  margin-top: 0.35rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
