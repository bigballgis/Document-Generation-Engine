<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import AuthoringSideBySideLayout from '@/components/templates/AuthoringSideBySideLayout.vue'
import AuthoringPreviewPane from '@/components/templates/AuthoringPreviewPane.vue'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import type {
  AnchorBinding,
  PasteCleaningEvidence,
  PreviewRecord,
  VariableSchema,
} from '@/types/template'

defineProps<{
  templateId: string
  editingRow: MasterAnchorBindingRow | null
  editingAnchorId: string | null
  draftDevVersionId: string
  contentTypes: readonly string[]
  visibilityEnabled: boolean
  visibilityExpression: string
  editingPasteResidueBlocked: boolean
  variables: VariableSchema[]
  contentModuleReferenceKeys: string[]
  baselineStructuredContentJson?: string
  bindings: AnchorBinding[]
  lastPreview: PreviewRecord | null
  previewStale: boolean
  previewRefreshing: boolean
  submitting: boolean
  pasteResidueItemLabel: (messageKey: string) => string
}>()

const declaredContentType = defineModel<string>('declaredContentType', { required: true })
const structuredContentJson = defineModel<string>('structuredContentJson', { required: true })

const emit = defineEmits<{
  back: []
  save: []
  'update:visibilityEnabled': [value: boolean]
  'update:visibilityExpression': [value: string]
  'clear-paste-residue': []
  'dirty-change': [dirty: boolean]
  'structure-change': []
  'paste-accepted': [evidence: PasteCleaningEvidence]
  'preview-refresh': []
}>()

const { t } = useI18n()
const structuredEditorRef = ref<InstanceType<typeof ControlledStructuredContentEditor> | null>(null)

function markPristine() {
  structuredEditorRef.value?.markPristine()
}

defineExpose({ markPristine })
</script>

<template>
  <div class="binding-editor">
    <div class="binding-editor__toolbar">
      <el-button @click="emit('back')">{{ t('common.back') }}</el-button>
      <div class="binding-editor__title">
        <strong>{{ editingRow?.anchorId }}</strong>
        <span v-if="editingRow?.displayLabel" class="binding-editor__subtitle">
          {{ editingRow.displayLabel }}
        </span>
      </div>
      <el-button type="primary" :loading="submitting" @click="emit('save')">
        {{ t('common.save') }}
      </el-button>
    </div>

    <p class="binding-editor__hint">{{ t('templates.authoring.bindingEditorSubtitle') }}</p>

    <AuthoringSideBySideLayout>
      <template #editor>
        <el-form label-position="top" class="binding-form">
          <el-form-item :label="t('templates.authoring.contentType')">
            <AppSearchSelect v-model="declaredContentType" style="width: 100%">
              <el-option v-for="type in contentTypes" :key="type" :label="type" :value="type" />
            </AppSearchSelect>
          </el-form-item>

          <div class="visibility-section">
            <h4>{{ t('templates.authoring.visibilityCondition.title') }}</h4>
            <p class="visibility-section__hint">
              {{ t('templates.authoring.visibilityCondition.description') }}
            </p>
            <el-form-item>
              <el-checkbox
                :model-value="visibilityEnabled"
                @update:model-value="emit('update:visibilityEnabled', $event as boolean)"
              >
                {{ t('templates.authoring.visibilityCondition.enable') }}
              </el-checkbox>
            </el-form-item>
            <el-form-item
              v-if="visibilityEnabled"
              :label="t('templates.authoring.visibilityCondition.expression')"
            >
              <el-input
                :model-value="visibilityExpression"
                :placeholder="t('templates.authoring.visibilityCondition.expressionPlaceholder')"
                @update:model-value="emit('update:visibilityExpression', String($event))"
              />
            </el-form-item>
          </div>

          <el-form-item :label="t('templates.authoring.structuredContentEditor')">
            <el-alert
              v-if="editingPasteResidueBlocked"
              type="error"
              show-icon
              :closable="false"
              class="paste-residue-alert"
              data-testid="binding-paste-residue-alert"
              :title="t('templates.authoring.pasteResidue.blockedTitle')"
              :description="t('templates.authoring.pasteResidue.blockedDescription')"
            />
            <ul
              v-if="editingPasteResidueBlocked && editingRow?.binding?.pasteCleaningEvidence?.items?.length"
              class="paste-residue-items"
              data-testid="binding-paste-residue-items"
            >
              <li
                v-for="(item, index) in editingRow.binding.pasteCleaningEvidence.items"
                :key="`${item.messageKey}-${index}`"
              >
                {{ pasteResidueItemLabel(item.messageKey) }}
              </li>
            </ul>
            <el-button
              v-if="editingPasteResidueBlocked"
              size="small"
              data-testid="binding-clear-paste-residue"
              @click="emit('clear-paste-residue')"
            >
              {{ t('templates.authoring.pasteResidue.clearAction') }}
            </el-button>
            <ControlledStructuredContentEditor
              ref="structuredEditorRef"
              v-model="structuredContentJson"
              :template-id="templateId"
              :dev-version-id="draftDevVersionId"
              :anchor-id="editingAnchorId ?? undefined"
              :variables="variables"
              :content-module-reference-keys="contentModuleReferenceKeys"
              :baseline="baselineStructuredContentJson"
              @dirty-change="emit('dirty-change', $event)"
              @structure-change="emit('structure-change')"
              @paste-accepted="emit('paste-accepted', $event)"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #preview>
        <AuthoringPreviewPane
          :template-id="templateId"
          :bindings="bindings"
          :preview="lastPreview"
          :stale="previewStale"
          :refreshing="previewRefreshing"
          @refresh="emit('preview-refresh')"
        />
      </template>
    </AuthoringSideBySideLayout>
  </div>
</template>

<style scoped lang="scss">
.binding-editor {
  &__toolbar {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 0.75rem;
  }

  &__title {
    flex: 1;
    min-width: 0;
  }

  &__subtitle {
    display: block;
    margin-top: 0.25rem;
    color: var(--text-muted);
    font-size: 0.875rem;
    font-weight: normal;
  }

  &__hint {
    margin: 0 0 1rem;
    color: var(--text-muted);
  }
}

.visibility-section {
  margin-bottom: 1rem;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-muted);

  h4 {
    margin: 0 0 0.25rem;
    font-size: 0.9375rem;
    font-weight: 650;
  }

  &__hint {
    margin: 0 0 0.75rem;
    color: var(--text-muted);
    font-size: 0.875rem;
  }
}

.paste-residue-alert {
  margin-bottom: 0.75rem;
}

.paste-residue-items {
  margin: 0 0 0.75rem;
  padding-left: 1.25rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
