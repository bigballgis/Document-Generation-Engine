import type { Ref } from 'vue'
import type { ComposerTranslation } from 'vue-i18n'

export type BatchPhase = 'running' | 'completed' | 'failed'

export interface BatchSampleResult {
  sampleIndex: number
  dataSetExternalId: string
  success: boolean
  errorDetail?: string
}

export interface BatchTestProgressStreamState {
  phase: Ref<BatchPhase>
  completedCount: Ref<number>
  samples: Ref<BatchSampleResult[]>
  currentSample: Ref<{ index: number; total: number; externalId: string } | null>
  summarySuccessCount: Ref<number>
  summaryFailedCount: Ref<number>
  anchorCoveragePct: Ref<number | null>
  variableCoveragePct: Ref<number | null>
  sampleCoveragePct: Ref<number | null>
  gatePassed: Ref<boolean | null>
  errorMessage: Ref<string>
}

export function handleBatchTestProgressEvent(
  event: { type: string; data: string },
  state: BatchTestProgressStreamState,
  controls: {
    disconnectSse: () => void
    onCompleted: () => void
  },
) {
  switch (event.type) {
    case 'sample_started': {
      const data = JSON.parse(event.data) as {
        sampleIndex: number
        totalSamples: number
        dataSetExternalId: string
      }
      state.currentSample.value = {
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
      state.completedCount.value = data.sampleIndex
      state.samples.value = [
        ...state.samples.value,
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
      controls.disconnectSse()
      state.phase.value = 'completed'
      state.summarySuccessCount.value = data.successCount
      state.summaryFailedCount.value = data.failedCount
      state.anchorCoveragePct.value = data.anchorCoveragePct
      state.variableCoveragePct.value = data.variableCoveragePct
      state.sampleCoveragePct.value = data.sampleCoveragePct
      state.gatePassed.value = data.gatePassed
      controls.onCompleted()
      break
    }
    case 'batch_failed': {
      const data = JSON.parse(event.data) as { error: string }
      controls.disconnectSse()
      state.phase.value = 'failed'
      state.errorMessage.value = data.error
      break
    }
    default:
      break
  }
}

export function resetBatchTestProgressState(state: BatchTestProgressStreamState) {
  state.phase.value = 'running'
  state.completedCount.value = 0
  state.samples.value = []
  state.currentSample.value = null
  state.summarySuccessCount.value = 0
  state.summaryFailedCount.value = 0
  state.anchorCoveragePct.value = null
  state.variableCoveragePct.value = null
  state.sampleCoveragePct.value = null
  state.gatePassed.value = null
  state.errorMessage.value = ''
}

export function failBatchTestProgressStream(
  state: BatchTestProgressStreamState,
  disconnectSse: () => void,
  t: ComposerTranslation,
  messageKey: string,
) {
  disconnectSse()
  state.phase.value = 'failed'
  state.errorMessage.value = t(messageKey)
}
