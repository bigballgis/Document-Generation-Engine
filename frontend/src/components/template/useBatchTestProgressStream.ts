import { computed, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { TOKEN_STORAGE_KEY } from '@/api/http'
import {
  connectAuthorizedEventStream,
  type AuthorizedEventStreamConnection,
} from '@/utils/authorizedEventStream'
import {
  failBatchTestProgressStream,
  handleBatchTestProgressEvent,
  resetBatchTestProgressState,
  type BatchPhase,
  type BatchSampleResult,
} from '@/components/template/batchTestProgressStreamHandlers'

export type { BatchSampleResult }

export interface UseBatchTestProgressStreamOptions {
  modelValue: () => boolean
  streamUrl: () => string
  dataSetCount: () => number
  onCompleted: () => void
  onClose: () => void
}

export function useBatchTestProgressStream(options: UseBatchTestProgressStreamOptions) {
  const { t } = useI18n()

  const phase = ref<BatchPhase>('running')
  const completedCount = ref(0)
  const samples = ref<BatchSampleResult[]>([])
  const currentSample = ref<{ index: number; total: number; externalId: string } | null>(null)
  const summarySuccessCount = ref(0)
  const summaryFailedCount = ref(0)
  const anchorCoveragePct = ref<number | null>(null)
  const variableCoveragePct = ref<number | null>(null)
  const sampleCoveragePct = ref<number | null>(null)
  const gatePassed = ref<boolean | null>(null)
  const errorMessage = ref('')

  const state = {
    phase,
    completedCount,
    samples,
    currentSample,
    summarySuccessCount,
    summaryFailedCount,
    anchorCoveragePct,
    variableCoveragePct,
    sampleCoveragePct,
    gatePassed,
    errorMessage,
  }

  let eventStream: AuthorizedEventStreamConnection | null = null
  let streamSessionId = 0

  function disconnectSse() {
    streamSessionId += 1
    if (eventStream) {
      eventStream.close()
      eventStream = null
    }
  }

  function connectSse() {
    disconnectSse()
    resetBatchTestProgressState(state)
    const sessionId = streamSessionId

    void connectAuthorizedEventStream({
      url: options.streamUrl(),
      token: localStorage.getItem(TOKEN_STORAGE_KEY),
      onMessage: (event) => {
        if (sessionId !== streamSessionId) {
          return
        }
        handleBatchTestProgressEvent(event, state, {
          disconnectSse,
          onCompleted: options.onCompleted,
        })
      },
      onError: () => {
        if (sessionId !== streamSessionId) {
          return
        }
        failBatchTestProgressStream(state, disconnectSse, t, 'templates.batchTest.error.stream')
      },
    })
      .then((stream) => {
        if (sessionId !== streamSessionId) {
          stream.close()
          return
        }
        eventStream = stream
      })
      .catch(() => {
        if (sessionId !== streamSessionId) {
          return
        }
        failBatchTestProgressStream(state, disconnectSse, t, 'templates.batchTest.error.stream')
      })

    setTimeout(() => {
      if (sessionId === streamSessionId && phase.value === 'running') {
        failBatchTestProgressStream(state, disconnectSse, t, 'templates.batchTest.error.timeout')
      }
    }, 10 * 60 * 1000)
  }

  watch(
    () => options.modelValue(),
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
    options.onClose()
  }

  const progressPercent = computed(() => {
    if (options.dataSetCount() === 0) return 0
    if (phase.value === 'completed') return 100
    return Math.round((completedCount.value / options.dataSetCount()) * 100)
  })

  const progressStatus = computed(() => {
    if (phase.value === 'failed') return 'exception'
    if (phase.value === 'completed') return 'success'
    return undefined
  })

  return {
    phase,
    completedCount,
    samples,
    currentSample,
    summarySuccessCount,
    summaryFailedCount,
    anchorCoveragePct,
    variableCoveragePct,
    sampleCoveragePct,
    gatePassed,
    errorMessage,
    progressPercent,
    progressStatus,
    handleClose,
  }
}
