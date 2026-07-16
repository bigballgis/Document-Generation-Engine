import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it } from 'vitest'
import AssetLibraryUploadDialog from '@/components/library/AssetLibraryUploadDialog.vue'
import en from '@/i18n/locales/en'
import { LIBRARY_ASSET_MAX_BYTES } from '@/types/libraryAsset'

function makeFile(name: string, size: number, type: string): File {
  const blob = new Blob([new Uint8Array(Math.min(size, 64))], { type })
  const file = new File([blob], name, { type })
  Object.defineProperty(file, 'size', { value: size })
  return file
}

describe('AssetLibraryUploadDialog', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountDialog(
    props: {
      modelValue?: boolean
      loading?: boolean
      serverErrorKey?: string | null
      canUploadImageOrOther?: boolean
      canUploadSeal?: boolean
    } = {},
  ) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    return mount(AssetLibraryUploadDialog, {
      props: {
        modelValue: true,
        canUploadImageOrOther: true,
        canUploadSeal: false,
        ...props,
      },
      attachTo: document.body,
      global: {
        plugins: [i18n, ElementPlus],
      },
    })
  }

  it('shows drag affordance and PNG/JPEG size hint', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const upload = wrapper.findComponent({ name: 'ElUpload' })
    expect(upload.props('drag')).toBe(true)
    expect(wrapper.text()).toMatch(/drop a png or jpeg/i)
    expect(wrapper.text()).toContain('5')
  })

  it('blocks oversized files with inline validation', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const oversized = makeFile('huge.png', LIBRARY_ASSET_MAX_BYTES + 1, 'image/png')
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: oversized, name: oversized.name })
    await flushPromises()

    expect(wrapper.text()).toContain('The file exceeds the 5 MiB limit.')
    const submit = wrapper.findAll('button').find((button) => button.text().includes('Upload'))
    expect(submit?.attributes('disabled')).toBeDefined()
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('blocks unsupported content types', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const pdf = makeFile('notes.pdf', 2048, 'application/pdf')
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: pdf, name: pdf.name })
    await flushPromises()

    expect(wrapper.text()).toContain('Only PNG and JPEG files are supported.')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('enables submit after a valid IMAGE key and PNG file are provided', async () => {
    const wrapper = mountDialog({ canUploadImageOrOther: true, canUploadSeal: false })
    await flushPromises()

    const keyInput = wrapper.find('.el-input__inner')
    await keyInput.setValue('IMG-LOGO-BANK')

    const png = makeFile('logo.png', 128, 'image/png')
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: png, name: png.name })
    await flushPromises()

    const submit = wrapper.findAll('button').find((button) => button.text() === 'Upload')
    expect(submit).toBeTruthy()
    expect(submit!.attributes('disabled')).toBeUndefined()
  })
})
