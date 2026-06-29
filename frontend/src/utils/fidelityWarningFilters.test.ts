import { describe, expect, it } from 'vitest'
import {
  DEFAULT_FIDELITY_WARNING_FILTERS,
  filterFidelityWarnings,
} from '@/utils/fidelityWarningFilters'
import type { FidelityWarning } from '@/types/template'

const sampleWarnings: FidelityWarning[] = [
  {
    code: 'IMAGE_SCALING_ADJUSTED',
    messageKey: 'authoring.fidelity.imageScaling',
    location: 'HEADER:node[0]',
    artifact: 'HEADER',
    viewed: false,
  },
  {
    code: 'UNRESOLVED_VARIABLE',
    messageKey: 'authoring.fidelity.unresolvedVariable',
    location: 'BODY:node[2]',
    artifact: 'BODY',
    viewed: true,
  },
  {
    code: 'IMAGE_SCALING_ADJUSTED',
    messageKey: 'authoring.fidelity.imageScaling',
    location: 'FOOTER:node[1]',
    artifact: 'FOOTER',
    viewed: false,
  },
]

describe('fidelityWarningFilters', () => {
  it('filters by warningCode', () => {
    const filtered = filterFidelityWarnings(sampleWarnings, {
      ...DEFAULT_FIDELITY_WARNING_FILTERS,
      warningCode: 'UNRESOLVED',
    })
    expect(filtered).toHaveLength(1)
    expect(filtered[0]?.code).toBe('UNRESOLVED_VARIABLE')
  })

  it('filters by location substring', () => {
    const filtered = filterFidelityWarnings(sampleWarnings, {
      ...DEFAULT_FIDELITY_WARNING_FILTERS,
      location: 'BODY',
    })
    expect(filtered).toHaveLength(1)
    expect(filtered[0]?.artifact).toBe('BODY')
  })

  it('filters by artifact', () => {
    const filtered = filterFidelityWarnings(sampleWarnings, {
      ...DEFAULT_FIDELITY_WARNING_FILTERS,
      artifact: 'FOOTER',
    })
    expect(filtered).toHaveLength(1)
    expect(filtered[0]?.location).toContain('FOOTER')
  })

  it('filters viewed and unviewed warnings', () => {
    const viewed = filterFidelityWarnings(sampleWarnings, {
      ...DEFAULT_FIDELITY_WARNING_FILTERS,
      viewed: 'viewed',
    })
    expect(viewed).toHaveLength(1)
    expect(viewed[0]?.viewed).toBe(true)

    const unviewed = filterFidelityWarnings(sampleWarnings, {
      ...DEFAULT_FIDELITY_WARNING_FILTERS,
      viewed: 'unviewed',
    })
    expect(unviewed).toHaveLength(2)
    expect(unviewed.every((warning) => !warning.viewed)).toBe(true)
  })
})
