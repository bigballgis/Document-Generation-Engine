import type { FormInstance, FormRules } from 'element-plus'
import { computed, type ComputedRef, type Ref } from 'vue'
import type { ComposerTranslation } from 'vue-i18n'
import {
  isApprovalPassDecisionValid,
  isLifecycleDecisionFormValid,
  isRejectDecisionValid,
  isTestPassDecisionValid,
} from '@/utils/templateLifecycleDecisionForm'
import type {
  LifecycleDecisionDialogMode,
  LifecycleDecisionFormState,
} from '@/components/templates/lifecycleDecisionDialogTypes'

export function createLifecycleDecisionDialogDerived(deps: {
  t: ComposerTranslation
  mode: () => LifecycleDecisionDialogMode
  form: LifecycleDecisionFormState
  canConfirmOnBehalf: ComputedRef<boolean>
}) {
  const { t, mode, form } = deps

  const isNegativeMode = computed(
    () => mode() === 'test-fail' || mode() === 'approval-reject',
  )
  const isTestPassMode = computed(() => mode() === 'test-pass')
  const isApprovalPassMode = computed(() => mode() === 'approval-approve')

  const dialogTitle = computed(() => {
    switch (mode()) {
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
    if (mode() === 'approval-reject' || mode() === 'test-fail') {
      return !isRejectDecisionValid(form)
    }
    return !isLifecycleDecisionFormValid(form)
  })

  return {
    isNegativeMode,
    isTestPassMode,
    isApprovalPassMode,
    dialogTitle,
    rules,
    submitDisabled,
  }
}

export async function submitLifecycleDecisionForm(deps: {
  submitDisabled: ComputedRef<boolean>
  isNegativeMode: ComputedRef<boolean>
  isApprovalPassMode: ComputedRef<boolean>
  formRef: Ref<FormInstance | undefined>
  form: LifecycleDecisionFormState
  mode: LifecycleDecisionDialogMode
  buildPayload: () => unknown
  emitSubmit: (payload: unknown) => void
}) {
  if (deps.submitDisabled.value) {
    return
  }
  const payload = deps.buildPayload()
  if (deps.isNegativeMode.value && deps.formRef.value) {
    await deps.formRef.value.validate((valid) => {
      if (!valid) {
        return
      }
      deps.emitSubmit(payload)
    })
    return
  }
  if (deps.isApprovalPassMode.value && deps.formRef.value) {
    await deps.formRef.value.validate((valid) => {
      if (!valid || !isApprovalPassDecisionValid(deps.form)) {
        return
      }
      deps.emitSubmit(payload)
    })
    return
  }
  deps.emitSubmit(payload)
}
