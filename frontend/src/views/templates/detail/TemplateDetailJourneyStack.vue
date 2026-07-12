<script setup lang="ts">
import TemplateWorkflowBanner from '@/components/templates/TemplateWorkflowBanner.vue'
import TemplateAuthorJourneyBlock from '@/components/journey/TemplateAuthorJourneyBlock.vue'
import TemplateTesterJourneyBlock from '@/components/journey/TemplateTesterJourneyBlock.vue'
import TemplateApproverJourneyBlock from '@/components/journey/TemplateApproverJourneyBlock.vue'
import TemplateTeamLeadJourneyBlock from '@/components/journey/TemplateTeamLeadJourneyBlock.vue'
import type { TemplateAuthorJourneyContext } from '@/utils/templateAuthorJourney'
import type { TemplateTesterJourneyContext } from '@/utils/templateTesterJourney'
import type { TemplateApproverJourneyContext } from '@/utils/templateApproverJourney'
import type { TemplateTeamLeadJourneyContext } from '@/utils/templateTeamLeadJourney'
import type { TemplateDetail } from '@/types/template'

defineProps<{
  template: TemplateDetail
  templateId: string
  showAuthorJourney: boolean
  authorJourneyContext: TemplateAuthorJourneyContext | null
  authorTemplates: boolean
  authorJourneyPrimaryCtaDisabled: boolean
  showTesterJourney: boolean
  testerJourneyContext: TemplateTesterJourneyContext | null
  decideTests: boolean
  showApproverJourney: boolean
  approverJourneyContext: TemplateApproverJourneyContext | null
  decideApprovals: boolean
  showTeamLeadJourney: boolean
  teamLeadJourneyContext: TemplateTeamLeadJourneyContext | null
  publishTemplates: boolean
}>()

const emit = defineEmits<{
  create: []
  design: []
  trialGenerate: []
  submitForTest: []
  submitForApproval: []
  reviewRequest: []
  checkEvidence: []
  recordResult: []
  reviewApproverRequest: []
  reviewSubmission: []
  recordDecision: []
  reviewGoLiveRequest: []
  runPreReleaseChecks: []
  confirmGoLive: []
  openLifecycle: []
}>()
</script>

<template>
  <TemplateAuthorJourneyBlock
    v-if="showAuthorJourney && authorJourneyContext"
    :journey-context="authorJourneyContext"
    :template-id="templateId"
    :can-write="authorTemplates"
    :primary-cta-disabled="authorJourneyPrimaryCtaDisabled"
    :show-primary-cta="false"
    @create="emit('create')"
    @design="emit('design')"
    @trial-generate="emit('trialGenerate')"
    @submit-for-test="emit('submitForTest')"
    @submit-for-approval="emit('submitForApproval')"
  />

  <TemplateTesterJourneyBlock
    v-if="showTesterJourney && testerJourneyContext"
    :journey-context="testerJourneyContext"
    :can-decide="decideTests"
    :show-primary-cta="false"
    @review-request="emit('reviewRequest')"
    @check-evidence="emit('checkEvidence')"
    @record-result="emit('recordResult')"
  />

  <TemplateApproverJourneyBlock
    v-if="showApproverJourney && approverJourneyContext"
    :journey-context="approverJourneyContext"
    :can-decide="decideApprovals"
    :show-primary-cta="false"
    @review-request="emit('reviewApproverRequest')"
    @review-submission="emit('reviewSubmission')"
    @record-decision="emit('recordDecision')"
  />

  <TemplateTeamLeadJourneyBlock
    v-if="showTeamLeadJourney && teamLeadJourneyContext"
    :journey-context="teamLeadJourneyContext"
    :can-publish="publishTemplates"
    :show-primary-cta="false"
    @review-go-live-request="emit('reviewGoLiveRequest')"
    @run-pre-release-checks="emit('runPreReleaseChecks')"
    @confirm-go-live="emit('confirmGoLive')"
  />

  <TemplateWorkflowBanner
    :template="template"
    @open-lifecycle="emit('openLifecycle')"
  />
</template>
