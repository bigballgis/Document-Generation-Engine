import { computed, ref, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useCredentialStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import type { ApiCredentialSummary, TemplateDetail } from '@/types/template'

export interface UseTemplatePolicyCredentialsOptions {
  templateId: ComputedRef<string>
  template: ComputedRef<TemplateDetail | null>
  errorMessage: ComputedRef<string>
  /** When set, secrets are revealed via this callback instead of the local secret dialog. */
  revealSecret?: (externalId: string, secret: string) => void
}

export function useTemplatePolicyCredentials(options: UseTemplatePolicyCredentialsOptions) {
  const { templateId, template, revealSecret } = options

  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const apiPolicyStore = useApiPolicyStore()
  const { manageApiPolicy } = useCapabilities()
  const { confirmAction } = useConfirmAction()

  const policyLoadFailed = ref(false)
  const credentialSecretDialogVisible = ref(false)
  const credentialSecretValue = ref('')
  const credentialSecretExternalId = ref('')

  const credentialsSource = computed(() => apiPolicyStore.credentials)
  const { filters: credentialColumnFilters, filteredRows: filteredCredentials } = useDataTableFilters(
    credentialsSource,
    [
      { key: 'externalId', getValue: (row) => row.externalId },
      { key: 'status', getValue: (row) => row.status, matchMode: 'exact' },
      { key: 'createdAt', getValue: (row) => formatDateTime(row.createdAt) },
    ],
  )
  const credentialsCurrentPage = ref(1)
  const { paginatedRows: paginatedCredentials, totalRows: totalCredentialRows } = useCatalogPagination(
    filteredCredentials,
    credentialsCurrentPage,
    CLIENT_TABLE_PAGE_SIZE,
  )
  const sortCredentialsByCreatedAt = rowSortMethod<ApiCredentialSummary>((row) => row.createdAt)
  const credentialStatusFilterOptions = useCredentialStatusFilterOptions()

  const canPolicy = computed(() => manageApiPolicy.value)

  const showPolicyPanel = computed(() => {
    const status = template.value?.lifecycleStatus
    return (
      canPolicy.value && (status === 'PUBLISHED' || status === 'PENDING_RELEASE')
    )
  })

  const displayedCredentialSecret = computed(() => {
    if (apiPolicyStore.lastCreatedCredential?.secret) {
      return apiPolicyStore.lastCreatedCredential.secret
    }
    return apiPolicyStore.lastRotatedCredential?.secret ?? ''
  })

  const apiPolicy = computed(() => apiPolicyStore.apiPolicy)
  const loadingPolicy = computed(() => apiPolicyStore.loadingPolicy)
  const policySubmitting = computed(() => apiPolicyStore.submitting)
  const policyLoadErrorKey = computed(() => apiPolicyStore.lastErrorMessageKey)

  function resolvePolicyErrorMessage(fallbackKey: string): string {
    const key = apiPolicyStore.lastErrorMessageKey
    if (!key) {
      return t(fallbackKey)
    }
    return te(key) ? t(key) : t(fallbackKey)
  }

  async function loadPolicyData() {
    policyLoadFailed.value = false
    apiPolicyStore.setActiveTemplate(templateId.value)
    try {
      await Promise.all([
        apiPolicyStore.fetchPolicy(templateId.value),
        apiPolicyStore.fetchCredentials(templateId.value),
      ])
    } catch {
      policyLoadFailed.value = true
    }
  }

  function openCredentialSecretDialog(externalId: string, secret: string) {
    credentialSecretExternalId.value = externalId
    credentialSecretValue.value = secret
    credentialSecretDialogVisible.value = true
  }

  function revealCredentialSecret(externalId: string, secret: string) {
    if (revealSecret) {
      revealSecret(externalId, secret)
      return
    }
    openCredentialSecretDialog(externalId, secret)
  }

  async function handleCreateCredential() {
    try {
      const created = await apiPolicyStore.createCredential(templateId.value)
      revealCredentialSecret(created.externalId, created.secret)
      ElMessage.success(t('templates.policy.createCredentialSuccess'))
    } catch {
      ElMessage.error(resolvePolicyErrorMessage('templates.error.createCredential'))
    }
  }

  async function handleRotateCredential(credentialId: string, externalId: string) {
    const confirmed = await confirmAction({
      titleKey: 'templates.policy.confirmRotateTitle',
      messageKey: 'templates.policy.confirmRotateMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }
    try {
      const rotated = await apiPolicyStore.rotateCredential(templateId.value, credentialId)
      revealCredentialSecret(externalId, rotated.secret)
      ElMessage.success(t('templates.policy.rotateCredentialSuccess'))
    } catch {
      ElMessage.error(resolvePolicyErrorMessage('templates.error.rotateCredential'))
    }
  }

  async function handleRevokeCredential(credentialId: string) {
    const confirmed = await confirmAction({
      titleKey: 'templates.policy.confirmRevokeTitle',
      messageKey: 'templates.policy.confirmRevokeMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }
    try {
      await apiPolicyStore.revokeCredential(templateId.value, credentialId)
      ElMessage.success(t('templates.policy.revokeCredentialSuccess'))
    } catch {
      ElMessage.error(resolvePolicyErrorMessage('templates.error.revokeCredential'))
    }
  }

  function resetPolicyCredentialsTransientState() {
    policyLoadFailed.value = false
  }

  return {
    canPolicy,
    showPolicyPanel,
    policyLoadFailed,
    credentialSecretDialogVisible,
    credentialSecretValue,
    credentialSecretExternalId,
    displayedCredentialSecret,
    apiPolicy,
    loadingPolicy,
    policySubmitting,
    policyLoadErrorKey,
    credentialColumnFilters,
    credentialsCurrentPage,
    paginatedCredentials,
    credentialStatusFilterOptions,
    totalCredentialRows,
    sortCredentialsByCreatedAt,
    loadPolicyData,
    openCredentialSecretDialog,
    handleCreateCredential,
    handleRotateCredential,
    handleRevokeCredential,
    resetPolicyCredentialsTransientState,
  }
}
