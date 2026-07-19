import { listAllContentModules } from '@/api/contentModules'
import { listAllTemplates } from '@/api/templates'
import type { ContentModuleSummary } from '@/types/contentModule'
import type { TemplateSummary } from '@/types/template'

export type LocaleVariantSibling = {
  id: string
  code: string
  name: string
  locale: string
  lifecycleLabel?: string | null
}

/**
 * Loads authorized locale-variant siblings for a template package (IBL-E1).
 * Uses catalog list (auth-scoped); no dedicated siblings endpoint.
 */
export async function fetchTemplateLocaleVariantSiblings(options: {
  templateId: string
  groupCode: string
  localeVariantFamilyId?: string | null
  signal?: AbortSignal
}): Promise<LocaleVariantSibling[]> {
  const familyId = options.localeVariantFamilyId?.trim()
  if (!familyId) {
    return []
  }
  const page = await listAllTemplates({
    groupCode: options.groupCode,
    signal: options.signal,
  })
  return page.content
    .filter(
      (row) =>
        row.localeVariantFamilyId === familyId &&
        row.id !== options.templateId &&
        Boolean(row.locale),
    )
    .map((row) => toTemplateSibling(row))
    .sort((a, b) => a.locale.localeCompare(b.locale))
}

/**
 * Loads authorized locale-variant siblings for a content module (IBL-E1).
 */
export async function fetchContentModuleLocaleVariantSiblings(options: {
  moduleId: string
  groupCode: string
  localeVariantFamilyId?: string | null
  signal?: AbortSignal
}): Promise<LocaleVariantSibling[]> {
  const familyId = options.localeVariantFamilyId?.trim()
  if (!familyId) {
    return []
  }
  const page = await listAllContentModules({
    groupCode: options.groupCode,
    signal: options.signal,
  })
  return page.content
    .filter(
      (row) =>
        row.localeVariantFamilyId === familyId &&
        row.moduleId !== options.moduleId &&
        Boolean(row.locale),
    )
    .map((row) => toContentModuleSibling(row))
    .sort((a, b) => a.locale.localeCompare(b.locale))
}

function toTemplateSibling(row: TemplateSummary): LocaleVariantSibling {
  return {
    id: row.id,
    code: row.externalId,
    name: row.name,
    locale: row.locale ?? '',
    lifecycleLabel: row.lifecycleStatus,
  }
}

function toContentModuleSibling(row: ContentModuleSummary): LocaleVariantSibling {
  return {
    id: row.moduleId,
    code: row.moduleCode,
    name: row.name,
    locale: row.locale ?? '',
    lifecycleLabel: row.lifecycleState ?? row.reviewState,
  }
}
