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
  searchQuery,
  variableDialogOpen,
  editingVariableKey,
  treeRef,
  variableTypes,
  variableForm,
  filteredTree,
  treeRenderKey,
  searchExpandedKeys,
  totalVariableCount,
  openAddVariable,
  openEditVariable,
  handleSaveVariable,
  handleDeleteVariable,
  filterTreeNode,
} = useTemplateVariableTreePanel({
  templateId: toRef(props, 'templateId'),
  variables: toRef(props, 'variables'),
  onUpdated: () => emit('updated'),
})
</script>

<template>
  <div class="variable-tree-panel">
    <SectionPanelHeader
      :title="t('templates.authoring.variablesTitle')"
      :help-title="t('templates.authoring.variablesHelpTitle')"
      :help-content="t('templates.authoring.variablesHelpContent')"
    >
      <template #actions>
        <el-button type="primary" plain @click="openAddVariable">
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
            <span v-if="data.technicalKey" class="tree-node__technical-key">{{ data.technicalKey }}</span>
            <span class="tree-node__actions">
              <el-button link type="primary" @click.stop="openEditVariable(data.variable)">
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
          <el-input v-model="variableForm.variableKey" :disabled="Boolean(editingVariableKey)" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.variableType')">
          <AppSearchSelect v-model="variableForm.variableType" style="width: 100%">
            <el-option v-for="type in variableTypes" :key="type" :label="type" :value="type" />
          </AppSearchSelect>
        </el-form-item>
        <el-form-item :label="t('templates.authoring.required')">
          <el-switch v-model="variableForm.required" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.defaultValue')">
          <el-input v-model="variableForm.defaultValue" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.description')">
          <el-input v-model="variableForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item
          v-if="variableForm.variableType === 'COMPUTED'"
          :label="t('templates.authoring.computeExpression')"
        >
          <el-input
            v-model="variableForm.computeExpression"
            :placeholder="t('templates.authoring.computeExpressionPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="variableDialogOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="templatesStore.submitting" @click="handleSaveVariable">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.variable-tree-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.panel-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.search-input {
  flex: 1 1 16rem;
  max-width: 28rem;
}

.variable-count {
  color: var(--text-muted);
  font-size: 0.875rem;
}

.variable-tree {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  max-height: 36rem;
  overflow: auto;
}

.tree-node {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  min-height: 1.75rem;
  padding-right: 0.5rem;

  &--leaf {
    width: 100%;
  }
}

.tree-node__label {
  font-weight: 500;
}

.tree-node__technical-key,
.tree-node__folder-key {
  color: var(--text-muted);
  font-size: 0.8125rem;
  font-family: var(--font-mono, ui-monospace, monospace);
}

.tree-node__folder-key {
  margin-left: 0.25rem;
}

.tree-node__actions {
  margin-left: auto;
  display: inline-flex;
  gap: 0.25rem;
}
</style>
