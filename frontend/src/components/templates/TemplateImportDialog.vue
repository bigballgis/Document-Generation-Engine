<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { type FormInstance, type FormRules } from 'element-plus'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'
import {
  parseTemplateExportBundleFile,
  TemplateExportBundleParseError,
} from '@/utils/parseTemplateExportBundleFile'
import type { TemplateExportBundle, TemplateImportConflictPolicy } from '@/types/template'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  imported: [templateId: string]
}>()

const { t, te } = useI18n()
const mastersStore = useMastersStore()
const templatesStore = useTemplatesStore()
const formRef = ref<FormInstance>()
const selectedFile = ref<File | null>(null)
const parsedBundle = ref<TemplateExportBundle | null>(null)
const parseErrorKey = ref<string | null>(null)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  masterId: '',
  importConflictPolicy: 'REJECT_IMPORT' as TemplateImportConflictPolicy,
})

const formRules = computed<FormRules>(() => ({
  masterId: [
    { required: true, message: t('templates.import.validation.masterRequired'), trigger: 'change' },
  ],
}))

const approvedMasters = computed(() => {
  const groupCode = parsedBundle.value?.metadata.groupCode
  return mastersStore.masters.filter(
    (master) =>
      master.status === 'APPROVED' &&
      (groupCode === undefined || master.groupCode === groupCode),
  )
})

const masterOptions = computed(() =>
  approvedMasters.value.map((master) => ({
    value: master.id,
    label: `${master.name} (${master.groupCode})`,
  })),
)

const apiErrorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.import')
})

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) {
      return
    }
    selectedFile.value = null
    parsedBundle.value = null
    parseErrorKey.value = null
    form.masterId = ''
    form.importConflictPolicy = 'REJECT_IMPORT'
    await mastersStore.fetchMasters()
  },
)

watch(parsedBundle, (bundle) => {
  if (!bundle) {
    form.masterId = ''
    return
  }
  const matchingMaster = approvedMasters.value.find((master) => master.id === bundle.metadata.masterId)
  form.masterId = matchingMaster?.id ?? approvedMasters.value[0]?.id ?? ''
})

function onFileRemove() {
  selectedFile.value = null
  parsedBundle.value = null
  parseErrorKey.value = null
}

async function onFileSelected(uploadFile: { raw?: File }) {
  selectedFile.value = uploadFile.raw ?? null
  parsedBundle.value = null
  parseErrorKey.value = null
  if (selectedFile.value) {
    await parseSelectedFile()
  }
}

async function parseSelectedFile() {
  if (!selectedFile.value) {
    parsedBundle.value = null
    parseErrorKey.value = 'templates.import.validation.fileRequired'
    return
  }
  try {
    parsedBundle.value = await parseTemplateExportBundleFile(selectedFile.value)
    parseErrorKey.value = null
  } catch (error) {
    parsedBundle.value = null
    parseErrorKey.value =
      error instanceof TemplateExportBundleParseError
        ? error.messageKey
        : 'templates.import.error.invalidBundle'
  }
}

async function handleSubmit() {
  await parseSelectedFile()
  if (!parsedBundle.value || parseErrorKey.value) {
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid === false) {
    return
  }
  try {
    const result = await templatesStore.importTemplate({
      masterId: form.masterId,
      bundle: parsedBundle.value,
      importConflictPolicy: form.importConflictPolicy,
    })
    visible.value = false
    emit('imported', result.template.id)
  } catch {
    // Error surfaced via store message key.
  }
}

defineExpose({
  form,
  onFileSelected,
  handleSubmit,
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.import.title')"
    width="560px"
    :close-on-click-modal="false"
  >
    <p class="dialog-description">{{ t('templates.import.description') }}</p>

    <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
      <el-form-item :label="t('templates.import.bundleFile')" required>
        <el-upload
          :auto-upload="false"
          :limit="1"
          accept=".json,.zip,application/json,application/zip"
          :on-change="onFileSelected"
          :on-remove="onFileRemove"
        >
          <el-button>{{ t('templates.import.chooseFile') }}</el-button>
          <template #tip>
            <div class="upload-tip">{{ t('templates.import.fileHint') }}</div>
          </template>
        </el-upload>
        <el-alert
          v-if="parseErrorKey"
          class="parse-alert"
          type="error"
          :title="t(parseErrorKey)"
          show-icon
          :closable="false"
        />
      </el-form-item>

      <el-card v-if="parsedBundle" shadow="never" class="bundle-summary">
        <h3>{{ t('templates.import.bundleSummaryTitle') }}</h3>
        <dl>
          <div>
            <dt>{{ t('templates.import.bundleName') }}</dt>
            <dd>{{ parsedBundle.metadata.name }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.import.bundleExternalId') }}</dt>
            <dd>{{ parsedBundle.metadata.externalId }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.import.bundleGroup') }}</dt>
            <dd>{{ parsedBundle.metadata.groupCode }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.import.bundleSourceTemplateId') }}</dt>
            <dd>{{ parsedBundle.metadata.templateId }}</dd>
          </div>
        </dl>
      </el-card>

      <el-form-item :label="t('templates.import.master')" prop="masterId">
        <AppSearchSelect
          v-model="form.masterId"
          :options="masterOptions"
          :placeholder="t('templates.import.masterPlaceholder')"
          :disabled="!parsedBundle"
        />
      </el-form-item>

      <el-form-item :label="t('templates.import.conflictPolicy')">
        <el-radio-group v-model="form.importConflictPolicy" :disabled="!parsedBundle">
          <el-radio value="REJECT_IMPORT">
            {{ t('templates.import.conflictReject') }}
          </el-radio>
          <el-radio value="KEEP_TEMPLATE_ID">
            {{ t('templates.import.conflictKeepId') }}
          </el-radio>
        </el-radio-group>
        <p class="field-hint">{{ t('templates.import.conflictHint') }}</p>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="apiErrorMessage"
      type="error"
      :title="apiErrorMessage"
      show-icon
      :closable="false"
    />

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="templatesStore.submitting" @click="handleSubmit">
        {{ t('templates.import.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dialog-description {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.upload-tip {
  color: var(--text-muted);
  font-size: 0.875rem;
}

.parse-alert {
  margin-top: 0.75rem;
}

.bundle-summary {
  margin-bottom: 1rem;

  h3 {
    margin: 0 0 0.75rem;
    font-size: 1rem;
  }

  dl {
    display: grid;
    gap: 0.5rem;
    margin: 0;
  }

  div {
    display: grid;
    grid-template-columns: 10rem 1fr;
    gap: 0.75rem;
  }

  dt {
    margin: 0;
    color: var(--text-muted);
  }

  dd {
    margin: 0;
    font-weight: 500;
    word-break: break-word;
  }
}

.field-hint {
  margin: 0.5rem 0 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
