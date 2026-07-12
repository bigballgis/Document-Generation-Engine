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

export type LifecycleDecisionDialogProps = {
  modelValue: boolean
  mode: LifecycleDecisionDialogMode
  templateId?: string
  loading?: boolean
  initialComment?: string
}

export type LifecycleDecisionDialogEmit = {
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: LifecycleDecisionSubmitPayload): void
}

export function useLifecycleDecisionDialog(
  props: LifecycleDecisionDialogProps,
  emit: LifecycleDecisionDialogEmit,
) {
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

  return {
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
  }
}
