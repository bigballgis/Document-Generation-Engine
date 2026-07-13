import type { TemplatePanelEntry } from '@/stores/templatePanelDataTypes'
import { createTemplatePanelTestPreviewActions } from '@/stores/createTemplatePanelTestPreviewActions'
import { createTemplatePanelVersionExportActions } from '@/stores/createTemplatePanelVersionExportActions'

/**
 * Panel-domain actions for {@link useTemplatePanelDataStore}.
 * Invalidation rules (SOR-F04) live on the store; actions call the provided invalidators.
 */
export function createTemplatePanelDataActions(deps: {
  entryFor: (templateId: string) => TemplatePanelEntry
  invalidateTestDataSetDomains: (templateId: string) => void
  invalidatePreviewDomains: (templateId: string) => void
  invalidateVersionLineDomains: (templateId: string) => void
  invalidateContentModuleReferenceDomains: (templateId: string) => void
  invalidateBatchTestDomains: (templateId: string) => void
}) {
  return {
    ...createTemplatePanelTestPreviewActions(deps),
    ...createTemplatePanelVersionExportActions(deps),
  }
}
