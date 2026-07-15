import { describe, expect, it } from 'vitest'
import { buildContentModuleDetailSummaryDescription } from '@/views/contentModules/contentModuleDetailSummary'

describe('buildContentModuleDetailSummaryDescription (SGC-003)', () => {
  const t = (key: string, params?: Record<string, string>) => {
    if (key === 'contentModules.detail.summary.owner') {
      return `Owner: ${params?.groupCode}`
    }
    if (key === 'contentModules.detail.summary.sharedWith') {
      return `Shared with: ${params?.codes}`
    }
    if (key === 'contentModules.detail.summary.notShared') {
      return 'Not shared outside owner group'
    }
    return key
  }

  it('shows owner and shared groups when present', () => {
    const text = buildContentModuleDetailSummaryDescription(
      {
        moduleCode: 'MOD-LOAN',
        groupCode: 'HQ',
        sharedGroupCodes: ['WEALTH', 'RETAIL'],
      },
      t,
    )
    expect(text).toBe('MOD-LOAN · Owner: HQ · Shared with: RETAIL, WEALTH')
  })

  it('shows explicit empty shared state', () => {
    const text = buildContentModuleDetailSummaryDescription(
      {
        moduleCode: 'MOD-LOAN',
        groupCode: 'HQ',
        sharedGroupCodes: [],
      },
      t,
    )
    expect(text).toBe('MOD-LOAN · Owner: HQ · Not shared outside owner group')
  })
})
