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
import type { TemplateExportBundle, TemplateImportDependencyReport } from '@/types/template'

vi.mock('@/api/masters', () => ({
  listMasters: vi.fn().mockResolvedValue({
    content: [],
    page: 0,
    size: 100,
    totalElements: 0,
    totalPages: 0,
  }),
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

const sampleBundle: TemplateExportBundle = {
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
}

const readyReport: TemplateImportDependencyReport = {
  items: [
    {
      dependencyType: 'MASTER_PIN',
      severity: 'OK',
      code: 'MASTER_FINGERPRINT_OK',
      messageKey: 'api.error.template.dep.masterFingerprintOk',
    },
    {
      dependencyType: 'ASSET_BINARY',
      severity: 'WILL_MATERIALIZE',
      code: 'ASSET_WILL_MATERIALIZE',
      messageKey: 'api.error.template.dep.assetWillMaterialize',
      detail: 'seal-key',
    },
    {
      dependencyType: 'CLAUSE_NESTING',
      severity: 'OK',
      code: 'CLAUSE_NESTING_OK',
      messageKey: 'api.error.template.dep.clauseNestingOk',
      detail: 'PARENT>CHILD',
    },
  ],
  blockingCount: 0,
  warningCount: 1,
  infoCount: 0,
  readyToCommit: true,
  bundleFormat: 'template-export-bundle-v2-json',
}

const blockingReport: TemplateImportDependencyReport = {
  items: [
    {
      dependencyType: 'ASSET_BINARY',
      severity: 'MISSING',
      code: 'ASSET_BINARY_ABSENT',
      messageKey: 'api.error.template.dep.assetBinaryAbsent',
      detail: 'missing-asset',
    },
  ],
  blockingCount: 1,
  warningCount: 0,
  infoCount: 0,
  readyToCommit: false,
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

  type DialogVm = {
    onFileSelected: (uploadFile: { raw?: File }) => Promise<void>
    handleCheckDependencies: () => Promise<void>
    handleSubmit: () => Promise<void>
    form: { masterId: string; importConflictPolicy: string }
    canCommitImport: boolean
    dependencyReport: TemplateImportDependencyReport | null
  }

  async function selectBundle(wrapper: ReturnType<typeof mount> extends never ? never : Awaited<ReturnType<typeof mountDialog>>['wrapper']) {
    const { parseTemplateExportBundleFile } = await import('@/utils/parseTemplateExportBundleFile')
    vi.mocked(parseTemplateExportBundleFile).mockResolvedValue(sampleBundle)
    const vm = wrapper.vm as unknown as DialogVm
    await vm.onFileSelected({ raw: new File(['{}'], 'bundle.json', { type: 'application/json' }) })
    await flushPromises()
    vm.form.masterId = 'master-1'
    return vm
  }

  it('shows import dialog copy and conflict policy options', async () => {
    const { wrapper } = mountDialog()
    await flushPromises()

    expect(wrapper.text()).toContain('Import template bundle')
    expect(wrapper.text()).toContain('Check dependencies')
    expect(wrapper.text()).toContain('Reject import when template ID already exists')
    expect(wrapper.text()).toContain('Keep template ID and create a new development version')
  })

  it('importDialog_importDisabledUntilReady — Import stays disabled until dry-run is ready', async () => {
    const { wrapper, store } = mountDialog()
    const dryRunSpy = vi.spyOn(store, 'dryRunImportTemplate').mockResolvedValue({
      imported: false,
      dependencyReport: blockingReport,
    })
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

    const vm = await selectBundle(wrapper)
    expect(vm.canCommitImport).toBe(false)

    const importButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Import template'))
    expect(importButton?.attributes('disabled')).toBeDefined()

    await vm.handleSubmit()
    await flushPromises()
    expect(importSpy).not.toHaveBeenCalled()

    await vm.handleCheckDependencies()
    await flushPromises()
    expect(dryRunSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        masterId: 'master-1',
        dryRun: true,
        importConflictPolicy: 'REJECT_IMPORT',
      }),
    )
    expect(vm.canCommitImport).toBe(false)
    expect(wrapper.text()).toContain('Not ready to import')
  })

  it('importDialog_dryRunRendersReport — Check dependencies renders ready report rows', async () => {
    const { wrapper, store } = mountDialog()
    vi.spyOn(store, 'dryRunImportTemplate').mockResolvedValue({
      imported: false,
      dependencyReport: readyReport,
    })

    const vm = await selectBundle(wrapper)
    await vm.handleCheckDependencies()
    await flushPromises()

    expect(vm.dependencyReport?.readyToCommit).toBe(true)
    expect(wrapper.text()).toContain('Ready to import')
    expect(wrapper.text()).toContain('Blocking')
    expect(wrapper.text()).toContain('Warnings')
    expect(wrapper.text()).toContain('ASSET_BINARY')
    expect(wrapper.text()).toContain('CLAUSE_NESTING')
    expect(wrapper.text()).toContain(
      'The asset binary in the pack will be materialized into the asset library.',
    )
    expect(vm.canCommitImport).toBe(true)
  })

  it('importDialog_scrollableBodyKeepsFooter — tall report keeps footer actions in dialog chrome', async () => {
    const { wrapper, store } = mountDialog()
    vi.spyOn(store, 'dryRunImportTemplate').mockResolvedValue({
      imported: false,
      dependencyReport: readyReport,
    })

    const vm = await selectBundle(wrapper)
    await vm.handleCheckDependencies()
    await flushPromises()

    const dialog = wrapper.find('[data-testid="template-import-dialog"]')
    expect(dialog.exists()).toBe(true)
    expect(dialog.classes()).toContain('template-import-dialog')

    expect(wrapper.find('[data-testid="template-import-dependency-report"]').exists()).toBe(true)

    const footer = wrapper.find('[data-testid="template-import-dialog-footer"]')
    expect(footer.exists()).toBe(true)
    expect(footer.text()).toContain('Cancel')
    expect(footer.text()).toContain('Check dependencies')
    expect(footer.text()).toContain('Import template')

    // Layout contract markers for viewport-bounded dialog (CSS in unscoped SFC block).
    // jsdom does not reliably apply SFC stylesheets to getComputedStyle — assert structure.
    const dialogRoot = document.querySelector('.template-import-dialog.el-dialog')
    expect(dialogRoot).not.toBeNull()
    expect(dialogRoot?.querySelector('.el-dialog__body')).not.toBeNull()
    expect(dialogRoot?.querySelector('.el-dialog__footer')).not.toBeNull()
  })

  it('importDialog_clearsReportOnInputChange — changing master clears report and re-gates Import', async () => {
    const { wrapper, store } = mountDialog()
    vi.spyOn(store, 'dryRunImportTemplate').mockResolvedValue({
      imported: false,
      dependencyReport: readyReport,
    })

    const vm = await selectBundle(wrapper)
    await vm.handleCheckDependencies()
    await flushPromises()
    expect(vm.canCommitImport).toBe(true)
    expect(vm.dependencyReport).not.toBeNull()

    vm.form.masterId = 'master-other'
    await flushPromises()

    expect(vm.dependencyReport).toBeNull()
    expect(vm.canCommitImport).toBe(false)
  })

  it('submits parsed bundle only after ready dry-run', async () => {
    const { wrapper, store } = mountDialog()
    vi.spyOn(store, 'dryRunImportTemplate').mockResolvedValue({
      imported: false,
      dependencyReport: readyReport,
    })
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

    const vm = await selectBundle(wrapper)
    vm.form.importConflictPolicy = 'KEEP_TEMPLATE_ID'
    await vm.handleCheckDependencies()
    await flushPromises()
    await vm.handleSubmit()
    await flushPromises()

    expect(importSpy).toHaveBeenCalledWith({
      masterId: 'master-1',
      bundle: expect.objectContaining({
        metadata: expect.objectContaining({ externalId: 'TPL-IMPORT' }),
      }),
      importConflictPolicy: 'KEEP_TEMPLATE_ID',
      dryRun: false,
    })
  })
})
