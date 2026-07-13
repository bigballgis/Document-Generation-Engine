import type { FormInstance } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCapabilities } from '@/composables/useCapabilities'
import { MANAGEMENT_ROLES } from '@/auth/roles'
import * as templateRiskPromptApi from '@/api/templateRiskPromptConfig'
import { TEMPLATE_DECISION_REASON_CATEGORIES } from '@/utils/templateLifecycleDecisionForm'
import {
  buildLifecycleDecisionSubmitPayload,
  resetLifecycleDecisionForm,
  type LifecycleDecisionDialogEmit,
  type LifecycleDecisionDialogProps,
  type LifecycleDecisionFormState,
} from '@/components/templates/lifecycleDecisionDialogTypes'
import {
  createLifecycleDecisionDialogDerived,
  submitLifecycleDecisionForm,
} from '@/components/templates/createLifecycleDecisionDialogDerived'

export type {
  LifecycleDecisionDialogMode,
  LifecycleDecisionSubmitPayload,
  LifecycleDecisionDialogProps,
  LifecycleDecisionDialogEmit,
} from '@/components/templates/lifecycleDecisionDialogTypes'

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

  const form = reactive<LifecycleDecisionFormState>({
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

  const derived = createLifecycleDecisionDialogDerived({
    t,
    mode: () => props.mode,
    form,
    canConfirmOnBehalf,
  })

  watch(
    () => props.modelValue,
    (open) => {
      if (open) {
        resetLifecycleDecisionForm(form, props.initialComment)
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

  async function submitForm() {
    await submitLifecycleDecisionForm({
      submitDisabled: derived.submitDisabled,
      isNegativeMode: derived.isNegativeMode,
      isApprovalPassMode: derived.isApprovalPassMode,
      formRef,
      form,
      mode: props.mode,
      buildPayload: () => buildLifecycleDecisionSubmitPayload(props.mode, form),
      emitSubmit: (payload) =>
        emit('submit', payload as ReturnType<typeof buildLifecycleDecisionSubmitPayload>),
    })
  }

  return {
    formRef,
    availableReasonCategories,
    loadingConfig,
    selectedReasonPrompt,
    visible,
    canConfirmOnBehalf,
    isTestPassMode: derived.isTestPassMode,
    isApprovalPassMode: derived.isApprovalPassMode,
    form,
    dialogTitle: derived.dialogTitle,
    rules: derived.rules,
    submitDisabled: derived.submitDisabled,
    closeDialog,
    submitForm,
  }
}
