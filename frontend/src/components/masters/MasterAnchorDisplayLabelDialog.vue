<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  modelValue: boolean
  anchorId: string
  initialDisplayLabel: string
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: { displayLabel: string }]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  displayLabel: '',
})

const validationError = computed(() => {
  if (!form.displayLabel.trim()) {
    return t('masters.revision.anchorLabelRequired')
  }
  return ''
})

const canSubmit = computed(() => !validationError.value && !props.loading)

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      form.displayLabel = props.initialDisplayLabel
    }
  },
)

function closeDialog() {
  visible.value = false
}

function submitForm() {
  if (!canSubmit.value) {
    return
  }
  emit('submit', { displayLabel: form.displayLabel.trim() })
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('masters.revision.editAnchorLabelTitle')"
    width="480px"
    destroy-on-close
    data-testid="master-anchor-display-label-dialog"
  >
    <el-form label-position="top" @submit.prevent="submitForm">
      <el-form-item :label="t('masters.revision.anchorId')">
        <el-input :model-value="anchorId" readonly disabled />
      </el-form-item>
      <el-form-item :label="t('masters.revision.anchorLabel')" required>
        <el-input
          v-model="form.displayLabel"
          maxlength="256"
          data-testid="master-anchor-display-label-input"
          :placeholder="t('masters.revision.anchorLabelPlaceholder')"
        />
        <p
          v-if="validationError"
          class="field-error"
          data-testid="master-anchor-display-label-error"
          role="alert"
        >
          {{ validationError }}
        </p>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">{{ t('masters.actions.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!canSubmit"
        data-testid="master-anchor-display-label-save"
        @click="submitForm"
      >
        {{ t('masters.revision.saveAnchorLabel') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.field-error {
  margin: var(--space-2) 0 0;
  font-size: var(--font-size-sm);
  color: var(--el-color-danger);
}
</style>
