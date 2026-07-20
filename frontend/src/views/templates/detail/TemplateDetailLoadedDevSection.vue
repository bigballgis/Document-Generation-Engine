<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- c is a reactive controller bag owned by the parent shell */
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import {
  templatePackageHubPath,
  templateReleaseDetailPath,
} from '@/routing/routeKeys'
import TemplateDetailDevWorkspace from '@/views/templates/detail/TemplateDetailDevWorkspace.vue'

defineProps<{
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  c: any
}>()

const { t } = useI18n()
const router = useRouter()

function openCorrectSurface(c: {
  templateId: string
  template: { releaseVersion?: string | null; lifecycleStatus?: string }
}) {
  const releaseVersion = c.template.releaseVersion
  if (
    releaseVersion &&
    (c.template.lifecycleStatus === 'PUBLISHED' ||
      c.template.lifecycleStatus === 'STOPPED' ||
      c.template.lifecycleStatus === 'DEPRECATED')
  ) {
    void router.push(templateReleaseDetailPath(c.templateId, releaseVersion))
    return
  }
  void router.push(templatePackageHubPath(c.templateId))
}
</script>

<template>
  <TemplateDetailDevWorkspace
    v-if="c.isDevEditor && c.showAuthoringSection"
    :template-id="c.templateId"
    :template="c.template"
    :master-id="c.template.masterId"
    :variables="c.template.variables"
    :bindings="c.template.bindings"
    :rules="c.template.rules"
    :group-code="c.template.groupCode"
    :lifecycle-status="c.template.lifecycleStatus"
    :approval-sub-state="c.template.approvalSubState"
    :approval-matrix-mode="c.template.approvalMatrixMode"
    :approval-stage="c.template.approvalStage"
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
    :open-submit-for-test-dialog="c.submitForTestDialogOpen"
    @updated="c.loadTemplate"
    @update:selected-test-data-set-id="c.selectedTestDataSetId = $event"
    @update:selected-preview-id="c.handlePreviewSelected"
    @update:publish-bump-level="c.publishBumpLevel = $event"
    @update:open-submit-for-test-dialog="c.submitForTestDialogOpen = $event"
    @test-generate="c.handleTestGenerate"
    @batch-test-completed="c.bumpCoverageRefresh"
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

  <div
    v-else-if="c.isDevEditor && !c.showAuthoringSection"
    class="dev-wrong-surface"
    data-testid="dev-editor-wrong-surface"
  >
    <EmptyStatePanel
      title-key="templates.devEditor.wrongSurfaceTitle"
      description-key="templates.devEditor.wrongSurfaceDescription"
    >
      <template #actions>
        <el-button
          type="primary"
          data-testid="dev-editor-open-correct-surface"
          @click="openCorrectSurface(c)"
        >
          {{ t('templates.devEditor.openCorrectSurface') }}
        </el-button>
      </template>
    </EmptyStatePanel>
  </div>
</template>

<style scoped lang="scss">
.dev-wrong-surface {
  margin-top: var(--space-4);
}
</style>
