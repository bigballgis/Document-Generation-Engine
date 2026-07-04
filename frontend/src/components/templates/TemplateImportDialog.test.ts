import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateImportDialog from '@/components/templates/TemplateImportDialog.vue'
import en from '@/i18n/locales/en'
import { TEMPLATE_EXPORT_BUNDLE_FORMAT } from '@/utils/parseTemplateExportBundleFile'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'

vi.mock('@/api/masters', () => ({
  listMasters: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/utils/parseTemplateExportBundleFile', async () => {
  const actual = await vi.importActual<typeof import('@/utils/parseTemplateExportBundleFile')>(
    '@/utils/parseTemplateExportBundleFile',
  )
  return {
    ...actual,
    parseTemplateExportBundleFile: vi.fn(),
  }
})

const selectStub = {
  props: ['modelValue', 'options', 'placeholder', 'disabled'],
  emits: ['update:modelValue'],
  template:
    '<select class="select-stub" :disabled="disabled" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</option></select>',
}

describe('TemplateImportDialog', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountDialog() {
    const pinia = createPinia()
    setActivePinia(pinia)
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    useMastersStore().$patch({
      masters: [
        {
          id: 'master-1',
          groupCode: 'RETAIL',
          name: 'Retail letterhead',
          status: 'APPROVED',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
    })

    const wrapper = mount(TemplateImportDialog, {
      props: { modelValue: true },
      attachTo: document.body,
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          AppSearchSelect: selectStub,
          'el-form': {
            template: '<form><slot /></form>',
            methods: {
              validate: () => Promise.resolve(true),
            },
          },
        },
      },
    })

    return { wrapper, store: useTemplatesStore() }
  }

  it('shows import dialog copy and conflict policy options', async () => {
    const { wrapper } = mountDialog()
    await flushPromises()

    expect(wrapper.text()).toContain('Import template bundle')
    expect(wrapper.text()).toContain('Reject import when template ID already exists')
    expect(wrapper.text()).toContain('Keep template ID and create a new development version')
  })

  it('submits parsed bundle with selected master and conflict policy', async () => {
    const { parseTemplateExportBundleFile } = await import('@/utils/parseTemplateExportBundleFile')
    vi.mocked(parseTemplateExportBundleFile).mockResolvedValue({
      format: TEMPLATE_EXPORT_BUNDLE_FORMAT,
      metadata: {
        templateId: '11111111-1111-1111-1111-111111111111',
        externalId: 'TPL-IMPORT',
        groupCode: 'RETAIL',
        name: 'Imported template',
        description: null,
        masterId: 'master-1',
        lifecycleStatus: 'PUBLISHED',
        releaseVersion: '1.0.0',
        devVersionId: '33333333-3333-3333-3333-333333333333',
        devVersionNumber: 1,
        exportedAt: '2026-06-26T00:00:00Z',
      },
      variables: [],
      bindings: [],
      rules: [],
      contentModuleReferences: [],
      policySnapshot: undefined,
    })

    const { wrapper, store } = mountDialog()
    const importSpy = vi.spyOn(store, 'importTemplate').mockResolvedValue({
      importSummary: {
        resolvedTemplateId: 'tpl-imported',
        newDevelopmentVersion: 1,
        importBatchId: 'batch-1',
      },
      template: {
        id: 'tpl-imported',
        externalId: 'TPL-IMPORT',
        groupCode: 'RETAIL',
        name: 'Imported template',
        description: null,
        masterId: 'master-1',
        lifecycleStatus: 'DRAFT',
        releaseVersion: null,
        devVersionId: 'dev-1',
        devVersionNumber: 1,
        variables: [],
        bindings: [],
        rules: [],
        createdAt: '2026-06-26T00:00:00Z',
        updatedAt: '2026-06-26T00:00:00Z',
      },
    })

    await flushPromises()

    const vm = wrapper.vm as {
      onFileSelected: (uploadFile: { raw?: File }) => Promise<void>
      handleSubmit: () => Promise<void>
      form: { masterId: string; importConflictPolicy: string }
    }

    await vm.onFileSelected({ raw: new File(['{}'], 'bundle.json', { type: 'application/json' }) })
    await flushPromises()

    vm.form.masterId = 'master-1'
    vm.form.importConflictPolicy = 'KEEP_TEMPLATE_ID'
    await vm.handleSubmit()
    await flushPromises()

    expect(importSpy).toHaveBeenCalledWith({
      masterId: 'master-1',
      bundle: expect.objectContaining({
        metadata: expect.objectContaining({ externalId: 'TPL-IMPORT' }),
      }),
      importConflictPolicy: 'KEEP_TEMPLATE_ID',
    })
  })
})
