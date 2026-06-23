<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import { useRuleValidationStatusFilterOptions } from '@/composables/useTableFilterOptions'
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

const rulesSource = computed(() => rules)
const { filters: ruleColumnFilters, filteredRows: filteredRules } = useDataTableFilters(rulesSource, [
  { key: 'ruleId', getValue: (row) => row.ruleId },
  { key: 'conditionExpression', getValue: (row) => row.conditionExpression },
  { key: 'targetAnchorId', getValue: (row) => row.targetAnchorId },
])

const validationRulesSource = computed(() => validationResult.value?.rules ?? [])
const { filters: validationColumnFilters, filteredRows: filteredValidationRules } =
  useDataTableFilters(validationRulesSource, [
    { key: 'ruleId', getValue: (row) => row.ruleId },
    { key: 'status', getValue: (row) => row.status, matchMode: 'exact' },
  ])

const validationStatusFilterOptions = useRuleValidationStatusFilterOptions()

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
    <AppDataTable :data="filteredRules">
      <el-table-column sortable prop="ruleId" min-width="120">
        <template #header>
          <TableColumnHeader
            :label="t('templates.rules.ruleId')"
            v-model="ruleColumnFilters.ruleId"
          />
        </template>
        <template #default="{ row }">
          <el-input v-model="row.ruleId" />
        </template>
      </el-table-column>
      <el-table-column min-width="220">
        <template #header>
          <TableColumnHeader
            :label="t('templates.rules.conditionExpression')"
            v-model="ruleColumnFilters.conditionExpression"
          />
        </template>
        <template #default="{ row }">
          <el-input v-model="row.conditionExpression" />
        </template>
      </el-table-column>
      <el-table-column min-width="160">
        <template #header>
          <TableColumnHeader
            :label="t('templates.rules.targetAnchorId')"
            v-model="ruleColumnFilters.targetAnchorId"
          />
        </template>
        <template #default="{ row }">
          <el-input v-model="row.targetAnchorId" />
        </template>
      </el-table-column>
    </AppDataTable>

    <div class="action-row">
      <el-button @click="addRule">{{ t('templates.rules.addRule') }}</el-button>
      <el-button type="primary" plain :loading="saving" @click="handleSaveRules">
        {{ t('templates.rules.save') }}
      </el-button>
      <el-button type="primary" :loading="validating" @click="handleValidateRules">
        {{ t('templates.rules.validate') }}
      </el-button>
    </div>

    <AppDataTable v-if="validationResult" :data="filteredValidationRules" class="result-table">
      <el-table-column prop="ruleId" sortable>
        <template #header>
          <TableColumnHeader
            :label="t('templates.rules.ruleId')"
            v-model="validationColumnFilters.ruleId"
          />
        </template>
      </el-table-column>
      <el-table-column prop="status" sortable>
        <template #header>
          <TableColumnHeader
            :label="t('templates.rules.status')"
            v-model="validationColumnFilters.status"
            filter-type="select"
            :options="validationStatusFilterOptions"
          />
        </template>
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
    </AppDataTable>
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
