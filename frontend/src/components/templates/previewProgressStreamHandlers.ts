import type { Ref } from 'vue'
import type { ComposerTranslation } from 'vue-i18n'
import type { AuthorizedEventStreamConnection } from '@/utils/authorizedEventStream'

export type PreviewProgressPhase = 'connecting' | 'progress' | 'success' | 'failed'

export interface PreviewProgressStreamState {
  phase: Ref<PreviewProgressPhase>
  percent: Ref<number>
  stage: Ref<string>
  errorMessage: Ref<string>
  docxDownloadUrl: Ref<string>
  pdfDownloadUrl: Ref<string>
}

export function createPreviewProgressCountdown(deps: {
  expiresAt: Ref<Date | null>
  countdown: Ref<string>
  t: ComposerTranslation
}) {
  const { expiresAt, countdown, t } = deps
  let countdownTimer: ReturnType<typeof setInterval> | null = null

  function updateCountdown() {
    if (!expiresAt.value) {
      countdown.value = ''
      return
    }
    const diffMs = expiresAt.value.getTime() - Date.now()
    if (diffMs <= 0) {
      countdown.value = t('templates.previewProgress.expired')
      return
    }
    const totalMinutes = Math.floor(diffMs / 60000)
    if (totalMinutes < 1) {
      countdown.value = t('templates.previewProgress.expiresLessThanMinute')
      return
    }
    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60
    countdown.value = t('templates.previewProgress.expiresIn', { hours, minutes })
  }

  function startCountdown(expiry: Date) {
    expiresAt.value = expiry
    updateCountdown()
    countdownTimer = setInterval(updateCountdown, 30000)
  }

  function stopCountdown() {
    if (countdownTimer !== null) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }

  return { startCountdown, stopCountdown, updateCountdown }
}

export function handlePreviewProgressEvent(
  event: { type: string; data: string },
  state: PreviewProgressStreamState,
  controls: {
    disconnectSse: () => void
    stageLabel: (stage: string) => string
    startCountdown: (expiry: Date) => void
  },
) {
  switch (event.type) {
    case 'progress': {
      const data = JSON.parse(event.data) as {
        stage: string
        percent: number
        message?: string
      }
      state.phase.value = 'progress'
      state.stage.value = controls.stageLabel(data.stage)
      state.percent.value = data.percent
      break
    }
    case 'completed': {
      const data = JSON.parse(event.data) as {
        previewId: string
        docxDownloadUrl: string
        pdfDownloadUrl: string
        expiresAt: string
      }
      controls.disconnectSse()
      state.phase.value = 'success'
      state.percent.value = 100
      state.docxDownloadUrl.value = data.docxDownloadUrl
      state.pdfDownloadUrl.value = data.pdfDownloadUrl
      controls.startCountdown(new Date(data.expiresAt))
      break
    }
    case 'failed': {
      const data = JSON.parse(event.data) as {
        error: string
        retryable?: boolean
      }
      controls.disconnectSse()
      state.phase.value = 'failed'
      state.errorMessage.value = data.error
      break
    }
    default:
      break
  }
}

export function failPreviewProgressStream(
  state: PreviewProgressStreamState,
  disconnectSse: () => void,
  message: string,
  eventStream?: AuthorizedEventStreamConnection | null,
) {
  void eventStream
  disconnectSse()
  state.phase.value = 'failed'
  state.errorMessage.value = message
}
