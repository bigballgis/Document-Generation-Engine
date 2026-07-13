<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { usePreviewProgressStream } from '@/components/template/usePreviewProgressStream'

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

const {
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
} = usePreviewProgressStream({
  modelValue: () => props.modelValue,
  streamUrl: () => props.streamUrl,
  onClose: () => emit('update:modelValue', false),
  onRetry: () => emit('retry'),
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
