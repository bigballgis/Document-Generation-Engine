import { defineComponent, ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import { useTemplateDetailController } from '@/views/templates/useTemplateDetailController'

const EXPECTED_RETURN_KEYS = [
  't',
  'te',
  'formatDateTime',
  'templateId',
  'devVersionId',
  'isDevEditor',
  'template',
  'showDetailSkeleton',
  'loadFailed',
  'activeDetailTab',
  'activeDevWorkspaceTab',
  'detailTabs',
  'authorTemplates',
  'decideTests',
  'decideApprovals',
  'publishTemplates',
  'reviewMasters',
  'showAuthorJourney',
  'authorJourneyContext',
  'showTesterJourney',
  'testerJourneyContext',
  'showApproverJourney',
  'approverJourneyContext',
  'showTeamLeadJourney',
  'teamLeadJourneyContext',
  'authorJourneyPrimaryCtaDisabled',
  'handleJourneyCreate',
  'handleJourneyDesign',
  'handleJourneyTrialGenerate',
  'handleJourneySubmitForTest',
  'handleJourneySubmitForApproval',
  'handleJourneyReviewRequest',
  'handleJourneyCheckEvidence',
  'handleJourneyRecordResult',
  'handleJourneyApproverReviewRequest',
  'handleJourneyApproverReviewSubmission',
  'handleJourneyApproverRecordDecision',
  'handleJourneyTeamLeadReviewGoLiveRequest',
  'handleJourneyTeamLeadRunPreReleaseChecks',
  'handleJourneyTeamLeadConfirmGoLive',
  'showLifecycleSection',
  'showGovernanceSection',
  'showDraftActions',
  'showTestingDecisionActions',
  'showSubmitForApproval',
  'showApprovalDecisionActions',
  'showPublishActions',
  'showTestGenerate',
  'showStopAction',
  'showRestoreAction',
  'showDeprecateAction',
  'showAuthoringSection',
  'canEditContentModuleReferences',
  'showPolicyPanel',
  'canPolicy',
  'showExportActions',
  'showDeleteTemplateAction',
  'showMetadataEdit',
  'policyLoadFailed',
  'apiPolicy',
  'loadingPolicy',
  'policySubmitting',
  'policyLoadErrorKey',
  'publishGateItems',
  'publishGateReady',
  'publishVersion',
  'publishBumpLevel',
  'publishBumpOptions',
  'publishVersionConflict',
  'loadingPublishGate',
  'publishGateLoadError',
  'publishCoverageSummary',
  'publishChangeDiffSummary',
  'submitGateItems',
  'submitGateReady',
  'loadingSubmitGate',
  'submitGateLoadError',
  'submitCoverageSummary',
  'submitChangeDiffSummary',
  'bindingGateResult',
  'lifecycleComment',
  'lifecycleCommentDialogOpen',
  'decisionDialogOpen',
  'decisionDialogMode',
  'publishSummaryOpen',
  'submitSummaryOpen',
  'metadataEditOpen',
  'credentialSecretDialogVisible',
  'credentialSecretValue',
  'credentialSecretExternalId',
  'displayedCredentialSecret',
  'lastPreview',
  'selectedPreviewId',
  'selectedTestDataSetId',
  'generatingPreview',
  'generatingPreviewId',
  'coverageRefreshToken',
  'submitForTestDialogOpen',
  'testerEvidenceViewed',
  'approverEvidenceViewed',
  'credentialColumnFilters',
  'credentialsCurrentPage',
  'paginatedCredentials',
  'credentialStatusFilterOptions',
  'totalCredentialRows',
  'sortCredentialsByCreatedAt',
  'selectedContractEnvironment',
  'templatesStore',
  'loadTemplate',
  'loadPublishGateData',
  'loadSubmitGateData',
  'loadPolicyData',
  'backToList',
  'openLifecyclePanel',
  'openDevWorkspaceTab',
  'handleTestGenerate',
  'bumpCoverageRefresh',
  'handlePreviewSelected',
  'handlePreviewRefreshed',
  'handleSubmitForTest',
  'handleTestDecision',
  'openApprovalRejectDialog',
  'submitLifecycleDecision',
  'handleSubmitForApproval',
  'confirmSubmitFromSummary',
  'handleApprovalDecision',
  'handlePublish',
  'confirmPublishFromSummary',
  'handleGovernanceAction',
  'handleMetadataUpdate',
  'handleCreateCredential',
  'handleRotateCredential',
  'handleRevokeCredential',
  'handleDeleteTemplate',
] as const

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { templateId: 'tpl-1' },
    query: {},
  }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}))

vi.mock('@/api/templates', () => ({
  listTestDataSets: vi.fn(),
  batchTestGenerate: vi.fn(),
  getPreview: vi.fn(),
  fetchDevVersionDetail: vi.fn(),
  fetchPublishGate: vi.fn().mockResolvedValue({ templateId: 'tpl-1', ready: true, blockerCount: 0, items: [] }),
  getTemplateCoverage: vi.fn().mockResolvedValue({ templateId: 'tpl-1', aggregatePercentage: 100, belowThreshold: false, blockerCodes: [], dimensions: [], appliedThreshold: { percentage: 80, scope: 'TEMPLATE' } }),
  fetchChangeDiff: vi.fn().mockResolvedValue({ templateId: 'tpl-1', baselineReleaseVersion: null, candidateVersionId: 'dev-1', hasChanges: false, totalChangeCount: 0, dimensions: [] }),
  fetchReleaseVersions: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => ({
    authorTemplates: ref(true),
    decideTests: ref(true),
    decideApprovals: ref(true),
    publishTemplates: ref(true),
    reviewMasters: ref(true),
    exportTemplates: ref(true),
    editTemplateMetadata: ref(true),
    manageApiPolicy: ref(true),
    context: ref({ roles: ['TEMPLATE_AUTHOR'] }),
  }),
}))

vi.mock('@/auth/roles', () => ({
  canViewCollaborationWorkItems: () => true,
}))

vi.mock('@/stores/session', () => ({
  useSessionStore: () => ({
    session: ref({ roles: ['TEMPLATE_AUTHOR'] }),
  }),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
    ElMessageBox: { prompt: vi.fn(), confirm: vi.fn() },
  }
})

describe('useTemplateDetailController', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('BDD-F6-A4-001: return shape keys match expected stable surface', () => {
    const Comp = defineComponent({
      setup() {
        const controller = useTemplateDetailController(ref('legacy'))
        return { controller }
      },
      template: '<div></div>',
    })
    const wrapper = mount(Comp, {
      global: {
        plugins: [createPinia(), createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus],
      },
    })
    const controller = (wrapper.vm as { controller: ReturnType<typeof useTemplateDetailController> }).controller
    const actualKeys = Object.keys(controller).sort()
    const expectedKeys = [...EXPECTED_RETURN_KEYS].sort()
    expect(actualKeys).toEqual(expectedKeys)
    wrapper.unmount()
  })
})
