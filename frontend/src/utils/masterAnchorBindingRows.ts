import type { AnchorBinding } from '@/types/template'
import type { MasterAnchor } from '@/types/master'

export interface MasterAnchorBindingRow {
  anchorId: string
  displayLabel: string
  declaredContentType: string | null
  validationStatus: string | null
  configured: boolean
  binding?: AnchorBinding
}

export function buildMasterAnchorBindingRows(
  masterAnchors: MasterAnchor[],
  bindings: AnchorBinding[],
): MasterAnchorBindingRow[] {
  const bindingByAnchor = new Map(bindings.map((binding) => [binding.anchorId, binding]))
  const masterOrder = new Set(masterAnchors.map((anchor) => anchor.anchorId))

  const rows: MasterAnchorBindingRow[] = masterAnchors.map((anchor) => {
    const binding = bindingByAnchor.get(anchor.anchorId)
    return {
      anchorId: anchor.anchorId,
      displayLabel: anchor.displayLabel,
      declaredContentType: binding?.declaredContentType ?? null,
      validationStatus: binding?.validationStatus ?? null,
      configured: binding != null,
      binding,
    }
  })

  for (const binding of bindings) {
    if (masterOrder.has(binding.anchorId)) {
      continue
    }
    rows.push({
      anchorId: binding.anchorId,
      displayLabel: binding.anchorId,
      declaredContentType: binding.declaredContentType,
      validationStatus: binding.validationStatus ?? null,
      configured: true,
      binding,
    })
  }

  return rows
}
