import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import {
  collectAllPageContent,
  type CollectedCatalogPage,
} from '@/api/catalogPageCollect'
import { normalizeGroupCode } from '@/api/templatesNormalize'
import type { ApiEnvelope } from '@/types/session'
import type { PageView } from '@/types/identity'
import type {
  CreateTemplatePayload,
  DeleteTemplatePayload,
  ImportTemplatePayload,
  TemplateDetail,
  TemplateExportResult,
  TemplateImportDryRunResult,
  TemplateImportResult,
  TemplateSummary,
} from '@/types/template'
import { isImportTemplateZipPayload } from '@/types/template'

export type TemplateListQueryOptions = {
  signal?: AbortSignal
  search?: string
  groupCode?: string
  lifecycleStatus?: string
  approvalSubState?: string
  /** IBL-E1 — optional exact BCP-47 locale filter (AND with other filters). */
  locale?: string
  sort?: string
}

export async function listTemplates(
  page = 0,
  size = 20,
  options: TemplateListQueryOptions = {},
): Promise<PageView<TemplateSummary>> {
  const params: Record<string, string | number> = { page, size }
  if (options.search) {
    params.search = options.search
  }
  const groupCode = normalizeGroupCode(options.groupCode)
  if (groupCode) {
    params.groupCode = groupCode
  }
  if (options.lifecycleStatus) {
    params.lifecycleStatus = options.lifecycleStatus
  }
  if (options.approvalSubState) {
    params.approvalSubState = options.approvalSubState
  }
  const locale = options.locale?.trim()
  if (locale) {
    params.locale = locale
  }
  if (options.sort) {
    params.sort = options.sort
  }
  const response = await http.get<ApiEnvelope<PageView<TemplateSummary>>>('/templates', {
    params,
    signal: options.signal,
  })
  return unwrapEnvelope(response.data)
}

/** Multi-page merge for dashboard / picker consumers (LR-C5 PageView). */
export async function listAllTemplates(
  options: TemplateListQueryOptions = {},
): Promise<CollectedCatalogPage<TemplateSummary>> {
  return collectAllPageContent((page, size) => listTemplates(page, size, options))
}

export async function createTemplate(payload: CreateTemplatePayload): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>('/templates', payload)
  return unwrapEnvelope(response.data)
}

export async function deleteTemplate(
  templateId: string,
  payload: DeleteTemplatePayload,
): Promise<void> {
  await http.delete(`/templates/${templateId}`, { data: payload })
}

export async function exportTemplateJson(templateId: string): Promise<TemplateExportResult> {
  const response = await http.get<ApiEnvelope<TemplateExportResult>>(`/templates/${templateId}/export`)
  return unwrapEnvelope(response.data)
}

export async function exportTemplateZip(templateId: string): Promise<{ blob: Blob; filename: string }> {
  const response = await http.get<Blob>(`/templates/${templateId}/export`, {
    params: { format: 'zip' },
    responseType: 'blob',
  })
  const disposition = response.headers['content-disposition'] ?? ''
  const filenameMatch = /filename="([^"]+)"/i.exec(disposition)
  const filename = filenameMatch?.[1] ?? 'template-export.zip'
  return { blob: response.data, filename }
}

function buildImportMultipartFormData(payload: {
  masterId: string
  file: File
  importConflictPolicy?: string
  dryRun: boolean
}): FormData {
  const formData = new FormData()
  formData.append('masterId', payload.masterId)
  formData.append('file', payload.file)
  if (payload.importConflictPolicy) {
    formData.append('importConflictPolicy', payload.importConflictPolicy)
  }
  formData.append('dryRun', payload.dryRun ? 'true' : 'false')
  return formData
}

export async function importTemplate(
  payload: ImportTemplatePayload & { dryRun: true },
): Promise<TemplateImportDryRunResult>
export async function importTemplate(
  payload: ImportTemplatePayload & { dryRun?: false },
): Promise<TemplateImportResult>
export async function importTemplate(
  payload: ImportTemplatePayload,
): Promise<TemplateImportResult | TemplateImportDryRunResult> {
  if (isImportTemplateZipPayload(payload)) {
    const response = await http.post<
      ApiEnvelope<TemplateImportResult | TemplateImportDryRunResult>
    >('/templates/import', buildImportMultipartFormData(payload), {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return unwrapEnvelope(response.data)
  }

  const response = await http.post<ApiEnvelope<TemplateImportResult | TemplateImportDryRunResult>>(
    '/templates/import',
    payload,
  )
  return unwrapEnvelope(response.data)
}
