import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { validateMasterDocxUploadFile } from '@/utils/validateMasterDocxUpload'

export function useMasterUploadDialog(deps: {
  modelValue: Ref<boolean>
  loading: Ref<boolean | undefined>
  uploadProgress: Ref<number | null | undefined>
  serverErrorKey: Ref<string | null | undefined>
  emitModelValue: (value: boolean) => void
  emitSubmit: (payload: {
    groupCode: string
    name: string
    description: string
    file: File
  }) => void
  emitClearServerError: () => void
}) {
  const { t, te } = useI18n()
  const { resolveDefaultGroupCode, ensureGroupCatalog } = useScopedGroupOptions()

  const visible = computed({
    get: () => deps.modelValue.value,
    set: (value: boolean) => deps.emitModelValue(value),
  })

  const form = reactive({
    groupCode: '',
    name: '',
    description: '',
  })

  const selectedFile = ref<File | null>(null)
  const fileList = ref<{ name: string }[]>([])
  const fileErrorKey = ref<string | null>(null)

  const inlineErrorKey = computed(() => fileErrorKey.value ?? deps.serverErrorKey.value ?? null)
  const inlineErrorText = computed(() => {
    const key = inlineErrorKey.value
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('masters.error.upload')
  })

  const progressPercent = computed(() => {
    if (deps.uploadProgress.value == null) {
      return 0
    }
    return Math.min(100, Math.max(0, deps.uploadProgress.value))
  })

  const progressIndeterminate = computed(
    () => deps.loading.value === true && deps.uploadProgress.value == null,
  )

  watch(
    () => deps.modelValue.value,
    async (open) => {
      if (!open) {
        return
      }
      await ensureGroupCatalog()
      form.groupCode = resolveDefaultGroupCode()
    },
  )

  function onFileChange(uploadFile: { raw?: File }) {
    const file = uploadFile.raw ?? null
    fileErrorKey.value = null
    deps.emitClearServerError()
    if (file) {
      const validation = validateMasterDocxUploadFile(file)
      if (!validation.ok) {
        fileErrorKey.value = validation.messageKey
        selectedFile.value = null
        fileList.value = []
        return
      }
    }
    selectedFile.value = file
    fileList.value = file ? [{ name: file.name }] : []
  }

  function onFileRemove() {
    selectedFile.value = null
    fileList.value = []
    fileErrorKey.value = null
    deps.emitClearServerError()
  }

  function resetForm() {
    form.groupCode = resolveDefaultGroupCode()
    form.name = ''
    form.description = ''
    selectedFile.value = null
    fileList.value = []
    fileErrorKey.value = null
  }

  function closeDialog() {
    if (deps.loading.value) {
      return
    }
    visible.value = false
    resetForm()
  }

  function submitUpload() {
    if (deps.loading.value || !selectedFile.value || !form.groupCode || !form.name.trim()) {
      return
    }
    deps.emitSubmit({
      groupCode: form.groupCode,
      name: form.name.trim(),
      description: form.description.trim(),
      file: selectedFile.value,
    })
  }

  const canSubmit = computed(
    () =>
      Boolean(!deps.loading.value && form.groupCode && form.name.trim() && selectedFile.value),
  )

  return {
    t,
    visible,
    form,
    fileList,
    inlineErrorKey,
    inlineErrorText,
    progressPercent,
    progressIndeterminate,
    canSubmit,
    onFileChange,
    onFileRemove,
    resetForm,
    closeDialog,
    submitUpload,
  }
}
