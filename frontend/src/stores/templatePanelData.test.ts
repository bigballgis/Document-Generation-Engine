import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as templatesApi from '@/api/templates'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'

vi.mock('@/api/templates', () => ({
  listTestDataSets: vi.fn(),
  createTestDataSet: vi.fn(),
  deleteTestDataSet: vi.fn(),
  getTemplateCoverage: vi.fn(),
  getSubmitTestEligibility: vi.fn(),
  listPreviewRuns: vi.fn(),
  startAsyncPreview: vi.fn(),
  listTemplateVersionLines: vi.fn(),
  fetchChangeDiff: vi.fn(),
}))

describe('templatePanelData store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.listTestDataSets).mockReset()
    vi.mocked(templatesApi.createTestDataSet).mockReset()
    vi.mocked(templatesApi.deleteTestDataSet).mockReset()
    vi.mocked(templatesApi.getTemplateCoverage).mockReset()
    vi.mocked(templatesApi.getSubmitTestEligibility).mockReset()
    vi.mocked(templatesApi.listPreviewRuns).mockReset()
    vi.mocked(templatesApi.startAsyncPreview).mockReset()
    vi.mocked(templatesApi.listTemplateVersionLines).mockReset()
    vi.mocked(templatesApi.fetchChangeDiff).mockReset()
  })

  it('caches test data sets per templateId', async () => {
    vi.mocked(templatesApi.listTestDataSets).mockResolvedValue([
      {
        testDataSetId: 'tds-1',
        templateId: 'tpl-1',
        name: 'Sample',
        description: null,
        variables: {},
        required: false,
        locked: false,
        scenarioName: null,
        coverageTags: [],
        datasetVersion: 1,
        derivedFromId: null,
        createdAt: '2026-06-24T10:00:00Z',
        updatedAt: '2026-06-24T10:00:00Z',
      },
    ])

    const store = useTemplatePanelDataStore()
    await store.fetchTestDataSets('tpl-1')

    expect(store.getEntry('tpl-1').testDataSets).toHaveLength(1)
    expect(vi.mocked(templatesApi.listTestDataSets)).toHaveBeenCalledTimes(1)
  })

  it('invalidates coverage after test data set mutation', async () => {
    vi.mocked(templatesApi.createTestDataSet).mockResolvedValue({
      testDataSetId: 'tds-new',
      templateId: 'tpl-1',
      name: 'New',
      description: null,
      variables: {},
      required: false,
      locked: false,
      scenarioName: null,
      coverageTags: [],
      datasetVersion: 1,
      derivedFromId: null,
      createdAt: '2026-06-24T10:00:00Z',
      updatedAt: '2026-06-24T10:00:00Z',
    })
    vi.mocked(templatesApi.listTestDataSets).mockResolvedValue([])

    const store = useTemplatePanelDataStore()
    store.getEntry('tpl-1').coverage = {
      templateId: 'tpl-1',
      aggregatePercentage: 90,
      belowThreshold: false,
      blockerCodes: [],
      dimensions: [],
      appliedThreshold: {
        scopeType: 'GLOBAL',
        groupCode: null,
        minRequiredVariablePct: 80,
        minRequiredSamplePct: 100,
        minAnchorBindingPct: 80,
      },
    }

    await store.createTestDataSet('tpl-1', {
      name: 'New',
      variables: {},
      required: false,
      coverageTags: [],
    })

    expect(store.getEntry('tpl-1').coverage).toBeNull()
    expect(vi.mocked(templatesApi.listTestDataSets)).toHaveBeenCalledTimes(1)
  })

  it('invalidates preview runs after async preview start', async () => {
    vi.mocked(templatesApi.startAsyncPreview).mockResolvedValue({
      previewId: 'prev-1',
      streamUrl: '/stream',
    })
    vi.mocked(templatesApi.listPreviewRuns).mockResolvedValue([
      {
        previewId: 'prev-0',
        templateVersionId: 'ver-1',
        testDataSetId: 'tds-1',
        status: 'SUCCEEDED',
        createdAt: '2026-06-24T10:00:00Z',
        createdBy: 'user-1',
        docxAvailable: true,
        pdfAvailable: false,
        fidelityWarningCount: 0,
        comparisonBlockerCount: 0,
        comparisonWarningCount: 0,
      },
    ])

    const store = useTemplatePanelDataStore()
    await store.fetchPreviewRuns('tpl-1')
    expect(store.getEntry('tpl-1').previewRuns).toHaveLength(1)

    await store.startAsyncPreview('tpl-1', { testDataSetId: 'tds-1' })

    expect(store.getEntry('tpl-1').previewRuns).toHaveLength(0)
    expect(vi.mocked(templatesApi.startAsyncPreview)).toHaveBeenCalledWith('tpl-1', {
      testDataSetId: 'tds-1',
    })
  })

  it('caches coverage and eligibility together', async () => {
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue({
      templateId: 'tpl-1',
      aggregatePercentage: 100,
      belowThreshold: false,
      blockerCodes: [],
      dimensions: [],
      appliedThreshold: {
        scopeType: 'GLOBAL',
        groupCode: null,
        minRequiredVariablePct: 80,
        minRequiredSamplePct: 100,
        minAnchorBindingPct: 80,
      },
    })
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue({
      eligible: true,
      hasValidTestResult: true,
      allSamplesSucceeded: true,
      coverageGatePassed: true,
      failedDataSetNames: [],
      uncoveredAnchors: [],
      uncoveredVariables: [],
    })

    const store = useTemplatePanelDataStore()
    await store.fetchCoverage('tpl-1')

    expect(store.getEntry('tpl-1').coverage?.aggregatePercentage).toBe(100)
    expect(store.getEntry('tpl-1').submitTestEligibility?.eligible).toBe(true)
  })

  it('clears template entry on clearTemplate', async () => {
    vi.mocked(templatesApi.listTestDataSets).mockResolvedValue([])

    const store = useTemplatePanelDataStore()
    await store.fetchTestDataSets('tpl-1')
    store.clearTemplate('tpl-1')

    expect(store.entries['tpl-1']).toBeUndefined()
  })

  it('invalidates version line domains including change diff', async () => {
    const store = useTemplatePanelDataStore()
    store.getEntry('tpl-1').changeDiff = {
      templateId: 'tpl-1',
      hasChanges: true,
      totalChangeCount: 2,
      baselineReleaseVersion: '1.0.0',
      candidateVersionId: 'dev-1',
      dimensions: [],
    }
    store.getEntry('tpl-1').versionLines = {
      page: 0,
      size: 20,
      content: [],
      totalElements: 0,
      totalPages: 0,
    }

    store.invalidateVersionLineDomains('tpl-1')

    expect(store.getEntry('tpl-1').changeDiff).toBeNull()
    expect(store.getEntry('tpl-1').versionLines).toBeNull()
    expect(store.getEntry('tpl-1').releaseVersions).toEqual([])
  })
})
