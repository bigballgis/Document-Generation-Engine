<script setup lang="ts">
import { toRef } from 'vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import { useTemplateContentModuleReferencesPanel } from '@/components/templates/useTemplateContentModuleReferencesPanel'

const props = defineProps<{
  templateId: string
  groupCode: string
  editable: boolean
  refreshToken?: number
}>()

const emit = defineEmits<{
  updated: []
}>()

const {
  t,
  contentModuleDetailLink,
  saving,
  dialogOpen,
  loading,
  references,
  moduleOptions,
  versionOptions,
  editingReferenceKey,
  form,
  dialogTitle,
  moduleOptionLabel,
  resolveModuleName,
  resolveModuleSubtitle,
  openCreateDialog,
  openEditDialog,
  handleModuleChange,
  handleSubmit,
} = useTemplateContentModuleReferencesPanel({
  templateId: toRef(props, 'templateId'),
  groupCode: toRef(props, 'groupCode'),
  editable: toRef(props, 'editable'),
  refreshToken: toRef(props, 'refreshToken'),
  emitUpdated: () => emit('updated'),
})
</script>

<template>
  <div v-loading="loading" class="content-module-references-panel">
    <div class="panel-header">
      <div>
        <h3>{{ t('templates.contentModuleReferences.title') }}</h3>
        <p>{{ t('templates.contentModuleReferences.description') }}</p>
      </div>
      <el-button v-if="editable" type="primary" @click="openCreateDialog">
        {{ t('templates.contentModuleReferences.add') }}
      </el-button>
    </div>

    <p v-if="!editable" class="read-only-hint">
      {{ t('templates.contentModuleReferences.readOnlyHint') }}
    </p>

    <AppDataTable v-if="references.length > 0" :data="references" class="references-table">
      <el-table-column
        prop="referenceKey"
        :label="t('templates.contentModuleReferences.columns.referenceKey')"
        min-width="180"
      />
      <el-table-column
        :label="t('templates.contentModuleReferences.columns.moduleId')"
        min-width="200"
      >
        <template #default="{ row }">
          <EntityLinkCell
            :label="resolveModuleName(row.moduleId)"
            :subtitle="resolveModuleSubtitle(row.moduleId)"
            :to="contentModuleDetailLink(row.moduleId)"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="semanticVersion"
        :label="t('templates.contentModuleReferences.columns.semanticVersion')"
        width="140"
      />
      <el-table-column
        :label="t('templates.contentModuleReferences.columns.locked')"
        width="120"
      >
        <template #default="{ row }">
          {{
            row.locked
              ? t('templates.contentModuleReferences.lockedYes')
              : t('templates.contentModuleReferences.lockedNo')
          }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="editable"
        :label="t('templates.contentModuleReferences.columns.actions')"
        width="120"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="row.locked"
            @click="openEditDialog(row)"
          >
            {{ t('templates.contentModuleReferences.edit') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>

    <EmptyStatePanel
      v-else-if="!loading"
      title-key="templates.contentModuleReferences.empty"
      description-key="templates.contentModuleReferences.emptyDescription"
    />

    <el-dialog v-model="dialogOpen" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item :label="t('templates.contentModuleReferences.form.referenceKey')">
          <el-input
            v-model="form.referenceKey"
            :disabled="Boolean(editingReferenceKey)"
            :placeholder="t('templates.contentModuleReferences.form.referenceKeyPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('templates.contentModuleReferences.form.moduleId')">
          <el-select
            :model-value="form.moduleId"
            filterable
            :placeholder="t('templates.contentModuleReferences.form.moduleIdPlaceholder')"
            @change="handleModuleChange"
          >
            <el-option
              v-for="module in moduleOptions"
              :key="module.moduleId"
              :label="moduleOptionLabel(module)"
              :value="module.moduleId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('templates.contentModuleReferences.form.semanticVersion')">
          <el-select
            v-model="form.semanticVersion"
            :disabled="!form.moduleId"
            :placeholder="t('templates.contentModuleReferences.form.semanticVersionPlaceholder')"
          >
            <el-option
              v-for="version in versionOptions"
              :key="version.versionId"
              :label="version.semanticVersion"
              :value="version.semanticVersion"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ t('templates.contentModuleReferences.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.content-module-references-panel {
  margin-top: 1.5rem;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;

  h3 {
    margin: 0 0 0.25rem;
    font-size: 1rem;
    font-weight: 650;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.read-only-hint {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.references-table {
  margin-top: 0.5rem;
}
</style>
