import { defineStore } from 'pinia'
import { ref } from 'vue'
import { createTemplatePanelDataActions } from '@/stores/createTemplatePanelDataActions'
import {
  createEmptyTemplatePanelEntry,
  type TemplatePanelEntry,
} from '@/stores/templatePanelDataTypes'

export type { TemplatePanelEntry } from '@/stores/templatePanelDataTypes'

/**
 * Invalidation rules (SOR-F04):
 * - Test data set mutations → testDataSets, coverage, submitTestEligibility
 * - Preview start / preview refresh → previewRuns (+ coverage/eligibility when preview completes elsewhere)
 * - Version line mutations → versionLines, releaseVersions, changeDiff
 * - Content module reference mutations → contentModuleReferences, coverage, changeDiff
 * - Batch test run → batchTestHistory, coverage, submitTestEligibility
 */
export const useTemplatePanelDataStore = defineStore('templatePanelData', () => {
  const entries = ref<Record<string, TemplatePanelEntry>>({})

  function entryFor(templateId: string): TemplatePanelEntry {
    if (!entries.value[templateId]) {
      entries.value = { ...entries.value, [templateId]: createEmptyTemplatePanelEntry() }
    }
    return entries.value[templateId]!
  }

  function clearTemplate(templateId: string) {
    const next = { ...entries.value }
    delete next[templateId]
    entries.value = next
  }

  function invalidateTestDataSetDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.testDataSets = []
    entry.coverage = null
    entry.submitTestEligibility = null
  }

  function invalidatePreviewDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.previewRuns = []
    entry.previewsById = {}
  }

  function invalidateVersionLineDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.versionLines = null
    entry.releaseVersions = []
    entry.changeDiff = null
  }

  function invalidateContentModuleReferenceDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.contentModuleReferences = []
    entry.coverage = null
    entry.submitTestEligibility = null
    entry.changeDiff = null
  }

  function invalidateBatchTestDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.batchTestHistory = []
    entry.coverage = null
    entry.submitTestEligibility = null
  }

  const actions = createTemplatePanelDataActions({
    entryFor,
    invalidateTestDataSetDomains,
    invalidatePreviewDomains,
    invalidateVersionLineDomains,
    invalidateContentModuleReferenceDomains,
    invalidateBatchTestDomains,
  })

  function getEntry(templateId: string): TemplatePanelEntry {
    return entryFor(templateId)
  }

  return {
    entries,
    clearTemplate,
    invalidateTestDataSetDomains,
    invalidatePreviewDomains,
    invalidateVersionLineDomains,
    invalidateContentModuleReferenceDomains,
    invalidateBatchTestDomains,
    ...actions,
    getEntry,
  }
})
