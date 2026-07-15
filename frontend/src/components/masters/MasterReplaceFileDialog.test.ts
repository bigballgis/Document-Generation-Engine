import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it } from 'vitest'
import MasterReplaceFileDialog from '@/components/masters/MasterReplaceFileDialog.vue'
import en from '@/i18n/locales/en'
import { MASTER_DOCX_MAX_UPLOAD_BYTES } from '@/utils/validateMasterDocxUpload'
import type { MasterImpactAnalysis } from '@/types/master'

function makeFile(name: string, size: number, type = ''): File {
  const blob = new Blob([new Uint8Array(Math.min(size, 64))], { type })
  const file = new File([blob], name, { type })
  Object.defineProperty(file, 'size', { value: size })
  return file
}

const sampleImpact: MasterImpactAnalysis = {
  masterId: 'master-1',
  referencedTemplateIds: ['tpl-1'],
  referencedTemplates: [{ templateId: 'tpl-1', name: 'Loan Contract' }],
  retestRequired: true,
  anchorDelta: {
    addedAnchors: [],
    removedAnchors: ['FOOTER'],
    renamedAnchors: [],
  },
}

describe('MasterReplaceFileDialog', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountDialog(
    props: {
      loading?: boolean
      serverErrorKey?: string | null
      uploadProgress?: number | null
      impact?: MasterImpactAnalysis | null
    } = {},
  ) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    return mount(MasterReplaceFileDialog, {
      props: {
        modelValue: true,
        currentFilename: 'current.docx',
        impact: sampleImpact,
        ...props,
      },
      attachTo: document.body,
      global: {
        plugins: [i18n, ElementPlus],
      },
    })
  }

  function findButton(wrapper: ReturnType<typeof mountDialog>, label: string) {
    return wrapper.findAll('button').find((button) => button.text().includes(label))
  }

  it('shows drag affordance and 50 MB / .docx limit hint', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const upload = wrapper.findComponent({ name: 'ElUpload' })
    expect(upload.props('drag')).toBe(true)
    expect(wrapper.text()).toMatch(/drop a \.docx file here/i)
    expect(wrapper.text()).toContain('50 MB')
    expect(wrapper.text()).toMatch(/\.docx/i)
  })

  it('shows readable error and blocks continue when file exceeds 50MB', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const oversized = makeFile('huge.docx', MASTER_DOCX_MAX_UPLOAD_BYTES + 1)
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: oversized, name: oversized.name })
    await flushPromises()

    expect(wrapper.text()).toContain(
      'The file exceeds the 50 MB upload limit. Reduce the file size and try again.',
    )
    expect(findButton(wrapper, 'Continue')?.attributes('disabled')).toBeDefined()
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('shows readable error and blocks continue for non-.docx files', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const pdf = makeFile('notes.pdf', 2048)
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: pdf, name: pdf.name })
    await flushPromises()

    expect(wrapper.text()).toContain('Only .docx letterhead files are accepted.')
    expect(findButton(wrapper, 'Continue')?.attributes('disabled')).toBeDefined()
  })

  it('renders translated inline server rejection and clears it when file changes', async () => {
    const wrapper = mountDialog({
      serverErrorKey: 'api.error.master.docxCorrupt',
    })
    await flushPromises()

    expect(wrapper.text()).toMatch(/corrupt|invalid|docx/i)
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)

    const docx = makeFile('retry.docx', 4096)
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: docx, name: docx.name })
    await flushPromises()

    expect(wrapper.emitted('clear-server-error')).toBeTruthy()
  })

  it('shows upload progress while loading', async () => {
    const wrapper = mountDialog({ loading: true, uploadProgress: 55 })
    await flushPromises()

    expect(wrapper.find('[data-testid="master-upload-progress"]').exists()).toBe(true)
    expect(wrapper.text()).toMatch(/55%|uploading/i)
  })

  it('MIR-008 — confirm step shows template name and cancel does not submit', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const docx = makeFile('replacement.docx', 4096)
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: docx, name: docx.name })
    await flushPromises()

    await findButton(wrapper, 'Continue')!.trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="master-replace-impact-confirm"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="master-replace-impact-names"]').text()).toContain(
      'Loan Contract',
    )
    expect(wrapper.find('[data-testid="master-replace-retest-required"]').exists()).toBe(true)

    await findButton(wrapper, 'Cancel')!.trigger('click')
    await flushPromises()
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('emits submit only after impact confirmation', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const docx = makeFile('replacement.docx', 4096)
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: docx, name: docx.name })
    await flushPromises()

    await findButton(wrapper, 'Continue')!.trigger('click')
    await flushPromises()
    expect(wrapper.emitted('submit')).toBeUndefined()

    await findButton(wrapper, 'Confirm replace')!.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('submit')?.[0]?.[0]).toBe(docx)
  })
})
