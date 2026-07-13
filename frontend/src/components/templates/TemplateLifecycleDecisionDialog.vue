<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import LifecycleDecisionApprovalPassFields from '@/components/templates/LifecycleDecisionApprovalPassFields.vue'
import LifecycleDecisionRejectFields from '@/components/templates/LifecycleDecisionRejectFields.vue'
import LifecycleDecisionTestPassFields from '@/components/templates/LifecycleDecisionTestPassFields.vue'
import {
  useLifecycleDecisionDialog,
  type LifecycleDecisionDialogMode,
  type LifecycleDecisionSubmitPayload,
} from '@/components/templates/useLifecycleDecisionDialog'

const props = defineProps<{
  modelValue: boolean
  mode: LifecycleDecisionDialogMode
  templateId?: string
  loading?: boolean
  initialComment?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: LifecycleDecisionSubmitPayload]
}>()

const { t } = useI18n()

const {
  formRef,
  availableReasonCategories,
  loadingConfig,
  selectedReasonPrompt,
  visible,
  canConfirmOnBehalf,
  isTestPassMode,
  isApprovalPassMode,
  form,
  dialogTitle,
  rules,
  submitDisabled,
  closeDialog,
  submitForm,
} = useLifecycleDecisionDialog(props, emit)
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="560px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div v-loading="loadingConfig">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <LifecycleDecisionTestPassFields v-if="isTestPassMode" :form="form" />
        <LifecycleDecisionApprovalPassFields v-else-if="isApprovalPassMode" :form="form" />
        <LifecycleDecisionRejectFields
          v-else
          :form="form"
          :mode="mode"
          :available-reason-categories="availableReasonCategories"
          :selected-reason-prompt="selectedReasonPrompt"
        />

        <template v-if="(isTestPassMode || isApprovalPassMode) && canConfirmOnBehalf">
          <el-divider />
          <p class="decision-intro">{{ t('templates.lifecycle.decisionForm.exceptionIntro') }}</p>
          <el-form-item>
            <el-checkbox v-model="form.exceptionIntervention">
              {{ t('templates.lifecycle.decisionForm.exceptionIntervention') }}
            </el-checkbox>
          </el-form-item>
          <template v-if="form.exceptionIntervention">
            <el-form-item
              :label="t('templates.lifecycle.decisionForm.exceptionReason')"
              required
            >
              <el-input
                v-model="form.exceptionReason"
                type="textarea"
                :rows="3"
                maxlength="2048"
                show-word-limit
                :placeholder="t('templates.lifecycle.decisionForm.exceptionReasonPlaceholder')"
              />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="form.secondaryConfirmed">
                {{ t('templates.lifecycle.decisionForm.exceptionSecondaryConfirm') }}
              </el-checkbox>
            </el-form-item>
          </template>
        </template>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="closeDialog">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" :disabled="submitDisabled" @click="submitForm">
        {{ t('templates.lifecycle.decisionForm.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.decision-intro {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
