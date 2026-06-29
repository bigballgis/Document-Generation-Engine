<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { type FormInstance, type FormRules } from 'element-plus'
import type { ContentModuleVersion } from '@/types/contentModule'
import { useContentModulesStore } from '@/stores/contentModules'

const props = defineProps<{
  modelValue: boolean
  moduleId: string
  mode: 'create' | 'edit'
  version?: ContentModuleVersion | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const { t, te } = useI18n()
const contentModulesStore = useContentModulesStore()
const formRef = ref<FormInstance>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const DEFAULT_CONTENT_STRUCTURE_JSON = '{\n  "blocks": []\n}'

const form = reactive({
  semanticVersion: '',
  contentStructureJson: DEFAULT_CONTENT_STRUCTURE_JSON,
  changeDescription: '',
})

function formatContentStructureJson(raw: string | null | undefined): string {
  if (raw == null || raw.trim() === '') {
    return ''
  }
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

const formRules = computed<FormRules>(() => ({
  semanticVersion: [
    {
      required: props.mode === 'create',
      message: t('contentModules.version.validation.semanticVersionRequired'),
      trigger: 'blur',
    },
  ],
  contentStructureJson: [
    {
      required: true,
      message: t('contentModules.version.validation.contentStructureRequired'),
      trigger: 'blur',
    },
  ],
}))

const dialogTitle = computed(() =>
  props.mode === 'create'
    ? t('contentModules.version.createTitle')
    : t('contentModules.version.editTitle', { version: props.version?.semanticVersion ?? '' }),
)

const apiErrorMessage = computed(() => {
  const key = contentModulesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('contentModules.error.updateVersion')
})

watch(visible, (open) => {
  if (!open) {
    return
  }
  if (props.mode === 'edit' && props.version) {
    form.semanticVersion = props.version.semanticVersion
    form.changeDescription = props.version.changeDescription ?? ''
    form.contentStructureJson = formatContentStructureJson(props.version.contentStructureJson)
  } else {
    form.semanticVersion = ''
    form.contentStructureJson = DEFAULT_CONTENT_STRUCTURE_JSON
    form.changeDescription = ''
  }
}, { immediate: true })

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  try {
    JSON.parse(form.contentStructureJson)
  } catch {
    return
  }
  try {
    if (props.mode === 'create') {
      await contentModulesStore.createVersion(props.moduleId, {
        semanticVersion: form.semanticVersion.trim(),
        contentStructureJson: form.contentStructureJson,
        changeDescription: form.changeDescription.trim() || undefined,
      })
    } else if (props.version) {
      await contentModulesStore.updateDraftVersion(props.moduleId, props.version.semanticVersion, {
        contentStructureJson: form.contentStructureJson,
        changeDescription: form.changeDescription.trim() || undefined,
      })
    }
    visible.value = false
    emit('saved')
  } catch {
    // Store surfaces message key.
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="640px" destroy-on-close>
    <el-alert
      v-if="apiErrorMessage"
      class="dialog-alert"
      type="error"
      :title="apiErrorMessage"
      show-icon
      :closable="false"
    />
    <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
      <el-form-item
        v-if="mode === 'create'"
        :label="t('contentModules.version.semanticVersion')"
        prop="semanticVersion"
      >
        <el-input v-model="form.semanticVersion" />
      </el-form-item>
      <el-form-item :label="t('contentModules.version.contentStructureJson')" prop="contentStructureJson">
        <el-input v-model="form.contentStructureJson" type="textarea" :rows="8" />
      </el-form-item>
      <el-form-item :label="t('contentModules.version.changeDescription')" prop="changeDescription">
        <el-input v-model="form.changeDescription" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="contentModulesStore.submitting" @click="handleSubmit">
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dialog-alert {
  margin-bottom: 1rem;
}
</style>
