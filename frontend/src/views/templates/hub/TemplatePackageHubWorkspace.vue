<script setup lang="ts">
import { ref } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import TemplateVersionLinesPanel from '@/components/templates/TemplateVersionLinesPanel.vue'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import TemplateDetailApiAccessTab from '@/views/templates/detail/TemplateDetailApiAccessTab.vue'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import type { WorkspaceTabOption } from '@/components/common/WorkspaceTabShell.vue'
import type { RuntimeEnvironment } from '@/config/environments'
import type { ApiCredentialSummary, ApiPolicy, TemplateDetail } from '@/types/template'

defineProps<{
  templateId: string
  template: TemplateDetail
  canClone: boolean
  canManageVersions: boolean
  hubWorkspaceTabs: WorkspaceTabOption[]
  showPolicyPanel: boolean
  loadingPolicy: boolean
  apiPolicy: ApiPolicy | null
  policyLoadFailed: boolean
  policyLoadErrorKey: string | null
  paginatedCredentials: ApiCredentialSummary[]
  credentialStatusFilterOptions: Array<{ label: string; value: string }>
  pageSize: number
  totalCredentialRows: number
  submitting: boolean
  formatDateTime: (value: string) => string
  sortCredentialsByCreatedAt: (a: ApiCredentialSummary, b: ApiCredentialSummary) => number
}>()

const activeHubTab = defineModel<string>('activeHubTab', { required: true })
const credentialColumnFilters = defineModel<Record<string, string>>('credentialColumnFilters', {
  required: true,
})
const credentialsCurrentPage = defineModel<number>('credentialsCurrentPage', { required: true })
const selectedContractEnvironment = defineModel<RuntimeEnvironment>('selectedContractEnvironment', {
  required: true,
})

const emit = defineEmits<{
  cloned: []
  changed: []
  createCredential: []
  rotateCredential: [credentialId: string, externalId: string]
  revokeCredential: [credentialId: string]
  retryPolicyLoad: []
}>()

const versionLinesPanelRef = ref<ComponentPublicInstance<{ reload: () => Promise<void> }> | null>(
  null,
)
const apiAccessTabRef = ref<ComponentPublicInstance<{
  revealCredentialSecret: (externalId: string, secret: string) => void
}> | null>(null)

defineExpose({
  reloadVersionLines: () => versionLinesPanelRef.value?.reload(),
  revealCredentialSecret: (externalId: string, secret: string) => {
    apiAccessTabRef.value?.revealCredentialSecret(externalId, secret)
  },
})
</script>

<template>
  <TemplateVersionLinesPanel
    ref="versionLinesPanelRef"
    :template-id="templateId"
    :can-clone="canClone"
    :can-manage-versions="canManageVersions"
    @cloned="emit('cloned')"
    @changed="emit('changed')"
  />

  <WorkspaceTabShell v-model="activeHubTab" :tabs="hubWorkspaceTabs">
    <template #overview>
      <TemplateDetailOverviewTab :template="template" :format-date-time="formatDateTime" />
    </template>

    <template #apiAccess>
      <TemplateDetailApiAccessTab
        ref="apiAccessTabRef"
        v-model:credential-column-filters="credentialColumnFilters"
        v-model:credentials-current-page="credentialsCurrentPage"
        v-model:selected-contract-environment="selectedContractEnvironment"
        :template-id="templateId"
        :show-policy-panel="showPolicyPanel"
        :loading-policy="loadingPolicy"
        :api-policy="apiPolicy"
        :policy-load-failed="policyLoadFailed"
        :policy-load-error-key="policyLoadErrorKey"
        :paginated-credentials="paginatedCredentials"
        :credential-status-filter-options="credentialStatusFilterOptions"
        :page-size="pageSize"
        :total-credential-rows="totalCredentialRows"
        :submitting="submitting"
        :format-date-time="formatDateTime"
        :sort-credentials-by-created-at="sortCredentialsByCreatedAt"
        @create-credential="emit('createCredential')"
        @rotate-credential="(credentialId, externalId) => emit('rotateCredential', credentialId, externalId)"
        @revoke-credential="emit('revokeCredential', $event)"
        @retry-policy-load="emit('retryPolicyLoad')"
      />
    </template>
  </WorkspaceTabShell>
</template>
