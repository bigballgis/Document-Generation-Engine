<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import * as contentModulesApi from '@/api/contentModules'
import * as templatesApi from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import { useContentModulesStore } from '@/stores/contentModules'
import { DEFAULT_STRUCTURED_CONTENT_JSON, serializeStructuredContent } from '@/utils/structuredContentNodes'
import { normalizeStructuredContentJson } from '@/utils/structuredContentCompat'
import type { ContentModuleSummary, ContentModuleVersion } from '@/types/contentModule'
import type { TemplateContentModuleReference } from '@/types/template'
import { ElMessage } from 'element-plus'

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

const { t, te } = useI18n()
const contentModulesStore = useContentModulesStore()

const loading = ref(false)
const saving = ref(false)
const savingClause = ref(false)
const referenceDialogOpen = ref(false)
const previewDialogOpen = ref(false)
const clauseEditDialogOpen = ref(false)
const references = ref<TemplateContentModuleReference[]>([])
const moduleOptions = ref<ContentModuleSummary[]>([])
const versionOptions = ref<ContentModuleVersion[]>([])
const editingReferenceKey = ref<string | null>(null)
const previewContentJson = ref(DEFAULT_STRUCTURED_CONTENT_JSON)
const clauseEditContentJson = ref(DEFAULT_STRUCTURED_CONTENT_JSON)
const clauseEditReadonly = ref(false)
const clauseEditVersion = ref<ContentModuleVersion | null>(null)
const clauseEditModuleId = ref('')

const form = reactive({
  referenceKey: '',
  moduleId: '',
  semanticVersion: '',
})

const referenceDialogTitle = computed(() =>
  editingReferenceKey.value
    ? t('templates.clauseAuthoring.editReferenceTitle', {
        referenceKey: editingReferenceKey.value,
      })
    : t('templates.clauseAuthoring.addReferenceTitle'),
)

function moduleOptionLabel(module: ContentModuleSummary): string {
  const base = `${module.moduleCode} — ${module.name}`
  if (module.groupCode === props.groupCode) {
    return base
  }
  return `${base} (${module.groupCode})`
}

function resolveModuleName(moduleId: string): string {
  const module = moduleOptions.value.find((item) => item.moduleId === moduleId)
  return module ? moduleOptionLabel(module) : moduleId
}

function approvedReferencableVersions(versions: ContentModuleVersion[]): ContentModuleVersion[] {
  return versions.filter(
    (version) =>
      version.reviewState === 'APPROVED' && (version.lifecycleState ?? 'ACTIVE') === 'ACTIVE',
  )
}

async function loadReferences() {
  loading.value = true
  try {
    references.value = await templatesApi.listTemplateContentModuleReferences(props.templateId)
    emit('referencesLoaded', references.value)
  } catch (error) {
    const key = resolveApiErrorMessageKey(error, 'templates.clauseAuthoring.error.load')
    ElMessage.error(te(key) ? t(key) : t('templates.clauseAuthoring.error.load'))
  } finally {
    loading.value = false
  }
}

async function loadModuleOptions() {
  if (!props.groupCode) {
    moduleOptions.value = []
    return
  }
  try {
    moduleOptions.value = await contentModulesApi.listContentModules(props.groupCode)
  } catch {
    moduleOptions.value = []
  }
}

async function loadVersionOptions(moduleId: string) {
  if (!moduleId) {
    versionOptions.value = []
    return
  }
  try {
    const detail = await contentModulesApi.getContentModule(moduleId)
    versionOptions.value = approvedReferencableVersions(detail.versions)
  } catch {
    versionOptions.value = []
  }
}

function resetForm() {
  form.referenceKey = ''
  form.moduleId = ''
  form.semanticVersion = ''
  versionOptions.value = []
}

function openCreateDialog() {
  editingReferenceKey.value = null
  resetForm()
  referenceDialogOpen.value = true
  void loadModuleOptions()
}

function openEditReferenceDialog(reference: TemplateContentModuleReference) {
  if (reference.locked || !props.editable) {
    return
  }
  editingReferenceKey.value = reference.referenceKey
  form.referenceKey = reference.referenceKey
  form.moduleId = reference.moduleId
  form.semanticVersion = reference.semanticVersion
  referenceDialogOpen.value = true
  void loadModuleOptions()
  void loadVersionOptions(reference.moduleId)
}

async function handleModuleChange(moduleId: string) {
  form.moduleId = moduleId
  form.semanticVersion = ''
  await loadVersionOptions(moduleId)
}

async function handleSubmitReference() {
  const referenceKey = form.referenceKey.trim()
  const moduleId = form.moduleId.trim()
  const semanticVersion = form.semanticVersion.trim()
  if (!referenceKey || !moduleId || !semanticVersion) {
    ElMessage.warning(t('templates.clauseAuthoring.validation.required'))
    return
  }

  saving.value = true
  try {
    const upsertKey = editingReferenceKey.value ?? referenceKey
    await templatesApi.upsertTemplateContentModuleReference(props.templateId, upsertKey, {
      referenceKey,
      moduleId,
      semanticVersion,
    })
    referenceDialogOpen.value = false
    ElMessage.success(t('templates.clauseAuthoring.saveReferenceSuccess'))
    await loadReferences()
    emit('updated')
  } catch (error) {
    const key = resolveApiErrorMessageKey(error, 'templates.clauseAuthoring.error.saveReference')
    ElMessage.error(te(key) ? t(key) : t('templates.clauseAuthoring.error.saveReference'))
  } finally {
    saving.value = false
  }
}

async function resolveReferencedVersion(
  reference: TemplateContentModuleReference,
): Promise<ContentModuleVersion | null> {
  const detail = await contentModulesApi.getContentModule(reference.moduleId)
  return detail.versions.find((version) => version.semanticVersion === reference.semanticVersion) ?? null
}

async function openPreviewDialog(reference: TemplateContentModuleReference) {
  try {
    const version = await resolveReferencedVersion(reference)
    if (!version?.contentStructureJson) {
      ElMessage.warning(t('templates.clauseAuthoring.noContentStructure'))
      return
    }
    previewContentJson.value = serializeStructuredContent(
      normalizeStructuredContentJson(version.contentStructureJson),
    )
    previewDialogOpen.value = true
  } catch {
    ElMessage.error(t('templates.clauseAuthoring.error.loadContent'))
  }
}

async function openClauseEditor(reference: TemplateContentModuleReference) {
  if (!props.editable) {
    return
  }
  try {
    const version = await resolveReferencedVersion(reference)
    if (!version) {
      ElMessage.warning(t('templates.clauseAuthoring.versionNotFound'))
      return
    }
    clauseEditVersion.value = version
    clauseEditModuleId.value = reference.moduleId
    clauseEditContentJson.value = version.contentStructureJson
      ? serializeStructuredContent(normalizeStructuredContentJson(version.contentStructureJson))
      : DEFAULT_STRUCTURED_CONTENT_JSON
    clauseEditReadonly.value = version.reviewState !== 'DRAFT'
    clauseEditDialogOpen.value = true
  } catch {
    ElMessage.error(t('templates.clauseAuthoring.error.loadContent'))
  }
}

async function handleSaveClauseContent() {
  const version = clauseEditVersion.value
  if (!version || clauseEditReadonly.value) {
    return
  }
  savingClause.value = true
  try {
    await contentModulesStore.updateDraftVersion(clauseEditModuleId.value, version.semanticVersion, {
      contentStructureJson: clauseEditContentJson.value,
    })
    ElMessage.success(t('templates.clauseAuthoring.saveClauseSuccess'))
    clauseEditDialogOpen.value = false
    emit('updated')
  } catch {
    ElMessage.error(t('templates.clauseAuthoring.error.saveClause'))
  } finally {
    savingClause.value = false
  }
}

onMounted(async () => {
  await loadModuleOptions()
  await loadReferences()
})

watch(
  () => props.refreshToken,
  () => {
    void loadReferences()
  },
)
</script>

<template>
  <div v-loading="loading" class="clause-authoring-panel">
    <SectionPanelHeader
      :title="t('templates.clauseAuthoring.title')"
      :help-title="t('templates.clauseAuthoring.helpTitle')"
      :help-content="t('templates.clauseAuthoring.helpContent')"
    >
      <template #actions>
        <el-button v-if="editable" type="primary" @click="openCreateDialog">
          {{ t('templates.clauseAuthoring.addReference') }}
        </el-button>
      </template>
    </SectionPanelHeader>

    <p v-if="!editable" class="read-only-hint">
      {{ t('templates.clauseAuthoring.readOnlyHint') }}
    </p>

    <AppDataTable v-if="references.length > 0" :data="references" class="references-table">
      <el-table-column
        prop="referenceKey"
        :label="t('templates.clauseAuthoring.columns.referenceKey')"
        min-width="160"
      />
      <el-table-column
        :label="t('templates.clauseAuthoring.columns.moduleName')"
        min-width="220"
      >
        <template #default="{ row }">
          {{ resolveModuleName(row.moduleId) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="semanticVersion"
        :label="t('templates.clauseAuthoring.columns.semanticVersion')"
        width="120"
      />
      <el-table-column
        :label="t('templates.clauseAuthoring.columns.locked')"
        width="100"
      >
        <template #default="{ row }">
          {{
            row.locked
              ? t('templates.clauseAuthoring.lockedYes')
              : t('templates.clauseAuthoring.lockedNo')
          }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('templates.clauseAuthoring.columns.actions')"
        width="280"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button link type="primary" @click="openPreviewDialog(row)">
            {{ t('templates.clauseAuthoring.preview') }}
          </el-button>
          <el-button
            v-if="editable"
            link
            type="primary"
            :disabled="row.locked"
            @click="openEditReferenceDialog(row)"
          >
            {{ t('templates.clauseAuthoring.editPin') }}
          </el-button>
          <el-button
            v-if="editable"
            link
            type="primary"
            @click="openClauseEditor(row)"
          >
            {{ t('templates.clauseAuthoring.editClause') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>

    <EmptyStatePanel
      v-else-if="!loading"
      title-key="templates.clauseAuthoring.empty"
      description-key="templates.clauseAuthoring.emptyDescription"
    />

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
        <el-button type="primary" :loading="saving" @click="handleSubmitReference">
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
          @click="handleSaveClauseContent"
        >
          {{ t('templates.clauseAuthoring.saveClause') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.clause-authoring-panel {
  margin-top: 0.5rem;
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

.readonly-alert {
  margin-bottom: 1rem;
}
</style>
