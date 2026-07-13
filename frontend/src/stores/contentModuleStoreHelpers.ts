import type { Ref } from 'vue'
import type {
  ContentModuleDetail,
  ContentModuleSummary,
} from '@/types/contentModule'

export function toContentModuleSummary(detail: ContentModuleDetail): ContentModuleSummary {
  return {
    moduleId: detail.moduleId,
    moduleCode: detail.moduleCode,
    groupCode: detail.groupCode,
    name: detail.name,
    description: detail.description,
    sharedGroupCodes: detail.sharedGroupCodes,
    createdAt: detail.versions[0]?.createdAt ?? '',
    updatedAt: detail.versions.reduce(
      (latest, version) => (version.updatedAt > latest ? version.updatedAt : latest),
      detail.versions[0]?.updatedAt ?? '',
    ),
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
