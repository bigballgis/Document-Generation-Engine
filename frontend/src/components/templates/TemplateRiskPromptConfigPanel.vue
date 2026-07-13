<script setup lang="ts">
import { toRef } from 'vue'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import {
  TEMPLATE_DECISION_REASON_CATEGORIES,
} from '@/utils/templateLifecycleDecisionForm'
import type { TemplateRiskPromptFormState } from '@/types/template'
import { useTemplateRiskPromptConfigPanel } from '@/components/templates/useTemplateRiskPromptConfigPanel'

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

const {
  t,
  TEMPLATE_DECISION_REASON_CATEGORIES: categories,
  loading,
  saving,
  updatedAt,
  editableCategories,
  categoryLabel,
  toggleCategory,
  handleCustomizeChange,
  loadConfig,
  saveConfig,
} = useTemplateRiskPromptConfigPanel({
  templateId: toRef(props, 'templateId'),
  createMode: toRef(props, 'createMode'),
  formState,
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
              v-for="category in categories"
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
