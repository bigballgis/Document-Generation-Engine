import { describe, expect, it } from 'vitest'
import router from '@/router/index'

describe('legacy API policy detail redirect', () => {
  const legacyRoute = router.getRoutes().find((route) => route.path === '/api/policies/:templateId')

  it('redirects to package hub external access tab', () => {
    expect(legacyRoute?.redirect).toBeTypeOf('function')
    const target =
      typeof legacyRoute?.redirect === 'function'
        ? legacyRoute.redirect({
            params: { templateId: 'tpl-1' },
            query: {},
          } as never, {} as never)
        : legacyRoute?.redirect

    expect(target).toEqual({
      path: '/templates/tpl-1',
      query: { tab: 'apiAccess' },
    })
  })

  it('preserves domain query as hash anchor on hub tab', () => {
    const target =
      typeof legacyRoute?.redirect === 'function'
        ? legacyRoute.redirect({
            params: { templateId: 'tpl-1' },
            query: { domain: 'OUTPUT_POLICY' },
          } as never, {} as never)
        : legacyRoute?.redirect

    expect(target).toEqual({
      path: '/templates/tpl-1',
      query: { tab: 'apiAccess' },
      hash: '#domain=OUTPUT_POLICY',
    })
  })
})
