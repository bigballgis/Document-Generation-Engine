import { formatSharedGroupCodesLabel, normalizeSharedGroupCodes } from '@/utils/contentModuleSharedGroups'

export function buildContentModuleDetailSummaryDescription(
  detail: {
    moduleCode: string
    groupCode: string
    sharedGroupCodes?: string[] | null
  },
  t: (key: string, params?: Record<string, string>) => string,
): string {
  const owner = t('contentModules.detail.summary.owner', { groupCode: detail.groupCode })
  const sharedCodes = normalizeSharedGroupCodes(detail.sharedGroupCodes)
  const shared =
    sharedCodes.length > 0
      ? t('contentModules.detail.summary.sharedWith', {
          codes: formatSharedGroupCodesLabel(sharedCodes),
        })
      : t('contentModules.detail.summary.notShared')
  return `${detail.moduleCode} · ${owner} · ${shared}`
}
