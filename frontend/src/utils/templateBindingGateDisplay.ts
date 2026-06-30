import type { AnchorBinding, BindingValidationResult, BindingValidationSummary } from '@/types/template'

export interface BindingGateIssueItem {
  issueKey: 'missingAnchor' | 'duplicateBinding' | 'incompatibleContentType'
  count: number
}

export function mapBindingGateIssueItems(summary: BindingValidationSummary): BindingGateIssueItem[] {
  const items: BindingGateIssueItem[] = []
  if (summary.missingAnchorCount > 0) {
    items.push({ issueKey: 'missingAnchor', count: summary.missingAnchorCount })
  }
  if (summary.duplicateBindingCount > 0) {
    items.push({ issueKey: 'duplicateBinding', count: summary.duplicateBindingCount })
  }
  if (summary.incompatibleContentTypeCount > 0) {
    items.push({ issueKey: 'incompatibleContentType', count: summary.incompatibleContentTypeCount })
  }
  return items
}

export function listInvalidBindings(bindings: AnchorBinding[]): AnchorBinding[] {
  return bindings.filter(
    (binding) => binding.validationStatus !== undefined && binding.validationStatus !== 'VALID',
  )
}

export function resolvePublishGateLoadErrorKey(
  storeErrorKey: string | null | undefined,
): string {
  return storeErrorKey ?? 'templates.error.loadPublishGate'
}

export function shouldShowBindingGatePanel(result: BindingValidationResult | null): boolean {
  return result !== null
}
