<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import * as riskPromptConfigApi from '@/api/riskPromptConfig'
import { useCapabilities } from '@/composables/useCapabilities'
import { useSessionStore } from '@/stores/session'
import { MANAGEMENT_ROLES } from '@/auth/roles'
import {
  TEMPLATE_DECISION_REASON_CATEGORIES,
  type TemplateDecisionReasonCategory,
} from '@/utils/templateLifecycleDecisionForm'
import type { RiskPromptConfig, RiskPromptScopeType } from '@/types/template'

const props = defineProps<{
  groupCode?: string | null
}>()

const { t } = useI18n()
const { context } = useCapabilities()
const sessionStore = useSessionStore()

const loading = ref(false)
const saving = ref(false)
const config = ref<RiskPromptConfig | null>(null)

const isGlobalAdmin = computed(() => context.value.roles.includes(MANAGEMENT_ROLES.GLOBAL_ADMIN))
const isGroupAdmin = computed(() => context.value.roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN))
const canMaintain = computed(() => isGlobalAdmin.value || isGroupAdmin.value)

const form = reactive({
  scopeType: 'GROUP' as RiskPromptScopeType,
  groupCode: props.groupCode ?? '',
  reasonCategories: [] as string[],
  riskPromptCopy: {} as Record<string, string>,
})

const editableCategories = computed(() =>
  TEMPLATE_DECISION_REASON_CATEGORIES.filter((category) =>
    form.reasonCategories.includes(category),
  ),
)

function initializeCopyDefaults(categories: string[]) {
  for (const category of categories) {
    if (!form.riskPromptCopy[category]) {
      form.riskPromptCopy[category] = t(`templates.lifecycle.decisionForm.reasonCategories.${category}`)
    }
  }
}

async function loadConfig() {
  if (!canMaintain.value) {
    return
  }
  loading.value = true
  try {
    const groupCode =
      form.scopeType === 'GROUP' ? form.groupCode || props.groupCode || undefined : undefined
    config.value = await riskPromptConfigApi.getRiskPromptConfig(groupCode)
    form.scopeType = config.value.scopeType
    form.groupCode = config.value.groupCode ?? form.groupCode
    form.reasonCategories = [...config.value.reasonCategories]
    form.riskPromptCopy = { ...config.value.riskPromptCopy }
    initializeCopyDefaults(form.reasonCategories)
  } catch {
    ElMessage.error(t('templates.riskPrompt.error.load'))
  } finally {
    loading.value = false
  }
}

function toggleCategory(category: TemplateDecisionReasonCategory, enabled: boolean) {
  if (enabled) {
    if (!form.reasonCategories.includes(category)) {
      form.reasonCategories.push(category)
      initializeCopyDefaults([category])
    }
    return
  }
  form.reasonCategories = form.reasonCategories.filter((entry) => entry !== category)
  delete form.riskPromptCopy[category]
}

async function saveConfig() {
  if (!form.reasonCategories.length) {
    ElMessage.warning(t('templates.riskPrompt.validation.categoriesRequired'))
    return
  }
  saving.value = true
  try {
    config.value = await riskPromptConfigApi.upsertRiskPromptConfig({
      scopeType: form.scopeType,
      groupCode: form.scopeType === 'GROUP' ? form.groupCode || props.groupCode || null : null,
      reasonCategories: form.reasonCategories,
      riskPromptCopy: form.riskPromptCopy,
    })
    ElMessage.success(t('templates.riskPrompt.saveSuccess'))
  } catch {
    ElMessage.error(t('templates.riskPrompt.error.save'))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (isGlobalAdmin.value) {
    form.scopeType = 'GLOBAL'
  } else if (props.groupCode) {
    form.groupCode = props.groupCode
  } else if (sessionStore.session?.authorizedGroupCodes.length) {
    form.groupCode = sessionStore.session.authorizedGroupCodes[0] ?? ''
  }
  void loadConfig()
})
</script>

<template>
  <el-card v-if="canMaintain" shadow="never" class="risk-prompt-card">
    <div class="risk-prompt-header">
      <div>
        <h2>{{ t('templates.riskPrompt.title') }}</h2>
        <p>{{ t('templates.riskPrompt.description') }}</p>
      </div>
      <el-button :loading="loading" @click="loadConfig">
        {{ t('templates.riskPrompt.refresh') }}
      </el-button>
    </div>

    <div v-loading="loading" class="risk-prompt-body">
      <el-form label-position="top">
        <el-form-item v-if="isGlobalAdmin" :label="t('templates.riskPrompt.scopeType')">
          <el-radio-group v-model="form.scopeType" @change="loadConfig">
            <el-radio value="GLOBAL">{{ t('templates.riskPrompt.scopeGlobal') }}</el-radio>
            <el-radio value="GROUP">{{ t('templates.riskPrompt.scopeGroup') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          v-if="form.scopeType === 'GROUP'"
          :label="t('templates.riskPrompt.groupCode')"
        >
          <el-input
            v-model="form.groupCode"
            :readonly="!isGlobalAdmin && isGroupAdmin"
            :placeholder="t('templates.riskPrompt.groupCodePlaceholder')"
            @change="loadConfig"
          />
        </el-form-item>

        <el-form-item :label="t('templates.riskPrompt.reasonCategories')">
          <div class="category-grid">
            <el-checkbox
              v-for="category in TEMPLATE_DECISION_REASON_CATEGORIES"
              :key="category"
              :model-value="form.reasonCategories.includes(category)"
              @change="(value: boolean) => toggleCategory(category, value)"
            >
              {{ t(`templates.lifecycle.decisionForm.reasonCategories.${category}`) }}
            </el-checkbox>
          </div>
        </el-form-item>

        <el-form-item
          v-for="category in editableCategories"
          :key="category"
          :label="t('templates.riskPrompt.promptCopyFor', { category })"
        >
          <el-input
            v-model="form.riskPromptCopy[category]"
            type="textarea"
            :rows="2"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <p v-if="config?.updatedAt" class="updated-at">
        {{ t('templates.riskPrompt.lastUpdated', { updatedAt: config.updatedAt }) }}
      </p>

      <el-button type="primary" :loading="saving" @click="saveConfig">
        {{ t('templates.riskPrompt.save') }}
      </el-button>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.risk-prompt-card {
  margin-top: 1.5rem;
}

.risk-prompt-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;

  h2 {
    margin: 0 0 0.35rem;
    font-size: 1.125rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.35rem 1rem;
}

.updated-at {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
