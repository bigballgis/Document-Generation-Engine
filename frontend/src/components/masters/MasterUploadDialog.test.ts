import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it, vi } from 'vitest'
import MasterUploadDialog from '@/components/masters/MasterUploadDialog.vue'
import en from '@/i18n/locales/en'
import { MASTER_DOCX_MAX_UPLOAD_BYTES } from '@/utils/validateMasterDocxUpload'

vi.mock('@/composables/useScopedGroupOptions', () => ({
  useScopedGroupOptions: () => ({
    resolveDefaultGroupCode: () => 'RETAIL',
    ensureGroupCatalog: vi.fn().mockResolvedValue(undefined),
    groupOptions: [{ value: 'RETAIL', label: 'RETAIL' }],
    isGroupLocked: { value: false },
    lockedGroupCode: { value: null },
  }),
}))

const selectStub = {
  props: ['modelValue', 'placeholder', 'disabled'],
  emits: ['update:modelValue'],
  template:
    '<input class="select-stub" :value="modelValue" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

function makeFile(name: string, size: number, type = ''): File {
  const blob = new Blob([new Uint8Array(Math.min(size, 64))], { type })
  const file = new File([blob], name, { type })
  Object.defineProperty(file, 'size', { value: size })
  return file
}

describe('MasterUploadDialog', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountDialog(
    props: {
      modelValue?: boolean
      loading?: boolean
      serverErrorKey?: string | null
      uploadProgress?: number | null
    } = {},
  ) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    return mount(MasterUploadDialog, {
      props: {
        modelValue: true,
        ...props,
      },
      attachTo: document.body,
      global: {
        plugins: [i18n, ElementPlus],
        stubs: {
          ScopedGroupSelect: selectStub,
        },
      },
    })
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

  it('shows readable error and blocks submit when file exceeds 50MB', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const oversized = makeFile('huge.docx', MASTER_DOCX_MAX_UPLOAD_BYTES + 1)
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: oversized, name: oversized.name })
    await flushPromises()

    expect(wrapper.text()).toContain(
      'The file exceeds the 50 MB upload limit. Reduce the file size and try again.',
    )
    expect(wrapper.text()).not.toContain('<html')
    const submit = wrapper.findAll('button').find((button) => button.text().includes('Upload'))
    expect(submit?.attributes('disabled')).toBeDefined()
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('shows readable error and blocks submit for non-.docx files', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const pdf = makeFile('notes.pdf', 2048)
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: pdf, name: pdf.name })
    await flushPromises()

    expect(wrapper.text()).toContain('Only .docx letterhead files are accepted.')
  })

  it('renders translated inline server rejection and clears it when file changes', async () => {
    const wrapper = mountDialog({
      serverErrorKey: 'api.error.master.docxTooLarge',
    })
    await flushPromises()

    expect(wrapper.text()).toContain('The uploaded DOCX exceeds the maximum allowed size.')
    expect(wrapper.text()).not.toContain('<html')
    expect(wrapper.find('[role="alert"]').exists()).toBe(true)

    const docx = makeFile('retry.docx', 4096)
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: docx, name: docx.name })
    await flushPromises()

    expect(wrapper.emitted('clear-server-error')).toBeTruthy()
  })

  it('shows upload progress while loading', async () => {
    const wrapper = mountDialog({ loading: true, uploadProgress: 42 })
    await flushPromises()

    expect(wrapper.find('[data-testid="master-upload-progress"]').exists()).toBe(true)
    expect(wrapper.text()).toMatch(/42%|uploading/i)
    const submit = wrapper.findAll('button').find((button) => button.text().includes('Upload'))
    expect(submit?.attributes('disabled')).toBeDefined()
  })

  it('shows indeterminate progress when loading without percentage', async () => {
    const wrapper = mountDialog({ loading: true, uploadProgress: null })
    await flushPromises()

    const progress = wrapper.find('[data-testid="master-upload-progress"]')
    expect(progress.exists()).toBe(true)
  })

  it('disables ScopedGroupSelect while loading', async () => {
    const wrapper = mountDialog({ loading: true })
    await flushPromises()

    const groupSelect = wrapper.find('.select-stub')
    expect(groupSelect.exists()).toBe(true)
    expect(groupSelect.attributes('disabled')).toBeDefined()
  })
})
