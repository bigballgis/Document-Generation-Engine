<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import * as globalRiskPromptApi from '@/api/riskPromptConfig'
import * as templateRiskPromptApi from '@/api/templateRiskPromptConfig'
import {
  TEMPLATE_DECISION_REASON_CATEGORIES,
  type TemplateDecisionReasonCategory,
} from '@/utils/templateLifecycleDecisionForm'
import type { TemplateRiskPromptFormState } from '@/types/template'

const props = withDefaults(
  defineProps<{
    templateId?: string | null
    createMode?: boolean
    showSave?: boolean
  }>(),
  {
    templateId: null,
    createMode: false,
    showSave: true,
  },
)

const formState = defineModel<TemplateRiskPromptFormState>('formState', {
  default: () => ({
    customize: false,
    reasonCategories: [...TEMPLATE_DECISION_REASON_CATEGORIES],
    riskPromptCopy: {},
  }),
})

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const updatedAt = ref<string | null>(null)

const editableCategories = computed(() =>
  TEMPLATE_DECISION_REASON_CATEGORIES.filter((category) =>
    formState.value.reasonCategories.includes(category),
  ),
)

function categoryLabel(category: TemplateDecisionReasonCategory): string {
  return t(`templates.lifecycle.decisionForm.reasonCategories.${category}`)
}

function initializeCopyDefaults(categories: string[]) {
  for (const category of categories) {
    if (!formState.value.riskPromptCopy[category]) {
      formState.value.riskPromptCopy[category] = categoryLabel(
        category as TemplateDecisionReasonCategory,
      )
    }
  }
}

function applyLoadedConfig(useDefault: boolean, reasonCategories: string[], riskPromptCopy: Record<string, string>) {
  formState.value.customize = !useDefault
  formState.value.reasonCategories = [...reasonCategories]
  formState.value.riskPromptCopy = { ...riskPromptCopy }
  initializeCopyDefaults(formState.value.reasonCategories)
}

async function loadGlobalDefaults() {
  const config = await globalRiskPromptApi.getGlobalRiskPromptConfig()
  applyLoadedConfig(true, config.reasonCategories, config.riskPromptCopy)
  updatedAt.value = config.updatedAt
}

async function loadTemplateConfig() {
  if (!props.templateId) {
    return
  }
  loading.value = true
  try {
    const config = await templateRiskPromptApi.getTemplateRiskPromptConfig(props.templateId)
    applyLoadedConfig(config.useDefault, config.reasonCategories, config.riskPromptCopy)
    updatedAt.value = config.updatedAt
  } catch {
    ElMessage.error(t('templates.riskPrompt.error.load'))
  } finally {
    loading.value = false
  }
}

async function loadConfig() {
  if (props.createMode || !props.templateId) {
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
    if (!formState.value.reasonCategories.includes(category)) {
      formState.value.reasonCategories.push(category)
      initializeCopyDefaults([category])
    }
    return
  }
  formState.value.reasonCategories = formState.value.reasonCategories.filter(
    (entry) => entry !== category,
  )
  delete formState.value.riskPromptCopy[category]
}

function handleCustomizeChange(customize: boolean) {
  formState.value.customize = customize
  if (!customize) {
    void loadGlobalDefaults()
  }
}

async function saveConfig() {
  if (!props.templateId) {
    return
  }
  if (formState.value.customize && !formState.value.reasonCategories.length) {
    ElMessage.warning(t('templates.riskPrompt.validation.categoriesRequired'))
    return
  }
  saving.value = true
  try {
    const config = await templateRiskPromptApi.upsertTemplateRiskPromptConfig(props.templateId, {
      useDefault: !formState.value.customize,
      reasonCategories: formState.value.customize ? formState.value.reasonCategories : undefined,
      riskPromptCopy: formState.value.customize ? formState.value.riskPromptCopy : undefined,
    })
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
  () => props.templateId,
  () => {
    void loadConfig()
  },
)

onMounted(() => {
  void loadConfig()
})

defineExpose({ loadConfig, saveConfig })
</script>

<template>
  <div v-loading="loading" class="risk-prompt-panel">
    <SectionPanelHeader
      :title="t('templates.riskPrompt.title')"
      :help-title="t('templates.riskPrompt.helpTitle')"
      :help-content="t('templates.riskPrompt.helpContent')"
    />

    <p class="risk-prompt-intro">{{ t('templates.riskPrompt.description') }}</p>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="risk-prompt-note"
      :title="t('templates.riskPrompt.gateNote')"
    />

    <el-form label-position="top" class="risk-prompt-form">
      <el-form-item>
        <el-checkbox
          :model-value="formState.customize"
          @change="(value: boolean) => handleCustomizeChange(value)"
        >
          {{ t('templates.riskPrompt.customizeForTemplate') }}
        </el-checkbox>
      </el-form-item>

      <template v-if="formState.customize">
        <el-form-item :label="t('templates.riskPrompt.reasonCategories')">
          <div class="category-grid">
            <div
              v-for="category in TEMPLATE_DECISION_REASON_CATEGORIES"
              :key="category"
              class="category-row"
            >
              <el-checkbox
                :model-value="formState.reasonCategories.includes(category)"
                @change="(value: boolean) => toggleCategory(category, value)"
              >
                {{ categoryLabel(category) }}
              </el-checkbox>
              <ContextHelpTrigger
                :title="categoryLabel(category)"
                :content="t(`templates.riskPrompt.categoryHelp.${category}`)"
              />
            </div>
          </div>
        </el-form-item>

        <el-form-item
          v-for="category in editableCategories"
          :key="`${category}-copy`"
          :label="t('templates.riskPrompt.promptCopyLabel', { label: categoryLabel(category) })"
        >
          <el-input
            v-model="formState.riskPromptCopy[category]"
            type="textarea"
            :rows="2"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
      </template>

      <p v-else class="inherit-hint">{{ t('templates.riskPrompt.inheritGlobalHint') }}</p>
    </el-form>

    <p v-if="updatedAt && !createMode" class="updated-at">
      {{ t('templates.riskPrompt.lastUpdated', { updatedAt }) }}
    </p>

    <el-button
      v-if="showSave && templateId && !createMode"
      type="primary"
      :loading="saving"
      @click="saveConfig"
    >
      {{ t('templates.riskPrompt.save') }}
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.risk-prompt-panel {
  margin-top: 0;
}

.risk-prompt-intro {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
  font-size: 0.875rem;
  line-height: 1.5;
}

.risk-prompt-note {
  margin-bottom: 1rem;
}

.risk-prompt-form {
  margin-bottom: 0.5rem;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 0.5rem 1rem;
}

.category-row {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.inherit-hint {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.updated-at {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
