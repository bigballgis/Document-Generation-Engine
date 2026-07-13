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

export type ImportTemplatePayload = Schema<'ImportTemplateRequest'>

export type TemplateImportResult = Omit<Schema<'TemplateImportResult'>, 'template'> & {
  template: TemplateDetail
}
