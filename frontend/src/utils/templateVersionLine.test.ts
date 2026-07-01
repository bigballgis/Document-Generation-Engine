import { describe, expect, it } from 'vitest'
import { isInFlightVersionLine, versionLineDisplayLabel } from '@/utils/templateVersionLine'

describe('templateVersionLine', () => {
  it('detects in-flight lines without release version', () => {
    expect(
      isInFlightVersionLine({
        devVersionId: 'dev-1',
        devVersionNumber: 1,
        releaseVersion: null,
        lifecycleStatus: 'DRAFT',
        updatedAt: '2026-06-23T10:00:00Z',
        updatedBy: '10000003',
        defaultRouteTarget: null,
      }),
    ).toBe(true)
  })

  it('formats release line labels', () => {
    const label = versionLineDisplayLabel(
      (key, params) => `${key}:${JSON.stringify(params ?? {})}`,
      {
        devVersionId: 'dev-1',
        devVersionNumber: 1,
        releaseVersion: '1.0.0',
        lifecycleStatus: 'PUBLISHED',
        updatedAt: '2026-06-23T10:00:00Z',
        updatedBy: '10000003',
        defaultRouteTarget: true,
      },
    )

    expect(label).toContain('templates.versionLines.releaseLabel')
    expect(label).toContain('1.0.0')
  })
})
