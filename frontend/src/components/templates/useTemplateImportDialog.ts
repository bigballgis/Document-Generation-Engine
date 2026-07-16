import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { type FormInstance, type FormRules } from 'element-plus'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'
import {
  parseTemplateExportBundleFile,
  TemplateExportBundleParseError,
} from '@/utils/parseTemplateExportBundleFile'
import type { TemplateExportBundle, TemplateImportConflictPolicy } from '@/types/template'

export interface UseTemplateImportDialogOptions {
  modelValue: Ref<boolean>
  emitUpdateModelValue: (value: boolean) => void
  emitImported: (templateId: string) => void
}

export function useTemplateImportDialog(options: UseTemplateImportDialogOptions) {
  const { t, te } = useI18n()
  const mastersStore = useMastersStore()
  const templatesStore = useTemplatesStore()
  const formRef = ref<FormInstance>()
  const selectedFile = ref<File | null>(null)
  const parsedBundle = ref<TemplateExportBundle | null>(null)
  const parseErrorKey = ref<string | null>(null)

  const visible = computed({
    get: () => options.modelValue.value,
    set: (value: boolean) => options.emitUpdateModelValue(value),
  })

  const form = reactive({
    masterId: '',
    importConflictPolicy: 'REJECT_IMPORT' as TemplateImportConflictPolicy,
  })

  const formRules = computed<FormRules>(() => ({
    masterId: [
      { required: true, message: t('templates.import.validation.masterRequired'), trigger: 'change' },
    ],
  }))

  const approvedMasters = computed(() => {
    const groupCode = parsedBundle.value?.metadata.groupCode
    return mastersStore.masters.filter(
      (master) =>
        master.status === 'APPROVED' &&
        (groupCode === undefined || master.groupCode === groupCode),
    )
  })

  const masterOptions = computed(() =>
    approvedMasters.value.map((master) => ({
      value: master.id,
      label: `${master.name} (${master.groupCode})`,
    })),
  )

  const apiErrorMessage = computed(() => {
    const key = templatesStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('templates.error.import')
  })

  watch(
    () => options.modelValue.value,
    async (open) => {
      if (!open) {
        return
      }
      selectedFile.value = null
      parsedBundle.value = null
      parseErrorKey.value = null
      form.masterId = ''
      form.importConflictPolicy = 'REJECT_IMPORT'
      await mastersStore.fetchAllMasters({ sort: 'groupCodeAsc' })
    },
  )

  watch(parsedBundle, (bundle) => {
    if (!bundle) {
      form.masterId = ''
      return
    }
    const matchingMaster = approvedMasters.value.find((master) => master.id === bundle.metadata.masterId)
    form.masterId = matchingMaster?.id ?? approvedMasters.value[0]?.id ?? ''
  })

  function onFileRemove() {
    selectedFile.value = null
    parsedBundle.value = null
    parseErrorKey.value = null
  }

  async function onFileSelected(uploadFile: { raw?: File }) {
    selectedFile.value = uploadFile.raw ?? null
    parsedBundle.value = null
    parseErrorKey.value = null
    if (selectedFile.value) {
      await parseSelectedFile()
    }
  }

  async function parseSelectedFile() {
    if (!selectedFile.value) {
      parsedBundle.value = null
      parseErrorKey.value = 'templates.import.validation.fileRequired'
      return
    }
    try {
      parsedBundle.value = await parseTemplateExportBundleFile(selectedFile.value)
      parseErrorKey.value = null
    } catch (error) {
      parsedBundle.value = null
      parseErrorKey.value =
        error instanceof TemplateExportBundleParseError
          ? error.messageKey
          : 'templates.import.error.invalidBundle'
    }
  }

  async function handleSubmit() {
    await parseSelectedFile()
    if (!parsedBundle.value || parseErrorKey.value) {
      return
    }
    const valid = await formRef.value?.validate().catch(() => false)
    if (valid === false) {
      return
    }
    try {
      const result = await templatesStore.importTemplate({
        masterId: form.masterId,
        bundle: parsedBundle.value,
        importConflictPolicy: form.importConflictPolicy,
        dryRun: false,
      })
      visible.value = false
      options.emitImported(result.template.id)
    } catch {
      // Error surfaced via store message key.
    }
  }

  return {
    t,
    templatesStore,
    formRef,
    parsedBundle,
    parseErrorKey,
    visible,
    form,
    formRules,
    masterOptions,
    apiErrorMessage,
    onFileRemove,
    onFileSelected,
    handleSubmit,
  }
}
