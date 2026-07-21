<script setup lang="ts">
import { toRef } from 'vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { useTemplateImportDialog } from '@/components/templates/useTemplateImportDialog'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  imported: [templateId: string]
}>()

const {
  t,
  templatesStore,
  formRef,
  parsedBundle,
  parseErrorKey,
  dependencyReport,
  checkingDependencies,
  dependencyBusy,
  canCommitImport,
  visible,
  form,
  formRules,
  masterOptions,
  apiErrorMessage,
  onFileRemove,
  onFileSelected,
  handleCheckDependencies,
  handleSubmit,
  dependencyItemMessage,
} = useTemplateImportDialog({
  modelValue: toRef(props, 'modelValue'),
  emitUpdateModelValue: (value) => emit('update:modelValue', value),
  emitImported: (templateId) => emit('imported', templateId),
})

defineExpose({
  form,
  onFileSelected,
  handleCheckDependencies,
  handleSubmit,
  canCommitImport,
  dependencyReport,
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.import.title')"
    width="720px"
    top="5vh"
    :close-on-click-modal="false"
    class="template-import-dialog"
    data-testid="template-import-dialog"
  >
    <p class="dialog-description">{{ t('templates.import.description') }}</p>

    <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
      <el-form-item :label="t('templates.import.bundleFile')" required>
        <el-upload
          :auto-upload="false"
          :limit="1"
          accept=".json,.zip,application/json,application/zip"
          :on-change="onFileSelected"
          :on-remove="onFileRemove"
        >
          <el-button>{{ t('templates.import.chooseFile') }}</el-button>
          <template #tip>
            <div class="upload-tip">{{ t('templates.import.fileHint') }}</div>
          </template>
        </el-upload>
        <el-alert
          v-if="parseErrorKey"
          class="parse-alert"
          type="error"
          :title="t(parseErrorKey)"
          show-icon
          :closable="false"
        />
      </el-form-item>

      <section v-if="parsedBundle" class="bundle-summary" aria-labelledby="import-bundle-summary-title">
        <h3 id="import-bundle-summary-title">{{ t('templates.import.bundleSummaryTitle') }}</h3>
        <dl>
          <div>
            <dt>{{ t('templates.import.bundleName') }}</dt>
            <dd>{{ parsedBundle.metadata.name }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.import.bundleExternalId') }}</dt>
            <dd>{{ parsedBundle.metadata.externalId }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.import.bundleGroup') }}</dt>
            <dd>{{ parsedBundle.metadata.groupCode }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.import.bundleSourceTemplateId') }}</dt>
            <dd>{{ parsedBundle.metadata.templateId }}</dd>
          </div>
        </dl>
      </section>

      <el-form-item :label="t('templates.import.master')" prop="masterId">
        <AppSearchSelect
          v-model="form.masterId"
          :options="masterOptions"
          :placeholder="t('templates.import.masterPlaceholder')"
          :disabled="!parsedBundle"
        />
      </el-form-item>

      <el-form-item :label="t('templates.import.conflictPolicy')">
        <el-radio-group
          v-model="form.importConflictPolicy"
          class="template-import-dialog__conflict-radios"
          :disabled="!parsedBundle"
        >
          <el-radio value="REJECT_IMPORT">
            {{ t('templates.import.conflictReject') }}
          </el-radio>
          <el-radio value="KEEP_TEMPLATE_ID">
            {{ t('templates.import.conflictKeepId') }}
          </el-radio>
        </el-radio-group>
        <p class="field-hint">{{ t('templates.import.conflictHint') }}</p>
      </el-form-item>
    </el-form>

    <section
      v-if="dependencyReport"
      class="dependency-report"
      aria-labelledby="import-dependency-report-title"
      data-testid="template-import-dependency-report"
    >
      <div class="dependency-report__header">
        <h3 id="import-dependency-report-title">{{ t('templates.import.dependencies.title') }}</h3>
        <el-tag
          :type="dependencyReport.readyToCommit ? 'success' : 'danger'"
          effect="plain"
        >
          {{
            dependencyReport.readyToCommit
              ? t('templates.import.dependencies.ready')
              : t('templates.import.dependencies.notReady')
          }}
        </el-tag>
      </div>
      <ul class="dependency-report__counts">
        <li>
          {{ t('templates.import.dependencies.blockingCount', { count: dependencyReport.blockingCount }) }}
        </li>
        <li>
          {{ t('templates.import.dependencies.warningCount', { count: dependencyReport.warningCount }) }}
        </li>
        <li>
          {{ t('templates.import.dependencies.infoCount', { count: dependencyReport.infoCount }) }}
        </li>
      </ul>
      <div class="dependency-report__list" role="list">
        <article
          v-for="(item, index) in dependencyReport.items"
          :key="`${item.code}-${index}`"
          class="dependency-report__item"
          role="listitem"
        >
          <div class="dependency-report__item-meta">
            <span class="dependency-report__type">{{ item.dependencyType }}</span>
            <el-tag size="small" effect="plain">{{ item.severity }}</el-tag>
            <span class="dependency-report__code">{{ item.code }}</span>
          </div>
          <p class="dependency-report__message">{{ dependencyItemMessage(item) }}</p>
        </article>
      </div>
    </section>

    <el-alert
      v-if="apiErrorMessage"
      type="error"
      :title="apiErrorMessage"
      show-icon
      :closable="false"
    />

    <template #footer>
      <div class="template-import-dialog__footer" data-testid="template-import-dialog-footer">
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          :loading="checkingDependencies"
          :disabled="!parsedBundle || dependencyBusy"
          @click="handleCheckDependencies"
        >
          {{ t('templates.import.checkDependencies') }}
        </el-button>
        <el-button
          type="primary"
          :loading="templatesStore.submitting && !checkingDependencies"
          :disabled="!canCommitImport || dependencyBusy"
          @click="handleSubmit"
        >
          {{ t('templates.import.submit') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dialog-description {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.upload-tip {
  color: var(--text-muted);
  font-size: 0.875rem;
}

.parse-alert {
  margin-top: 0.75rem;
}

.bundle-summary {
  margin-bottom: 1rem;
  padding: 0.75rem 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);

  h3 {
    margin: 0 0 0.75rem;
    font-size: 1rem;
  }

  dl {
    display: grid;
    gap: 0.5rem;
    margin: 0;
  }

  div {
    display: grid;
    grid-template-columns: 10rem 1fr;
    gap: 0.75rem;
  }

  dt {
    margin: 0;
    color: var(--text-muted);
  }

  dd {
    margin: 0;
    font-weight: 500;
    word-break: break-word;
  }
}

.field-hint {
  margin: 0.5rem 0 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.template-import-dialog__conflict-radios {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
}

.dependency-report {
  margin: 1rem 0;
  padding: 0.75rem 0 0;
  border-top: 1px solid var(--border-color);

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    margin-bottom: 0.75rem;

    h3 {
      margin: 0;
      font-size: 1rem;
    }
  }

  &__counts {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem 1.25rem;
    margin: 0 0 0.75rem;
    padding: 0;
    list-style: none;
    color: var(--text-muted);
    font-size: 0.875rem;
  }

  &__list {
    max-height: 14rem;
    overflow: auto;
    display: grid;
    gap: 0.5rem;
  }

  &__item {
    padding: 0.625rem 0.75rem;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    background: var(--surface-muted);
  }

  &__item-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.35rem;
  }

  &__type,
  &__code {
    font-size: 0.75rem;
    font-weight: 600;
    letter-spacing: 0.02em;
    color: var(--text-muted);
  }

  &__message {
    margin: 0;
    font-size: 0.875rem;
    word-break: break-word;
  }
}

.template-import-dialog__footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
}
</style>

<!-- Teleported el-dialog chrome — unscoped so max-height / sticky footer apply at 1440×900. -->
<style lang="scss">
.template-import-dialog.el-dialog {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 10vh);
  margin-bottom: 0;
}

.template-import-dialog .el-dialog__header {
  flex-shrink: 0;
}

.template-import-dialog .el-dialog__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
}

.template-import-dialog .el-dialog__footer {
  flex-shrink: 0;
  position: sticky;
  bottom: 0;
  z-index: 1;
  margin-top: 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-default);
  background: var(--surface-card);
}

.template-import-dialog__conflict-radios {
  --el-radio-input-border-color-hover: var(--brand-primary);
  --el-color-primary: var(--brand-primary);
}
</style>
