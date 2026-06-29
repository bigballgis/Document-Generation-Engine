import { NAV_GROUPS } from '@/navigation/navStructure'

export interface BreadcrumbSegment {
  labelKey: string
  path?: string
}

const MASTER_REVISION_PATH = /^\/masters\/([^/]+)\/revisions\/[^/]+$/
const MASTER_HUB_PATH = /^\/masters\/([^/]+)$/

const DETAIL_PREFIXES: Array<{ prefix: string; listPath: string; listLabelKey: string; groupLabelKey: string }> = [
  {
    prefix: '/templates/',
    listPath: '/templates',
    listLabelKey: 'nav.items.templates',
    groupLabelKey: 'nav.groups.content',
  },
  {
    prefix: '/content-modules/',
    listPath: '/content-modules',
    listLabelKey: 'nav.items.contentModules',
    groupLabelKey: 'nav.groups.content',
  },
]

export function buildBreadcrumbTrail(path: string): BreadcrumbSegment[] {
  const revisionMatch = MASTER_REVISION_PATH.exec(path)
  if (revisionMatch) {
    const masterId = revisionMatch[1]
    return [
      { labelKey: 'nav.groups.content' },
      { labelKey: 'nav.items.masters', path: '/masters' },
      { labelKey: 'masters.hub.breadcrumbLabel', path: `/masters/${masterId}` },
      { labelKey: 'masters.revision.breadcrumbLabel' },
    ]
  }

  const hubMatch = MASTER_HUB_PATH.exec(path)
  if (hubMatch) {
    return [
      { labelKey: 'nav.groups.content' },
      { labelKey: 'nav.items.masters', path: '/masters' },
      { labelKey: 'masters.hub.breadcrumbLabel' },
    ]
  }

  for (const detail of DETAIL_PREFIXES) {
    if (path.startsWith(detail.prefix)) {
      return [
        { labelKey: detail.groupLabelKey },
        { labelKey: detail.listLabelKey, path: detail.listPath },
        { labelKey: 'nav.breadcrumb.detail' },
      ]
    }
  }

  for (const group of NAV_GROUPS) {
    for (const item of group.items) {
      if (path === item.path) {
        return [
          { labelKey: group.labelKey },
          { labelKey: item.labelKey, path: item.path },
        ]
      }
    }
  }

  return [{ labelKey: 'nav.breadcrumb.home', path: '/dashboard' }]
}
