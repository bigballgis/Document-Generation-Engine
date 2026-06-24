<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

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

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      return
    }
    selectedFile.value = null
    fileList.value = []
  },
)

function onFileChange(uploadFile: { raw?: File }) {
  selectedFile.value = uploadFile.raw ?? null
  fileList.value = selectedFile.value ? [{ name: selectedFile.value.name }] : []
}

function onFileRemove() {
  selectedFile.value = null
  fileList.value = []
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
      accept=".docx"
      :limit="1"
      @change="onFileChange"
      @remove="onFileRemove"
    >
      <el-button>{{ t('masters.upload.chooseFile') }}</el-button>
      <template #tip>
        <div class="el-upload__tip">{{ t('masters.upload.fileHint') }}</div>
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
</style>
