/**
 * Wave 2 package API settings route helpers (API model A).
 * Canonical shell: `/api/packages/:templateId/settings`
 */

export type ApiPackageSettingsQuery = {
  panel?: string
  releaseVersion?: string
  domain?: string
}

export function apiPackageSettingsPath(
  templateId: string,
  query?: ApiPackageSettingsQuery,
): string {
  const base = `/api/packages/${encodeURIComponent(templateId)}/settings`
  if (!query) {
    return base
  }
  const params = new URLSearchParams()
  if (query.panel) {
    params.set('panel', query.panel)
  }
  if (query.releaseVersion) {
    params.set('releaseVersion', query.releaseVersion)
  }
  if (query.domain) {
    params.set('domain', query.domain)
    if (!query.panel) {
      params.set('panel', 'domain')
    }
  }
  const qs = params.toString()
  return qs ? `${base}?${qs}` : base
}

/** Map legacy `#domain=` / `#apiAccess` hash fragments to settings query. */
export function apiPackageSettingsQueryFromLegacyHash(hash: string): ApiPackageSettingsQuery {
  const normalized = hash.startsWith('#') ? hash.slice(1) : hash
  if (!normalized || normalized === 'apiAccess') {
    return {}
  }
  if (normalized.startsWith('domain=')) {
    return { panel: 'domain', domain: decodeURIComponent(normalized.slice('domain='.length)) }
  }
  return { panel: normalized }
}
