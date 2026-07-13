import { type ComputedRef, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  useTemplateJourneyHandlers,
  type TemplateJourneyLifecycleDeps,
} from '@/views/templates/useTemplateJourneyHandlers'
import { createTemplateJourneyVisibility } from '@/views/templates/createTemplateJourneyVisibility'
import type { PreviewRecord, TemplateDetail } from '@/types/template'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'

export type { TemplateJourneyLifecycleDeps }

export interface UseTemplateJourneyContextOptions {
  isDevEditor: ComputedRef<boolean>
  template: ComputedRef<TemplateDetail | null>
  lastPreview: Ref<PreviewRecord | null>
  lifecycle: TemplateJourneyLifecycleDeps
  openDevWorkspaceTab: (tab: TemplateDevWorkspaceTab) => void
  openLifecyclePanel: () => void
  handleTestGenerate: (testDataSetId?: string) => Promise<void>
}

export function useTemplateJourneyContext(options: UseTemplateJourneyContextOptions) {
  const {
    isDevEditor,
    template,
    lastPreview,
    lifecycle,
    openDevWorkspaceTab,
    openLifecyclePanel,
    handleTestGenerate,
  } = options

  const router = useRouter()

  const visibility = createTemplateJourneyVisibility({
    template,
    lastPreview,
    publishGateReady: lifecycle.publishGateReady,
  })

  const handlers = useTemplateJourneyHandlers({
    isDevEditor,
    router,
    lifecycle,
    openDevWorkspaceTab,
    openLifecyclePanel,
    handleTestGenerate,
    testerEvidenceViewed: visibility.testerEvidenceViewed,
    approverEvidenceViewed: visibility.approverEvidenceViewed,
    teamLeadGoLiveViewed: visibility.teamLeadGoLiveViewed,
  })

  return {
    showAuthorJourney: visibility.showAuthorJourney,
    authorJourneyContext: visibility.authorJourneyContext,
    showTesterJourney: visibility.showTesterJourney,
    testerJourneyContext: visibility.testerJourneyContext,
    showApproverJourney: visibility.showApproverJourney,
    approverJourneyContext: visibility.approverJourneyContext,
    showTeamLeadJourney: visibility.showTeamLeadJourney,
    teamLeadJourneyContext: visibility.teamLeadJourneyContext,
    testerEvidenceViewed: visibility.testerEvidenceViewed,
    approverEvidenceViewed: visibility.approverEvidenceViewed,
    ...handlers,
  }
}
