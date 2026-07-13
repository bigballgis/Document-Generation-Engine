import { computed, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  DOCUMENT_RETENTION_PRESETS,
  INVOCATION_RECORD_RETENTION_PRESETS,
} from '@/constants/apiPolicyPlatformDefaults'
import {
  type AdGroupsDomainForm,
  type BatchLimitsDomainForm,
  type DefaultRouteDomainForm,
  type EncryptionDomainForm,
  type OutputPolicyDomainForm,
  type InvocationRetentionDomainForm,
} from '@/types/apiPolicyDomain'
import type { ApiPolicy, ApiPolicyImpactPreview } from '@/types/template'
import { createApiPolicyDomainSaveHandlers } from '@/composables/createApiPolicyDomainSaveHandlers'

export interface ApiPolicyDomainEditorForms {
  adGroupsForm: AdGroupsDomainForm
  defaultRouteForm: DefaultRouteDomainForm
  retentionForm: InvocationRetentionDomainForm
  outputForm: OutputPolicyDomainForm
  batchForm: BatchLimitsDomainForm
  encryptionForm: EncryptionDomainForm
}

export function useApiPolicyDomainEditorActions(
  templateId: Ref<string>,
  apiPolicy: Ref<ApiPolicy | null>,
  canEdit: Ref<boolean>,
  forms: ApiPolicyDomainEditorForms,
) {
  const { t, te } = useI18n()

  const retentionSaveFeedback = ref<'success' | 'error' | null>(null)
  const lastImpactPreview = ref<ApiPolicyImpactPreview | null>(null)
  const saveBlockedByImpact = computed(() => lastImpactPreview.value?.blocking === true)

  const recordRetentionOptions = computed(() =>
    INVOCATION_RECORD_RETENTION_PRESETS.map((days) => ({
      value: days,
      label: t('templates.policy.retention.presetDays', { days }),
    })),
  )

  const documentRetentionOptions = computed(() =>
    DOCUMENT_RETENTION_PRESETS.map((days) => ({
      value: days,
      label: t('templates.policy.retention.presetDays', { days }),
    })),
  )

  const retentionBaseline = computed(() => ({
    saveGeneratedDocuments: apiPolicy.value?.saveGeneratedDocuments ?? false,
    invocationRecordRetentionDays: apiPolicy.value?.invocationRecordRetentionDays ?? 90,
    documentRetentionDays: apiPolicy.value?.documentRetentionDays ?? 30,
  }))

  const retentionDirty = computed(() => {
    if (!apiPolicy.value) {
      return false
    }
    const baseline = retentionBaseline.value
    return (
      forms.retentionForm.saveGeneratedDocuments !== baseline.saveGeneratedDocuments ||
      forms.retentionForm.invocationRecordRetentionDays !== baseline.invocationRecordRetentionDays ||
      forms.retentionForm.documentRetentionDays !== baseline.documentRetentionDays
    )
  })

  const retentionPresetsValid = computed(
    () =>
      INVOCATION_RECORD_RETENTION_PRESETS.includes(
        forms.retentionForm.invocationRecordRetentionDays as (typeof INVOCATION_RECORD_RETENTION_PRESETS)[number],
      ) &&
      DOCUMENT_RETENTION_PRESETS.includes(
        forms.retentionForm.documentRetentionDays as (typeof DOCUMENT_RETENTION_PRESETS)[number],
      ),
  )

  const canSaveRetention = computed(
    () => canEdit.value && retentionDirty.value && retentionPresetsValid.value,
  )

  const saveHandlers = createApiPolicyDomainSaveHandlers({
    t,
    te,
    templateId,
    apiPolicy,
    canEdit,
    forms,
    canSaveRetention,
    retentionSaveFeedback,
    lastImpactPreview,
    saveBlockedByImpact,
  })

  function clearRetentionSaveFeedback() {
    retentionSaveFeedback.value = null
  }

  function clearLastImpactPreview() {
    lastImpactPreview.value = null
  }

  return {
    retentionSaveFeedback,
    lastImpactPreview,
    saveBlockedByImpact,
    recordRetentionOptions,
    documentRetentionOptions,
    retentionDirty,
    retentionPresetsValid,
    canSaveRetention,
    ...saveHandlers,
    clearRetentionSaveFeedback,
    clearLastImpactPreview,
  }
}
