<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import AuthoringSideBySideLayout from '@/components/templates/AuthoringSideBySideLayout.vue'
import AuthoringPreviewPane from '@/components/templates/AuthoringPreviewPane.vue'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import ConditionExpressionInput from '@/components/authoring/ConditionExpressionInput.vue'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import type {
  AnchorBinding,
  PasteCleaningEvidence,
  PreviewRecord,
  VariableSchema,
} from '@/types/template'

const props = defineProps<{
  templateId: string
  editingRow: MasterAnchorBindingRow | null
  editingAnchorId: string | null
  draftDevVersionId: string
  contentTypes: readonly string[]
  editingPasteResidueBlocked: boolean
  variables: VariableSchema[]
  contentModuleReferenceKeys: string[]
  baselineStructuredContentJson?: string
  /** CE-U21 — concurrency token held for Save / draft meta (binding.updatedAt). */
  serverUpdatedAt?: string | null
  bindings: AnchorBinding[]
  lastPreview: PreviewRecord | null
  previewStale: boolean
  previewRefreshing: boolean
  submitting: boolean
  pasteResidueItemLabel: (messageKey: string) => string
}>()

const declaredContentType = defineModel<string>('declaredContentType', { required: true })
const structuredContentJson = defineModel<string>('structuredContentJson', { required: true })
const visibilityEnabled = defineModel<boolean>('visibilityEnabled', { required: true })
const visibilityExpression = defineModel<string>('visibilityExpression', { required: true })

const emit = defineEmits<{
  back: []
  save: []
  'clear-paste-residue': []
  'dirty-change': [dirty: boolean]
  'structure-change': []
  'paste-accepted': [evidence: PasteCleaningEvidence]
  'preview-refresh': []
}>()

const { t } = useI18n()
const structuredEditorRef = ref<InstanceType<typeof ControlledStructuredContentEditor> | null>(null)
const visibilityAdvancedNames = ref<string[]>([])
const visibilityVariableKeys = computed(() => props.variables.map((item) => item.variableKey))

function markPristine() {
  structuredEditorRef.value?.markPristine()
}

defineExpose({ markPristine })
</script>

<template>
  <div class="binding-editor" data-testid="binding-editor">
    <header class="binding-editor__action-rail" data-testid="binding-editor-action-rail">
      <el-button data-testid="binding-editor-back" @click="emit('back')">
        {{ t('common.back') }}
      </el-button>
      <div class="binding-editor__title" data-testid="binding-editor-anchor-title">
        <strong>{{ editingRow?.anchorId }}</strong>
        <span v-if="editingRow?.displayLabel" class="binding-editor__subtitle">
          {{ editingRow.displayLabel }}
        </span>
      </div>
      <el-button
        type="primary"
        :loading="submitting"
        :title="t('templates.authoring.saveBindingShortcutHint')"
        data-testid="binding-editor-save"
        @click="emit('save')"
      >
        {{ t('common.save') }}
      </el-button>
    </header>

    <p class="binding-editor__hint">{{ t('templates.authoring.bindingEditorSubtitle') }}</p>

    <AuthoringSideBySideLayout class="binding-editor__layout">
      <template #editor>
        <el-form label-position="top" class="binding-form" data-testid="binding-editor-form">
          <section class="binding-editor__section" data-testid="binding-editor-content-type">
            <el-form-item :label="t('templates.authoring.contentType')">
              <AppSearchSelect v-model="declaredContentType" style="width: 100%">
                <el-option v-for="type in contentTypes" :key="type" :label="type" :value="type" />
              </AppSearchSelect>
            </el-form-item>
          </section>

          <el-collapse
            v-model="visibilityAdvancedNames"
            class="binding-editor__visibility-advanced"
            data-testid="binding-editor-visibility-advanced"
          >
            <el-collapse-item
              :title="t('templates.authoring.visibilityCondition.advancedTitle')"
              name="visibility"
            >
              <p class="binding-editor__section-hint">
                {{ t('templates.authoring.visibilityCondition.description') }}
              </p>
              <el-form-item>
                <el-checkbox
                  v-model="visibilityEnabled"
                  data-testid="enable-visibility-checkbox"
                >
                  {{ t('templates.authoring.visibilityCondition.enable') }}
                </el-checkbox>
              </el-form-item>
              <el-form-item
                v-if="visibilityEnabled"
                :label="t('templates.authoring.visibilityCondition.expression')"
              >
                <ConditionExpressionInput
                  v-model="visibilityExpression"
                  :variable-keys="visibilityVariableKeys"
                  test-id="visibility-expression-input"
                  :placeholder="t('templates.authoring.visibilityCondition.expressionPlaceholder')"
                />
              </el-form-item>
            </el-collapse-item>
          </el-collapse>

          <section class="binding-editor__section" data-testid="binding-editor-structured">
            <h3 class="binding-editor__section-title">
              {{ t('templates.authoring.structuredContentEditor') }}
            </h3>
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
              :server-updated-at="serverUpdatedAt ?? editingRow?.binding?.updatedAt ?? null"
              :variables="variables"
              :content-module-reference-keys="contentModuleReferenceKeys"
              :baseline="baselineStructuredContentJson"
              compact-toolbar
              @dirty-change="emit('dirty-change', $event)"
              @structure-change="emit('structure-change')"
              @paste-accepted="emit('paste-accepted', $event)"
            />
          </section>
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

<style scoped lang="scss" src="./TemplateAuthoringBindingEditor.scss"></style>
