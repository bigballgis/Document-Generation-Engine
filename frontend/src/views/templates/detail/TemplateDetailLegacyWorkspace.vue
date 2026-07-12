<script setup lang="ts">
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import TemplateDetailLifecycleTab from '@/views/templates/detail/TemplateDetailLifecycleTab.vue'
import TemplateDetailAuthoringTab from '@/views/templates/detail/TemplateDetailAuthoringTab.vue'
import TemplateDetailReleaseVersionsTab from '@/views/templates/detail/TemplateDetailReleaseVersionsTab.vue'
import TemplateDetailApiAccessTab from '@/views/templates/detail/TemplateDetailApiAccessTab.vue'
import TemplateDetailLifecycleActions from '@/views/templates/detail/TemplateDetailLifecycleActions.vue'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import type { RuntimeEnvironment } from '@/config/environments'
import type { SemverBumpLevel } from '@/utils/semver'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type {
  ApiCredentialSummary,
  ApiPolicy,
  BindingValidationResult,
  TemplateDetail,
} from '@/types/template'

type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

defineProps<{
  template: TemplateDetail
  templateId: string
  detailTabs: Array<{ name: string; labelKey: string }>
  formatDateTime: (value: string) => string
  showLifecycleSection: boolean
  showGovernanceSection: boolean
  showDraftActions: boolean
  showTestingDecisionActions: boolean
  showSubmitForApproval: boolean
  showApprovalDecisionActions: boolean
  showPublishActions: boolean
  showTestGenerate: boolean
  showStopAction: boolean
  showRestoreAction: boolean
  showDeprecateAction: boolean
  showAuthoringSection: boolean
  canEditContentModuleReferences: boolean
  showPolicyPanel: boolean
  coverageRefreshToken: number
  publishGateItems: PublishGateDisplayItem[]
  loadingPublishGate: boolean
  publishVersionConflict: boolean
  publishGateReady: boolean
  publishBumpOptions: PublishBumpOption[]
  bindingGateResult: BindingValidationResult | null
  publishGateLoadError: string | null
  submitGateItems: PublishGateDisplayItem[]
  loadingSubmitGate: boolean
  submitGateReady: boolean
  submitGateLoadError: string | null
  submitting: boolean
  loadingPolicy: boolean
  apiPolicy: ApiPolicy | null
  policyLoadFailed: boolean
  policyLoadErrorKey: string | null
  paginatedCredentials: ApiCredentialSummary[]
  credentialStatusFilterOptions: Array<{ label: string; value: string }>
  totalCredentialRows: number
  policySubmitting: boolean
  sortCredentialsByCreatedAt: (a: ApiCredentialSummary, b: ApiCredentialSummary) => number
}>()

const activeDetailTab = defineModel<string>('activeDetailTab', { required: true })
const publishBumpLevel = defineModel<SemverBumpLevel>('publishBumpLevel', { required: true })
const credentialColumnFilters = defineModel<Record<string, string>>('credentialColumnFilters', {
  required: true,
})
const credentialsCurrentPage = defineModel<number>('credentialsCurrentPage', { required: true })
const selectedContractEnvironment = defineModel<RuntimeEnvironment>('selectedContractEnvironment', {
  required: true,
})

const emit = defineEmits<{
  openSubmitForTest: []
  testDecision: [decision: 'PASSED' | 'FAILED']
  submitForApproval: []
  approvalDecision: [decision: 'APPROVED' | 'REJECTED']
  publish: []
  testGenerate: []
  governanceAction: [action: 'stop' | 'restore' | 'deprecate']
  retryPublishGate: []
  retrySubmitGate: []
  updated: []
  createCredential: []
  rotateCredential: [credentialId: string, externalId: string]
  revokeCredential: [credentialId: string]
  retryPolicyLoad: []
}>()
</script>

<template>
  <WorkspaceTabShell
    v-model="activeDetailTab"
    :tabs="detailTabs"
    class="detail-tabs"
  >
    <template #actions>
      <template v-if="activeDetailTab === 'lifecycle'">
        <TemplateDetailLifecycleActions
          :submitting="submitting"
          :show-draft-actions="showDraftActions"
          :show-testing-decision-actions="showTestingDecisionActions"
          :show-submit-for-approval="showSubmitForApproval"
          :submit-gate-ready="submitGateReady"
          :loading-submit-gate="loadingSubmitGate"
          :show-approval-decision-actions="showApprovalDecisionActions"
          :show-publish-actions="showPublishActions"
          :publish-gate-ready="publishGateReady"
          :loading-publish-gate="loadingPublishGate"
          :show-test-generate="showTestGenerate"
          :show-stop-action="showStopAction"
          :show-restore-action="showRestoreAction"
          :show-deprecate-action="showDeprecateAction"
          @open-submit-for-test="emit('openSubmitForTest')"
          @test-decision="emit('testDecision', $event)"
          @submit-for-approval="emit('submitForApproval')"
          @approval-decision="emit('approvalDecision', $event)"
          @publish="emit('publish')"
          @test-generate="emit('testGenerate')"
          @governance-action="emit('governanceAction', $event)"
        />
      </template>
    </template>

    <template #overview>
      <TemplateDetailOverviewTab :template="template" :format-date-time="formatDateTime" />
    </template>

    <template #lifecycle>
      <TemplateDetailLifecycleTab
        :template-id="templateId"
        :show-lifecycle-section="showLifecycleSection"
        :show-governance-section="showGovernanceSection"
        :show-submit-for-approval="showSubmitForApproval"
        :show-publish-actions="showPublishActions"
        :publish-gate-items="publishGateItems"
        :loading-publish-gate="loadingPublishGate"
        :publish-bump-level="publishBumpLevel"
        :publish-version-conflict="publishVersionConflict"
        :publish-gate-ready="publishGateReady"
        :publish-bump-options="publishBumpOptions"
        :binding-gate-result="bindingGateResult"
        :publish-gate-load-error="publishGateLoadError"
        :submit-gate-items="submitGateItems"
        :loading-submit-gate="loadingSubmitGate"
        :submit-gate-ready="submitGateReady"
        :submit-gate-load-error="submitGateLoadError"
        @update:publish-bump-level="publishBumpLevel = $event"
        @retry-publish-gate="emit('retryPublishGate')"
        @retry-submit-gate="emit('retrySubmitGate')"
      />
    </template>

    <template v-if="showAuthoringSection" #authoring>
      <TemplateDetailAuthoringTab
        :template-id="templateId"
        :master-id="template.masterId"
        :variables="template.variables"
        :bindings="template.bindings"
        :rules="template.rules"
        :group-code="template.groupCode"
        :can-edit-content-module-references="canEditContentModuleReferences"
        :coverage-refresh-token="coverageRefreshToken"
        @updated="emit('updated')"
      />
    </template>

    <template #releaseVersions>
      <TemplateDetailReleaseVersionsTab
        :template-id="templateId"
        :template-lifecycle-status="template.lifecycleStatus"
        @changed="emit('updated')"
      />
    </template>

    <template v-if="showPolicyPanel" #apiAccess>
      <TemplateDetailApiAccessTab
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
        :page-size="CLIENT_TABLE_PAGE_SIZE"
        :total-credential-rows="totalCredentialRows"
        :submitting="policySubmitting"
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

<style scoped lang="scss">
.detail-tabs {
  margin-top: var(--space-2);
}
</style>
