import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AssetLibraryUploadDialog from '@/components/library/AssetLibraryUploadDialog.vue'
import en from '@/i18n/locales/en'
import { LIBRARY_ASSET_MAX_BYTES } from '@/types/libraryAsset'

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
        stubs: {
          ScopedGroupSelect: selectStub,
        },
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

  it('BDD-ALGI-015 — requires group selection before submit', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    expect(wrapper.find('[data-testid="asset-library-upload-group"]').exists()).toBe(true)
    const groupInput = wrapper.find('.select-stub')
    await groupInput.setValue('')
    await flushPromises()

    const keyInput = wrapper.find('.el-input__inner')
    await keyInput.setValue('IMG-LOGO-BANK')
    const png = makeFile('logo.png', 128, 'image/png')
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: png, name: png.name })
    await flushPromises()

    const submit = wrapper.findAll('button').find((button) => button.text() === 'Upload')
    expect(submit?.attributes('disabled')).toBeDefined()
    expect(wrapper.emitted('submit')).toBeUndefined()
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

  it('enables submit after group, IMAGE key, and PNG file are provided', async () => {
    const wrapper = mountDialog({ canUploadImageOrOther: true, canUploadSeal: false })
    await flushPromises()

    expect(wrapper.find('[data-testid="asset-library-upload-group"]').exists()).toBe(true)

    const keyInput = wrapper.find('.el-input__inner')
    await keyInput.setValue('IMG-LOGO-BANK')

    const png = makeFile('logo.png', 128, 'image/png')
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: png, name: png.name })
    await flushPromises()

    const submit = wrapper.findAll('button').find((button) => button.text() === 'Upload')
    expect(submit).toBeTruthy()
    expect(submit!.attributes('disabled')).toBeUndefined()

    await submit!.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('submit')?.[0]?.[0]).toMatchObject({
      groupCode: 'RETAIL',
      assetKey: 'IMG-LOGO-BANK',
      assetClass: 'IMAGE',
    })
  })

  it('keeps SEAL option gated while still requiring groupCode', async () => {
    const wrapper = mountDialog({ canUploadImageOrOther: false, canUploadSeal: true })
    await flushPromises()

    expect(wrapper.text()).toContain('Seal')
    expect(wrapper.find('[data-testid="asset-library-upload-group"]').exists()).toBe(true)

    const keyInput = wrapper.find('.el-input__inner')
    await keyInput.setValue('SEAL-BANK')
    const png = makeFile('seal.png', 128, 'image/png')
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    await upload.vm.$emit('change', { raw: png, name: png.name })
    await flushPromises()

    const submit = wrapper.findAll('button').find((button) => button.text() === 'Upload')
    await submit!.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('submit')?.[0]?.[0]).toMatchObject({
      groupCode: 'RETAIL',
      assetKey: 'SEAL-BANK',
      assetClass: 'SEAL',
    })
  })
})
