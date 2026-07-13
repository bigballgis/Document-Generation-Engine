import { describe, expect, it } from 'vitest'
import {
  NOTIFICATION_POLL_INTERVAL_MS,
  buildNotificationTaskHubLocation,
  formatUnreadBadgeLabel,
} from '@/utils/notificationCenter'

describe('notificationCenter utils', () => {
  it('exposes default poll interval of 30 seconds', () => {
    expect(NOTIFICATION_POLL_INTERVAL_MS).toBe(30_000)
  })

  it('hides badge when unread is zero', () => {
    expect(formatUnreadBadgeLabel(0)).toBeNull()
  })

  it('shows exact count for 1–99', () => {
    expect(formatUnreadBadgeLabel(1)).toBe('1')
    expect(formatUnreadBadgeLabel(99)).toBe('99')
  })

  it('caps badge display at 99+', () => {
    expect(formatUnreadBadgeLabel(100)).toBe('99+')
    expect(formatUnreadBadgeLabel(250)).toBe('99+')
  })

  it('builds dashboard deep-link with queue and tasks-section hash', () => {
    expect(buildNotificationTaskHubLocation('TEST')).toEqual({
      path: '/dashboard',
      query: { queue: 'TEST' },
      hash: '#tasks-section',
    })
    expect(buildNotificationTaskHubLocation('ESCALATION')).toEqual({
      path: '/dashboard',
      query: { queue: 'ESCALATION' },
      hash: '#tasks-section',
    })
  })
})
