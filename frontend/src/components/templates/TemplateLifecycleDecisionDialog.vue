<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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
      <template v-if="isTestPassMode">
        <p class="decision-intro">{{ t('templates.lifecycle.decisionForm.passTestIntro') }}</p>
        <el-form-item>
          <el-checkbox
            v-model="form.fidelityViewedConfirmed"
            data-testid="confirm-fidelity-viewed"
          >
            {{ t('templates.lifecycle.decisionForm.confirmFidelityViewed') }}
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.coverageViewedConfirmed">
            {{ t('templates.lifecycle.decisionForm.confirmCoverageViewed') }}
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.previewViewedConfirmed">
            {{ t('templates.lifecycle.decisionForm.confirmPreviewViewed') }}
          </el-checkbox>
        </el-form-item>
      </template>

      <template v-else-if="isApprovalPassMode">
        <p class="decision-intro">{{ t('templates.lifecycle.decisionForm.approveIntro') }}</p>
        <el-form-item
          :label="t('templates.lifecycle.decisionForm.approvalRationale')"
          prop="commentSummary"
          required
        >
          <el-input
            v-model="form.commentSummary"
            type="textarea"
            :rows="3"
            maxlength="2048"
            show-word-limit
            :placeholder="t('templates.lifecycle.decisionForm.approvalRationalePlaceholder')"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox
            v-model="form.fidelityViewedConfirmed"
            data-testid="confirm-fidelity-viewed"
          >
            {{ t('templates.lifecycle.decisionForm.confirmFidelityViewed') }}
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.keyEvidenceConfirmed">
            {{ t('templates.lifecycle.decisionForm.confirmKeyEvidence') }}
          </el-checkbox>
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item
          :label="t('templates.lifecycle.decisionForm.reasonCategory')"
          prop="reasonCategory"
          required
        >
          <el-select
            v-model="form.reasonCategory"
            filterable
            :placeholder="t('templates.lifecycle.decisionForm.reasonCategoryPlaceholder')"
            style="width: 100%"
          >
            <el-option
              v-for="category in availableReasonCategories"
              :key="category"
              :label="t(`templates.lifecycle.decisionForm.reasonCategories.${category}`)"
              :value="category"
            />
          </el-select>
        </el-form-item>
        <p v-if="selectedReasonPrompt" class="decision-intro">{{ selectedReasonPrompt }}</p>
        <el-form-item
          :label="t('templates.lifecycle.decisionForm.impactSummary')"
          prop="impactSummary"
          required
        >
          <el-input
            v-model="form.impactSummary"
            type="textarea"
            :rows="4"
            maxlength="2048"
            show-word-limit
            :placeholder="t('templates.lifecycle.decisionForm.impactSummaryPlaceholder')"
          />
        </el-form-item>
        <template v-if="mode === 'approval-reject' || mode === 'test-fail'">
          <p class="decision-intro">{{ t('templates.lifecycle.decisionForm.remediationIntro') }}</p>
          <el-form-item :label="t('templates.lifecycle.decisionForm.remediationTestRecordId')">
            <el-input v-model="form.remediationTestRecordId" maxlength="64" />
          </el-form-item>
          <el-form-item :label="t('templates.lifecycle.decisionForm.remediationChangeDiffRef')">
            <el-input v-model="form.remediationChangeDiffRef" maxlength="64" />
          </el-form-item>
          <el-form-item :label="t('templates.lifecycle.decisionForm.remediationChecklistCode')">
            <el-input v-model="form.remediationChecklistCode" maxlength="64" />
          </el-form-item>
        </template>
        <el-form-item :label="t('templates.lifecycle.decisionForm.optionalComment')">
          <el-input
            v-model="form.commentSummary"
            type="textarea"
            :rows="2"
            maxlength="2048"
            :placeholder="t('templates.lifecycle.commentPlaceholder')"
          />
        </el-form-item>
      </template>

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
