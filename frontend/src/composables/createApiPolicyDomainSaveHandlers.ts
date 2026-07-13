import { type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import {
  buildUpsertPayloadForDomain,
  type ApiPolicyDomainFormMap,
} from '@/types/apiPolicyDomain'
import type { ApiPolicy, ApiPolicyImpactPreview } from '@/types/template'
import type { ApiPolicyDomainEditorForms } from '@/composables/useApiPolicyDomainEditorActions'

type Translate = (key: string) => string
type HasKey = (key: string) => boolean

export function createApiPolicyDomainSaveHandlers(options: {
  t: Translate
  te: HasKey
  templateId: Ref<string>
  apiPolicy: Ref<ApiPolicy | null>
  canEdit: Ref<boolean>
  forms: ApiPolicyDomainEditorForms
  canSaveRetention: Ref<boolean>
  retentionSaveFeedback: Ref<'success' | 'error' | null>
  lastImpactPreview: Ref<ApiPolicyImpactPreview | null>
  saveBlockedByImpact: Ref<boolean>
}) {
  const {
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
  } = options
  const apiPolicyStore = useApiPolicyStore()

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

  async function saveDomainWithImpactPreview<
    D extends 'DEFAULT_ROUTE_TARGET' | 'OUTPUT_POLICY' | 'BATCH_LIMIT',
  >(domain: D, form: ApiPolicyDomainFormMap[D], fallbackConfirmKey: string) {
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

  return {
    resolveErrorMessage,
    saveAdGroupsDomain,
    saveDefaultRouteDomain,
    saveRetentionDomain,
    saveOutputDomain,
    saveBatchDomain,
    saveEncryptionDomain,
  }
}
