<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { TOKEN_STORAGE_KEY } from '@/api/http'

const props = defineProps<{
  modelValue: boolean
  templateId: string
  runId: string
  streamUrl: string
  dataSetCount: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  completed: []
}>()

const { t } = useI18n()

type BatchPhase = 'running' | 'completed' | 'failed'

interface SampleResult {
  sampleIndex: number
  dataSetExternalId: string
  success: boolean
  errorDetail?: string
}

const phase = ref<BatchPhase>('running')
const completedCount = ref(0)
const samples = ref<SampleResult[]>([])
const currentSample = ref<{ index: number; total: number; externalId: string } | null>(null)
const summarySuccessCount = ref(0)
const summaryFailedCount = ref(0)
const anchorCoveragePct = ref<number | null>(null)
const variableCoveragePct = ref<number | null>(null)
const sampleCoveragePct = ref<number | null>(null)
const gatePassed = ref<boolean | null>(null)
const errorMessage = ref('')

let eventSource: EventSource | null = null

function buildSseUrl(url: string): string {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY) ?? ''
  const separator = url.includes('?') ? '&' : '?'
  return `${url}${separator}token=${encodeURIComponent(token)}`
}

function disconnectSse() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

function resetState() {
  phase.value = 'running'
  completedCount.value = 0
  samples.value = []
  currentSample.value = null
  summarySuccessCount.value = 0
  summaryFailedCount.value = 0
  anchorCoveragePct.value = null
  variableCoveragePct.value = null
  sampleCoveragePct.value = null
  gatePassed.value = null
  errorMessage.value = ''
}

function connectSse() {
  disconnectSse()
  resetState()

  const url = buildSseUrl(props.streamUrl)
  eventSource = new EventSource(url)

  eventSource.addEventListener('sample_started', (evt: MessageEvent) => {
    const data = JSON.parse(evt.data as string) as {
      sampleIndex: number
      totalSamples: number
      dataSetExternalId: string
    }
    currentSample.value = {
      index: data.sampleIndex,
      total: data.totalSamples,
      externalId: data.dataSetExternalId,
    }
  })

  eventSource.addEventListener('sample_done', (evt: MessageEvent) => {
    const data = JSON.parse(evt.data as string) as {
      sampleIndex: number
      success: boolean
      dataSetExternalId: string
      errorDetail?: string
    }
    completedCount.value = data.sampleIndex
    samples.value = [
      ...samples.value,
      {
        sampleIndex: data.sampleIndex,
        dataSetExternalId: data.dataSetExternalId,
        success: data.success,
        errorDetail: data.errorDetail,
      },
    ]
  })

  eventSource.addEventListener('batch_completed', (evt: MessageEvent) => {
    const data = JSON.parse(evt.data as string) as {
      runId: string
      successCount: number
      failedCount: number
      anchorCoveragePct: number
      variableCoveragePct: number
      sampleCoveragePct: number
      gatePassed: boolean
    }
    disconnectSse()
    phase.value = 'completed'
    summarySuccessCount.value = data.successCount
    summaryFailedCount.value = data.failedCount
    anchorCoveragePct.value = data.anchorCoveragePct
    variableCoveragePct.value = data.variableCoveragePct
    sampleCoveragePct.value = data.sampleCoveragePct
    gatePassed.value = data.gatePassed
    emit('completed')
  })

  eventSource.addEventListener('batch_failed', (evt: MessageEvent) => {
    const data = JSON.parse(evt.data as string) as { error: string }
    disconnectSse()
    phase.value = 'failed'
    errorMessage.value = data.error
  })

  setTimeout(() => {
    if (eventSource && phase.value === 'running') {
      disconnectSse()
      phase.value = 'failed'
      errorMessage.value = t('templates.batchTest.error.timeout')
    }
  }, 10 * 60 * 1000)
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      connectSse()
    } else {
      disconnectSse()
    }
  },
  { immediate: true },
)

onUnmounted(() => {
  disconnectSse()
})

function handleClose() {
  disconnectSse()
  emit('update:modelValue', false)
}

const progressPercent = computed(() => {
  if (props.dataSetCount === 0) return 0
  if (phase.value === 'completed') return 100
  return Math.round((completedCount.value / props.dataSetCount) * 100)
})

const progressStatus = computed(() => {
  if (phase.value === 'failed') return 'exception'
  if (phase.value === 'completed') return 'success'
  return undefined
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('templates.batchTest.progressTitle')"
    width="560px"
    :close-on-click-modal="false"
    :close-on-press-escape="phase !== 'running'"
    @update:model-value="handleClose"
  >
    <div class="batch-progress">
      <div class="batch-progress__counter">
        {{ t('templates.batchTest.completedCount', { done: completedCount, total: dataSetCount }) }}
      </div>

      <el-progress
        :percentage="progressPercent"
        :status="progressStatus"
        :striped="phase === 'running'"
        :striped-flow="phase === 'running'"
        class="batch-progress__bar"
      />

      <p
        v-if="phase === 'running' && currentSample"
        class="batch-progress__current"
      >
        {{
          t('templates.batchTest.currentSample', {
            externalId: currentSample.externalId,
            index: currentSample.index,
            total: currentSample.total,
          })
        }}
      </p>

      <div v-if="samples.length" class="batch-progress__results">
        <div
          v-for="s in samples"
          :key="s.sampleIndex"
          class="batch-progress__result-row"
          :class="s.success ? 'is-success' : 'is-failed'"
        >
          <el-tag :type="s.success ? 'success' : 'danger'" size="small">
            {{ s.success ? t('templates.batchTest.resultSuccess') : t('templates.batchTest.resultFailed') }}
          </el-tag>
          <span class="batch-progress__result-name">{{ s.dataSetExternalId }}</span>
          <span v-if="!s.success && s.errorDetail" class="batch-progress__result-error">
            {{ s.errorDetail }}
          </span>
        </div>
      </div>

      <div v-if="phase === 'completed'" class="batch-progress__summary">
        <el-divider />
        <h4 class="batch-progress__summary-title">{{ t('templates.batchTest.summaryTitle') }}</h4>
        <div class="batch-progress__summary-counts">
          <span class="is-success">{{ t('templates.batchTest.summarySuccess', { count: summarySuccessCount }) }}</span>
          <span v-if="summaryFailedCount > 0" class="is-failed">
            {{ t('templates.batchTest.summaryFailed', { count: summaryFailedCount }) }}
          </span>
        </div>
        <div class="batch-progress__summary-gate">
          <el-tag :type="gatePassed ? 'success' : 'warning'" size="small">
            {{ gatePassed ? t('templates.batchTest.readinessPassed') : t('templates.batchTest.readinessFailed') }}
          </el-tag>
        </div>
        <div v-if="anchorCoveragePct !== null" class="batch-progress__coverage">
          <span>{{ t('templates.batchTest.placeholderBindingCoverage', { pct: anchorCoveragePct }) }}</span>
          <span>{{ t('templates.batchTest.variableCoverage', { pct: variableCoveragePct }) }}</span>
          <span>{{ t('templates.batchTest.sampleCoverage', { pct: sampleCoveragePct }) }}</span>
        </div>
      </div>

      <el-alert
        v-if="phase === 'failed'"
        type="error"
        :title="errorMessage"
        :closable="false"
        show-icon
        class="batch-progress__error"
      />
    </div>

    <template #footer>
      <el-button @click="handleClose">
        {{ t('templates.previewProgress.closeButton') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.batch-progress {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;

  &__counter {
    font-size: 0.875rem;
    color: var(--text-muted);
  }

  &__bar {
    width: 100%;
  }

  &__current {
    margin: 0;
    font-size: 0.8125rem;
    color: var(--text-muted);
  }

  &__results {
    max-height: 220px;
    overflow-y: auto;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    padding: 0.5rem;
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
  }

  &__result-row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.8125rem;
  }

  &__result-name {
    font-weight: 500;
  }

  &__result-error {
    color: var(--el-color-danger);
    font-size: 0.75rem;
  }

  &__summary {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  &__summary-title {
    margin: 0;
    font-size: 0.9375rem;
    font-weight: 600;
  }

  &__summary-counts {
    display: flex;
    gap: 1rem;

    .is-success {
      color: var(--el-color-success);
    }

    .is-failed {
      color: var(--el-color-danger);
    }
  }

  &__coverage {
    display: flex;
    gap: 1rem;
    font-size: 0.8125rem;
    color: var(--text-muted);
  }

  &__error {
    margin-top: 0.5rem;
  }
}
</style>
