<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: { changeSummary: string }]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  changeSummary: '',
})

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      form.changeSummary = ''
    }
  },
)

function closeDialog() {
  visible.value = false
}

function submitForm() {
  if (!form.changeSummary.trim()) {
    return
  }
  emit('submit', { changeSummary: form.changeSummary.trim() })
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('masters.submitReview.title')"
    width="520px"
    destroy-on-close
  >
    <el-form label-position="top">
      <el-form-item :label="t('masters.submitReview.changeSummary')" required>
        <el-input
          v-model="form.changeSummary"
          type="textarea"
          maxlength="2048"
          :rows="4"
          :placeholder="t('masters.submitReview.changeSummaryPlaceholder')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">{{ t('masters.actions.cancel') }}</el-button>
      <el-button type="primary" :disabled="!form.changeSummary.trim()" @click="submitForm">
        {{ t('masters.submitReview.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>
