<script setup lang="ts">
import { toRef } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import { useMasterUploadDialog } from '@/components/masters/useMasterUploadDialog'
import { MASTER_DOCX_MAX_UPLOAD_MB } from '@/utils/validateMasterDocxUpload'

const props = defineProps<{
  modelValue: boolean
  loading?: boolean
  uploadProgress?: number | null
  serverErrorKey?: string | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: { groupCode: string; name: string; description: string; file: File }]
  'clear-server-error': []
}>()

const {
  t,
  visible,
  form,
  fileList,
  inlineErrorKey,
  inlineErrorText,
  progressPercent,
  progressIndeterminate,
  canSubmit,
  onFileChange,
  onFileRemove,
  resetForm,
  closeDialog,
  submitUpload,
} = useMasterUploadDialog({
  modelValue: toRef(props, 'modelValue'),
  loading: toRef(props, 'loading'),
  uploadProgress: toRef(props, 'uploadProgress'),
  serverErrorKey: toRef(props, 'serverErrorKey'),
  emitModelValue: (value) => emit('update:modelValue', value),
  emitSubmit: (payload) => emit('submit', payload),
  emitClearServerError: () => emit('clear-server-error'),
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('masters.upload.title')"
    width="520px"
    destroy-on-close
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    @closed="resetForm"
  >
    <el-form label-position="top">
      <el-form-item :label="t('masters.upload.groupCode')" required>
        <ScopedGroupSelect
          v-model="form.groupCode"
          :placeholder="t('masters.upload.groupCodePlaceholder')"
          :disabled="loading"
        />
      </el-form-item>
      <el-form-item :label="t('masters.upload.name')" required>
        <el-input v-model="form.name" maxlength="256" :disabled="loading" />
      </el-form-item>
      <el-form-item :label="t('masters.upload.description')">
        <el-input
          v-model="form.description"
          type="textarea"
          maxlength="1024"
          :rows="3"
          :disabled="loading"
        />
      </el-form-item>
      <el-form-item :label="t('masters.upload.file')" required>
        <el-upload
          drag
          :auto-upload="false"
          :limit="1"
          accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          :file-list="fileList"
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
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="loading" @click="closeDialog">{{ t('masters.actions.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" :disabled="!canSubmit" @click="submitUpload">
        {{ t('masters.upload.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss" src="./MasterUploadDialog.scss"></style>
