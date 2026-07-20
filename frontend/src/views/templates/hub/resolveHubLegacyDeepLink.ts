import type { LocationQuery } from 'vue-router'
import {
  apiPackageSettingsPath,
  apiPackageSettingsQueryFromLegacyHash,
} from '@/routing/apiPackageSettings'
import { templateReleaseDetailPath } from '@/routing/routeKeys'

export type HubLegacyDeepLinkResult =
  | { kind: 'none' }
  | { kind: 'properties' }
  | { kind: 'apiSettings'; path: string }
  | { kind: 'dependencies'; path?: string; guidance: boolean }
  | { kind: 'stripTab' }

/**
 * Wave 2 hub deep-link resolution — removed Overview / Dependencies / External access tabs.
 * BDD-SYS-NORM-W2-010…012
 */
export function resolveHubLegacyDeepLink(options: {
  templateId: string
  query: LocationQuery
  hash?: string
  preferredReleaseVersion?: string | null
  preferredDevVersionId?: string | null
}): HubLegacyDeepLinkResult {
  const { templateId, query, hash, preferredReleaseVersion } = options
  const tab = typeof query.tab === 'string' ? query.tab : undefined
  const hashValue = hash ?? ''

  if (tab === 'apiAccess' || hashValue === '#apiAccess' || hashValue.startsWith('#domain=')) {
    const fromHash = apiPackageSettingsQueryFromLegacyHash(hashValue)
    return {
      kind: 'apiSettings',
      path: apiPackageSettingsPath(templateId, fromHash),
    }
  }

  if (tab === 'overview') {
    return { kind: 'properties' }
  }

  if (tab === 'dependencies') {
    if (preferredReleaseVersion) {
      return {
        kind: 'dependencies',
        path: `${templateReleaseDetailPath(templateId, preferredReleaseVersion)}?workspaceTab=dependencies`,
        guidance: false,
      }
    }
    return { kind: 'dependencies', guidance: true }
  }

  if (tab === 'releaseVersions') {
    return { kind: 'stripTab' }
  }

  return { kind: 'none' }
}
