import { describe, expect, it } from 'vitest'
import router from '@/router/index'

describe('legacy API policy detail redirect (BDD-SYS-NORM-W2-011)', () => {
  const legacyRoute = router.getRoutes().find((route) => route.path === '/api/policies/:templateId')
  const settingsRoute = router.getRoutes().find(
    (route) => route.path === '/api/packages/:templateId/settings',
  )

  it('registers package settings shell route', () => {
    expect(settingsRoute?.name).toBe('api-package-settings')
  })

  it('redirects to package settings shell', () => {
    expect(legacyRoute?.redirect).toBeTypeOf('function')
    const target =
      typeof legacyRoute?.redirect === 'function'
        ? legacyRoute.redirect(
            {
              params: { templateId: 'tpl-1' },
              query: {},
            } as never,
            {} as never,
          )
        : legacyRoute?.redirect

    expect(target).toEqual({
      path: '/api/packages/tpl-1/settings',
      query: {},
    })
  })

  it('preserves domain query on settings shell', () => {
    const target =
      typeof legacyRoute?.redirect === 'function'
        ? legacyRoute.redirect(
            {
              params: { templateId: 'tpl-1' },
              query: { domain: 'OUTPUT_POLICY' },
            } as never,
            {} as never,
          )
        : legacyRoute?.redirect

    expect(target).toEqual({
      path: '/api/packages/tpl-1/settings',
      query: {
        domain: 'OUTPUT_POLICY',
        panel: 'domain',
      },
    })
  })
})
