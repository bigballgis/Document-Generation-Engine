import { computed, reactive, watch, type Ref } from 'vue'
import {
  createDomainFormFromPolicy,
  createRetentionFormFromPolicy,
  type AdGroupsDomainForm,
  type ApiPolicyDomain,
  type ApiPolicyDomainFormMap,
  type BatchLimitsDomainForm,
  type DefaultRouteDomainForm,
  type EncryptionDomainForm,
  type OutputPolicyDomainForm,
} from '@/types/apiPolicyDomain'
import type { ApiPolicy } from '@/types/template'

export function useApiPolicyDomainForms(apiPolicy: Ref<ApiPolicy | null>) {
  const adGroupsForm = reactive<AdGroupsDomainForm>({ allowedAdGroups: [] })
  const defaultRouteForm = reactive<DefaultRouteDomainForm>({ defaultRouteReleaseVersion: '' })
  const retentionForm = reactive(
    createRetentionFormFromPolicy({
      templateId: '',
      policyVersion: 0,
      allowedAdGroups: [],
      defaultRouteReleaseVersion: '',
      outputFormats: [],
      outputModes: [],
      batchEnabled: false,
      maxBatchSize: 1,
      docxEncryptionEnabled: false,
      pdfEncryptionEnabled: false,
      saveGeneratedDocuments: true,
      invocationRecordRetentionDays: 90,
      documentRetentionDays: 30,
      updatedAt: '',
    }),
  )
  const outputForm = reactive<OutputPolicyDomainForm>({ outputFormats: [], outputModes: [] })
  const batchForm = reactive<BatchLimitsDomainForm>({
    batchEnabled: false,
    syncMaxItems: 100,
    asyncMaxItems: 10_000,
  })
  const encryptionForm = reactive<EncryptionDomainForm>({
    docxEncryptionEnabled: false,
    pdfEncryptionEnabled: false,
  })

  const allowedAdGroupsText = computed(() => apiPolicy.value?.allowedAdGroups.join(', ') ?? '')

  function syncFormsFromPolicy() {
    const policy = apiPolicy.value
    if (!policy) {
      return
    }
    Object.assign(adGroupsForm, createDomainFormFromPolicy(policy, 'AD_GROUP_AUTHORIZATION'))
    Object.assign(defaultRouteForm, createDomainFormFromPolicy(policy, 'DEFAULT_ROUTE_TARGET'))
    Object.assign(retentionForm, createRetentionFormFromPolicy(policy))
    Object.assign(outputForm, createDomainFormFromPolicy(policy, 'OUTPUT_POLICY'))
    Object.assign(batchForm, createDomainFormFromPolicy(policy, 'BATCH_LIMIT'))
    Object.assign(encryptionForm, createDomainFormFromPolicy(policy, 'ENCRYPTION_CAPABILITY'))
  }

  function domainCandidate<D extends ApiPolicyDomain>(domain: D): ApiPolicyDomainFormMap[D] {
    switch (domain) {
      case 'AD_GROUP_AUTHORIZATION':
        return adGroupsForm as ApiPolicyDomainFormMap[D]
      case 'OUTPUT_POLICY':
        return outputForm as ApiPolicyDomainFormMap[D]
      case 'BATCH_LIMIT':
        return batchForm as ApiPolicyDomainFormMap[D]
      case 'ENCRYPTION_CAPABILITY':
        return encryptionForm as ApiPolicyDomainFormMap[D]
      case 'DEFAULT_ROUTE_TARGET':
        return defaultRouteForm as ApiPolicyDomainFormMap[D]
      default:
        return adGroupsForm as ApiPolicyDomainFormMap[D]
    }
  }

  function currentSummary(domain: ApiPolicyDomain, t: (key: string, params?: Record<string, unknown>) => string): string {
    const policy = apiPolicy.value
    if (!policy) {
      return ''
    }
    switch (domain) {
      case 'AD_GROUP_AUTHORIZATION':
        return policy.allowedAdGroups.join(', ') || t('apiPolicy.detail.summary.empty')
      case 'OUTPUT_POLICY':
        return `${policy.outputFormats.join(', ')} / ${policy.outputModes.join(', ')}`
      case 'BATCH_LIMIT':
        return policy.batchEnabled
          ? t('apiPolicy.detail.summary.batchEnabled', {
              sync: policy.batchSyncMaxItems ?? policy.maxBatchSize,
              async: policy.batchAsyncMaxItems ?? 10_000,
            })
          : t('apiPolicy.detail.summary.batchDisabled')
      case 'ENCRYPTION_CAPABILITY':
        return (
          [policy.docxEncryptionEnabled ? 'DOCX' : null, policy.pdfEncryptionEnabled ? 'PDF' : null]
            .filter(Boolean)
            .join(', ') || t('apiPolicy.detail.summary.encryptionNone')
        )
      case 'DEFAULT_ROUTE_TARGET':
        return policy.defaultRouteReleaseVersion || t('apiPolicy.detail.summary.empty')
      default:
        return ''
    }
  }

  watch(apiPolicy, syncFormsFromPolicy, { immediate: true })

  return {
    adGroupsForm,
    defaultRouteForm,
    retentionForm,
    outputForm,
    batchForm,
    encryptionForm,
    allowedAdGroupsText,
    syncFormsFromPolicy,
    domainCandidate,
    currentSummary,
  }
}
