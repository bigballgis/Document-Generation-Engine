import { computed, type ComputedRef } from 'vue'
import type { ComposerTranslation } from 'vue-i18n'
import {
  hasApprovedActiveVersion,
  hasApprovedStoppedVersion,
  latestDraftVersion,
  latestSubmittedVersion,
} from '@/auth/contentModuleRoles'
import type { ContentModuleVersion } from '@/types/contentModule'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  serializeStructuredContent,
} from '@/utils/structuredContentNodes'
import { normalizeStructuredContentJson } from '@/utils/structuredContentCompat'

export function createContentModuleDetailDerived(deps: {
  versions: ComputedRef<ContentModuleVersion[]>
  authorContentModules: ComputedRef<boolean>
  decideContentModuleReviews: ComputedRef<boolean>
  manageContentModuleLifecycle: ComputedRef<boolean>
  t: ComposerTranslation
  getLastErrorMessageKey: () => string | null
  te: (key: string) => boolean
}) {
  const {
    versions,
    authorContentModules,
    decideContentModuleReviews,
    manageContentModuleLifecycle,
    t,
    getLastErrorMessageKey,
    te,
  } = deps

  const canSubmitReview = computed(
    () => authorContentModules.value && Boolean(latestDraftVersion(versions.value)),
  )
  const canApproveReview = computed(
    () => decideContentModuleReviews.value && Boolean(latestSubmittedVersion(versions.value)),
  )
  const canCreateVersion = computed(() => authorContentModules.value)
  const canEditDraft = computed(
    () => authorContentModules.value && Boolean(latestDraftVersion(versions.value)),
  )
  const canStop = computed(
    () => manageContentModuleLifecycle.value && hasApprovedActiveVersion(versions.value),
  )
  const canRecover = computed(
    () => manageContentModuleLifecycle.value && hasApprovedStoppedVersion(versions.value),
  )
  const canDeprecate = canRecover

  const previewVersion = computed(() => {
    const draft = latestDraftVersion(versions.value)
    if (draft) {
      return draft as ContentModuleVersion
    }
    return (
      versions.value.find(
        (version) =>
          version.reviewState === 'APPROVED' && (version.lifecycleState ?? 'ACTIVE') === 'ACTIVE',
      ) ?? null
    )
  })

  const previewContentJson = computed(() => {
    const version = previewVersion.value
    if (!version?.contentStructureJson) {
      return DEFAULT_STRUCTURED_CONTENT_JSON
    }
    return serializeStructuredContent(normalizeStructuredContentJson(version.contentStructureJson))
  })

  const previewVersionLabel = computed(() => {
    if (!previewVersion.value) {
      return ''
    }
    return t('contentModules.detail.contentPreviewVersion', {
      version: previewVersion.value.semanticVersion,
      state: previewVersion.value.reviewState,
    })
  })

  const errorMessage = computed(() => {
    const key = getLastErrorMessageKey()
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('contentModules.error.loadDetail')
  })

  return {
    canSubmitReview,
    canApproveReview,
    canCreateVersion,
    canEditDraft,
    canStop,
    canRecover,
    canDeprecate,
    previewVersion,
    previewContentJson,
    previewVersionLabel,
    errorMessage,
  }
}
