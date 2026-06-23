import { computed } from 'vue'
import {
  canAuthorTemplates,
  canDecideApprovals,
  canDecideTests,
  canManageApiPolicy,
  canPublishTemplates,
  canManageReleaseVersionState,
  canRestoreOrDeprecateTemplates,
  canReviewMasters,
  canStopTemplates,
  canUploadMasters,
  sessionContext,
} from '@/auth/roles'
import { useSessionStore } from '@/stores/session'

export function useCapabilities() {
  const sessionStore = useSessionStore()

  const context = computed(() => sessionContext(sessionStore.session))

  const manageMasters = computed(() => canUploadMasters(context.value))
  const reviewMasters = computed(() => canReviewMasters(context.value))
  const authorTemplates = computed(() => canAuthorTemplates(context.value))
  const decideTests = computed(() => canDecideTests(context.value))
  const decideApprovals = computed(() => canDecideApprovals(context.value))
  const publishTemplates = computed(() => canPublishTemplates(context.value))
  const stopTemplates = computed(() => canStopTemplates(context.value))
  const restoreOrDeprecateTemplates = computed(() => canRestoreOrDeprecateTemplates(context.value))
  const manageReleaseVersionState = computed(() => canManageReleaseVersionState(context.value))
  const manageApiPolicy = computed(() => canManageApiPolicy(context.value))

  return {
    context,
    manageMasters,
    reviewMasters,
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    stopTemplates,
    restoreOrDeprecateTemplates,
    manageReleaseVersionState,
    manageApiPolicy,
  }
}
