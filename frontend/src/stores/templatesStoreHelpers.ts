import type { Ref } from 'vue'
import type { TemplateDetail, TemplateSummary } from '@/types/template'

export function toTemplateSummary(
  detail: TemplateDetail,
  templates: TemplateSummary[],
): TemplateSummary {
  const existing = templates.find((item) => item.id === detail.id)
  const releaseVersionCount =
    existing?.releaseVersionCount ?? (detail.releaseVersion ? 1 : 0)
  return {
    id: detail.id,
    externalId: detail.externalId,
    groupCode: detail.groupCode,
    name: detail.name,
    lifecycleStatus: detail.lifecycleStatus,
    approvalSubState: detail.approvalSubState,
    releaseVersion: detail.releaseVersion,
    releaseVersionCount,
    masterId: detail.masterId,
    updatedBy: existing?.updatedBy ?? '',
    updatedAt: detail.updatedAt,
    nextReviewDue: detail.nextReviewDue ?? existing?.nextReviewDue ?? null,
    locale: detail.locale ?? existing?.locale,
    localeVariantFamilyId:
      detail.localeVariantFamilyId ?? existing?.localeVariantFamilyId ?? null,
  }
}

/** CE-G05 — merge annual-review complete summary into selected detail + catalog row. */
export function applyAnnualReviewSummary(
  selectedTemplate: Ref<TemplateDetail | null>,
  templates: Ref<TemplateSummary[]>,
  summary: TemplateSummary,
) {
  if (selectedTemplate.value?.id === summary.id) {
    selectedTemplate.value = {
      ...selectedTemplate.value,
      nextReviewDue: summary.nextReviewDue ?? null,
      updatedAt: summary.updatedAt,
      lifecycleStatus: summary.lifecycleStatus,
      name: summary.name,
    }
  }
  templates.value = templates.value.map((item) =>
    item.id === summary.id
      ? {
          ...item,
          ...summary,
          nextReviewDue: summary.nextReviewDue ?? null,
        }
      : item,
  )
}

export function applyUpdatedTemplate(
  selectedTemplate: Ref<TemplateDetail | null>,
  templates: Ref<TemplateSummary[]>,
  updated: TemplateDetail,
) {
  selectedTemplate.value = updated
  templates.value = templates.value.map((item) =>
    item.id === updated.id ? toTemplateSummary(updated, templates.value) : item,
  )
}
