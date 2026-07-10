<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCapabilities } from '@/composables/useCapabilities'
import { MANAGEMENT_ROLES } from '@/auth/roles'
import * as templateRiskPromptApi from '@/api/templateRiskPromptConfig'
import {
  TEMPLATE_DECISION_REASON_CATEGORIES,
  isApprovalPassDecisionValid,
  isLifecycleDecisionFormValid,
  isRejectDecisionValid,
  isTestPassDecisionValid,
} from '@/utils/templateLifecycleDecisionForm'

export type LifecycleDecisionDialogMode =
  | 'test-fail'
  | 'test-pass'
  | 'approval-reject'
  | 'approval-approve'

export interface LifecycleDecisionSubmitPayload {
  reasonCategory?: string
  impactSummary?: string
  commentSummary?: string
  fidelityViewedConfirmed?: boolean
  coverageViewedConfirmed?: boolean
  previewViewedConfirmed?: boolean
  keyEvidenceConfirmed?: boolean
  remediationTestRecordId?: string
  remediationChangeDiffRef?: string
  remediationChecklistCode?: string
  exceptionIntervention?: boolean
  exceptionReason?: string
  secondaryConfirmed?: boolean
}

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
const { context } = useCapabilities()
const formRef = ref<FormInstance>()
const availableReasonCategories = ref<string[]>([...TEMPLATE_DECISION_REASON_CATEGORIES])
const reasonPromptCopy = ref<Record<string, string>>({})
const loadingConfig = ref(false)

const selectedReasonPrompt = computed(() => {
  if (!form.reasonCategory) {
    return ''
  }
  return reasonPromptCopy.value[form.reasonCategory] ?? ''
})

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const canConfirmOnBehalf = computed(
  () =>
    context.value.roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN) ||
    context.value.roles.includes(MANAGEMENT_ROLES.GLOBAL_ADMIN),
)
const isNegativeMode = computed(
  () => props.mode === 'test-fail' || props.mode === 'approval-reject',
)
const isTestPassMode = computed(() => props.mode === 'test-pass')
const isApprovalPassMode = computed(() => props.mode === 'approval-approve')

const form = reactive({
  reasonCategory: '',
  impactSummary: '',
  commentSummary: '',
  fidelityViewedConfirmed: false,
  coverageViewedConfirmed: false,
  previewViewedConfirmed: false,
  keyEvidenceConfirmed: false,
  remediationTestRecordId: '',
  remediationChangeDiffRef: '',
  remediationChecklistCode: '',
  exceptionIntervention: false,
  exceptionReason: '',
  secondaryConfirmed: false,
})

const dialogTitle = computed(() => {
  switch (props.mode) {
    case 'test-fail':
      return t('templates.lifecycle.decisionForm.failTestTitle')
    case 'test-pass':
      return t('templates.lifecycle.decisionForm.passTestTitle')
    case 'approval-reject':
      return t('templates.lifecycle.decisionForm.rejectTitle')
    case 'approval-approve':
      return t('templates.lifecycle.decisionForm.approveTitle')
    default:
      return t('templates.lifecycle.decisionForm.submit')
  }
})

const rules = computed<FormRules>(() => {
  if (isTestPassMode.value) {
    return {}
  }
  if (isApprovalPassMode.value) {
    return {
      commentSummary: [
        {
          required: true,
          message: t('templates.lifecycle.decisionForm.validation.rationaleRequired'),
          trigger: 'blur',
        },
      ],
    }
  }
  return {
    reasonCategory: [
      {
        required: true,
        message: t('templates.lifecycle.decisionForm.validation.reasonCategoryRequired'),
        trigger: 'change',
      },
    ],
    impactSummary: [
      {
        required: true,
        message: t('templates.lifecycle.decisionForm.validation.impactSummaryRequired'),
        trigger: 'blur',
      },
    ],
  }
})

const submitDisabled = computed(() => {
  if (isTestPassMode.value) {
    return !isTestPassDecisionValid(form)
  }
  if (isApprovalPassMode.value) {
    return !isApprovalPassDecisionValid(form)
  }
  if (props.mode === 'approval-reject' || props.mode === 'test-fail') {
    return !isRejectDecisionValid(form)
  }
  return !isLifecycleDecisionFormValid(form)
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      form.reasonCategory = ''
      form.impactSummary = ''
      form.commentSummary = props.initialComment ?? ''
      form.fidelityViewedConfirmed = false
      form.coverageViewedConfirmed = false
      form.previewViewedConfirmed = false
      form.keyEvidenceConfirmed = false
      form.remediationTestRecordId = ''
      form.remediationChangeDiffRef = ''
      form.remediationChecklistCode = ''
      form.exceptionIntervention = false
      form.exceptionReason = ''
      form.secondaryConfirmed = false
      formRef.value?.clearValidate()
      void loadDecisionFormConfig()
    }
  },
)

async function loadDecisionFormConfig() {
  if (!props.templateId) {
    availableReasonCategories.value = [...TEMPLATE_DECISION_REASON_CATEGORIES]
    reasonPromptCopy.value = {}
    return
  }
  loadingConfig.value = true
  try {
    const config = await templateRiskPromptApi.getDecisionFormConfig(props.templateId)
    availableReasonCategories.value =
      config.reasonCategories.length > 0
        ? config.reasonCategories
        : [...TEMPLATE_DECISION_REASON_CATEGORIES]
    reasonPromptCopy.value = config.riskPromptCopy ?? {}
  } catch {
    availableReasonCategories.value = [...TEMPLATE_DECISION_REASON_CATEGORIES]
    reasonPromptCopy.value = {}
  } finally {
    loadingConfig.value = false
  }
}

function closeDialog() {
  visible.value = false
}

function buildSubmitPayload(): LifecycleDecisionSubmitPayload {
  if (isTestPassMode.value) {
    return {
      fidelityViewedConfirmed: form.fidelityViewedConfirmed,
      coverageViewedConfirmed: form.coverageViewedConfirmed,
      previewViewedConfirmed: form.previewViewedConfirmed,
      commentSummary: form.commentSummary.trim() || undefined,
      exceptionIntervention: form.exceptionIntervention || undefined,
      exceptionReason: form.exceptionIntervention ? form.exceptionReason.trim() : undefined,
      secondaryConfirmed: form.exceptionIntervention ? form.secondaryConfirmed : undefined,
    }
  }
  if (isApprovalPassMode.value) {
    return {
      commentSummary: form.commentSummary.trim(),
      fidelityViewedConfirmed: form.fidelityViewedConfirmed,
      keyEvidenceConfirmed: form.keyEvidenceConfirmed,
      exceptionIntervention: form.exceptionIntervention || undefined,
      exceptionReason: form.exceptionIntervention ? form.exceptionReason.trim() : undefined,
      secondaryConfirmed: form.exceptionIntervention ? form.secondaryConfirmed : undefined,
    }
  }
  return {
    reasonCategory: form.reasonCategory.trim(),
    impactSummary: form.impactSummary.trim(),
    commentSummary: form.commentSummary.trim() || undefined,
    remediationTestRecordId: form.remediationTestRecordId.trim() || undefined,
    remediationChangeDiffRef: form.remediationChangeDiffRef.trim() || undefined,
    remediationChecklistCode: form.remediationChecklistCode.trim() || undefined,
  }
}

async function submitForm() {
  if (submitDisabled.value) {
    return
  }
  if (isNegativeMode.value && formRef.value) {
    await formRef.value.validate((valid) => {
      if (!valid) {
        return
      }
      emit('submit', buildSubmitPayload())
    })
    return
  }
  if (isApprovalPassMode.value && formRef.value) {
    await formRef.value.validate((valid) => {
      if (!valid || !isApprovalPassDecisionValid(form)) {
        return
      }
      emit('submit', buildSubmitPayload())
    })
    return
  }
  emit('submit', buildSubmitPayload())
}
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
