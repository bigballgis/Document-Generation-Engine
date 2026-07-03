<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { TOKEN_STORAGE_KEY } from '@/api/http'
import {
  connectAuthorizedEventStream,
  type AuthorizedEventStreamConnection,
} from '@/utils/authorizedEventStream'

const props = defineProps<{
  modelValue: boolean
  templateId: string
  previewId: string
  streamUrl: string
  dataSetName?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  retry: []
}>()

const { t } = useI18n()

type Phase = 'connecting' | 'progress' | 'success' | 'failed'

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
    url: props.streamUrl,
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
  () => props.modelValue,
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
  emit('update:modelValue', false)
}

function handleRetry() {
  emit('update:modelValue', false)
  emit('retry')
}

const progressStatus = computed(() => {
  if (phase.value === 'failed') return 'exception'
  if (phase.value === 'success') return 'success'
  return undefined
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('templates.previewProgress.title')"
    width="480px"
    :close-on-click-modal="false"
    :close-on-press-escape="phase === 'success' || phase === 'failed'"
    @update:model-value="handleClose"
  >
    <div class="preview-progress">
      <div v-if="dataSetName" class="preview-progress__dataset">
        <span class="preview-progress__dataset-label">{{ dataSetName }}</span>
      </div>

      <el-progress
        :percentage="percent"
        :status="progressStatus"
        :striped="phase === 'progress' || phase === 'connecting'"
        :striped-flow="phase === 'progress' || phase === 'connecting'"
        class="preview-progress__bar"
      />

      <p v-if="phase === 'connecting'" class="preview-progress__stage">
        {{ t('templates.previewProgress.stage.QUEUED') }}
      </p>

      <p v-else-if="phase === 'progress' && stage" class="preview-progress__stage">
        {{ stage }}
      </p>

      <div v-else-if="phase === 'success'" class="preview-progress__success">
        <div class="preview-progress__downloads">
          <el-button type="primary" :href="docxDownloadUrl" tag="a" target="_blank">
            {{ t('templates.previewProgress.downloadDocx') }}
          </el-button>
          <el-button type="primary" :href="pdfDownloadUrl" tag="a" target="_blank">
            {{ t('templates.previewProgress.downloadPdf') }}
          </el-button>
        </div>
        <p v-if="countdown" class="preview-progress__expiry">{{ countdown }}</p>
      </div>

      <div v-else-if="phase === 'failed'" class="preview-progress__error">
        <el-alert type="error" :title="errorMessage" :closable="false" show-icon />
        <div class="preview-progress__retry-row">
          <el-button data-testid="retry-btn" @click="handleRetry">
            {{ t('templates.previewProgress.retryButton') }}
          </el-button>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">
        {{ t('templates.previewProgress.closeButton') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.preview-progress {
  display: flex;
  flex-direction: column;
  gap: 1rem;

  &__dataset {
    font-size: 0.875rem;
    color: var(--text-muted);
  }

  &__bar {
    width: 100%;
  }

  &__stage {
    margin: 0;
    font-size: 0.875rem;
    color: var(--text-muted);
    text-align: center;
  }

  &__downloads {
    display: flex;
    gap: 0.75rem;
    justify-content: center;
  }

  &__expiry {
    margin: 0.5rem 0 0;
    font-size: 0.8125rem;
    color: var(--text-muted);
    text-align: center;
  }

  &__error {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  &__retry-row {
    display: flex;
    justify-content: center;
  }
}
</style>
