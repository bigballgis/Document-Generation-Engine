<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  MASTER_DOCX_MAX_UPLOAD_MB,
  validateMasterDocxUploadFile,
} from '@/utils/validateMasterDocxUpload'
import type { MasterImpactAnalysis } from '@/types/master'

const props = defineProps<{
  modelValue: boolean
  loading?: boolean
  uploadProgress?: number | null
  serverErrorKey?: string | null
  currentFilename?: string
  impact?: MasterImpactAnalysis | null
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
const confirmStep = ref(false)

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

const referencedNames = computed(() => {
  const impact = props.impact
  if (!impact) {
    return [] as string[]
  }
  if (impact.referencedTemplates?.length) {
    return impact.referencedTemplates.map((item) => item.name)
  }
  return impact.referencedTemplateIds ?? []
})

const shownNames = computed(() => referencedNames.value.slice(0, 5))
const hiddenNameCount = computed(() => Math.max(0, referencedNames.value.length - shownNames.value.length))

const anchorDeltaSummary = computed(() => {
  const delta = props.impact?.anchorDelta
  if (!delta) {
    return t('masters.replaceFile.confirmNoAnchorDelta')
  }
  const parts: string[] = []
  if (delta.addedAnchors.length) {
    parts.push(t('masters.replaceFile.confirmAdded', { count: delta.addedAnchors.length }))
  }
  if (delta.removedAnchors.length) {
    parts.push(t('masters.replaceFile.confirmRemoved', { count: delta.removedAnchors.length }))
  }
  if (delta.renamedAnchors.length) {
    parts.push(t('masters.replaceFile.confirmRenamed', { count: delta.renamedAnchors.length }))
  }
  return parts.length > 0 ? parts.join(' · ') : t('masters.replaceFile.confirmNoAnchorDelta')
})

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      return
    }
    selectedFile.value = null
    fileList.value = []
    fileErrorKey.value = null
    confirmStep.value = false
  },
)

function onFileChange(uploadFile: { raw?: File }) {
  const file = uploadFile.raw ?? null
  fileErrorKey.value = null
  emit('clear-server-error')
  confirmStep.value = false
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
  confirmStep.value = false
  emit('clear-server-error')
}

function closeDialog() {
  if (props.loading) {
    return
  }
  visible.value = false
}

function goToConfirmStep() {
  if (props.loading || !selectedFile.value) {
    return
  }
  confirmStep.value = true
}

function backToUploadStep() {
  if (props.loading) {
    return
  }
  confirmStep.value = false
}

function submitReplace() {
  if (props.loading || !selectedFile.value || !confirmStep.value) {
    return
  }
  emit('submit', selectedFile.value)
}

const canProceedToConfirm = computed(() => Boolean(!props.loading && selectedFile.value && !confirmStep.value))
const canSubmit = computed(() => Boolean(!props.loading && selectedFile.value && confirmStep.value))
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('masters.replaceFile.title')"
    width="560px"
    destroy-on-close
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    data-testid="master-replace-file-dialog"
    @close="closeDialog"
  >
    <template v-if="!confirmStep">
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
    </template>

    <div
      v-else
      class="confirm-step"
      data-testid="master-replace-impact-confirm"
    >
      <p class="confirm-lead">{{ t('masters.replaceFile.confirmLead') }}</p>
      <p
        v-if="impact?.retestRequired"
        class="confirm-retest"
        data-testid="master-replace-retest-required"
      >
        {{ t('masters.impact.retestRequired') }}
      </p>
      <p v-else class="confirm-retest muted">{{ t('masters.impact.retestNotRequired') }}</p>
      <p class="confirm-delta" data-testid="master-replace-anchor-delta">
        {{ anchorDeltaSummary }}
      </p>
      <div v-if="shownNames.length" class="confirm-templates">
        <p class="list-label">{{ t('masters.impact.referencedTemplates') }}</p>
        <ul data-testid="master-replace-impact-names">
          <li v-for="name in shownNames" :key="name">{{ name }}</li>
        </ul>
        <p v-if="hiddenNameCount > 0" class="muted">
          {{ t('masters.replaceFile.confirmMoreTemplates', { count: hiddenNameCount }) }}
        </p>
      </div>
      <p v-else class="muted">{{ t('masters.impact.noReferencedTemplates') }}</p>
      <div v-if="inlineErrorKey" class="upload-error" role="alert">
        {{ inlineErrorText }}
      </div>
    </div>

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
      <el-button :disabled="loading" data-testid="master-replace-cancel" @click="closeDialog">
        {{ t('masters.actions.cancel') }}
      </el-button>
      <el-button
        v-if="confirmStep"
        :disabled="loading"
        data-testid="master-replace-back"
        @click="backToUploadStep"
      >
        {{ t('masters.replaceFile.back') }}
      </el-button>
      <el-button
        v-if="!confirmStep"
        type="primary"
        :disabled="!canProceedToConfirm"
        data-testid="master-replace-continue"
        @click="goToConfirmStep"
      >
        {{ t('masters.replaceFile.continue') }}
      </el-button>
      <el-button
        v-else
        type="primary"
        :loading="loading"
        :disabled="!canSubmit"
        data-testid="master-replace-confirm"
        @click="submitReplace"
      >
        {{ t('masters.replaceFile.confirmSubmit') }}
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

.confirm-step {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.confirm-lead {
  margin: 0;
  font-weight: 600;
}

.confirm-retest {
  margin: 0;
}

.confirm-delta {
  margin: 0;
  font-size: 0.9rem;
}

.confirm-templates ul {
  margin: 0.25rem 0 0;
  padding-left: 1.25rem;
}

.list-label {
  margin: 0;
  font-weight: 600;
}

.muted {
  margin: 0;
  color: var(--text-muted);
}
</style>
