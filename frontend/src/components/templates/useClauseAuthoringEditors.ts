import { type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import * as contentModulesApi from '@/api/contentModules'
import { useContentModulesStore } from '@/stores/contentModules'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  serializeStructuredContent,
} from '@/utils/structuredContentNodes'
import { normalizeStructuredContentJson } from '@/utils/structuredContentCompat'
import type { ContentModuleVersion } from '@/types/contentModule'
import type { TemplateContentModuleReference } from '@/types/template'
import { ElMessage } from 'element-plus'

export function useClauseAuthoringEditors(options: {
  editable: () => boolean
  previewDialogOpen: Ref<boolean>
  previewContentJson: Ref<string>
  clauseEditDialogOpen: Ref<boolean>
  clauseEditContentJson: Ref<string>
  clauseEditReadonly: Ref<boolean>
  clauseEditVersion: Ref<ContentModuleVersion | null>
  clauseEditModuleId: Ref<string>
  savingClause: Ref<boolean>
  emitUpdated: () => void
}) {
  const { t } = useI18n()
  const contentModulesStore = useContentModulesStore()

  async function resolveReferencedVersion(
    reference: TemplateContentModuleReference,
  ): Promise<ContentModuleVersion | null> {
    const detail = await contentModulesApi.getContentModule(reference.moduleId)
    return (
      detail.versions.find((version) => version.semanticVersion === reference.semanticVersion) ??
      null
    )
  }

  async function openPreviewDialog(reference: TemplateContentModuleReference) {
    try {
      const version = await resolveReferencedVersion(reference)
      if (!version?.contentStructureJson) {
        ElMessage.warning(t('templates.clauseAuthoring.noContentStructure'))
        return
      }
      options.previewContentJson.value = serializeStructuredContent(
        normalizeStructuredContentJson(version.contentStructureJson),
      )
      options.previewDialogOpen.value = true
    } catch {
      ElMessage.error(t('templates.clauseAuthoring.error.loadContent'))
    }
  }

  async function openClauseEditor(reference: TemplateContentModuleReference) {
    if (!options.editable()) {
      return
    }
    try {
      const version = await resolveReferencedVersion(reference)
      if (!version) {
        ElMessage.warning(t('templates.clauseAuthoring.versionNotFound'))
        return
      }
      options.clauseEditVersion.value = version
      options.clauseEditModuleId.value = reference.moduleId
      options.clauseEditContentJson.value = version.contentStructureJson
        ? serializeStructuredContent(normalizeStructuredContentJson(version.contentStructureJson))
        : DEFAULT_STRUCTURED_CONTENT_JSON
      options.clauseEditReadonly.value = version.reviewState !== 'DRAFT'
      options.clauseEditDialogOpen.value = true
    } catch {
      ElMessage.error(t('templates.clauseAuthoring.error.loadContent'))
    }
  }

  async function handleSaveClauseContent() {
    const version = options.clauseEditVersion.value
    if (!version || options.clauseEditReadonly.value) {
      return
    }
    options.savingClause.value = true
    try {
      await contentModulesStore.updateDraftVersion(
        options.clauseEditModuleId.value,
        version.semanticVersion,
        {
          contentStructureJson: options.clauseEditContentJson.value,
        },
      )
      ElMessage.success(t('templates.clauseAuthoring.saveClauseSuccess'))
      options.clauseEditDialogOpen.value = false
      options.emitUpdated()
    } catch {
      ElMessage.error(t('templates.clauseAuthoring.error.saveClause'))
    } finally {
      options.savingClause.value = false
    }
  }

  return {
    openPreviewDialog,
    openClauseEditor,
    handleSaveClauseContent,
  }
}
