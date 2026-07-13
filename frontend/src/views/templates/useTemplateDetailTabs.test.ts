import { computed, defineComponent, ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import { useTemplateDetailTabs } from '@/views/templates/useTemplateDetailTabs'

const routerReplace = vi.fn()
const routeQuery = ref<Record<string, string | string[]>>({})

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { templateId: 'tpl-1' }, query: routeQuery.value }),
  useRouter: () => ({ replace: routerReplace }),
}))

function mountTabs(isDevEditor = false, showPolicyPanel = false) {
  const activeDetailTab = ref<TemplateDetailTab>('overview')
  const activeDevWorkspaceTab = ref<TemplateDevWorkspaceTab>('design')
  const Comp = defineComponent({
    setup() {
      const tabs = useTemplateDetailTabs({
        isDevEditor: computed(() => isDevEditor),
        showAuthoringSection: computed(() => true),
        showPolicyPanel: computed(() => showPolicyPanel),
        activeDetailTab,
        activeDevWorkspaceTab,
      })
      return { tabs, activeDetailTab }
    },
    template: '<div></div>',
  })
  const wrapper = mount(Comp, {
    global: { plugins: [createPinia(), createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus] },
  })
  return {
    wrapper,
    tabs: (wrapper.vm as { tabs: ReturnType<typeof useTemplateDetailTabs> }).tabs,
    activeDetailTab,
  }
}

describe('useTemplateDetailTabs', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerReplace.mockReset()
    routeQuery.value = {}
  })

  it('BDD-F6-A3-001: detailTabs includes overview and lifecycle', () => {
    const { tabs, wrapper } = mountTabs()
    const names = tabs.detailTabs.value.map((t) => t.name)
    expect(names).toContain('overview')
    expect(names).toContain('lifecycle')
    expect(names).toContain('authoring')
    expect(names).not.toContain('apiAccess')
    wrapper.unmount()
  })

  it('SCEN-AOD-03: detailTabs registers apiAccess when showPolicyPanel is true', () => {
    const { tabs, wrapper } = mountTabs(false, true)
    expect(tabs.detailTabs.value.map((t) => t.name)).toContain('apiAccess')
    wrapper.unmount()
  })

  it('BDD-F6-A3-001: syncTabFromRoute sets lifecycle tab when focus=lifecycle', () => {
    routeQuery.value = { focus: 'lifecycle', tab: 'overview' }
    const { tabs, activeDetailTab, wrapper } = mountTabs()
    tabs.syncTabFromRoute()
    expect(activeDetailTab.value).toBe('lifecycle')
    expect(routerReplace).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('BDD-F6-A3-001: openDevWorkspaceTab updates tab and route', () => {
    const { tabs, wrapper } = mountTabs(true)
    tabs.openDevWorkspaceTab('testing')
    expect(routerReplace).toHaveBeenCalled()
    wrapper.unmount()
  })
})
