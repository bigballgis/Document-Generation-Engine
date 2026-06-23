<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [templateId: string]
}>()

const { t } = useI18n()
const sessionStore = useSessionStore()
const mastersStore = useMastersStore()
const templatesStore = useTemplatesStore()

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
    resetForm()
    if (mastersStore.masters.length === 0) {
      try {
        await mastersStore.fetchMasters()
      } catch {
        // Parent surfaces errors via store.
      }
    }
    if (!form.groupCode && groupOptions.value.length === 1) {
      form.groupCode = groupOptions.value[0]?.value ?? ''
    }
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
  form.groupCode = groupOptions.value.length === 1 ? (groupOptions.value[0]?.value ?? '') : ''
  form.masterId = ''
  form.externalId = ''
  form.name = ''
  form.description = ''
}

async function handleSubmit() {
  if (!form.groupCode.trim() || !form.masterId || !form.externalId.trim() || !form.name.trim()) {
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
    // Parent surfaces errors via store message key.
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
    <el-form label-position="top">
      <el-form-item :label="t('templates.create.groupCode')">
        <AppSearchSelect
          v-if="!useGroupInput"
          v-model="form.groupCode"
          :placeholder="t('templates.create.groupCodePlaceholder')"
        >
          <el-option
            v-for="option in groupOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </AppSearchSelect>
        <el-input
          v-else
          v-model="form.groupCode"
          :placeholder="t('templates.create.groupCodeManualPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('templates.create.master')">
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
      <el-form-item :label="t('templates.create.externalId')">
        <el-input v-model="form.externalId" :placeholder="t('templates.create.externalIdPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('templates.create.name')">
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
