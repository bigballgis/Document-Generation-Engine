<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { type FormInstance, type FormRules } from 'element-plus'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { useContentModulesStore } from '@/stores/contentModules'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [moduleId: string]
}>()

const { t, te } = useI18n()
const contentModulesStore = useContentModulesStore()
const { resolveDefaultGroupCode, ensureGroupCatalog } = useScopedGroupOptions()

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
  semanticVersion: '1.0.0',
  contentStructureJson: '{\n  "blocks": []\n}',
  changeDescription: '',
})

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
  semanticVersion: [
    {
      required: true,
      message: t('contentModules.create.validation.semanticVersionRequired'),
      trigger: 'blur',
    },
  ],
  contentStructureJson: [
    {
      required: true,
      message: t('contentModules.create.validation.contentStructureRequired'),
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
})

function resetForm() {
  form.groupCode = resolveDefaultGroupCode('')
  form.moduleCode = ''
  form.name = ''
  form.description = ''
  form.semanticVersion = '1.0.0'
  form.contentStructureJson = '{\n  "blocks": []\n}'
  form.changeDescription = ''
}

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
    await ensureGroupCatalog()
    const created = await contentModulesStore.createModule({
      groupCode: form.groupCode,
      moduleCode: form.moduleCode.trim(),
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      semanticVersion: form.semanticVersion.trim(),
      contentStructureJson: form.contentStructureJson,
      changeDescription: form.changeDescription.trim() || undefined,
    })
    visible.value = false
    resetForm()
    emit('created', created.moduleId)
  } catch {
    // Store surfaces message key.
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('contentModules.create.title')"
    width="640px"
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
      <el-form-item :label="t('contentModules.create.moduleCode')" prop="moduleCode">
        <el-input v-model="form.moduleCode" :placeholder="t('contentModules.create.moduleCodePlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.name')" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.description')" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.semanticVersion')" prop="semanticVersion">
        <el-input v-model="form.semanticVersion" />
      </el-form-item>
      <el-form-item :label="t('contentModules.create.contentStructureJson')" prop="contentStructureJson">
        <el-input v-model="form.contentStructureJson" type="textarea" :rows="6" />
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
</style>
