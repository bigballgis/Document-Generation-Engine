import { NAV_GROUPS } from '@/navigation/navStructure'

export interface BreadcrumbSegment {
  labelKey: string
  path?: string
}

const TOP_LEVEL_LIST_PATHS = new Set([
  '/dashboard',
  '/entitlement/users',
  '/entitlement/groups',
  '/masters',
  '/templates',
  '/content-modules',
  '/library/assets',
  '/api/policies',
  '/audit',
])

const MASTER_REVISION_PATH = /^\/masters\/([^/]+)\/revisions\/[^/]+$/
const MASTER_HUB_PATH = /^\/masters\/([^/]+)$/
const TEMPLATE_DEV_PATH = /^\/templates\/([^/]+)\/dev\/[^/]+$/
const TEMPLATE_RELEASE_PATH = /^\/templates\/([^/]+)\/releases\/[^/]+$/
const TEMPLATE_HUB_PATH = /^\/templates\/([^/]+)$/

const DETAIL_PREFIXES: Array<{ prefix: string; listPath: string; listLabelKey: string }> = [
  {
    prefix: '/templates/',
    listPath: '/templates',
    listLabelKey: 'nav.items.templates',
  },
  {
    prefix: '/content-modules/',
    listPath: '/content-modules',
    listLabelKey: 'nav.items.contentModules',
  },
]

export function buildBreadcrumbTrail(path: string): BreadcrumbSegment[] {
  if (TOP_LEVEL_LIST_PATHS.has(path)) {
    return []
  }

  const templateDevMatch = TEMPLATE_DEV_PATH.exec(path)
  if (templateDevMatch) {
    const templateId = templateDevMatch[1]
    return [
      { labelKey: 'nav.items.templates', path: '/templates' },
      { labelKey: 'templates.packageHub.breadcrumbLabel', path: `/templates/${templateId}` },
      { labelKey: 'templates.devEditor.breadcrumbLabel' },
    ]
  }

  const templateReleaseMatch = TEMPLATE_RELEASE_PATH.exec(path)
  if (templateReleaseMatch) {
    const templateId = templateReleaseMatch[1]
    return [
      { labelKey: 'nav.items.templates', path: '/templates' },
      { labelKey: 'templates.packageHub.breadcrumbLabel', path: `/templates/${templateId}` },
      { labelKey: 'templates.releaseDetail.breadcrumbLabel' },
    ]
  }

  const templateHubMatch = TEMPLATE_HUB_PATH.exec(path)
  if (templateHubMatch) {
    return [
      { labelKey: 'nav.items.templates', path: '/templates' },
      { labelKey: 'templates.packageHub.breadcrumbLabel' },
    ]
  }

  const revisionMatch = MASTER_REVISION_PATH.exec(path)
  if (revisionMatch) {
    const masterId = revisionMatch[1]
    return [
      { labelKey: 'nav.items.masters', path: '/masters' },
      { labelKey: 'masters.hub.breadcrumbLabel', path: `/masters/${masterId}` },
      { labelKey: 'masters.revision.breadcrumbLabel' },
    ]
  }

  const hubMatch = MASTER_HUB_PATH.exec(path)
  if (hubMatch) {
    return [
      { labelKey: 'nav.items.masters', path: '/masters' },
      { labelKey: 'masters.hub.breadcrumbLabel' },
    ]
  }

  for (const detail of DETAIL_PREFIXES) {
    if (path.startsWith(detail.prefix)) {
      return [
        { labelKey: detail.listLabelKey, path: detail.listPath },
        { labelKey: 'nav.breadcrumb.detail' },
      ]
    }
  }

  for (const group of NAV_GROUPS) {
    for (const item of group.items) {
      if (path === item.path) {
        return []
      }
    }
  }

  return [{ labelKey: 'nav.breadcrumb.home', path: '/dashboard' }]
}
