<script setup lang="ts">
import { toRef } from 'vue'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import type { VariableSchema } from '@/types/template'
import { useTemplateVariableTreePanel } from '@/components/templates/useTemplateVariableTreePanel'

const props = defineProps<{
  templateId: string
  variables: VariableSchema[]
}>()

const emit = defineEmits<{
  updated: []
}>()

const {
  t,
  templatesStore,
  canWriteVariables,
  searchQuery,
  variableDialogOpen,
  editingVariableKey,
  treeRef,
  variableTypes,
  piiCategories,
  variableForm,
  filteredTree,
  treeRenderKey,
  searchExpandedKeys,
  totalVariableCount,
  computeValidationError,
  sampleJson,
  sampleResult,
  sampleError,
  sampleEvaluating,
  openAddVariable,
  openEditVariable,
  handleSaveVariable,
  handleSampleEvaluate,
  handleDeleteVariable,
  filterTreeNode,
} = useTemplateVariableTreePanel({
  templateId: toRef(props, 'templateId'),
  variables: toRef(props, 'variables'),
  onUpdated: () => emit('updated'),
})
</script>

<template>
  <div class="variable-tree-panel" data-testid="variable-tree-panel">
    <SectionPanelHeader
      :title="t('templates.authoring.variablesTitle')"
      :help-title="t('templates.authoring.variablesHelpTitle')"
      :help-content="t('templates.authoring.variablesHelpContent')"
    >
      <template #actions>
        <el-button
          v-if="canWriteVariables"
          type="primary"
          plain
          data-testid="add-variable-button"
          @click="openAddVariable"
        >
          {{ t('templates.authoring.addVariable') }}
        </el-button>
      </template>
    </SectionPanelHeader>

    <div class="panel-toolbar">
      <el-input
        v-model="searchQuery"
        clearable
        class="search-input"
        :placeholder="t('templates.authoring.variableTreeSearchPlaceholder')"
      />
      <span class="variable-count">
        {{ t('templates.authoring.variableTreeCount', { count: totalVariableCount }) }}
      </span>
    </div>

    <el-empty
      v-if="variables.length === 0"
      :description="t('templates.authoring.noVariables')"
    />

    <el-tree
      v-else
      :key="treeRenderKey"
      ref="treeRef"
      class="variable-tree"
      :data="filteredTree"
      node-key="id"
      :default-expand-all="false"
      :default-expanded-keys="searchExpandedKeys"
      :expand-on-click-node="true"
      :filter-node-method="filterTreeNode"
    >
      <template #default="{ data }">
        <div class="tree-node" :class="{ 'tree-node--leaf': data.isLeaf }">
          <span class="tree-node__label">{{ data.displayLabel }}</span>
          <template v-if="!data.isLeaf && data.containerType">
            <el-tag size="small" type="primary">{{ data.containerType }}</el-tag>
          </template>
          <template v-if="data.isLeaf && data.variable">
            <el-tag size="small" type="info">{{ data.variable.variableType }}</el-tag>
            <el-tag v-if="data.variable.required" size="small" type="warning">
              {{ t('templates.authoring.required') }}
            </el-tag>
            <el-tag
              v-if="data.variable.piiCategory && data.variable.piiCategory !== 'NONE'"
              size="small"
              type="danger"
              effect="plain"
              :data-testid="`variable-pii-badge-${data.variable.variableKey}`"
            >
              {{ t('templates.authoring.piiBadge') }}
            </el-tag>
            <span v-if="data.technicalKey" class="tree-node__technical-key">{{ data.technicalKey }}</span>
            <span v-if="canWriteVariables" class="tree-node__actions">
              <el-button
                link
                type="primary"
                data-testid="edit-variable-button"
                @click.stop="openEditVariable(data.variable)"
              >
                {{ t('common.edit') }}
              </el-button>
              <el-button link type="danger" @click.stop="handleDeleteVariable(data.variable.variableKey)">
                {{ t('common.delete') }}
              </el-button>
            </span>
          </template>
          <span v-else-if="!data.isLeaf" class="tree-node__folder-key">{{ data.label }}</span>
        </div>
      </template>
    </el-tree>

    <el-dialog
      v-model="variableDialogOpen"
      :title="editingVariableKey ? t('templates.authoring.editVariable') : t('templates.authoring.addVariable')"
      width="520px"
    >
      <el-form label-position="top">
        <el-form-item :label="t('templates.authoring.variableKey')">
          <el-input
            v-model="variableForm.variableKey"
            data-testid="variable-key-input"
            :disabled="!canWriteVariables"
          />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.variableType')">
          <AppSearchSelect
            v-model="variableForm.variableType"
            style="width: 100%"
            :disabled="!canWriteVariables"
          >
            <el-option v-for="type in variableTypes" :key="type" :label="t(`templates.authoring.variableTypes.${type}`)" :value="type" />
          </AppSearchSelect>
        </el-form-item>
        <el-form-item :label="t('templates.authoring.required')">
          <el-switch v-model="variableForm.required" :disabled="!canWriteVariables" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.defaultValue')">
          <el-input v-model="variableForm.defaultValue" :disabled="!canWriteVariables" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.description')">
          <el-input
            v-model="variableForm.description"
            type="textarea"
            :rows="2"
            :disabled="!canWriteVariables"
          />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.piiCategory')">
          <AppSearchSelect
            v-model="variableForm.piiCategory"
            style="width: 100%"
            data-testid="variable-pii-category"
          >
            <el-option
              v-for="category in piiCategories"
              :key="category"
              :label="t(`templates.authoring.piiCategories.${category}`)"
              :value="category"
            />
          </AppSearchSelect>
        </el-form-item>
        <el-form-item
          v-if="variableForm.variableType === 'COMPUTED'"
          :label="t('templates.authoring.computeExpression')"
          :error="computeValidationError || undefined"
        >
          <el-input
            v-model="variableForm.computeExpression"
            type="textarea"
            :rows="3"
            :disabled="!canWriteVariables"
            :placeholder="t('templates.authoring.computeExpressionPlaceholder')"
          />
        </el-form-item>
        <template v-if="variableForm.variableType === 'COMPUTED'">
          <el-form-item :label="t('templates.authoring.computeSampleJson')">
            <el-input v-model="sampleJson" type="textarea" :rows="4" :disabled="!canWriteVariables" />
          </el-form-item>
          <div class="compute-sample-actions">
            <el-button :loading="sampleEvaluating" :disabled="!canWriteVariables" @click="handleSampleEvaluate">
              {{ t('templates.authoring.computeSampleEvaluate') }}
            </el-button>
          </div>
          <p v-if="sampleResult !== null" class="compute-sample-result">
            {{ t('templates.authoring.computeSampleResult') }}: {{ sampleResult }}
          </p>
          <p v-if="sampleError" class="compute-sample-error">{{ sampleError }}</p>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="variableDialogOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button
          v-if="canWriteVariables"
          type="primary"
          data-testid="save-variable-button"
          :loading="templatesStore.submitting"
          @click="handleSaveVariable"
        >
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss" src="./TemplateVariableTreePanel.scss"></style>
