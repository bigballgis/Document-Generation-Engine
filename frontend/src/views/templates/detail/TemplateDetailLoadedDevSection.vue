<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- c is a reactive controller bag owned by the parent shell */
import TemplateDetailDevWorkspace from '@/views/templates/detail/TemplateDetailDevWorkspace.vue'

defineProps<{
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  c: any
}>()
</script>

<template>
  <TemplateDetailDevWorkspace
    v-if="c.isDevEditor && c.showAuthoringSection"
    :template-id="c.templateId"
    :master-id="c.template.masterId"
    :variables="c.template.variables"
    :bindings="c.template.bindings"
    :rules="c.template.rules"
    :group-code="c.template.groupCode"
    :lifecycle-status="c.template.lifecycleStatus"
    :can-edit-content-module-references="c.canEditContentModuleReferences"
    :coverage-refresh-token="c.coverageRefreshToken"
    :last-preview="c.lastPreview"
    :selected-preview-id="c.selectedPreviewId"
    :selected-test-data-set-id="c.selectedTestDataSetId"
    :show-draft-actions="c.showDraftActions"
    :show-testing-decision-actions="c.showTestingDecisionActions"
    :show-submit-for-approval="c.showSubmitForApproval"
    :show-approval-decision-actions="c.showApprovalDecisionActions"
    :show-publish-actions="c.showPublishActions"
    :show-test-generate="c.showTestGenerate"
    :show-stop-action="c.showStopAction"
    :show-restore-action="c.showRestoreAction"
    :show-deprecate-action="c.showDeprecateAction"
    :show-governance-section="c.showGovernanceSection"
    :publish-gate-items="c.publishGateItems"
    :loading-publish-gate="c.loadingPublishGate"
    :publish-bump-level="c.publishBumpLevel"
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
    :generating-preview="c.generatingPreview"
    :generating-preview-id="c.generatingPreviewId"
    :batch-testing="c.batchTesting"
    :open-submit-for-test-dialog="c.submitForTestDialogOpen"
    @updated="c.loadTemplate"
    @update:selected-test-data-set-id="c.selectedTestDataSetId = $event"
    @update:selected-preview-id="c.handlePreviewSelected"
    @update:publish-bump-level="c.publishBumpLevel = $event"
    @update:open-submit-for-test-dialog="c.submitForTestDialogOpen = $event"
    @test-generate="c.handleTestGenerate"
    @test-generate-batch="c.handleBatchTestGenerate"
    @submit-for-test="c.handleSubmitForTest"
    @test-decision="c.handleTestDecision"
    @submit-for-approval="c.handleSubmitForApproval"
    @approval-decision="c.handleApprovalDecision"
    @publish="c.handlePublish"
    @governance-action="c.handleGovernanceAction"
    @retry-publish-gate="c.loadPublishGateData"
    @retry-submit-gate="c.loadSubmitGateData"
    @preview-refreshed="c.handlePreviewRefreshed"
  />
</template>
