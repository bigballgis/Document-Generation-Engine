<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  APPROVAL_MATRIX_MODE_VALUES,
  type ApprovalMatrixMode,
} from '@/types/approvalMatrix'
import { approvalMatrixModeLabelKey } from '@/utils/approvalMatrix'

const props = withDefaults(
  defineProps<{
    modelValue: ApprovalMatrixMode
    disabled?: boolean
    showHint?: boolean
  }>(),
  {
    disabled: false,
    showHint: true,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: ApprovalMatrixMode]
}>()

const { t } = useI18n()

const selected = computed({
  get: () => props.modelValue,
  set: (value: ApprovalMatrixMode) => emit('update:modelValue', value),
})
</script>

<template>
  <div class="approval-matrix-mode" data-testid="approval-matrix-mode-field">
    <el-form-item :label="t('templates.approvalMatrix.label')">
      <el-select
        v-model="selected"
        data-testid="approval-matrix-mode-select"
        class="approval-matrix-mode__select"
        :disabled="disabled"
        :placeholder="t('templates.approvalMatrix.placeholder')"
      >
        <el-option
          v-for="mode in APPROVAL_MATRIX_MODE_VALUES"
          :key="mode"
          :label="t(approvalMatrixModeLabelKey(mode))"
          :value="mode"
        />
      </el-select>
      <p v-if="showHint" class="approval-matrix-mode__hint">
        {{ t('templates.approvalMatrix.hint') }}
      </p>
    </el-form-item>
  </div>
</template>

<style scoped lang="scss">
.approval-matrix-mode {
  &__select {
    width: 100%;
  }

  &__hint {
    margin: 0.35rem 0 0;
    font-size: 0.85rem;
    color: var(--text-muted);
    line-height: 1.4;
  }
}
</style>
