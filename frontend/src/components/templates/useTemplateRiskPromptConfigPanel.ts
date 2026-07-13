import { computed, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import * as globalRiskPromptApi from '@/api/riskPromptConfig'
import * as templateRiskPromptApi from '@/api/templateRiskPromptConfig'
import {
  TEMPLATE_DECISION_REASON_CATEGORIES,
  type TemplateDecisionReasonCategory,
} from '@/utils/templateLifecycleDecisionForm'
import type { TemplateRiskPromptFormState } from '@/types/template'

export interface UseTemplateRiskPromptConfigPanelOptions {
  templateId: Ref<string | null | undefined>
  createMode: Ref<boolean>
  formState: Ref<TemplateRiskPromptFormState>
}

export function useTemplateRiskPromptConfigPanel(options: UseTemplateRiskPromptConfigPanelOptions) {
  const { t } = useI18n()

  const loading = ref(false)
  const saving = ref(false)
  const updatedAt = ref<string | null>(null)

  const editableCategories = computed(() =>
    TEMPLATE_DECISION_REASON_CATEGORIES.filter((category) =>
      options.formState.value.reasonCategories.includes(category),
    ),
  )

  function categoryLabel(category: TemplateDecisionReasonCategory): string {
    return t(`templates.lifecycle.decisionForm.reasonCategories.${category}`)
  }

  function initializeCopyDefaults(categories: string[]) {
    for (const category of categories) {
      if (!options.formState.value.riskPromptCopy[category]) {
        options.formState.value.riskPromptCopy[category] = categoryLabel(
          category as TemplateDecisionReasonCategory,
        )
      }
    }
  }

  function applyLoadedConfig(
    useDefault: boolean,
    reasonCategories: string[],
    riskPromptCopy: Record<string, string>,
  ) {
    options.formState.value.customize = !useDefault
    options.formState.value.reasonCategories = [...reasonCategories]
    options.formState.value.riskPromptCopy = { ...riskPromptCopy }
    initializeCopyDefaults(options.formState.value.reasonCategories)
  }

  async function loadGlobalDefaults() {
    const config = await globalRiskPromptApi.getGlobalRiskPromptConfig()
    applyLoadedConfig(true, config.reasonCategories, config.riskPromptCopy)
    updatedAt.value = config.updatedAt
  }

  async function loadTemplateConfig() {
    if (!options.templateId.value) {
      return
    }
    loading.value = true
    try {
      const config = await templateRiskPromptApi.getTemplateRiskPromptConfig(
        options.templateId.value,
      )
      applyLoadedConfig(config.useDefault, config.reasonCategories, config.riskPromptCopy)
      updatedAt.value = config.updatedAt
    } catch {
      ElMessage.error(t('templates.riskPrompt.error.load'))
    } finally {
      loading.value = false
    }
  }

  async function loadConfig() {
    if (options.createMode.value || !options.templateId.value) {
      loading.value = true
      try {
        await loadGlobalDefaults()
      } catch {
        ElMessage.error(t('templates.riskPrompt.error.load'))
      } finally {
        loading.value = false
      }
      return
    }
    await loadTemplateConfig()
  }

  function toggleCategory(category: TemplateDecisionReasonCategory, enabled: boolean) {
    if (enabled) {
      if (!options.formState.value.reasonCategories.includes(category)) {
        options.formState.value.reasonCategories.push(category)
        initializeCopyDefaults([category])
      }
      return
    }
    options.formState.value.reasonCategories = options.formState.value.reasonCategories.filter(
      (entry) => entry !== category,
    )
    delete options.formState.value.riskPromptCopy[category]
  }

  function handleCustomizeChange(customize: boolean) {
    options.formState.value.customize = customize
    if (!customize) {
      void loadGlobalDefaults()
    }
  }

  async function saveConfig() {
    if (!options.templateId.value) {
      return
    }
    if (
      options.formState.value.customize &&
      !options.formState.value.reasonCategories.length
    ) {
      ElMessage.warning(t('templates.riskPrompt.validation.categoriesRequired'))
      return
    }
    saving.value = true
    try {
      const config = await templateRiskPromptApi.upsertTemplateRiskPromptConfig(
        options.templateId.value,
        {
          useDefault: !options.formState.value.customize,
          reasonCategories: options.formState.value.customize
            ? options.formState.value.reasonCategories
            : undefined,
          riskPromptCopy: options.formState.value.customize
            ? options.formState.value.riskPromptCopy
            : undefined,
        },
      )
      applyLoadedConfig(config.useDefault, config.reasonCategories, config.riskPromptCopy)
      updatedAt.value = config.updatedAt
      ElMessage.success(t('templates.riskPrompt.saveSuccess'))
    } catch {
      ElMessage.error(t('templates.riskPrompt.error.save'))
    } finally {
      saving.value = false
    }
  }

  watch(
    () => options.templateId.value,
    () => {
      void loadConfig()
    },
  )

  onMounted(() => {
    void loadConfig()
  })

  return {
    t,
    TEMPLATE_DECISION_REASON_CATEGORIES,
    loading,
    saving,
    updatedAt,
    editableCategories,
    categoryLabel,
    toggleCategory,
    handleCustomizeChange,
    loadConfig,
    saveConfig,
  }
}
