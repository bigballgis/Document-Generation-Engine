import { computed, ref, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useCredentialStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { apiPolicyDetailPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { ApiCredentialSummary, TemplateDetail } from '@/types/template'

export interface UseTemplatePolicyCredentialsOptions {
  templateId: ComputedRef<string>
  template: ComputedRef<TemplateDetail | null>
  errorMessage: ComputedRef<string>
}

export function useTemplatePolicyCredentials(options: UseTemplatePolicyCredentialsOptions) {
  const { templateId, template, errorMessage } = options

  const { t } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const { manageApiPolicy } = useCapabilities()
  const { confirmAction } = useConfirmAction()

  const policyLoadFailed = ref(false)
  const credentialSecretDialogVisible = ref(false)
  const credentialSecretValue = ref('')
  const credentialSecretExternalId = ref('')

  const credentialsSource = computed(() => templatesStore.credentials)
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

  const showPolicyPanel = computed(
    () => template.value?.lifecycleStatus === 'PUBLISHED' && canPolicy.value,
  )

  const displayedCredentialSecret = computed(() => {
    if (templatesStore.lastCreatedCredential?.secret) {
      return templatesStore.lastCreatedCredential.secret
    }
    return templatesStore.lastRotatedCredential?.secret ?? ''
  })

  async function loadPolicyData() {
    policyLoadFailed.value = false
    try {
      await Promise.all([
        templatesStore.fetchApiPolicy(templateId.value),
        templatesStore.fetchCredentials(templateId.value),
      ])
    } catch {
      policyLoadFailed.value = true
    }
  }

  function openApiPolicyConsole() {
    router.push(apiPolicyDetailPath(templateId.value))
  }

  function openCredentialSecretDialog(externalId: string, secret: string) {
    credentialSecretExternalId.value = externalId
    credentialSecretValue.value = secret
    credentialSecretDialogVisible.value = true
  }

  async function handleCreateCredential() {
    try {
      const created = await templatesStore.createCredential(templateId.value)
      openCredentialSecretDialog(created.externalId, created.secret)
      ElMessage.success(t('templates.policy.createCredentialSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.createCredential'))
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
      const rotated = await templatesStore.rotateCredential(templateId.value, credentialId)
      openCredentialSecretDialog(externalId, rotated.secret)
      ElMessage.success(t('templates.policy.rotateCredentialSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.rotateCredential'))
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
      await templatesStore.revokeCredential(templateId.value, credentialId)
      ElMessage.success(t('templates.policy.revokeCredentialSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.revokeCredential'))
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
    credentialColumnFilters,
    credentialsCurrentPage,
    paginatedCredentials,
    credentialStatusFilterOptions,
    totalCredentialRows,
    sortCredentialsByCreatedAt,
    loadPolicyData,
    openApiPolicyConsole,
    openCredentialSecretDialog,
    handleCreateCredential,
    handleRotateCredential,
    handleRevokeCredential,
    resetPolicyCredentialsTransientState,
  }
}
