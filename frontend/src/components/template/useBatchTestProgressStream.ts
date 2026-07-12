import { computed, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { TOKEN_STORAGE_KEY } from '@/api/http'
import {
  connectAuthorizedEventStream,
  type AuthorizedEventStreamConnection,
} from '@/utils/authorizedEventStream'

type BatchPhase = 'running' | 'completed' | 'failed'

export interface BatchSampleResult {
  sampleIndex: number
  dataSetExternalId: string
  success: boolean
  errorDetail?: string
}

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

  let eventStream: AuthorizedEventStreamConnection | null = null
  let streamSessionId = 0

  function disconnectSse() {
    streamSessionId += 1
    if (eventStream) {
      eventStream.close()
      eventStream = null
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
    const sessionId = streamSessionId

    void connectAuthorizedEventStream({
      url: options.streamUrl(),
      token: localStorage.getItem(TOKEN_STORAGE_KEY),
      onMessage: (event) => {
        if (sessionId !== streamSessionId) {
          return
        }

        switch (event.type) {
          case 'sample_started': {
            const data = JSON.parse(event.data) as {
              sampleIndex: number
              totalSamples: number
              dataSetExternalId: string
            }
            currentSample.value = {
              index: data.sampleIndex,
              total: data.totalSamples,
              externalId: data.dataSetExternalId,
            }
            break
          }
          case 'sample_done': {
            const data = JSON.parse(event.data) as {
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
            break
          }
          case 'batch_completed': {
            const data = JSON.parse(event.data) as {
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
            options.onCompleted()
            break
          }
          case 'batch_failed': {
            const data = JSON.parse(event.data) as { error: string }
            disconnectSse()
            phase.value = 'failed'
            errorMessage.value = data.error
            break
          }
          default:
            break
        }
      },
      onError: () => {
        if (sessionId !== streamSessionId) {
          return
        }
        disconnectSse()
        phase.value = 'failed'
        errorMessage.value = t('templates.batchTest.error.stream')
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
        disconnectSse()
        phase.value = 'failed'
        errorMessage.value = t('templates.batchTest.error.stream')
      })

    setTimeout(() => {
      if (sessionId === streamSessionId && phase.value === 'running') {
        disconnectSse()
        phase.value = 'failed'
        errorMessage.value = t('templates.batchTest.error.timeout')
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
