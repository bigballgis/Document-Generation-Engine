import type { Ref } from 'vue'
import type {
  ContentModuleDetail,
  ContentModuleSummary,
  ContentModuleVersion,
} from '@/types/contentModule'

/** CE-U20 U20-D5 — head version = max updatedAt; ties → greater semanticVersion. */
function selectHeadVersion(versions: ContentModuleVersion[]): ContentModuleVersion | undefined {
  if (versions.length === 0) {
    return undefined
  }
  return versions.reduce((head, version) => {
    if (version.updatedAt > head.updatedAt) {
      return version
    }
    if (version.updatedAt < head.updatedAt) {
      return head
    }
    return version.semanticVersion > head.semanticVersion ? version : head
  })
}

export function toContentModuleSummary(detail: ContentModuleDetail): ContentModuleSummary {
  const head = selectHeadVersion(detail.versions)
  return {
    moduleId: detail.moduleId,
    moduleCode: detail.moduleCode,
    groupCode: detail.groupCode,
    name: detail.name,
    description: detail.description,
    sharedGroupCodes: detail.sharedGroupCodes,
    reviewState: head?.reviewState ?? 'DRAFT',
    lifecycleState: head?.lifecycleState,
    createdAt: detail.versions[0]?.createdAt ?? '',
    updatedAt: head?.updatedAt ?? detail.versions[0]?.updatedAt ?? '',
    locale: detail.locale,
    localeVariantFamilyId: detail.localeVariantFamilyId ?? null,
  }
}

export function applyUpdatedContentModule(
  selectedModule: Ref<ContentModuleDetail | null>,
  modules: Ref<ContentModuleSummary[]>,
  updated: ContentModuleDetail,
) {
  selectedModule.value = updated
  modules.value = modules.value.map((item) =>
    item.moduleId === updated.moduleId ? toContentModuleSummary(updated) : item,
  )
}
