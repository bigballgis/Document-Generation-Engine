import { describe, expect, it } from 'vitest'
import {
  apiPackageSettingsPath,
  apiPackageSettingsQueryFromLegacyHash,
} from '@/routing/apiPackageSettings'

describe('apiPackageSettings (BDD-SYS-NORM-W2-007/008/011)', () => {
  it('builds canonical package settings path', () => {
    expect(apiPackageSettingsPath('tpl-1')).toBe('/api/packages/tpl-1/settings')
  })

  it('adds releaseVersion and panel deep-link query', () => {
    expect(
      apiPackageSettingsPath('tpl-1', { releaseVersion: '1.0.0', panel: 'credentials' }),
    ).toBe('/api/packages/tpl-1/settings?panel=credentials&releaseVersion=1.0.0')
  })

  it('maps legacy domain hash to settings query', () => {
    expect(apiPackageSettingsQueryFromLegacyHash('#domain=OUTPUT_POLICY')).toEqual({
      panel: 'domain',
      domain: 'OUTPUT_POLICY',
    })
    expect(apiPackageSettingsQueryFromLegacyHash('#apiAccess')).toEqual({})
  })
})
