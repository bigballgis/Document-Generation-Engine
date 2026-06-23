<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  modelValue: boolean
  initialName: string
  initialDescription: string | null
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: { name: string; description: string | null }]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  name: '',
  description: '',
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      form.name = props.initialName
      form.description = props.initialDescription ?? ''
    }
  },
)

function closeDialog() {
  visible.value = false
}

function submitForm() {
  if (!form.name.trim()) {
    return
  }
  emit('submit', {
    name: form.name.trim(),
    description: form.description.trim() || null,
  })
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('masters.metadata.editTitle')"
    width="520px"
    destroy-on-close
  >
    <el-form label-position="top">
      <el-form-item :label="t('masters.metadata.name')" required>
        <el-input v-model="form.name" maxlength="256" />
      </el-form-item>
      <el-form-item :label="t('masters.metadata.description')">
        <el-input
          v-model="form.description"
          type="textarea"
          maxlength="1024"
          :rows="4"
          :placeholder="t('masters.metadata.descriptionPlaceholder')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">{{ t('masters.actions.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!form.name.trim()"
        @click="submitForm"
      >
        {{ t('masters.metadata.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>
