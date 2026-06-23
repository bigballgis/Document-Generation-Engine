<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import * as templatesApi from '@/api/templates'
import type { AnchorBinding, BindingValidationResult, VariableSchema } from '@/types/template'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  templateId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
}>()

const { t } = useI18n()
const validating = ref(false)
const validationResult = ref<BindingValidationResult | null>(null)

const hasBindings = computed(() => props.bindings.length > 0)

async function handleValidateBindings() {
  validating.value = true
  try {
    validationResult.value = await templatesApi.validateBindings(props.templateId)
    if (validationResult.value.summary.blocking) {
      ElMessage.warning(t('templates.authoring.bindingValidationBlocking'))
    } else {
      ElMessage.success(t('templates.authoring.bindingValidationSuccess'))
    }
  } catch {
    ElMessage.error(t('templates.error.bindingValidation'))
  } finally {
    validating.value = false
  }
}
</script>

<template>
  <div class="authoring-panel">
    <h3>{{ t('templates.authoring.variablesTitle') }}</h3>
    <el-table :data="variables" stripe empty-text="">
      <template #empty>
        <el-empty :description="t('templates.authoring.noVariables')" />
      </template>
      <el-table-column prop="variableKey" :label="t('templates.authoring.variableKey')" />
      <el-table-column prop="variableType" :label="t('templates.authoring.variableType')" />
      <el-table-column :label="t('templates.authoring.required')">
        <template #default="{ row }">
          {{ row.required ? t('common.yes') : t('common.no') }}
        </template>
      </el-table-column>
    </el-table>

    <h3>{{ t('templates.authoring.bindingsTitle') }}</h3>
    <el-table :data="bindings" stripe empty-text="">
      <template #empty>
        <el-empty :description="t('templates.authoring.noBindings')" />
      </template>
      <el-table-column prop="anchorId" :label="t('templates.authoring.anchorId')" />
      <el-table-column prop="declaredContentType" :label="t('templates.authoring.contentType')" />
    </el-table>

    <div class="action-row">
      <el-button
        type="primary"
        :loading="validating"
        :disabled="!hasBindings"
        @click="handleValidateBindings"
      >
        {{ t('templates.authoring.validateBindings') }}
      </el-button>
    </div>

    <el-alert
      v-if="validationResult"
      :type="validationResult.summary.blocking ? 'warning' : 'success'"
      :title="t('templates.authoring.bindingValidationSummary', {
        valid: validationResult.summary.validCount,
        total: validationResult.summary.totalBindings,
      })"
      show-icon
      :closable="false"
    />
  </div>
</template>

<style scoped lang="scss">
.authoring-panel {
  h3 {
    margin: 1.25rem 0 0.75rem;
    font-size: 1rem;

    &:first-child {
      margin-top: 0;
    }
  }
}

.action-row {
  margin-top: 1rem;
}
</style>
