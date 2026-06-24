import { NAV_GROUPS } from '@/navigation/navStructure'

export interface BreadcrumbSegment {
  labelKey: string
  path?: string
}

const DETAIL_PREFIXES: Array<{ prefix: string; listPath: string; listLabelKey: string; groupLabelKey: string }> = [
  {
    prefix: '/masters/',
    listPath: '/masters',
    listLabelKey: 'nav.items.masters',
    groupLabelKey: 'nav.groups.content',
  },
  {
    prefix: '/templates/',
    listPath: '/templates',
    listLabelKey: 'nav.items.templates',
    groupLabelKey: 'nav.groups.content',
  },
]

export function buildBreadcrumbTrail(path: string): BreadcrumbSegment[] {
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
