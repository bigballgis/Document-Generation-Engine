<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { type FormInstance, type FormRules } from 'element-plus'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { useContentModulesStore } from '@/stores/contentModules'
import {
  excludeOwnerFromSharedGroupCodes,
} from '@/utils/contentModuleSharedGroups'
import { DOCUMENT_LOCALE_OPTIONS } from '@/constants/documentLocales'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  parseStructuredContent,
  serializeStructuredContent,
} from '@/utils/structuredContentNodes'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [moduleId: string]
}>()

const { t, te } = useI18n()
const contentModulesStore = useContentModulesStore()
const { resolveDefaultGroupCode, ensureGroupCatalog, groupOptions } = useScopedGroupOptions()
const { configureContentModuleSharedGroups } = useCapabilities()

const formRef = ref<FormInstance>()
const groupSelectRef = ref<InstanceType<typeof ScopedGroupSelect> | null>(null)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  groupCode: '',
  moduleCode: '',
  name: '',
  description: '',
  locale: '',
  localeVariantFamilyId: '',
  semanticVersion: '1.0.0',
  contentStructureJson: DEFAULT_STRUCTURED_CONTENT_JSON,
  changeDescription: '',
  sharedGroupCodes: [] as string[],
})

const canConfigureSharedGroups = computed(() => configureContentModuleSharedGroups.value)

const sharedGroupSelectOptions = computed(() =>
  groupOptions.value.filter(
    (option) => option.value.toUpperCase() !== form.groupCode.trim().toUpperCase(),
  ),
)

const formRules = computed<FormRules>(() => ({
  groupCode: [
    {
      required: true,
      message: t('contentModules.create.validation.groupCodeRequired'),
      trigger: 'change',
    },
  ],
  moduleCode: [
    {
      required: true,
      message: t('contentModules.create.validation.moduleCodeRequired'),
      trigger: 'blur',
    },
    {
      pattern: /^[A-Z0-9][A-Z0-9_-]{0,127}$/,
      message: t('contentModules.create.validation.moduleCodePattern'),
      trigger: 'blur',
    },
  ],
  name: [
    { required: true, message: t('contentModules.create.validation.nameRequired'), trigger: 'blur' },
  ],
  locale: [
    {
      required: true,
      message: t('contentModules.create.validation.localeRequired'),
      trigger: 'change',
    },
    {
      validator: (_rule, value: unknown, callback) => {
        if (typeof value !== 'string' || !value.trim()) {
          callback(new Error(t('contentModules.create.validation.localeRequired')))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  semanticVersion: [
    {
      required: true,
      message: t('contentModules.create.validation.semanticVersionRequired'),
      trigger: 'blur',
    },
  ],
}))

const apiErrorMessage = computed(() => {
  const key = contentModulesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('contentModules.error.create')
})

watch(visible, async (open) => {
  if (!open) {
    return
  }
  await groupSelectRef.value?.prepare()
  form.groupCode = resolveDefaultGroupCode(form.groupCode)
  form.contentStructureJson = DEFAULT_STRUCTURED_CONTENT_JSON
})

watch(
  () => form.groupCode,
  () => {
    form.sharedGroupCodes = excludeOwnerFromSharedGroupCodes(
      form.sharedGroupCodes,
      form.groupCode,
    )
  },
)

function resetForm() {
  form.groupCode = resolveDefaultGroupCode('')
  form.moduleCode = ''
  form.name = ''
  form.description = ''
  form.locale = ''
  form.localeVariantFamilyId = ''
  form.semanticVersion = '1.0.0'
  form.contentStructureJson = DEFAULT_STRUCTURED_CONTENT_JSON
  form.changeDescription = ''
  form.sharedGroupCodes = []
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  // BDD-IBL-E1-013 — never omit/blank locale on create.
  if (!form.locale.trim()) {
    return
  }
  const contentStructureJson = serializeStructuredContent(
    parseStructuredContent(form.contentStructureJson),
  )
  try {
    await ensureGroupCatalog()
    const sharedGroupCodes = canConfigureSharedGroups.value
      ? excludeOwnerFromSharedGroupCodes(form.sharedGroupCodes, form.groupCode)
      : []
    const familyId = form.localeVariantFamilyId.trim()
    const created = await contentModulesStore.createModule({
      groupCode: form.groupCode,
      moduleCode: form.moduleCode.trim(),
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      locale: form.locale.trim(),
      localeVariantFamilyId: familyId || undefined,
      semanticVersion: form.semanticVersion.trim(),
      contentStructureJson,
      changeDescription: form.changeDescription.trim() || undefined,
      sharedGroupCodes,
    })
    visible.value = false
    resetForm()
    emit('created', created.moduleId)
  } catch {
    // Store surfaces message key.
  }
}

defineExpose({
  form,
  sharedGroupSelectOptions,
  canConfigureSharedGroups,
  handleSubmit,
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('contentModules.create.title')"
    width="900px"
    destroy-on-close
    @closed="resetForm"
  >
    <el-alert
      v-if="apiErrorMessage"
      class="dialog-alert"
      type="error"
      :title="apiErrorMessage"
      show-icon
      :closable="false"
    />
    <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
      <el-form-item :label="t('contentModules.create.groupCode')" prop="groupCode">
        <ScopedGroupSelect ref="groupSelectRef" v-model="form.groupCode" />
      </el-form-item>
      <el-form-item
        v-if="canConfigureSharedGroups"
        :label="t('contentModules.create.sharedGroupCodes')"
        prop="sharedGroupCodes"
      >
        <el-select
          v-model="form.sharedGroupCodes"
          data-testid="content-module-shared-groups-select"
          multiple
          filterable
          clearable
          class="shared-groups-select"
          :placeholder="t('contentModules.create.sharedGroupCodesPlaceholder')"
        >
          <el-option
            v-for="option in sharedGroupSelectOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('contentModules.create.moduleCode')" prop="moduleCode">
        <el-input
          v-model="form.moduleCode"
          data-testid="module-code-input"
          :placeholder="t('contentModules.create.moduleCodePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.name')" prop="name">
        <el-input v-model="form.name" data-testid="module-name-input" />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.locale')" prop="locale">
        <el-select
          v-model="form.locale"
          data-testid="content-module-create-locale"
          filterable
          clearable
          class="locale-select"
          :placeholder="t('contentModules.create.localePlaceholder')"
        >
          <el-option
            v-for="option in DOCUMENT_LOCALE_OPTIONS"
            :key="option.value"
            :label="t(option.labelKey)"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('contentModules.create.localeVariantFamilyId')">
        <el-input
          v-model="form.localeVariantFamilyId"
          data-testid="content-module-create-locale-family"
          :placeholder="t('contentModules.create.localeVariantFamilyIdPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.description')" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.semanticVersion')" prop="semanticVersion">
        <el-input v-model="form.semanticVersion" />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.contentStructure')">
        <ControlledStructuredContentEditor v-model="form.contentStructureJson" />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.changeDescription')" prop="changeDescription">
        <el-input v-model="form.changeDescription" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="contentModulesStore.submitting" @click="handleSubmit">
        {{ t('contentModules.create.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dialog-alert {
  margin-bottom: 1rem;
}

.shared-groups-select {
  width: 100%;
}

.locale-select {
  width: 100%;
}
</style>
