import { computed, defineComponent, ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import { useTemplateDetailLoad } from '@/views/templates/useTemplateDetailLoad'

const routerPush = vi.fn()
const routeQuery = ref<Record<string, string | string[]>>({})

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { templateId: 'tpl-1' }, query: routeQuery.value }),
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/auth/roles', () => ({
  canViewCollaborationWorkItems: () => true,
}))

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => ({
    authorTemplates: ref(true),
    context: ref({ roles: ['DOCUMENT_AUTHOR'] }),
  }),
}))

function mountLoad(pinia: ReturnType<typeof createPinia>) {
  const activeDetailTab = ref<TemplateDetailTab>('overview')
  const activeDevWorkspaceTab = ref<TemplateDevWorkspaceTab>('design')
  const loadTemplateHolder = { fn: async () => {} }
  const lastPreview = ref(null)
  const policy = {
    showPolicyPanel: computed(() => false),
    loadPolicyData: vi.fn(async () => {}),
    resetPolicyCredentialsTransientState: vi.fn(),
  }
  const Comp = defineComponent({
    setup() {
      const load = useTemplateDetailLoad({
        isDevEditor: computed(() => false),
        templateId: computed(() => 'tpl-1'),
        devVersionId: computed(() => ''),
        activeDetailTab,
        activeDevWorkspaceTab,
        loadTemplateHolder,
        policy,
        lastPreview,
        resetLifecycleTransientState: vi.fn(),
        resetJourneyEvidenceState: vi.fn(),
      })
      return { load }
    },
    template: '<div></div>',
  })
  const wrapper = mount(Comp, {
    global: { plugins: [pinia, createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus] },
  })
  return {
    wrapper,
    load: (wrapper.vm as { load: ReturnType<typeof useTemplateDetailLoad> }).load,
    store: useTemplatesStore(),
  }
}

describe('useTemplateDetailLoad', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    routerPush.mockReset()
    routeQuery.value = {}
  })

  it('BDD-F6-A3-003: loadTemplate sets loadFailed when fetchTemplate throws', async () => {
    const { load, store, wrapper } = mountLoad(pinia)
    vi.spyOn(store, 'fetchTemplate').mockRejectedValue(new Error('network'))
    await load.loadTemplate()
    expect(load.loadFailed.value).toBe(true)
    wrapper.unmount()
  })

  it('BDD-F6-A3-003: backToList navigates to template management', () => {
    const { load, wrapper } = mountLoad(pinia)
    load.backToList()
    expect(routerPush).toHaveBeenCalledWith(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
    wrapper.unmount()
  })
})
