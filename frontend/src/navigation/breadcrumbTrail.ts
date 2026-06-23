import { NAV_GROUPS } from '@/navigation/navStructure'

export interface BreadcrumbSegment {
  labelKey: string
  path?: string
}

const DETAIL_PREFIXES: Array<{ prefix: string; listPath: string; listLabelKey: string; groupLabelKey: string }> = [
  {
    prefix: '/masters/',
    listPath: '/masters',
    listLabelKey: 'nav.items.masterVersions',
    groupLabelKey: 'nav.groups.versionCatalog',
  },
  {
    prefix: '/templates/',
    listPath: '/templates',
    listLabelKey: 'nav.items.templateVersions',
    groupLabelKey: 'nav.groups.versionCatalog',
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
