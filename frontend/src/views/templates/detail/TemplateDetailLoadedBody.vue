<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- c is a reactive controller bag owned by the parent shell */
import TemplateDetailJourneyStack from '@/views/templates/detail/TemplateDetailJourneyStack.vue'
import TemplateDetailDialogs from '@/views/templates/detail/TemplateDetailDialogs.vue'
import TemplateDetailLoadedDevSection from '@/views/templates/detail/TemplateDetailLoadedDevSection.vue'
import TemplateDetailLoadedLegacySection from '@/views/templates/detail/TemplateDetailLoadedLegacySection.vue'

/** Parent passes reactive(useTemplateDetailController(...)); refs are auto-unwrapped. */
defineProps<{
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  c: any
}>()
</script>

<template>
  <TemplateDetailJourneyStack
    v-if="!c.isDevEditor"
    :template="c.template"
    :template-id="c.templateId"
    :show-author-journey="c.showAuthorJourney"
    :author-journey-context="c.authorJourneyContext"
    :author-templates="c.authorTemplates"
    :author-journey-primary-cta-disabled="c.authorJourneyPrimaryCtaDisabled"
    :show-tester-journey="c.showTesterJourney"
    :tester-journey-context="c.testerJourneyContext"
    :decide-tests="c.decideTests"
    :show-approver-journey="c.showApproverJourney"
    :approver-journey-context="c.approverJourneyContext"
    :decide-approvals="c.decideApprovals"
    :show-team-lead-journey="c.showTeamLeadJourney"
    :team-lead-journey-context="c.teamLeadJourneyContext"
    :publish-templates="c.publishTemplates"
    @create="c.handleJourneyCreate"
    @design="c.handleJourneyDesign"
    @trial-generate="c.handleJourneyTrialGenerate"
    @submit-for-test="c.handleJourneySubmitForTest"
    @submit-for-approval="c.handleJourneySubmitForApproval"
    @review-request="c.handleJourneyReviewRequest"
    @check-evidence="c.handleJourneyCheckEvidence"
    @record-result="c.handleJourneyRecordResult"
    @review-approver-request="c.handleJourneyApproverReviewRequest"
    @review-submission="c.handleJourneyApproverReviewSubmission"
    @record-decision="c.handleJourneyApproverRecordDecision"
    @review-go-live-request="c.handleJourneyTeamLeadReviewGoLiveRequest"
    @run-pre-release-checks="c.handleJourneyTeamLeadRunPreReleaseChecks"
    @confirm-go-live="c.handleJourneyTeamLeadConfirmGoLive"
    @open-lifecycle="c.openLifecyclePanel"
  />

  <TemplateDetailLoadedDevSection :c="c" />
  <TemplateDetailLoadedLegacySection :c="c" />

  <TemplateDetailDialogs
    v-model:metadata-edit-open="c.metadataEditOpen"
    v-model:publish-summary-open="c.publishSummaryOpen"
    v-model:submit-summary-open="c.submitSummaryOpen"
    v-model:decision-dialog-open="c.decisionDialogOpen"
    v-model:lifecycle-comment-dialog-open="c.lifecycleCommentDialogOpen"
    v-model:credential-secret-dialog-visible="c.credentialSecretDialogVisible"
    :template-name="c.template.name"
    :template-description="c.template.description ?? null"
    :template-id="c.templateId"
    :submitting="c.templatesStore.submitting"
    :publish-version="c.publishVersion"
    :publish-gate-items="c.publishGateItems"
    :publish-coverage-summary="c.publishCoverageSummary"
    :publish-change-diff-summary="c.publishChangeDiffSummary"
    :submit-gate-items="c.submitGateItems"
    :submit-coverage-summary="c.submitCoverageSummary"
    :submit-change-diff-summary="c.submitChangeDiffSummary"
    :preview-comparison="c.lastPreview?.previewComparison ?? null"
    :decision-dialog-mode="c.decisionDialogMode"
    :lifecycle-comment="c.lifecycleComment"
    :credential-secret-external-id="c.credentialSecretExternalId"
    :displayed-credential-secret="c.displayedCredentialSecret"
    :credential-secret-value="c.credentialSecretValue"
    @metadata-submit="c.handleMetadataUpdate"
    @confirm-publish="c.confirmPublishFromSummary"
    @confirm-submit="c.confirmSubmitFromSummary"
    @submit-decision="c.submitLifecycleDecision"
    @submit-for-test="c.handleSubmitForTest"
  />
</template>
