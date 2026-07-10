<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { validateMasterDocxUploadFile } from '@/utils/validateMasterDocxUpload'

const props = defineProps<{
  modelValue: boolean
  loading?: boolean
  currentFilename?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [file: File]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const selectedFile = ref<File | null>(null)
const fileList = ref<{ name: string }[]>([])
const fileErrorKey = ref<string | null>(null)

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
}

function closeDialog() {
  visible.value = false
}

function submitReplace() {
  if (!selectedFile.value) {
    return
  }
  emit('submit', selectedFile.value)
}

const canSubmit = computed(() => Boolean(selectedFile.value))
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('masters.replaceFile.title')"
    width="520px"
    destroy-on-close
    @close="closeDialog"
  >
    <p class="replace-hint">{{ t('masters.replaceFile.description') }}</p>
    <p v-if="currentFilename" class="current-file">
      {{ t('masters.replaceFile.currentFile', { filename: currentFilename }) }}
    </p>
    <el-upload
      :auto-upload="false"
      :file-list="fileList"
      accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      :limit="1"
      @change="onFileChange"
      @remove="onFileRemove"
    >
      <el-button>{{ t('masters.upload.chooseFile') }}</el-button>
      <template #tip>
        <div class="upload-tip">{{ t('masters.upload.fileHint') }}</div>
        <div v-if="fileErrorKey" class="upload-error" role="alert">
          {{ t(fileErrorKey) }}
        </div>
      </template>
    </el-upload>
    <template #footer>
      <el-button @click="closeDialog">{{ t('masters.actions.cancel') }}</el-button>
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
  color: var(--color-danger, #f56c6c);
  font-size: 0.875rem;
}
</style>
