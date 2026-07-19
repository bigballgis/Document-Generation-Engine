import type { PreviewRunSummary } from '@/types/template'

export type RenderedCompareHintKey =
  | 'templates.previewHistory.renderedCompare.hintNone'
  | 'templates.previewHistory.renderedCompare.hintOne'
  | 'templates.previewHistory.renderedCompare.hintTooMany'
  | 'templates.previewHistory.renderedCompare.hintIneligible'
  | 'templates.previewHistory.renderedCompare.hintReady'

export type ComparablePreviewRun = Pick<PreviewRunSummary, 'status' | 'pdfAvailable'>

/** SUCCEEDED preview runs that expose a PDF artifact are eligible for side-by-side compare. */
export function isComparablePreviewRun(run: ComparablePreviewRun): boolean {
  return run.status === 'SUCCEEDED' && run.pdfAvailable
}

/** Exactly two eligible runs must be selected before Compare rendered outputs is enabled. */
export function canOpenRenderedCompare(selected: ComparablePreviewRun[]): boolean {
  return selected.length === 2 && selected.every(isComparablePreviewRun)
}

export function resolveRenderedCompareHintKey(selected: ComparablePreviewRun[]): RenderedCompareHintKey {
  const count = selected.length
  if (count === 0) {
    return 'templates.previewHistory.renderedCompare.hintNone'
  }
  if (count === 1) {
    return 'templates.previewHistory.renderedCompare.hintOne'
  }
  if (count > 2) {
    return 'templates.previewHistory.renderedCompare.hintTooMany'
  }
  if (!canOpenRenderedCompare(selected)) {
    return 'templates.previewHistory.renderedCompare.hintIneligible'
  }
  return 'templates.previewHistory.renderedCompare.hintReady'
}
