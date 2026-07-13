import { hasUnresolvedPasteBlockers } from '@/utils/pasteCleaningEvidence'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import type { PasteCleaningEvidence } from '@/types/template'
import type { Ref } from 'vue'
import { computed } from 'vue'

export function useTemplateAuthoringBindingsPasteResidue(options: {
  pendingPasteEvidence: Ref<PasteCleaningEvidence | null>
  pendingClearPasteEvidence: Ref<boolean>
  editingRow: { value: MasterAnchorBindingRow | null }
  te: (key: string) => boolean
  t: (key: string) => string
}) {
  const {
    pendingPasteEvidence,
    pendingClearPasteEvidence,
    editingRow,
    te,
    t,
  } = options

  function bindingHasPasteBlockers(row: MasterAnchorBindingRow): boolean {
    return hasUnresolvedPasteBlockers(row.binding?.pasteCleaningEvidence)
  }

  const editingPasteResidueBlocked = computed(() => {
    if (pendingPasteEvidence.value && !hasUnresolvedPasteBlockers(pendingPasteEvidence.value)) {
      return false
    }
    if (pendingClearPasteEvidence.value) {
      return false
    }
    return hasUnresolvedPasteBlockers(editingRow.value?.binding?.pasteCleaningEvidence)
  })

  function pasteResidueItemLabel(messageKey: string): string {
    return te(messageKey) ? t(messageKey) : messageKey
  }

  function handlePasteAccepted(evidence: PasteCleaningEvidence) {
    pendingPasteEvidence.value = evidence
    pendingClearPasteEvidence.value = false
  }

  function clearPendingPasteResidue() {
    pendingPasteEvidence.value = null
    pendingClearPasteEvidence.value = true
  }

  return {
    bindingHasPasteBlockers,
    editingPasteResidueBlocked,
    pasteResidueItemLabel,
    handlePasteAccepted,
    clearPendingPasteResidue,
  }
}
