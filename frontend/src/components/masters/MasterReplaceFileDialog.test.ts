import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it } from 'vitest'
import MasterReplaceFileDialog from '@/components/masters/MasterReplaceFileDialog.vue'
import en from '@/i18n/locales/en'
import { MASTER_DOCX_MAX_UPLOAD_BYTES } from '@/utils/validateMasterDocxUpload'

function makeFile(name: string, size: number, type = ''): File {
  const blob = new Blob([new Uint8Array(Math.min(size, 64))], { type })
  const file = new File([blob], name, { type })
  Object.defineProperty(file, 'size', { value: size })
  return file
}

describe('MasterReplaceFileDialog', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountDialog() {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    return mount(MasterReplaceFileDialog, {
      props: {
        modelValue: true,
        currentFilename: 'current.docx',
      },
      attachTo: document.body,
      global: {
        plugins: [i18n, ElementPlus],
      },
    })
  }

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
    const submit = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Replace file'))
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
    const submit = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Replace file'))
    expect(submit?.attributes('disabled')).toBeDefined()
  })

  it('emits submit for a valid .docx under the size limit', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const docx = makeFile('replacement.docx', 4096)
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: docx, name: docx.name })
    await flushPromises()

    const submit = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Replace file'))
    await submit!.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('submit')?.[0]?.[0]).toBe(docx)
  })
})
