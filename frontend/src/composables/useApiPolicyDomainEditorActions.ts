import { computed, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DOCUMENT_RETENTION_PRESETS,
  INVOCATION_RECORD_RETENTION_PRESETS,
} from '@/constants/apiPolicyPlatformDefaults'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import {
  buildUpsertPayloadForDomain,
  type AdGroupsDomainForm,
  type ApiPolicyDomainFormMap,
  type BatchLimitsDomainForm,
  type DefaultRouteDomainForm,
  type EncryptionDomainForm,
  type OutputPolicyDomainForm,
  type InvocationRetentionDomainForm,
} from '@/types/apiPolicyDomain'
import type { ApiPolicy, ApiPolicyImpactPreview } from '@/types/template'

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
  const apiPolicyStore = useApiPolicyStore()

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

  function resolveErrorMessage(fallbackKey: string): string {
    const key = apiPolicyStore.entryFor(templateId.value).lastErrorMessageKey
    if (!key) {
      return t(fallbackKey)
    }
    return te(key) ? t(key) : t(fallbackKey)
  }

  async function confirmSave(messageKey: string): Promise<boolean> {
    try {
      await ElMessageBox.confirm(t(messageKey), t('apiPolicy.detail.impact.confirmTitle'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      })
      return true
    } catch {
      return false
    }
  }

  async function saveAdGroupsDomain() {
    if (!canEdit.value) {
      return
    }
    if (!(await confirmSave('templates.policy.retention.confirmSaveAdGroups'))) {
      return
    }
    try {
      await apiPolicyStore.savePolicyDomain(
        templateId.value,
        'AD_GROUP_AUTHORIZATION',
        forms.adGroupsForm,
      )
      ElMessage.success(t('apiPolicy.detail.saveSuccess'))
    } catch {
      ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
    }
  }

  async function saveDomainWithImpactPreview<D extends 'DEFAULT_ROUTE_TARGET' | 'OUTPUT_POLICY' | 'BATCH_LIMIT'>(
    domain: D,
    form: ApiPolicyDomainFormMap[D],
    fallbackConfirmKey: string,
  ) {
    if (!canEdit.value || !apiPolicy.value || saveBlockedByImpact.value) {
      return
    }
    try {
      const payload = buildUpsertPayloadForDomain(apiPolicy.value, domain, form)
      const preview = await apiPolicyStore.previewImpact(templateId.value, payload)
      lastImpactPreview.value = preview
      if (preview.blocking) {
        ElMessage.error(t('apiPolicy.detail.impact.blockedSave'))
        return
      }
      if (preview.warnings.length > 0) {
        const warningLines = preview.warnings.map((key) => (te(key) ? t(key) : key))
        try {
          await ElMessageBox.confirm(
            [t('apiPolicy.detail.impact.confirmWarningsIntro'), ...warningLines].join('\n\n'),
            t('apiPolicy.detail.impact.confirmTitle'),
            {
              confirmButtonText: t('common.confirm'),
              cancelButtonText: t('common.cancel'),
              type: 'warning',
            },
          )
        } catch {
          return
        }
      } else if (!(await confirmSave(fallbackConfirmKey))) {
        return
      }
      await apiPolicyStore.savePolicyDomain(templateId.value, domain, form)
      lastImpactPreview.value = null
      ElMessage.success(t('apiPolicy.detail.saveSuccess'))
    } catch {
      ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
    }
  }

  async function saveDefaultRouteDomain() {
    await saveDomainWithImpactPreview(
      'DEFAULT_ROUTE_TARGET',
      forms.defaultRouteForm,
      'templates.policy.retention.confirmSaveDefaultRoute',
    )
  }

  async function saveRetentionDomain() {
    if (!canSaveRetention.value) {
      return
    }
    if (!(await confirmSave('templates.policy.retention.confirmSaveRetention'))) {
      return
    }
    retentionSaveFeedback.value = null
    try {
      await apiPolicyStore.saveInvocationRetentionDomain(templateId.value, {
        ...forms.retentionForm,
      })
      retentionSaveFeedback.value = 'success'
    } catch {
      retentionSaveFeedback.value = 'error'
      ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
    }
  }

  async function saveOutputDomain() {
    await saveDomainWithImpactPreview(
      'OUTPUT_POLICY',
      forms.outputForm,
      'templates.policy.retention.confirmSaveAdvanced',
    )
  }

  async function saveBatchDomain() {
    await saveDomainWithImpactPreview(
      'BATCH_LIMIT',
      forms.batchForm,
      'templates.policy.retention.confirmSaveAdvanced',
    )
  }

  async function saveEncryptionDomain() {
    if (!canEdit.value) {
      return
    }
    if (!(await confirmSave('templates.policy.retention.confirmSaveAdvanced'))) {
      return
    }
    try {
      await apiPolicyStore.savePolicyDomain(
        templateId.value,
        'ENCRYPTION_CAPABILITY',
        forms.encryptionForm,
      )
      ElMessage.success(t('apiPolicy.detail.saveSuccess'))
    } catch {
      ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
    }
  }

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
    resolveErrorMessage,
    saveAdGroupsDomain,
    saveDefaultRouteDomain,
    saveRetentionDomain,
    saveOutputDomain,
    saveBatchDomain,
    saveEncryptionDomain,
    clearRetentionSaveFeedback,
    clearLastImpactPreview,
  }
}

