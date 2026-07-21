import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { type FormInstance, type FormRules } from 'element-plus'
import { resolveApiDependencyReport } from '@/api/errorEnvelope'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'
import {
  parseTemplateExportBundleFile,
  TemplateExportBundleParseError,
} from '@/utils/parseTemplateExportBundleFile'
import type {
  ImportTemplatePayload,
  TemplateExportBundle,
  TemplateImportConflictPolicy,
  TemplateImportDependencyReport,
} from '@/types/template'

export interface UseTemplateImportDialogOptions {
  modelValue: Ref<boolean>
  emitUpdateModelValue: (value: boolean) => void
  emitImported: (templateId: string) => void
}

function isZipFile(file: File | null): boolean {
  return !!file && file.name.toLowerCase().endsWith('.zip')
}

export function useTemplateImportDialog(options: UseTemplateImportDialogOptions) {
  const { t, te } = useI18n()
  const mastersStore = useMastersStore()
  const templatesStore = useTemplatesStore()
  const formRef = ref<FormInstance>()
  const selectedFile = ref<File | null>(null)
  const parsedBundle = ref<TemplateExportBundle | null>(null)
  const parseErrorKey = ref<string | null>(null)
  const dependencyReport = ref<TemplateImportDependencyReport | null>(null)
  const checkingDependencies = ref(false)

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

  const canCommitImport = computed(() => dependencyReport.value?.readyToCommit === true)

  const dependencyBusy = computed(
    () => checkingDependencies.value || templatesStore.submitting,
  )

  function clearDependencyReport() {
    dependencyReport.value = null
  }

  function resetDialogState() {
    selectedFile.value = null
    parsedBundle.value = null
    parseErrorKey.value = null
    clearDependencyReport()
    form.masterId = ''
    form.importConflictPolicy = 'REJECT_IMPORT'
  }

  watch(
    () => options.modelValue.value,
    async (open) => {
      if (!open) {
        return
      }
      resetDialogState()
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

  watch(
    () => [form.masterId, form.importConflictPolicy] as const,
    () => {
      clearDependencyReport()
    },
  )

  function onFileRemove() {
    selectedFile.value = null
    parsedBundle.value = null
    parseErrorKey.value = null
    clearDependencyReport()
  }

  async function onFileSelected(uploadFile: { raw?: File }) {
    selectedFile.value = uploadFile.raw ?? null
    parsedBundle.value = null
    parseErrorKey.value = null
    clearDependencyReport()
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

  function buildImportPayload(dryRun: boolean): ImportTemplatePayload | null {
    if (!selectedFile.value || !parsedBundle.value || parseErrorKey.value) {
      return null
    }
    if (isZipFile(selectedFile.value)) {
      return {
        masterId: form.masterId,
        file: selectedFile.value,
        importConflictPolicy: form.importConflictPolicy,
        dryRun,
      }
    }
    return {
      masterId: form.masterId,
      bundle: parsedBundle.value,
      importConflictPolicy: form.importConflictPolicy,
      dryRun,
    }
  }

  async function prepareValidatedPayload(dryRun: boolean): Promise<ImportTemplatePayload | null> {
    await parseSelectedFile()
    if (!parsedBundle.value || parseErrorKey.value) {
      return null
    }
    const valid = await formRef.value?.validate().catch(() => false)
    if (valid === false) {
      return null
    }
    return buildImportPayload(dryRun)
  }

  async function handleCheckDependencies() {
    const payload = await prepareValidatedPayload(true)
    if (!payload) {
      return
    }
    checkingDependencies.value = true
    templatesStore.lastErrorMessageKey = null
    try {
      const result = await templatesStore.dryRunImportTemplate(payload)
      dependencyReport.value = result.dependencyReport
    } catch {
      clearDependencyReport()
      // Error surfaced via store message key.
    } finally {
      checkingDependencies.value = false
    }
  }

  async function handleSubmit() {
    if (!canCommitImport.value) {
      return
    }
    const payload = await prepareValidatedPayload(false)
    if (!payload) {
      return
    }
    try {
      const result = await templatesStore.importTemplate(payload)
      visible.value = false
      options.emitImported(result.template.id)
    } catch (error) {
      const report = resolveApiDependencyReport(error)
      if (report) {
        dependencyReport.value = report
      }
      // Error surfaced via store message key.
    }
  }

  function dependencyItemMessage(item: { messageKey: string; detail?: string | null }): string {
    const localized = te(item.messageKey) ? t(item.messageKey) : item.messageKey
    if (item.detail) {
      return `${localized} (${item.detail})`
    }
    return localized
  }

  return {
    t,
    templatesStore,
    formRef,
    parsedBundle,
    parseErrorKey,
    dependencyReport,
    checkingDependencies,
    dependencyBusy,
    canCommitImport,
    visible,
    form,
    formRules,
    masterOptions,
    apiErrorMessage,
    onFileRemove,
    onFileSelected,
    handleCheckDependencies,
    handleSubmit,
    dependencyItemMessage,
  }
}
