import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as securityAuditApi from '@/api/securityAudit'

vi.mock('@/api/http', () => ({
  http: {
    post: vi.fn(),
  },
}))

describe('securityAudit API', () => {
  beforeEach(() => {
    vi.mocked(http.post).mockReset()
  })

  it('posts route-access-denied with routeKey and traceId', async () => {
    vi.mocked(http.post).mockResolvedValue({ status: 204, data: '' })

    await securityAuditApi.reportRouteAccessDenied({
      routeKey: 'route.audit-console',
      traceId: 'TRC-DENY-1',
    })

    expect(http.post).toHaveBeenCalledWith('/security-audit/route-access-denied', {
      routeKey: 'route.audit-console',
      traceId: 'TRC-DENY-1',
    })
  })
})
