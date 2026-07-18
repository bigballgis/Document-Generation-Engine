import { computed, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { TOKEN_STORAGE_KEY } from '@/api/http'
import {
  connectAuthorizedEventStream,
  type AuthorizedEventStreamConnection,
} from '@/utils/authorizedEventStream'
import {
  createPreviewProgressCountdown,
  failPreviewProgressStream,
  handlePreviewProgressEvent,
  type PreviewProgressPhase,
} from '@/components/templates/previewProgressStreamHandlers'

export interface UsePreviewProgressStreamOptions {
  modelValue: () => boolean
  streamUrl: () => string
  onClose: () => void
  onRetry: () => void
}

export function usePreviewProgressStream(options: UsePreviewProgressStreamOptions) {
  const { t } = useI18n()

  const phase = ref<PreviewProgressPhase>('connecting')
  const percent = ref(0)
  const stage = ref('')
  const errorMessage = ref('')
  const docxDownloadUrl = ref('')
  const pdfDownloadUrl = ref('')
  const expiresAt = ref<Date | null>(null)
  const countdown = ref('')

  const state = { phase, percent, stage, errorMessage, docxDownloadUrl, pdfDownloadUrl }
  const { startCountdown, stopCountdown } = createPreviewProgressCountdown({
    expiresAt,
    countdown,
    t,
  })

  let eventStream: AuthorizedEventStreamConnection | null = null
  let streamSessionId = 0

  function stageLabel(s: string): string {
    return t(`templates.previewProgress.stage.${s}`)
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
        handlePreviewProgressEvent(event, state, { disconnectSse, stageLabel, startCountdown })
      },
      onError: () => {
        if (sessionId !== streamSessionId) {
          return
        }
        failPreviewProgressStream(
          state,
          disconnectSse,
          t('templates.previewProgress.error.generic'),
        )
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
        failPreviewProgressStream(
          state,
          disconnectSse,
          t('templates.previewProgress.error.generic'),
        )
      })

    setTimeout(() => {
      if (sessionId === streamSessionId && phase.value !== 'success' && phase.value !== 'failed') {
        failPreviewProgressStream(
          state,
          disconnectSse,
          t('templates.previewProgress.error.timeout'),
        )
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
    handleClose: () => options.onClose(),
    handleRetry: () => {
      options.onClose()
      options.onRetry()
    },
  }
}
