<script setup lang="ts">
import { computed, ref, watch } from 'vue'
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

const props = defineProps<{
  referenceDialogTitle: string
  editingReferenceKey: string | null
  moduleOptions: ContentModuleSummary[]
  versionOptions: ContentModuleVersion[]
  moduleOptionLabel: (module: ContentModuleSummary) => string
  saving: boolean
  previewContentJson: string
  clauseEditReadonly: boolean
  savingClause: boolean
  referenceKeyUserOverridden?: boolean
}>()

const emit = defineEmits<{
  moduleChange: [moduleId: string]
  submitReference: []
  saveClause: []
  referenceKeyOverride: []
  clearKeyOverride: []
}>()

const { t } = useI18n()
const advancedActiveNames = ref<string[]>([])

const isEditPath = computed(() => Boolean(props.editingReferenceKey))

watch(referenceDialogOpen, (open) => {
  if (!open) {
    advancedActiveNames.value = []
  }
})

watch(
  () => props.editingReferenceKey,
  () => {
    advancedActiveNames.value = []
  },
)

function onOverrideInput() {
  emit('referenceKeyOverride')
}

function onClearOverride() {
  emit('clearKeyOverride')
  advancedActiveNames.value = []
}
</script>

<template>
  <el-dialog
    v-model="referenceDialogOpen"
    :title="referenceDialogTitle"
    width="560px"
    destroy-on-close
    data-testid="clause-reference-dialog"
  >
    <el-form label-position="top" data-testid="clause-reference-form">
      <el-form-item :label="t('templates.clauseAuthoring.form.moduleId')">
        <el-select
          :model-value="form.moduleId"
          filterable
          data-testid="clause-reference-module-select"
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
          data-testid="clause-reference-version-select"
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
      <el-form-item :label="t('templates.clauseAuthoring.form.referenceKey')">
        <el-input
          v-model="form.referenceKey"
          disabled
          data-testid="clause-reference-key-input"
          :placeholder="t('templates.clauseAuthoring.form.referenceKeyPlaceholder')"
        />
        <p
          v-if="!isEditPath && form.referenceKey && !referenceKeyUserOverridden"
          class="reference-key-hint"
          data-testid="clause-reference-key-auto-hint"
        >
          {{ t('templates.clauseAuthoring.form.referenceKeyAutoHint') }}
        </p>
        <p
          v-else-if="isEditPath"
          class="reference-key-hint"
          data-testid="clause-reference-key-locked-hint"
        >
          {{ t('templates.clauseAuthoring.form.referenceKeyLockedHint') }}
        </p>
      </el-form-item>
      <el-collapse
        v-if="!isEditPath"
        v-model="advancedActiveNames"
        class="reference-advanced"
        data-testid="clause-reference-advanced"
      >
        <el-collapse-item
          :title="t('templates.clauseAuthoring.form.advancedTitle')"
          name="advanced"
        >
          <p class="reference-advanced__hint">
            {{ t('templates.clauseAuthoring.form.advancedHint') }}
          </p>
          <el-form-item :label="t('templates.clauseAuthoring.form.customReferenceKey')">
            <el-input
              v-model="form.referenceKey"
              data-testid="clause-reference-key-override"
              :placeholder="t('templates.clauseAuthoring.form.referenceKeyPlaceholder')"
              @input="onOverrideInput"
            />
          </el-form-item>
          <el-button
            data-testid="clause-reference-key-reset"
            @click="onClearOverride"
          >
            {{ t('templates.clauseAuthoring.form.resetAutoKey') }}
          </el-button>
        </el-collapse-item>
      </el-collapse>
    </el-form>
    <template #footer>
      <el-button data-testid="clause-reference-cancel" @click="referenceDialogOpen = false">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="saving"
        data-testid="clause-reference-save"
        @click="emit('submitReference')"
      >
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

.reference-key-hint {
  margin: 0.35rem 0 0;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.reference-advanced {
  margin-top: 0.25rem;

  &__hint {
    margin: 0 0 0.75rem;
    color: var(--text-muted);
    font-size: var(--font-size-sm);
  }
}
</style>
