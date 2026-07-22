<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import ClauseAuthoringReferencesTable from '@/components/templates/ClauseAuthoringReferencesTable.vue'
import ClauseAuthoringDialogs from '@/components/templates/ClauseAuthoringDialogs.vue'
import { useClauseAuthoringPanel } from '@/components/templates/useClauseAuthoringPanel'
import type { TemplateContentModuleReference } from '@/types/template'

const props = defineProps<{
  templateId: string
  groupCode: string
  editable: boolean
  refreshToken?: number
}>()

const emit = defineEmits<{
  updated: []
  referencesLoaded: [references: TemplateContentModuleReference[]]
}>()

const { t } = useI18n()

const {
  saving,
  savingClause,
  bumping,
  hasOutdatedUnlockedReferences,
  referenceDialogOpen,
  previewDialogOpen,
  clauseEditDialogOpen,
  loading,
  references,
  moduleOptions,
  versionOptions,
  editingReferenceKey,
  previewContentJson,
  clauseEditContentJson,
  clauseEditReadonly,
  form,
  referenceDialogTitle,
  moduleOptionLabel,
  resolveModuleName,
  openCreateDialog,
  openEditReferenceDialog,
  handleModuleChange,
  handleSubmitReference,
  referenceKeyUserOverridden,
  markReferenceKeyOverridden,
  clearReferenceKeyOverride,
  bumpReference,
  bumpAllOutdatedReferences,
  openPreviewDialog,
  openClauseEditor,
  handleSaveClauseContent,
} = useClauseAuthoringPanel(props, emit)
</script>

<template>
  <div v-loading="loading" class="clause-authoring-panel">
    <SectionPanelHeader
      :title="t('templates.clauseAuthoring.title')"
      :help-title="t('templates.clauseAuthoring.helpTitle')"
      :help-content="t('templates.clauseAuthoring.helpContent')"
    >
      <template #actions>
        <el-button
          v-if="editable && hasOutdatedUnlockedReferences"
          type="warning"
          plain
          :loading="bumping"
          data-testid="clause-bump-all-outdated"
          @click="bumpAllOutdatedReferences"
        >
          {{ t('templates.clauseAuthoring.bumpAllOutdated') }}
        </el-button>
        <el-button v-if="editable" type="primary" @click="openCreateDialog">
          {{ t('templates.clauseAuthoring.addReference') }}
        </el-button>
      </template>
    </SectionPanelHeader>

    <p v-if="!editable" class="read-only-hint">
      {{ t('templates.clauseAuthoring.readOnlyHint') }}
    </p>

    <ClauseAuthoringReferencesTable
      :references="references"
      :loading="loading"
      :editable="editable"
      :resolve-module-name="resolveModuleName"
      @preview="openPreviewDialog"
      @edit-pin="openEditReferenceDialog"
      @edit-clause="openClauseEditor"
      @bump="bumpReference"
    />

    <ClauseAuthoringDialogs
      v-model:reference-dialog-open="referenceDialogOpen"
      v-model:preview-dialog-open="previewDialogOpen"
      v-model:clause-edit-dialog-open="clauseEditDialogOpen"
      v-model:clause-edit-content-json="clauseEditContentJson"
      v-model:form="form"
      :reference-dialog-title="referenceDialogTitle"
      :editing-reference-key="editingReferenceKey"
      :module-options="moduleOptions"
      :version-options="versionOptions"
      :module-option-label="moduleOptionLabel"
      :saving="saving"
      :preview-content-json="previewContentJson"
      :clause-edit-readonly="clauseEditReadonly"
      :saving-clause="savingClause"
      :reference-key-user-overridden="referenceKeyUserOverridden"
      @module-change="handleModuleChange"
      @submit-reference="handleSubmitReference"
      @reference-key-override="markReferenceKeyOverridden"
      @clear-key-override="clearReferenceKeyOverride"
      @save-clause="handleSaveClauseContent"
    />
  </div>
</template>

<style scoped lang="scss">
.clause-authoring-panel {
  margin-top: 0.5rem;
}

.read-only-hint {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
