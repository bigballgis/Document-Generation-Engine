import { computed, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { TOKEN_STORAGE_KEY } from '@/api/http'
import {
  connectAuthorizedEventStream,
  type AuthorizedEventStreamConnection,
} from '@/utils/authorizedEventStream'

type Phase = 'connecting' | 'progress' | 'success' | 'failed'

export interface UsePreviewProgressStreamOptions {
  modelValue: () => boolean
  streamUrl: () => string
  onClose: () => void
  onRetry: () => void
}

export function usePreviewProgressStream(options: UsePreviewProgressStreamOptions) {
  const { t } = useI18n()

  const phase = ref<Phase>('connecting')
  const percent = ref(0)
  const stage = ref('')
  const errorMessage = ref('')
  const docxDownloadUrl = ref('')
  const pdfDownloadUrl = ref('')
  const expiresAt = ref<Date | null>(null)
  const countdown = ref('')

  let eventStream: AuthorizedEventStreamConnection | null = null
  let countdownTimer: ReturnType<typeof setInterval> | null = null
  let streamSessionId = 0

  function stageLabel(s: string): string {
    const key = `templates.previewProgress.stage.${s}`
    return t(key)
  }

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

  function disconnectSse() {
    streamSessionId += 1
    if (eventStream) {
      eventStream.close()
      eventStream = null
    }
  }

  function connectSse() {
    disconnectSse()
    phase.value = 'connecting'
    percent.value = 0
    stage.value = ''
    errorMessage.value = ''
    const sessionId = streamSessionId

    void connectAuthorizedEventStream({
      url: options.streamUrl(),
      token: localStorage.getItem(TOKEN_STORAGE_KEY),
      onMessage: (event) => {
        if (sessionId !== streamSessionId) {
          return
        }

        switch (event.type) {
          case 'progress': {
            const data = JSON.parse(event.data) as {
              stage: string
              percent: number
              message?: string
            }
            phase.value = 'progress'
            stage.value = stageLabel(data.stage)
            percent.value = data.percent
            break
          }
          case 'completed': {
            const data = JSON.parse(event.data) as {
              previewId: string
              docxDownloadUrl: string
              pdfDownloadUrl: string
              expiresAt: string
            }
            disconnectSse()
            phase.value = 'success'
            percent.value = 100
            docxDownloadUrl.value = data.docxDownloadUrl
            pdfDownloadUrl.value = data.pdfDownloadUrl
            startCountdown(new Date(data.expiresAt))
            break
          }
          case 'failed': {
            const data = JSON.parse(event.data) as {
              error: string
              retryable?: boolean
            }
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
        errorMessage.value = t('templates.previewProgress.error.generic')
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
        errorMessage.value = t('templates.previewProgress.error.generic')
      })

    // Timeout after 3 minutes
    setTimeout(() => {
      if (sessionId === streamSessionId && phase.value !== 'success' && phase.value !== 'failed') {
        disconnectSse()
        phase.value = 'failed'
        errorMessage.value = t('templates.previewProgress.error.timeout')
      }
    }, 3 * 60 * 1000)
  }

  watch(
    () => options.modelValue(),
    (visible) => {
      if (visible) {
        connectSse()
      } else {
        disconnectSse()
        stopCountdown()
      }
    },
    { immediate: true },
  )

  onUnmounted(() => {
    disconnectSse()
    stopCountdown()
  })

  function handleClose() {
    options.onClose()
  }

  function handleRetry() {
    options.onClose()
    options.onRetry()
  }

  const progressStatus = computed(() => {
    if (phase.value === 'failed') return 'exception'
    if (phase.value === 'success') return 'success'
    return undefined
  })

  return {
    phase,
    percent,
    stage,
    errorMessage,
    docxDownloadUrl,
    pdfDownloadUrl,
    countdown,
    progressStatus,
    handleClose,
    handleRetry,
  }
}
