import { describe, expect, it } from 'vitest'
import {
  canOpenRenderedCompare,
  isComparablePreviewRun,
  resolveRenderedCompareHintKey,
} from '@/components/templates/renderedCompareSelection'

describe('renderedCompareSelection (BDD-IBL-C2-003)', () => {
  it('treats only SUCCEEDED runs with PDF as comparable', () => {
    expect(isComparablePreviewRun({ status: 'SUCCEEDED', pdfAvailable: true })).toBe(true)
    expect(isComparablePreviewRun({ status: 'SUCCEEDED', pdfAvailable: false })).toBe(false)
    expect(isComparablePreviewRun({ status: 'FAILED', pdfAvailable: true })).toBe(false)
    expect(isComparablePreviewRun({ status: 'PROCESSING', pdfAvailable: true })).toBe(false)
  })

  it('enables compare only when exactly two comparable runs are selected', () => {
    const a = { status: 'SUCCEEDED' as const, pdfAvailable: true }
    const b = { status: 'SUCCEEDED' as const, pdfAvailable: true }
    const noPdf = { status: 'SUCCEEDED' as const, pdfAvailable: false }
    const failed = { status: 'FAILED' as const, pdfAvailable: true }

    expect(canOpenRenderedCompare([])).toBe(false)
    expect(canOpenRenderedCompare([a])).toBe(false)
    expect(canOpenRenderedCompare([a, b])).toBe(true)
    expect(canOpenRenderedCompare([a, b, a])).toBe(false)
    expect(canOpenRenderedCompare([a, noPdf])).toBe(false)
    expect(canOpenRenderedCompare([a, failed])).toBe(false)
  })

  it('resolves English-first hint keys for selection states', () => {
    const a = { status: 'SUCCEEDED' as const, pdfAvailable: true }
    const b = { status: 'SUCCEEDED' as const, pdfAvailable: true }
    const noPdf = { status: 'SUCCEEDED' as const, pdfAvailable: false }

    expect(resolveRenderedCompareHintKey([])).toBe(
      'templates.previewHistory.renderedCompare.hintNone',
    )
    expect(resolveRenderedCompareHintKey([a])).toBe(
      'templates.previewHistory.renderedCompare.hintOne',
    )
    expect(resolveRenderedCompareHintKey([a, b, a])).toBe(
      'templates.previewHistory.renderedCompare.hintTooMany',
    )
    expect(resolveRenderedCompareHintKey([a, noPdf])).toBe(
      'templates.previewHistory.renderedCompare.hintIneligible',
    )
    expect(resolveRenderedCompareHintKey([a, b])).toBe(
      'templates.previewHistory.renderedCompare.hintReady',
    )
  })
})
