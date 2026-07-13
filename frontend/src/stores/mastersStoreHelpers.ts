import type { Ref } from 'vue'
import type { MasterDocumentDetail, MasterDocumentSummary } from '@/types/master'

export function toMasterSummary(detail: MasterDocumentDetail): MasterDocumentSummary {
  return {
    id: detail.id,
    groupCode: detail.groupCode,
    name: detail.name,
    status: detail.status,
    originalFilename: detail.originalFilename,
    anchorCount: detail.anchors.length,
    updatedBy: detail.updatedBy,
    updatedAt: detail.updatedAt,
  }
}

export function applyUpdatedMaster(
  selectedMaster: Ref<MasterDocumentDetail | null>,
  masters: Ref<MasterDocumentSummary[]>,
  updated: MasterDocumentDetail,
) {
  selectedMaster.value = updated
  masters.value = masters.value.map((item) =>
    item.id === updated.id ? toMasterSummary(updated) : item,
  )
}
