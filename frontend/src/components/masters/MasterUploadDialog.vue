<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSessionStore } from '@/stores/session'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: { groupCode: string; name: string; description: string; file: File }]
}>()

const { t } = useI18n()
const sessionStore = useSessionStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  groupCode: '',
  name: '',
  description: '',
})

const selectedFile = ref<File | null>(null)
const fileList = ref<{ name: string }[]>([])

const groupOptions = computed(() =>
  (sessionStore.session?.authorizedGroupCodes ?? [])
    .filter((code) => code !== '*')
    .map((code) => ({ value: code, label: code })),
)

const hasWildcardGroupScope = computed(() =>
  (sessionStore.session?.authorizedGroupCodes ?? []).includes('*'),
)

const useGroupInput = computed(
  () => groupOptions.value.length === 0 && hasWildcardGroupScope.value,
)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      return
    }
    if (!form.groupCode && groupOptions.value.length === 1) {
      form.groupCode = groupOptions.value[0]?.value ?? ''
    }
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

function resetForm() {
  form.groupCode = groupOptions.value.length === 1 ? (groupOptions.value[0]?.value ?? '') : ''
  form.name = ''
  form.description = ''
  selectedFile.value = null
  fileList.value = []
}

function closeDialog() {
  visible.value = false
  resetForm()
}

function submitUpload() {
  if (!selectedFile.value || !form.groupCode || !form.name.trim()) {
    return
  }
  emit('submit', {
    groupCode: form.groupCode,
    name: form.name.trim(),
    description: form.description.trim(),
    file: selectedFile.value,
  })
}

const canSubmit = computed(
  () => Boolean(form.groupCode && form.name.trim() && selectedFile.value),
)
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('masters.upload.title')"
    width="520px"
    destroy-on-close
    @closed="resetForm"
  >
    <el-form label-position="top">
      <el-form-item :label="t('masters.upload.groupCode')" required>
        <el-input
          v-if="useGroupInput"
          v-model="form.groupCode"
          maxlength="64"
          :placeholder="t('masters.upload.groupCodeManualPlaceholder')"
        />
        <el-select
          v-else
          v-model="form.groupCode"
          :placeholder="t('masters.upload.groupCodePlaceholder')"
        >
          <el-option
            v-for="option in groupOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('masters.upload.name')" required>
        <el-input v-model="form.name" maxlength="256" />
      </el-form-item>
      <el-form-item :label="t('masters.upload.description')">
        <el-input v-model="form.description" type="textarea" maxlength="1024" :rows="3" />
      </el-form-item>
      <el-form-item :label="t('masters.upload.file')" required>
        <el-upload
          :auto-upload="false"
          :limit="1"
          accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          :file-list="fileList"
          @change="onFileChange"
          @remove="onFileRemove"
        >
          <el-button>{{ t('masters.upload.chooseFile') }}</el-button>
          <template #tip>
            <div class="upload-tip">{{ t('masters.upload.fileHint') }}</div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">{{ t('masters.actions.cancel') }}</el-button>
      <el-button type="primary" :disabled="!canSubmit" @click="submitUpload">
        {{ t('masters.upload.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.upload-tip {
  margin-top: 0.5rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
