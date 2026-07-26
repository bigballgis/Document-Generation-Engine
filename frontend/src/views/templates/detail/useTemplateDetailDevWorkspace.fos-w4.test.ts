import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick, ref } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useTemplateDetailDevWorkspace } from '@/views/templates/detail/useTemplateDetailDevWorkspace'

const refresh = vi.fn().mockResolvedValue(undefined)
const tooltipContent = ref('')
const loadError = ref<string | null>(null)
const isEligible = ref(false)

vi.mock('@/composables/useSubmitTestEligibility', () => ({
  useSubmitTestEligibility: () => ({
    isEligible,
    tooltipContent,
    loadError,
    refresh,
  }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: { workspaceTab: 'testing' },
  }),
  useRouter: () => ({
    replace: vi.fn(),
  }),
}))

vi.mock('@/stores/templatePanelData', () => ({
  useTemplatePanelDataStore: () => ({
    getEntry: () => ({
      testDataSets: [],
      loadingTestDataSets: false,
    }),
    runBatchTest: vi.fn(),
  }),
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}))

vi.mock('@/composables/workspaceTabDirtyLeave', () => ({
  requestWorkspaceTabLeave: vi.fn(async () => true),
}))

describe('useTemplateDetailDevWorkspace (FOS-W4-2)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    refresh.mockClear()
    loadError.value = null
    isEligible.value = false
    tooltipContent.value = ''
  })

  it('refreshes eligibility immediately when testing tab is active on mount', async () => {
    const Host = defineComponent({
      setup() {
        return useTemplateDetailDevWorkspace({
          templateId: 'tpl-1',
          openSubmitForTestDialog: false,
          onClearOpenSubmitForTestDialog: () => undefined,
          onSubmitForTest: () => undefined,
          onBatchCompleted: () => undefined,
        })
      },
      template: '<div />',
    })

    mount(Host)
    await flushPromises()
    await nextTick()

    expect(refresh).toHaveBeenCalledTimes(1)
  })
})
