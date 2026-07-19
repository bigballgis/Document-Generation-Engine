import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { type FormInstance, type FormRules } from 'element-plus'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'
import * as templateRiskPromptApi from '@/api/templateRiskPromptConfig'
import type { ApprovalMatrixMode } from '@/types/approvalMatrix'
import type { TemplateRiskPromptFormState } from '@/types/template'

export function useTemplateCreateDialog(deps: {
  modelValue: Ref<boolean>
  emitModelValue: (value: boolean) => void
  emitCreated: (templateId: string) => void
}) {
  const { t, te } = useI18n()
  const mastersStore = useMastersStore()
  const templatesStore = useTemplatesStore()
  const { resolveDefaultGroupCode, ensureGroupCatalog } = useScopedGroupOptions()

  const formRef = ref<FormInstance>()

  const visible = computed({
    get: () => deps.modelValue.value,
    set: (value: boolean) => deps.emitModelValue(value),
  })

  const form = reactive({
    groupCode: '',
    masterId: '',
    externalId: '',
    name: '',
    description: '',
    locale: '',
    localeVariantFamilyId: '',
    approvalMatrixMode: 'SINGLE_TRACK' as ApprovalMatrixMode,
  })

  const advancedSections = ref<string[]>([])
  const riskPromptFormState = ref<TemplateRiskPromptFormState>({
    customize: false,
    reasonCategories: [],
    riskPromptCopy: {},
  })

  const formRules = computed<FormRules>(() => ({
    groupCode: [
      { required: true, message: t('templates.create.validation.groupCodeRequired'), trigger: 'change' },
    ],
    masterId: [
      { required: true, message: t('templates.create.validation.masterRequired'), trigger: 'change' },
    ],
    externalId: [
      { required: true, message: t('templates.create.validation.externalIdRequired'), trigger: 'blur' },
      {
        pattern: /^[A-Z0-9][A-Z0-9_-]{0,127}$/,
        message: t('templates.create.validation.externalIdPattern'),
        trigger: 'blur',
      },
    ],
    name: [
      { required: true, message: t('templates.create.validation.nameRequired'), trigger: 'blur' },
    ],
    locale: [
      {
        required: true,
        message: t('templates.create.validation.localeRequired'),
        trigger: 'change',
      },
      {
        validator: (_rule, value: unknown, callback) => {
          if (typeof value !== 'string' || !value.trim()) {
            callback(new Error(t('templates.create.validation.localeRequired')))
            return
          }
          callback()
        },
        trigger: 'change',
      },
    ],
  }))

  const apiErrorMessage = computed(() => {
    const key = templatesStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('templates.error.create')
  })

  const approvedMasters = computed(() =>
    mastersStore.masters.filter(
      (master) =>
        master.status === 'APPROVED' &&
        (form.groupCode === '' || master.groupCode === form.groupCode),
    ),
  )

  const masterOptions = computed(() =>
    approvedMasters.value.map((master) => ({
      value: master.id,
      label: `${master.name} (${master.groupCode})`,
    })),
  )

  function resetForm() {
    form.groupCode = resolveDefaultGroupCode()
    form.masterId = ''
    form.externalId = ''
    form.name = ''
    form.description = ''
    form.locale = ''
    form.localeVariantFamilyId = ''
    form.approvalMatrixMode = 'SINGLE_TRACK'
    advancedSections.value = []
    riskPromptFormState.value = {
      customize: false,
      reasonCategories: [],
      riskPromptCopy: {},
    }
    formRef.value?.clearValidate()
  }

  watch(
    () => deps.modelValue.value,
    async (open) => {
      if (!open) {
        return
      }
      templatesStore.lastErrorMessageKey = null
      await ensureGroupCatalog()
      resetForm()
    },
  )

  watch(
    () => form.groupCode,
    () => {
      if (!approvedMasters.value.some((master) => master.id === form.masterId)) {
        form.masterId = ''
      }
    },
  )

  async function saveRiskPromptOverride(templateId: string) {
    if (!riskPromptFormState.value.customize) {
      return
    }
    await templateRiskPromptApi.upsertTemplateRiskPromptConfig(templateId, {
      useDefault: false,
      reasonCategories: riskPromptFormState.value.reasonCategories,
      riskPromptCopy: riskPromptFormState.value.riskPromptCopy,
    })
  }

  async function handleSubmit() {
    if (!formRef.value) {
      return
    }
    try {
      await formRef.value.validate()
    } catch {
      return
    }
    // BDD-IBL-E1-013 — never omit/blank locale on create.
    if (!form.locale.trim()) {
      return
    }
    try {
      const familyId = form.localeVariantFamilyId.trim()
      const created = await templatesStore.createTemplate({
        groupCode: form.groupCode.trim(),
        masterId: form.masterId,
        externalId: form.externalId.trim(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        locale: form.locale.trim(),
        localeVariantFamilyId: familyId || undefined,
        approvalMatrixMode: form.approvalMatrixMode,
      })
      if (riskPromptFormState.value.customize) {
        await saveRiskPromptOverride(created.id)
      }
      visible.value = false
      deps.emitCreated(created.id)
    } catch {
      // Inline alert surfaces store message key.
    }
  }

  return {
    t,
    templatesStore,
    formRef,
    visible,
    form,
    advancedSections,
    riskPromptFormState,
    formRules,
    apiErrorMessage,
    masterOptions,
    handleSubmit,
  }
}
