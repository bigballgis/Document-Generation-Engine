import { computed, defineComponent } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus, { ElMessage } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { PreviewRecord } from '@/types/template'
import { useTemplatesStore } from '@/stores/templates'
import {
  useTemplatePreviewActions,
  type UseTemplatePreviewActionsOptions,
} from '@/views/templates/useTemplatePreviewActions'

const openDevWorkspaceTab = vi.fn()

vi.mock('@/api/templates', () => ({
  listTestDataSets: vi.fn(),
  batchTestGenerate: vi.fn(),
  getPreview: vi.fn(),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  }
})

function makePreview(overrides: Partial<PreviewRecord> = {}): PreviewRecord {
  return {
    previewId: 'prev-1',
    templateId: 'tpl-1',
    templateVersionId: 'ver-1',
    status: 'SUCCEEDED',
    outputFormat: 'DOCX',
    artifactStorageKey: null,
    pdfArtifactStorageKey: null,
    fidelityWarnings: [],
    previewComparison: null,
    testDataSetId: 'ds-1',
    createdAt: '2026-06-23T10:00:00Z',
    ...overrides,
  }
}

function mountPreviewActions(
  pinia: ReturnType<typeof createPinia>,
  overrides: Partial<UseTemplatePreviewActionsOptions> = {},
) {
  const templateId = computed(() => 'tpl-1')
  const errorMessage = computed(() => '')

  const Comp = defineComponent({
    setup() {
      const preview = useTemplatePreviewActions({
        templateId,
        errorMessage,
        openDevWorkspaceTab,
        ...overrides,
      })
      return { preview }
    },
    template: '<div></div>',
  })

  const wrapper = mount(Comp, {
    global: { plugins: [pinia, createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus] },
  })

  return {
    wrapper,
    preview: (wrapper.vm as { preview: ReturnType<typeof useTemplatePreviewActions> }).preview,
    store: useTemplatesStore(),
  }
}

describe('useTemplatePreviewActions', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    openDevWorkspaceTab.mockReset()
    vi.mocked(ElMessage.success).mockReset()
    vi.mocked(ElMessage.error).mockReset()
    vi.mocked(ElMessage.warning).mockReset()
    vi.mocked(templatesApi.listTestDataSets).mockReset()
    vi.mocked(templatesApi.batchTestGenerate).mockReset()
    vi.mocked(templatesApi.getPreview).mockReset()
  })

  it('BDD-F6-A1-001: handleTestGenerate sets generatingPreview and updates preview on success', async () => {
    const previewRecord = makePreview()
    const store = useTemplatesStore()
    vi.spyOn(store, 'testGenerate').mockResolvedValue(previewRecord)

    const { preview, wrapper } = mountPreviewActions(pinia)
    const generatePromise = preview.handleTestGenerate('ds-1')
    expect(preview.generatingPreview.value).toBe(true)
    await generatePromise
    await flushPromises()

    expect(preview.generatingPreview.value).toBe(false)
    expect(preview.lastPreview.value).toEqual(previewRecord)
    expect(preview.selectedPreviewId.value).toBe('prev-1')
    expect(preview.selectedTestDataSetId.value).toBe('ds-1')
    expect(openDevWorkspaceTab).toHaveBeenCalledWith('testing')
    expect(ElMessage.success).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('BDD-F6-A1-001: handleTestGenerate shows error on failure', async () => {
    const store = useTemplatesStore()
    vi.spyOn(store, 'testGenerate').mockRejectedValue(new Error('fail'))

    const { preview, wrapper } = mountPreviewActions(pinia)
    await preview.handleTestGenerate('ds-1')
    await flushPromises()

    expect(preview.generatingPreview.value).toBe(false)
    expect(ElMessage.error).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('BDD-CE-U18-BTH-005/006: bumpCoverageRefresh does not call sync batchTestGenerate', () => {
    const { preview, wrapper } = mountPreviewActions(pinia)
    const tokenBefore = preview.coverageRefreshToken.value

    preview.bumpCoverageRefresh()

    expect(preview.coverageRefreshToken.value).toBe(tokenBefore + 1)
    expect(templatesApi.batchTestGenerate).not.toHaveBeenCalled()
    expect(ElMessage.success).not.toHaveBeenCalled()
    expect(preview).not.toHaveProperty('handleBatchTestGenerate')
    wrapper.unmount()
  })

  it('BDD-F6-A1-003: handlePreviewSelected clears lastPreview when previewId is null', async () => {
    const { preview, wrapper } = mountPreviewActions(pinia)
    preview.lastPreview.value = makePreview()
    preview.selectedPreviewId.value = 'prev-1'

    await preview.handlePreviewSelected(null)

    expect(preview.lastPreview.value).toBeNull()
    expect(preview.selectedPreviewId.value).toBeNull()
    wrapper.unmount()
  })

  it('BDD-F6-A1-003: handlePreviewSelected loads preview from API', async () => {
    const previewRecord = makePreview({ previewId: 'prev-2' })
    vi.mocked(templatesApi.getPreview).mockResolvedValue(previewRecord)

    const { preview, wrapper } = mountPreviewActions(pinia)
    await preview.handlePreviewSelected('prev-2')
    await flushPromises()

    expect(templatesApi.getPreview).toHaveBeenCalledWith('tpl-1', 'prev-2')
    expect(preview.lastPreview.value).toEqual(previewRecord)
    expect(preview.selectedPreviewId.value).toBe('prev-2')
    wrapper.unmount()
  })
})
