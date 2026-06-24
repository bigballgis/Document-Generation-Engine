<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { type FormInstance, type FormRules } from 'element-plus'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [templateId: string]
}>()

const { t, te } = useI18n()
const mastersStore = useMastersStore()
const templatesStore = useTemplatesStore()
const { resolveDefaultGroupCode, ensureGroupCatalog } = useScopedGroupOptions()

const formRef = ref<FormInstance>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  groupCode: '',
  masterId: '',
  externalId: '',
  name: '',
  description: '',
})

const formRules = computed<FormRules>(() => ({
  groupCode: [
    { required: true, message: t('templates.create.validation.groupCodeRequired'), trigger: 'change' },
  ],
  masterId: [
    { required: true, message: t('templates.create.validation.masterRequired'), trigger: 'change' },
  ],
  externalId: [
    { required: true, message: t('templates.create.validation.externalIdRequired'), trigger: 'blur' },
    {
      pattern: /^[A-Z0-9][A-Z0-9_-]{0,127}$/,
      message: t('templates.create.validation.externalIdPattern'),
      trigger: 'blur',
    },
  ],
  name: [
    { required: true, message: t('templates.create.validation.nameRequired'), trigger: 'blur' },
  ],
}))

const apiErrorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.create')
})

const approvedMasters = computed(() =>
  mastersStore.masters.filter(
    (master) =>
      master.status === 'APPROVED' &&
      (form.groupCode === '' || master.groupCode === form.groupCode),
  ),
)

const masterOptions = computed(() =>
  approvedMasters.value.map((master) => ({
    value: master.id,
    label: `${master.name} (${master.groupCode})`,
  })),
)

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) {
      return
    }
    templatesStore.lastErrorMessageKey = null
    await ensureGroupCatalog()
    resetForm()
  },
)

watch(
  () => form.groupCode,
  () => {
    if (!approvedMasters.value.some((master) => master.id === form.masterId)) {
      form.masterId = ''
    }
  },
)

function resetForm() {
  form.groupCode = resolveDefaultGroupCode()
  form.masterId = ''
  form.externalId = ''
  form.name = ''
  form.description = ''
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    const created = await templatesStore.createTemplate({
      groupCode: form.groupCode.trim(),
      masterId: form.masterId,
      externalId: form.externalId.trim(),
      name: form.name.trim(),
      description: form.description.trim() || undefined,
    })
    visible.value = false
    emit('created', created.id)
  } catch {
    // Inline alert surfaces store message key.
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.create.title')"
    width="560px"
    destroy-on-close
  >
    <el-alert
      v-if="apiErrorMessage"
      type="error"
      :closable="false"
      show-icon
      class="create-error"
      :title="apiErrorMessage"
    />
    <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
      <el-form-item :label="t('templates.create.groupCode')" prop="groupCode">
        <ScopedGroupSelect
          v-model="form.groupCode"
          :placeholder="t('templates.create.groupCodePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('templates.create.master')" prop="masterId">
        <AppSearchSelect
          v-model="form.masterId"
          :placeholder="t('templates.create.masterPlaceholder')"
          :disabled="!form.groupCode"
        >
          <el-option
            v-for="option in masterOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </AppSearchSelect>
      </el-form-item>
      <el-form-item :label="t('templates.create.externalId')" prop="externalId">
        <el-input v-model="form.externalId" :placeholder="t('templates.create.externalIdPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('templates.create.name')" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item :label="t('templates.create.description')">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('templates.create.cancel') }}</el-button>
      <el-button type="primary" :loading="templatesStore.submitting" @click="handleSubmit">
        {{ t('templates.create.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.create-error {
  margin-bottom: 1rem;
}
</style>
