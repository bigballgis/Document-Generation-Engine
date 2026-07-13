import * as templatesApi from '@/api/templates'
import type { PageView } from '@/types/identity'
import type {
  ChangeDiffSummary,
  TemplateContentModuleReference,
  TemplateDetail,
  TemplateDevVersionCreated,
  TemplateExportResult,
  TemplateReleaseVersion,
  TemplateVersionLineSummary,
  UpsertContentModuleReferencePayload,
} from '@/types/template'
import type { TemplatePanelEntry } from '@/stores/templatePanelDataTypes'
import { releaseVersionDetailKey } from '@/stores/templatePanelDataTypes'

export function createTemplatePanelVersionExportActions(deps: {
  entryFor: (templateId: string) => TemplatePanelEntry
  invalidateVersionLineDomains: (templateId: string) => void
  invalidateContentModuleReferenceDomains: (templateId: string) => void
}) {
  const { entryFor, invalidateVersionLineDomains, invalidateContentModuleReferenceDomains } = deps

  async function fetchVersionLines(
    templateId: string,
    page: number,
    size: number,
  ): Promise<PageView<TemplateVersionLineSummary>> {
    const entry = entryFor(templateId)
    entry.loadingVersionLines = true
    try {
      const pageView = await templatesApi.listTemplateVersionLines(templateId, page, size)
      entry.versionLines = {
        page: pageView.page,
        size: pageView.size,
        content: pageView.content,
        totalElements: pageView.totalElements,
        totalPages: pageView.totalPages,
      }
      return pageView
    } finally {
      entry.loadingVersionLines = false
    }
  }

  async function cloneReleaseVersion(
    templateId: string,
    releaseVersion: string,
  ): Promise<TemplateDevVersionCreated> {
    const created = await templatesApi.cloneReleaseVersion(templateId, releaseVersion)
    invalidateVersionLineDomains(templateId)
    return created
  }

  async function abandonDevVersion(templateId: string, devVersionId: string): Promise<void> {
    await templatesApi.abandonDevVersion(templateId, devVersionId)
    invalidateVersionLineDomains(templateId)
  }

  async function fetchChangeDiff(templateId: string): Promise<ChangeDiffSummary | null> {
    const entry = entryFor(templateId)
    entry.loadingChangeDiff = true
    try {
      entry.changeDiff = await templatesApi.fetchChangeDiff(templateId)
      return entry.changeDiff
    } finally {
      entry.loadingChangeDiff = false
    }
  }

  async function fetchReleaseVersions(templateId: string): Promise<TemplateReleaseVersion[]> {
    const entry = entryFor(templateId)
    entry.loadingReleaseVersions = true
    try {
      entry.releaseVersions = await templatesApi.fetchReleaseVersions(templateId)
      return entry.releaseVersions
    } finally {
      entry.loadingReleaseVersions = false
    }
  }

  async function fetchContentModuleReferences(
    templateId: string,
  ): Promise<TemplateContentModuleReference[]> {
    const entry = entryFor(templateId)
    entry.loadingContentModuleReferences = true
    try {
      entry.contentModuleReferences = await templatesApi.listTemplateContentModuleReferences(templateId)
      return entry.contentModuleReferences
    } finally {
      entry.loadingContentModuleReferences = false
    }
  }

  async function upsertContentModuleReference(
    templateId: string,
    referenceKey: string,
    payload: UpsertContentModuleReferencePayload,
  ): Promise<TemplateContentModuleReference> {
    const reference = await templatesApi.upsertTemplateContentModuleReference(
      templateId,
      referenceKey,
      payload,
    )
    invalidateContentModuleReferenceDomains(templateId)
    await fetchContentModuleReferences(templateId)
    return reference
  }

  async function fetchReleaseVersionDetail(
    templateId: string,
    releaseVersion: string,
  ): Promise<TemplateDetail> {
    const entry = entryFor(templateId)
    const key = releaseVersionDetailKey(templateId, releaseVersion)
    entry.loadingReleaseVersionDetail = { ...entry.loadingReleaseVersionDetail, [key]: true }
    try {
      const detail = await templatesApi.fetchReleaseVersionDetail(templateId, releaseVersion)
      entry.releaseVersionDetails = { ...entry.releaseVersionDetails, [key]: detail }
      return detail
    } finally {
      entry.loadingReleaseVersionDetail = { ...entry.loadingReleaseVersionDetail, [key]: false }
    }
  }

  async function exportTemplateJson(templateId: string): Promise<TemplateExportResult> {
    const entry = entryFor(templateId)
    entry.exporting = true
    try {
      return await templatesApi.exportTemplateJson(templateId)
    } finally {
      entry.exporting = false
    }
  }

  async function exportTemplateZip(templateId: string): Promise<{ blob: Blob; filename: string }> {
    const entry = entryFor(templateId)
    entry.exporting = true
    try {
      return await templatesApi.exportTemplateZip(templateId)
    } finally {
      entry.exporting = false
    }
  }

  return {
    fetchVersionLines,
    cloneReleaseVersion,
    abandonDevVersion,
    fetchChangeDiff,
    fetchReleaseVersions,
    fetchContentModuleReferences,
    upsertContentModuleReference,
    fetchReleaseVersionDetail,
    exportTemplateJson,
    exportTemplateZip,
  }
}
