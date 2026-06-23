<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTemplatesStore } from '@/stores/templates'
import type { CompositionRule, CompositionRuleInput, RuleValidationResult } from '@/types/template'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  templateId: string
  initialRules: CompositionRule[]
}>()

const emit = defineEmits<{
  updated: []
}>()

const { t } = useI18n()
const templatesStore = useTemplatesStore()

const validating = ref(false)
const saving = ref(false)
const validationResult = ref<RuleValidationResult | null>(null)

const rules = reactive<CompositionRuleInput[]>([])

watch(
  () => props.initialRules,
  (nextRules) => {
    rules.splice(
      0,
      rules.length,
      ...nextRules.map((rule) => ({
        ruleId: rule.ruleId,
        conditionExpression: rule.conditionExpression,
        targetAnchorId: rule.targetAnchorId,
        trueBranchRuleId: rule.trueBranchRuleId ?? undefined,
        falseBranchRuleId: rule.falseBranchRuleId ?? undefined,
      })),
    )
  },
  { immediate: true, deep: true },
)

function addRule() {
  rules.push({
    ruleId: `rule-${rules.length + 1}`,
    conditionExpression: '',
    targetAnchorId: '',
  })
}

function statusTagType(status: string) {
  return status === 'VALID' ? 'success' : 'danger'
}

function toPayload(): CompositionRuleInput[] {
  return rules.map((rule) => ({
    ruleId: rule.ruleId,
    conditionExpression: rule.conditionExpression,
    targetAnchorId: rule.targetAnchorId,
    trueBranchRuleId: rule.trueBranchRuleId || undefined,
    falseBranchRuleId: rule.falseBranchRuleId || undefined,
  }))
}

async function handleSaveRules() {
  saving.value = true
  try {
    await templatesStore.saveRules(props.templateId, toPayload())
    ElMessage.success(t('templates.rules.saveSuccess'))
    emit('updated')
  } catch {
    ElMessage.error(t('templates.error.saveRules'))
  } finally {
    saving.value = false
  }
}

async function handleValidateRules() {
  validating.value = true
  try {
    validationResult.value = await templatesStore.validateRules(props.templateId, toPayload())
    if (validationResult.value.summary.blocking) {
      ElMessage.warning(t('templates.rules.validationBlocking'))
    } else {
      ElMessage.success(t('templates.rules.validationSuccess'))
    }
  } catch {
    ElMessage.error(t('templates.error.ruleValidation'))
  } finally {
    validating.value = false
  }
}
</script>

<template>
  <div class="rule-configurator">
    <p>{{ t('templates.rules.description') }}</p>
    <el-table :data="rules" stripe>
      <el-table-column :label="t('templates.rules.ruleId')" min-width="120">
        <template #default="{ row }">
          <el-input v-model="row.ruleId" />
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.rules.conditionExpression')" min-width="220">
        <template #default="{ row }">
          <el-input v-model="row.conditionExpression" />
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.rules.targetAnchorId')" min-width="160">
        <template #default="{ row }">
          <el-input v-model="row.targetAnchorId" />
        </template>
      </el-table-column>
    </el-table>

    <div class="action-row">
      <el-button @click="addRule">{{ t('templates.rules.addRule') }}</el-button>
      <el-button type="primary" plain :loading="saving" @click="handleSaveRules">
        {{ t('templates.rules.save') }}
      </el-button>
      <el-button type="primary" :loading="validating" @click="handleValidateRules">
        {{ t('templates.rules.validate') }}
      </el-button>
    </div>

    <el-table v-if="validationResult" :data="validationResult.rules" stripe class="result-table">
      <el-table-column prop="ruleId" :label="t('templates.rules.ruleId')" />
      <el-table-column prop="status" :label="t('templates.rules.status')">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.rule-configurator {
  margin-top: 1.5rem;

  p {
    margin: 0 0 1rem;
    color: var(--text-muted);
  }
}

.action-row {
  display: flex;
  gap: 0.75rem;
  margin: 1rem 0;
}

.result-table {
  margin-top: 1rem;
}
</style>
