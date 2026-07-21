<script setup lang="ts">
import { toRef } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import { useAssetLibraryUploadDialog } from '@/components/library/useAssetLibraryUploadDialog'
import type { LibraryAssetClass } from '@/types/libraryAsset'
import { LIBRARY_ASSET_MAX_BYTES } from '@/types/libraryAsset'

const props = defineProps<{
  modelValue: boolean
  loading?: boolean
  serverErrorKey?: string | null
  canUploadImageOrOther: boolean
  canUploadSeal: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [
    payload: { groupCode: string; assetKey: string; assetClass: LibraryAssetClass; file: File },
  ]
  'clear-server-error': []
}>()

const {
  t,
  visible,
  form,
  fileList,
  inlineErrorKey,
  inlineErrorText,
  serverErrorText,
  assetClassOptions,
  keyHint,
  canSubmit,
  onFileChange,
  onFileRemove,
  resetForm,
  closeDialog,
  submitUpload,
} = useAssetLibraryUploadDialog({
  modelValue: toRef(props, 'modelValue'),
  loading: toRef(props, 'loading'),
  serverErrorKey: toRef(props, 'serverErrorKey'),
  canUploadImageOrOther: toRef(props, 'canUploadImageOrOther'),
  canUploadSeal: toRef(props, 'canUploadSeal'),
  emitModelValue: (value) => emit('update:modelValue', value),
  emitSubmit: (payload) => emit('submit', payload),
  emitClearServerError: () => emit('clear-server-error'),
})

const maxMb = LIBRARY_ASSET_MAX_BYTES / (1024 * 1024)
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('assetLibrary.upload.title')"
    width="520px"
    destroy-on-close
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    @closed="resetForm"
  >
    <el-form label-position="top" data-testid="asset-library-upload-form">
      <el-form-item :label="t('assetLibrary.upload.groupCode')" required>
        <div data-testid="asset-library-upload-group">
          <ScopedGroupSelect
            v-model="form.groupCode"
            :placeholder="t('assetLibrary.upload.groupCodePlaceholder')"
            :disabled="loading"
          />
        </div>
      </el-form-item>
      <el-form-item :label="t('assetLibrary.upload.assetClass')" required>
        <el-select
          v-model="form.assetClass"
          :placeholder="t('assetLibrary.upload.assetClassPlaceholder')"
          :disabled="loading || assetClassOptions.length === 0"
          style="width: 100%"
        >
          <el-option
            v-for="option in assetClassOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('assetLibrary.upload.assetKey')" required>
        <el-input
          v-model="form.assetKey"
          maxlength="128"
          :disabled="loading"
          :placeholder="t('assetLibrary.upload.assetKeyPlaceholder')"
        />
        <p class="upload-hint">{{ keyHint }}</p>
      </el-form-item>
      <el-form-item :label="t('assetLibrary.upload.file')" required>
        <el-upload
          drag
          :auto-upload="false"
          :limit="1"
          accept=".png,.jpg,.jpeg,image/png,image/jpeg"
          :file-list="fileList"
          :disabled="loading"
          @change="onFileChange"
          @remove="onFileRemove"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">{{ t('assetLibrary.upload.dragHint') }}</div>
          <template #tip>
            <div class="upload-tip">
              {{ t('assetLibrary.upload.fileHint', { maxMb }) }}
            </div>
            <div v-if="inlineErrorKey" class="upload-error" role="alert">
              {{ inlineErrorText }}
            </div>
            <div v-else-if="serverErrorKey" class="upload-error" role="alert">
              {{ serverErrorText }}
            </div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="loading" @click="closeDialog">
        {{ t('common.cancel') }}
      </el-button>
      <el-button type="primary" :loading="loading" :disabled="!canSubmit" @click="submitUpload">
        {{ t('assetLibrary.upload.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.upload-hint,
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
</style>
