import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus, { ElMessageBox } from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ContentModuleSharedGroupsSettingsDialog from '@/components/contentModules/ContentModuleSharedGroupsSettingsDialog.vue'
import en from '@/i18n/locales/en'
import { useContentModulesStore } from '@/stores/contentModules'

const groupOptions = {
  value: [
    { value: 'HQ', label: 'HQ' },
    { value: 'RETAIL', label: 'RETAIL' },
    { value: 'WEALTH', label: 'WEALTH' },
  ],
}

vi.mock('@/composables/useScopedGroupOptions', () => ({
  useScopedGroupOptions: () => ({
    groupOptions,
    ensureGroupCatalog: vi.fn().mockResolvedValue(undefined),
  }),
}))

type SettingsExposed = {
  setSelectedSharedGroupCodes: (codes: string[]) => void
  handleSave: () => Promise<void>
}

describe('ContentModuleSharedGroupsSettingsDialog (CE-U10)', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.spyOn(ElMessageBox, 'confirm').mockReset()
  })

  it('SGC-005: canceling confirm does not send update; confirming does', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleSharedGroupsSettingsDialog, {
      props: {
        modelValue: true,
        moduleId: 'mod-1',
        ownerGroupCode: 'HQ',
        sharedGroupCodes: ['RETAIL'],
        canConfigure: true,
      },
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()

    const store = useContentModulesStore()
    const updateShared = vi.spyOn(store, 'updateSharedGroupCodes').mockResolvedValue({
      moduleId: 'mod-1',
      moduleCode: 'MOD',
      groupCode: 'HQ',
      name: 'Loan',
      sharedGroupCodes: ['RETAIL', 'WEALTH'],
      versions: [],
      reviewHistory: [],
    })

    const vm = wrapper.vm as unknown as SettingsExposed
    vm.setSelectedSharedGroupCodes(['RETAIL', 'WEALTH'])

    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')
    await vm.handleSave()
    await flushPromises()
    expect(updateShared).not.toHaveBeenCalled()

    vi.mocked(ElMessageBox.confirm).mockResolvedValueOnce('confirm' as never)
    await vm.handleSave()
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(updateShared).toHaveBeenCalledWith('mod-1', {
      sharedGroupCodes: ['RETAIL', 'WEALTH'],
    })
  })

  it('SGC-004: save without selection change skips confirm and still persists', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleSharedGroupsSettingsDialog, {
      props: {
        modelValue: true,
        moduleId: 'mod-1',
        ownerGroupCode: 'HQ',
        sharedGroupCodes: ['RETAIL'],
        canConfigure: true,
      },
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()

    const store = useContentModulesStore()
    const updateShared = vi.spyOn(store, 'updateSharedGroupCodes').mockResolvedValue({
      moduleId: 'mod-1',
      moduleCode: 'MOD',
      groupCode: 'HQ',
      name: 'Loan',
      sharedGroupCodes: ['RETAIL'],
      versions: [],
      reviewHistory: [],
    })

    vi.spyOn(ElMessageBox, 'confirm')
    const vm = wrapper.vm as unknown as SettingsExposed
    await vm.handleSave()
    await flushPromises()

    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(updateShared).toHaveBeenCalledWith('mod-1', {
      sharedGroupCodes: ['RETAIL'],
    })
  })

  it('SGC-006: save control is disabled when canConfigure is false', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleSharedGroupsSettingsDialog, {
      props: {
        modelValue: true,
        moduleId: 'mod-1',
        ownerGroupCode: 'HQ',
        sharedGroupCodes: ['RETAIL'],
        canConfigure: false,
      },
      global: { plugins: [pinia, i18n, ElementPlus] },
    })
    await flushPromises()

    const saveButton = wrapper.find('[data-testid="content-module-shared-groups-save"]')
    expect(saveButton.exists()).toBe(true)
    expect(saveButton.attributes('disabled')).toBeDefined()
  })
})
