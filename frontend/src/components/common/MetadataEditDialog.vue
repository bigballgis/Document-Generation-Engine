<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  modelValue: boolean
  titleKey: string
  initialName: string
  initialDescription: string
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [payload: { name: string; description: string }]
}>()

const { t } = useI18n()

const form = reactive({
  name: '',
  description: '',
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      form.name = props.initialName
      form.description = props.initialDescription
    }
  },
)

function closeDialog() {
  emit('update:modelValue', false)
}

function handleSave() {
  emit('save', {
    name: form.name.trim(),
    description: form.description.trim(),
  })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t(titleKey)"
    width="480px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form label-position="top">
      <el-form-item :label="t('metadataEdit.name')">
        <el-input v-model="form.name" maxlength="200" />
      </el-form-item>
      <el-form-item :label="t('metadataEdit.description')">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" :disabled="!form.name.trim()" @click="handleSave">
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>
