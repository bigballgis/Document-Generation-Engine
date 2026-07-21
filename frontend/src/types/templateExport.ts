import type { Schema } from '@/types/openapi'
import type { TemplateDetail } from '@/types/templateCore'

export type TemplateImportConflictPolicy = Schema<'TemplateImportConflictPolicy'>

export type TemplateExportMetadata = Schema<'TemplateExportMetadataView'>

export type TemplateContentModuleReference = Schema<'TemplateExportContentModuleReferenceView'>

/** Not yet modeled in `openapi-v1.yaml` (management content module reference upsert). */
export interface UpsertContentModuleReferencePayload {
  referenceKey: string
  moduleId: string
  semanticVersion: string
}

export type TemplateExportBundle = Schema<'TemplateExportBundleView'>

export type TemplateExportResult = Schema<'TemplateExportResult'>

export type TemplateImportSummary = Schema<'TemplateImportSummaryView'>

export type TemplateImportDependencyReport = Schema<'TemplateImportDependencyReportView'>

export type TemplateImportDependencyItem = Schema<'TemplateImportDependencyItemView'>

export type TemplateImportDryRunResult = Schema<'TemplateImportDryRunResult'>

export type ImportTemplateJsonPayload = Schema<'ImportTemplateRequest'>

/** Multipart ZIP carrier (CE-E01 / Wave 7 promotion packs). */
export interface ImportTemplateZipPayload {
  masterId: string
  file: File
  importConflictPolicy?: TemplateImportConflictPolicy
  dryRun: boolean
}

export type ImportTemplatePayload = ImportTemplateJsonPayload | ImportTemplateZipPayload

export type TemplateImportResult = Omit<Schema<'TemplateImportResult'>, 'template'> & {
  template: TemplateDetail
}

export function isImportTemplateZipPayload(
  payload: ImportTemplatePayload,
): payload is ImportTemplateZipPayload {
  return 'file' in payload && payload.file instanceof File
}
