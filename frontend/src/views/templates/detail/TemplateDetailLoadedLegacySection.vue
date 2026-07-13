<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- c is a reactive controller bag owned by the parent shell */
import TemplateDetailLegacyWorkspace from '@/views/templates/detail/TemplateDetailLegacyWorkspace.vue'

defineProps<{
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  c: any
}>()
</script>

<template>
  <TemplateDetailLegacyWorkspace
    v-if="!c.isDevEditor"
    v-model:active-detail-tab="c.activeDetailTab"
    v-model:publish-bump-level="c.publishBumpLevel"
    v-model:credential-column-filters="c.credentialColumnFilters"
    v-model:credentials-current-page="c.credentialsCurrentPage"
    v-model:selected-contract-environment="c.selectedContractEnvironment"
    :template="c.template"
    :template-id="c.templateId"
    :detail-tabs="c.detailTabs"
    :format-date-time="c.formatDateTime"
    :show-lifecycle-section="c.showLifecycleSection"
    :show-governance-section="c.showGovernanceSection"
    :show-draft-actions="c.showDraftActions"
    :show-testing-decision-actions="c.showTestingDecisionActions"
    :show-submit-for-approval="c.showSubmitForApproval"
    :show-approval-decision-actions="c.showApprovalDecisionActions"
    :show-publish-actions="c.showPublishActions"
    :show-test-generate="c.showTestGenerate"
    :show-stop-action="c.showStopAction"
    :show-restore-action="c.showRestoreAction"
    :show-deprecate-action="c.showDeprecateAction"
    :show-authoring-section="c.showAuthoringSection"
    :can-edit-content-module-references="c.canEditContentModuleReferences"
    :show-policy-panel="c.showPolicyPanel"
    :coverage-refresh-token="c.coverageRefreshToken"
    :publish-gate-items="c.publishGateItems"
    :loading-publish-gate="c.loadingPublishGate"
    :publish-version-conflict="c.publishVersionConflict"
    :publish-gate-ready="c.publishGateReady"
    :publish-bump-options="c.publishBumpOptions"
    :binding-gate-result="c.bindingGateResult"
    :publish-gate-load-error="c.publishGateLoadError"
    :submit-gate-items="c.submitGateItems"
    :loading-submit-gate="c.loadingSubmitGate"
    :submit-gate-ready="c.submitGateReady"
    :submit-gate-load-error="c.submitGateLoadError"
    :submitting="c.templatesStore.submitting"
    :loading-policy="c.loadingPolicy"
    :api-policy="c.apiPolicy"
    :policy-load-failed="c.policyLoadFailed"
    :policy-load-error-key="c.policyLoadErrorKey"
    :paginated-credentials="c.paginatedCredentials"
    :credential-status-filter-options="c.credentialStatusFilterOptions"
    :total-credential-rows="c.totalCredentialRows"
    :policy-submitting="c.policySubmitting"
    :sort-credentials-by-created-at="c.sortCredentialsByCreatedAt"
    @open-submit-for-test="c.lifecycleCommentDialogOpen = true"
    @test-decision="c.handleTestDecision"
    @submit-for-approval="c.handleSubmitForApproval"
    @approval-decision="c.handleApprovalDecision"
    @publish="c.handlePublish"
    @test-generate="c.handleTestGenerate()"
    @governance-action="c.handleGovernanceAction"
    @retry-publish-gate="c.loadPublishGateData"
    @retry-submit-gate="c.loadSubmitGateData"
    @updated="c.loadTemplate"
    @create-credential="c.handleCreateCredential"
    @rotate-credential="c.handleRotateCredential"
    @revoke-credential="c.handleRevokeCredential"
    @retry-policy-load="c.loadPolicyData"
  />
</template>
