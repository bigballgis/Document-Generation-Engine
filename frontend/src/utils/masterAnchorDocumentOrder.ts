import type { MasterAnchor } from '@/types/master'

/** 1-based document position for UI (API `documentSequence` is 0-based). */
export function toMasterAnchorDocumentPosition(documentSequence: number): number {
  return documentSequence + 1
}

/** Stable document-order sort for CE-U06 position highlight list. */
export function sortMasterAnchorsByDocumentSequence(
  anchors: readonly MasterAnchor[],
): MasterAnchor[] {
  return [...anchors].sort((left, right) => {
    if (left.documentSequence !== right.documentSequence) {
      return left.documentSequence - right.documentSequence
    }
    return left.anchorId.localeCompare(right.anchorId)
  })
}
