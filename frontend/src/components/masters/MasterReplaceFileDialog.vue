<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  MASTER_DOCX_MAX_UPLOAD_MB,
  validateMasterDocxUploadFile,
} from '@/utils/validateMasterDocxUpload'

const props = defineProps<{
  modelValue: boolean
  loading?: boolean
  uploadProgress?: number | null
  serverErrorKey?: string | null
  currentFilename?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [file: File]
  'clear-server-error': []
}>()

const { t, te } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const selectedFile = ref<File | null>(null)
const fileList = ref<{ name: string }[]>([])
const fileErrorKey = ref<string | null>(null)

const inlineErrorKey = computed(() => fileErrorKey.value ?? props.serverErrorKey ?? null)
const inlineErrorText = computed(() => {
  const key = inlineErrorKey.value
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('masters.error.replaceFile')
})

const progressPercent = computed(() => {
  if (props.uploadProgress == null) {
    return 0
  }
  return Math.min(100, Math.max(0, props.uploadProgress))
})

const progressIndeterminate = computed(() => props.loading === true && props.uploadProgress == null)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      return
    }
    selectedFile.value = null
    fileList.value = []
    fileErrorKey.value = null
  },
)

function onFileChange(uploadFile: { raw?: File }) {
  const file = uploadFile.raw ?? null
  fileErrorKey.value = null
  emit('clear-server-error')
  if (file) {
    const validation = validateMasterDocxUploadFile(file)
    if (!validation.ok) {
      fileErrorKey.value = validation.messageKey
      selectedFile.value = null
      fileList.value = []
      return
    }
  }
  selectedFile.value = file
  fileList.value = file ? [{ name: file.name }] : []
}

function onFileRemove() {
  selectedFile.value = null
  fileList.value = []
  fileErrorKey.value = null
  emit('clear-server-error')
}

function closeDialog() {
  if (props.loading) {
    return
  }
  visible.value = false
}

function submitReplace() {
  if (props.loading || !selectedFile.value) {
    return
  }
  emit('submit', selectedFile.value)
}

const canSubmit = computed(() => Boolean(!props.loading && selectedFile.value))
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('masters.replaceFile.title')"
    width="520px"
    destroy-on-close
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    @close="closeDialog"
  >
    <p class="replace-hint">{{ t('masters.replaceFile.description') }}</p>
    <p v-if="currentFilename" class="current-file">
      {{ t('masters.replaceFile.currentFile', { filename: currentFilename }) }}
    </p>
    <el-upload
      drag
      :auto-upload="false"
      :file-list="fileList"
      accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      :limit="1"
      :disabled="loading"
      @change="onFileChange"
      @remove="onFileRemove"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">{{ t('masters.upload.dragHint') }}</div>
      <template #tip>
        <div class="upload-tip">
          {{ t('masters.upload.fileHint', { maxMb: MASTER_DOCX_MAX_UPLOAD_MB }) }}
        </div>
        <div v-if="inlineErrorKey" class="upload-error" role="alert">
          {{ inlineErrorText }}
        </div>
      </template>
    </el-upload>
    <div
      v-if="loading"
      data-testid="master-upload-progress"
      class="upload-progress"
      aria-live="polite"
    >
      <el-progress
        :percentage="progressPercent"
        :indeterminate="progressIndeterminate"
        :striped="true"
        :striped-flow="true"
      />
      <p class="upload-progress__label">
        <template v-if="uploadProgress != null">
          {{ t('masters.upload.progressPercent', { percent: progressPercent }) }}
        </template>
        <template v-else>
          {{ t('masters.upload.progressLabel') }}
        </template>
      </p>
    </div>
    <template #footer>
      <el-button :disabled="loading" @click="closeDialog">{{ t('masters.actions.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" :disabled="!canSubmit" @click="submitReplace">
        {{ t('masters.replaceFile.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.replace-hint {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
}

.current-file {
  margin: 0 0 1rem;
  font-size: 0.9rem;
}

.upload-tip {
  margin-top: 0.5rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.upload-error {
  margin-top: 0.25rem;
  color: var(--color-danger, #c45656);
  font-size: 0.875rem;
}

.upload-progress {
  margin-top: 0.75rem;
}

.upload-progress__label {
  margin: 0.5rem 0 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
