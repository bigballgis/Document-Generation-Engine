<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import * as contentModulesApi from '@/api/contentModules'
import * as templatesApi from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
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
}>()

const { t, te } = useI18n()

const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const references = ref<TemplateContentModuleReference[]>([])
const moduleOptions = ref<ContentModuleSummary[]>([])
const versionOptions = ref<ContentModuleVersion[]>([])
const editingReferenceKey = ref<string | null>(null)

const form = reactive({
  referenceKey: '',
  moduleId: '',
  semanticVersion: '',
})

const dialogTitle = computed(() =>
  editingReferenceKey.value
    ? t('templates.contentModuleReferences.editTitle', {
        referenceKey: editingReferenceKey.value,
      })
    : t('templates.contentModuleReferences.addTitle'),
)

function moduleOptionLabel(module: ContentModuleSummary): string {
  const base = `${module.moduleCode} — ${module.name}`
  if (module.groupCode === props.groupCode) {
    return base
  }
  return `${base} (${module.groupCode})`
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
  } catch (error) {
    const key = resolveApiErrorMessageKey(error, 'templates.contentModuleReferences.error.load')
    ElMessage.error(te(key) ? t(key) : t('templates.contentModuleReferences.error.load'))
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
  dialogOpen.value = true
  void loadModuleOptions()
}

function openEditDialog(reference: TemplateContentModuleReference) {
  if (reference.locked || !props.editable) {
    return
  }
  editingReferenceKey.value = reference.referenceKey
  form.referenceKey = reference.referenceKey
  form.moduleId = reference.moduleId
  form.semanticVersion = reference.semanticVersion
  dialogOpen.value = true
  void loadModuleOptions()
  void loadVersionOptions(reference.moduleId)
}

async function handleModuleChange(moduleId: string) {
  form.moduleId = moduleId
  form.semanticVersion = ''
  await loadVersionOptions(moduleId)
}

async function handleSubmit() {
  const referenceKey = form.referenceKey.trim()
  const moduleId = form.moduleId.trim()
  const semanticVersion = form.semanticVersion.trim()
  if (!referenceKey || !moduleId || !semanticVersion) {
    ElMessage.warning(t('templates.contentModuleReferences.validation.required'))
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
    dialogOpen.value = false
    ElMessage.success(t('templates.contentModuleReferences.saveSuccess'))
    await loadReferences()
    emit('updated')
  } catch (error) {
    const key = resolveApiErrorMessageKey(error, 'templates.contentModuleReferences.error.save')
    ElMessage.error(te(key) ? t(key) : t('templates.contentModuleReferences.error.save'))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadReferences()
})

watch(
  () => props.refreshToken,
  () => {
    void loadReferences()
  },
)
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
        prop="moduleId"
        :label="t('templates.contentModuleReferences.columns.moduleId')"
        min-width="200"
      />
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
