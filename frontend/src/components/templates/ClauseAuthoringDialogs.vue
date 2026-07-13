<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import type { ContentModuleSummary, ContentModuleVersion } from '@/types/contentModule'

const referenceDialogOpen = defineModel<boolean>('referenceDialogOpen', { required: true })
const previewDialogOpen = defineModel<boolean>('previewDialogOpen', { required: true })
const clauseEditDialogOpen = defineModel<boolean>('clauseEditDialogOpen', { required: true })
const clauseEditContentJson = defineModel<string>('clauseEditContentJson', { required: true })
const form = defineModel<{
  referenceKey: string
  moduleId: string
  semanticVersion: string
}>('form', { required: true })

defineProps<{
  referenceDialogTitle: string
  editingReferenceKey: string | null
  moduleOptions: ContentModuleSummary[]
  versionOptions: ContentModuleVersion[]
  moduleOptionLabel: (module: ContentModuleSummary) => string
  saving: boolean
  previewContentJson: string
  clauseEditReadonly: boolean
  savingClause: boolean
}>()

const emit = defineEmits<{
  moduleChange: [moduleId: string]
  submitReference: []
  saveClause: []
}>()

const { t } = useI18n()
</script>

<template>
  <el-dialog v-model="referenceDialogOpen" :title="referenceDialogTitle" width="560px" destroy-on-close>
    <el-form label-position="top">
      <el-form-item :label="t('templates.clauseAuthoring.form.referenceKey')">
        <el-input
          v-model="form.referenceKey"
          :disabled="Boolean(editingReferenceKey)"
          :placeholder="t('templates.clauseAuthoring.form.referenceKeyPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('templates.clauseAuthoring.form.moduleId')">
        <el-select
          :model-value="form.moduleId"
          filterable
          :placeholder="t('templates.clauseAuthoring.form.moduleIdPlaceholder')"
          @change="emit('moduleChange', $event)"
        >
          <el-option
            v-for="module in moduleOptions"
            :key="module.moduleId"
            :label="moduleOptionLabel(module)"
            :value="module.moduleId"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('templates.clauseAuthoring.form.semanticVersion')">
        <el-select
          v-model="form.semanticVersion"
          :disabled="!form.moduleId"
          :placeholder="t('templates.clauseAuthoring.form.semanticVersionPlaceholder')"
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
      <el-button @click="referenceDialogOpen = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="emit('submitReference')">
        {{ t('templates.clauseAuthoring.saveReference') }}
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="previewDialogOpen"
    :title="t('templates.clauseAuthoring.previewTitle')"
    width="900px"
    destroy-on-close
  >
    <ControlledStructuredContentEditor
      :model-value="previewContentJson"
      readonly
    />
  </el-dialog>

  <el-dialog
    v-model="clauseEditDialogOpen"
    :title="t('templates.clauseAuthoring.editClauseTitle')"
    width="900px"
    destroy-on-close
  >
    <el-alert
      v-if="clauseEditReadonly"
      type="info"
      :closable="false"
      show-icon
      class="readonly-alert"
      :title="t('templates.clauseAuthoring.approvedReadonlyHint')"
    />
    <ControlledStructuredContentEditor
      v-model="clauseEditContentJson"
      :readonly="clauseEditReadonly"
    />
    <template #footer>
      <el-button @click="clauseEditDialogOpen = false">{{ t('common.cancel') }}</el-button>
      <el-button
        v-if="!clauseEditReadonly"
        type="primary"
        :loading="savingClause"
        @click="emit('saveClause')"
      >
        {{ t('templates.clauseAuthoring.saveClause') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.readonly-alert {
  margin-bottom: 1rem;
}
</style>
