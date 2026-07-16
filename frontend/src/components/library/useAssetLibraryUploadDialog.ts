import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { UploadFile, UploadUserFile } from 'element-plus'
import type { LibraryAssetClass } from '@/types/libraryAsset'
import {
  LIBRARY_ASSET_ACCEPTED_MIME,
  LIBRARY_ASSET_KEY_PATTERN,
  LIBRARY_ASSET_MAX_BYTES,
} from '@/types/libraryAsset'

export interface AssetLibraryUploadForm {
  assetKey: string
  assetClass: LibraryAssetClass | ''
  file: File | null
}

export function useAssetLibraryUploadDialog(options: {
  modelValue: Ref<boolean>
  loading: Ref<boolean | undefined>
  serverErrorKey: Ref<string | null | undefined>
  canUploadImageOrOther: Ref<boolean>
  canUploadSeal: Ref<boolean>
  emitModelValue: (value: boolean) => void
  emitSubmit: (payload: { assetKey: string; assetClass: LibraryAssetClass; file: File }) => void
  emitClearServerError: () => void
}) {
  const { t } = useI18n()

  const form = reactive<AssetLibraryUploadForm>({
    assetKey: '',
    assetClass: '',
    file: null,
  })
  const fileList = ref<UploadUserFile[]>([])
  const inlineErrorKey = ref<string | null>(null)

  const visible = computed({
    get: () => options.modelValue.value,
    set: (value: boolean) => options.emitModelValue(value),
  })

  const assetClassOptions = computed(() => {
    const optionsList: Array<{ value: LibraryAssetClass; label: string }> = []
    if (options.canUploadImageOrOther.value) {
      optionsList.push(
        { value: 'IMAGE', label: t('assetLibrary.assetClass.IMAGE') },
        { value: 'OTHER', label: t('assetLibrary.assetClass.OTHER') },
      )
    }
    if (options.canUploadSeal.value) {
      optionsList.push({ value: 'SEAL', label: t('assetLibrary.assetClass.SEAL') })
    }
    return optionsList
  })

  const keyHint = computed(() => {
    switch (form.assetClass) {
      case 'IMAGE':
        return t('assetLibrary.upload.keyHintImage')
      case 'SEAL':
        return t('assetLibrary.upload.keyHintSeal')
      case 'OTHER':
        return t('assetLibrary.upload.keyHintOther')
      default:
        return t('assetLibrary.upload.keyHintDefault')
    }
  })

  const inlineErrorText = computed(() => {
    if (!inlineErrorKey.value) {
      return ''
    }
    return t(inlineErrorKey.value)
  })

  const serverErrorText = computed(() => {
    const key = options.serverErrorKey.value
    if (!key) {
      return ''
    }
    return t(key)
  })

  const canSubmit = computed(() => {
    if (options.loading.value) {
      return false
    }
    if (!form.assetKey.trim() || !form.assetClass || !form.file) {
      return false
    }
    if (form.assetClass === 'SEAL' && !options.canUploadSeal.value) {
      return false
    }
    if (
      (form.assetClass === 'IMAGE' || form.assetClass === 'OTHER') &&
      !options.canUploadImageOrOther.value
    ) {
      return false
    }
    return inlineErrorKey.value == null
  })

  function resetForm() {
    form.assetKey = ''
    form.assetClass = assetClassOptions.value[0]?.value ?? ''
    form.file = null
    fileList.value = []
    inlineErrorKey.value = null
    options.emitClearServerError()
  }

  function closeDialog() {
    if (options.loading.value) {
      return
    }
    visible.value = false
  }

  function validateFile(file: File): string | null {
    if (file.size <= 0) {
      return 'assetLibrary.upload.validation.fileEmpty'
    }
    if (file.size > LIBRARY_ASSET_MAX_BYTES) {
      return 'assetLibrary.upload.validation.fileTooLarge'
    }
    const mime = file.type.toLowerCase()
    if (!(LIBRARY_ASSET_ACCEPTED_MIME as readonly string[]).includes(mime)) {
      return 'assetLibrary.upload.validation.contentTypeUnsupported'
    }
    return null
  }

  function onFileChange(uploadFile: UploadFile) {
    options.emitClearServerError()
    const raw = uploadFile.raw
    if (!raw) {
      form.file = null
      fileList.value = []
      inlineErrorKey.value = 'assetLibrary.upload.validation.fileRequired'
      return
    }
    const errorKey = validateFile(raw)
    if (errorKey) {
      form.file = null
      fileList.value = []
      inlineErrorKey.value = errorKey
      return
    }
    form.file = raw
    fileList.value = [uploadFile]
    inlineErrorKey.value = null
  }

  function onFileRemove() {
    form.file = null
    fileList.value = []
    inlineErrorKey.value = null
    options.emitClearServerError()
  }

  function submitUpload() {
    options.emitClearServerError()
    const assetKey = form.assetKey.trim()
    if (!LIBRARY_ASSET_KEY_PATTERN.test(assetKey)) {
      inlineErrorKey.value = 'assetLibrary.upload.validation.assetKeyInvalid'
      return
    }
    if (!form.assetClass) {
      inlineErrorKey.value = 'assetLibrary.upload.validation.assetClassRequired'
      return
    }
    if (form.assetClass === 'SEAL' && !options.canUploadSeal.value) {
      inlineErrorKey.value = 'assetLibrary.upload.validation.sealNotAllowed'
      return
    }
    if (
      (form.assetClass === 'IMAGE' || form.assetClass === 'OTHER') &&
      !options.canUploadImageOrOther.value
    ) {
      inlineErrorKey.value = 'assetLibrary.upload.validation.uploadNotAllowed'
      return
    }
    if (!form.file) {
      inlineErrorKey.value = 'assetLibrary.upload.validation.fileRequired'
      return
    }
    const fileError = validateFile(form.file)
    if (fileError) {
      inlineErrorKey.value = fileError
      return
    }
    inlineErrorKey.value = null
    options.emitSubmit({
      assetKey,
      assetClass: form.assetClass,
      file: form.file,
    })
  }

  watch(
    () => options.modelValue.value,
    (open) => {
      if (open) {
        resetForm()
      }
    },
    { immediate: true },
  )

  watch(
    assetClassOptions,
    (next) => {
      if (!form.assetClass && next[0]) {
        form.assetClass = next[0].value
      }
      if (form.assetClass && !next.some((option) => option.value === form.assetClass)) {
        form.assetClass = next[0]?.value ?? ''
      }
    },
    { immediate: true },
  )

  return {
    t,
    visible,
    form,
    fileList,
    inlineErrorKey,
    inlineErrorText,
    serverErrorText,
    assetClassOptions,
    keyHint,
    canSubmit,
    onFileChange,
    onFileRemove,
    resetForm,
    closeDialog,
    submitUpload,
  }
}
