import { describe, expect, it } from 'vitest'
import {
  selectPinReleaseVersion,
  truncateMasterFileHash,
} from '@/utils/templateDependencyPin'
import type { TemplateVersionLineSummary } from '@/types/template'

function line(
  partial: Partial<TemplateVersionLineSummary> &
    Pick<TemplateVersionLineSummary, 'devVersionId' | 'devVersionNumber' | 'lineKind'>,
): TemplateVersionLineSummary {
  return {
    releaseVersion: null,
    lifecycleStatus: 'DRAFT',
    updatedAt: '2026-07-17T00:00:00Z',
    updatedBy: '10000001',
    cloneable: false,
    defaultRouteTarget: null,
    ...partial,
  }
}

describe('templateDependencyPin', () => {
  it('prefers defaultRouteTarget published line for pin source', () => {
    const selected = selectPinReleaseVersion([
      line({
        devVersionId: 'dev-2',
        devVersionNumber: 2,
        lineKind: 'IN_FLIGHT',
        lifecycleStatus: 'DRAFT',
      }),
      line({
        devVersionId: 'dev-1b',
        devVersionNumber: 1,
        lineKind: 'PUBLISHED',
        releaseVersion: '2.0.0',
        lifecycleStatus: 'PUBLISHED',
        defaultRouteTarget: false,
      }),
      line({
        devVersionId: 'dev-1a',
        devVersionNumber: 1,
        lineKind: 'PUBLISHED',
        releaseVersion: '1.0.0',
        lifecycleStatus: 'PUBLISHED',
        defaultRouteTarget: true,
      }),
    ])
    expect(selected).toBe('1.0.0')
  })

  it('falls back to first published line when no default route', () => {
    const selected = selectPinReleaseVersion([
      line({
        devVersionId: 'dev-2',
        devVersionNumber: 2,
        lineKind: 'PUBLISHED',
        releaseVersion: '2.0.0',
        lifecycleStatus: 'PUBLISHED',
      }),
      line({
        devVersionId: 'dev-1',
        devVersionNumber: 1,
        lineKind: 'PUBLISHED',
        releaseVersion: '1.0.0',
        lifecycleStatus: 'PUBLISHED',
      }),
    ])
    expect(selected).toBe('2.0.0')
  })

  it('returns null when only in-flight lines exist', () => {
    expect(
      selectPinReleaseVersion([
        line({
          devVersionId: 'dev-1',
          devVersionNumber: 1,
          lineKind: 'IN_FLIGHT',
        }),
      ]),
    ).toBeNull()
  })

  it('truncates long master file hashes', () => {
    expect(truncateMasterFileHash('abcdef0123456789ffff')).toBe('abcdef012345…')
    expect(truncateMasterFileHash('short')).toBe('short')
  })
})
