import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useVersionLinesPanel } from '@/components/templates/useVersionLinesPanel'
import type { TemplateVersionLineSummary } from '@/types/template'

const versionLines = ref<{
  content: TemplateVersionLineSummary[]
  totalElements: number
  totalPages: number
} | null>(null)

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    te: () => true,
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

vi.mock('@/composables/useLocaleFormatters', () => ({
  useLocaleFormatters: () => ({ formatDateTime: (v: string) => v }),
}))

vi.mock('@/composables/useActivatableTableRow', () => ({
  useActivatableTableRow: () => ({ onRowClick: vi.fn() }),
}))

vi.mock('@/stores/templates', () => ({
  useTemplatesStore: () => ({
    lastErrorMessageKey: null,
    submitting: false,
  }),
}))

vi.mock('@/stores/templatePanelData', () => ({
  useTemplatePanelDataStore: () => ({
    getEntry: () => ({
      loadingVersionLines: false,
      versionLines: versionLines.value,
    }),
    fetchVersionLines: vi.fn().mockResolvedValue(undefined),
    invalidateVersionLineDomains: vi.fn(),
  }),
}))

vi.mock('@/components/templates/useVersionLinesActions', () => ({
  useVersionLinesActions: () => ({
    handleClone: vi.fn(),
    handleCreateFromLatestRelease: vi.fn(),
    handleAbandon: vi.fn(),
    handleVersionAction: vi.fn(),
  }),
}))

function published(cloneable: boolean): TemplateVersionLineSummary {
  return {
    devVersionId: 'pub-1',
    devVersionNumber: 1,
    releaseVersion: '1.0.0',
    lifecycleStatus: 'PUBLISHED',
    approvalSubState: null,
    lineKind: 'PUBLISHED',
    updatedAt: '2026-07-01T00:00:00Z',
    updatedBy: 'u1',
    cloneable,
  }
}

describe('useVersionLinesPanel (FOS-W4-7)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    versionLines.value = {
      content: [published(false)],
      totalElements: 1,
      totalPages: 1,
    }
  })

  it('disables create-from-latest when cloneable=false signals whole-collection in-flight', async () => {
    const Host = defineComponent({
      setup() {
        const panel = useVersionLinesPanel(
          { templateId: 'tpl-1', canClone: true, canManageVersions: true },
          () => undefined,
        )
        return {
          hasInFlightLine: panel.hasInFlightLine,
          showCreateFromLatestRelease: panel.showCreateFromLatestRelease,
          createFromLatestReleaseDisabled: panel.createFromLatestReleaseDisabled,
        }
      },
      template: '<div />',
    })
    const wrapper = mount(Host)
    await flushPromises()

    expect(wrapper.vm.hasInFlightLine).toBe(true)
    expect(wrapper.vm.showCreateFromLatestRelease).toBe(true)
    expect(wrapper.vm.createFromLatestReleaseDisabled).toBe(true)
  })
})
