import { computed, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import type { TemplateContentModuleReference } from '@/types/template'

type Translate = (key: string, params?: Record<string, unknown>) => string
type HasKey = (key: string) => boolean

export function useClauseOutdatedBumpActions(options: {
  t: Translate
  te: HasKey
  templateId: string
  editable: Ref<boolean> | (() => boolean)
  references: Ref<TemplateContentModuleReference[]>
  bumping: Ref<boolean>
  emitUpdated: () => void
}) {
  const { t, te, templateId, references, bumping, emitUpdated } = options
  const panelDataStore = useTemplatePanelDataStore()

  const editable = computed(() =>
    typeof options.editable === 'function' ? options.editable() : options.editable.value,
  )

  const outdatedUnlockedReferences = computed(() =>
    references.value.filter(
      (reference) => reference.outOfDate && !reference.locked && reference.latestApprovedSemanticVersion,
    ),
  )

  const hasOutdatedUnlockedReferences = computed(() => outdatedUnlockedReferences.value.length > 0)

  async function bumpReference(reference: TemplateContentModuleReference): Promise<void> {
    if (!editable.value || reference.locked || !reference.outOfDate || !reference.latestApprovedSemanticVersion) {
      return
    }
    bumping.value = true
    try {
      await panelDataStore.upsertContentModuleReference(templateId, reference.referenceKey, {
        referenceKey: reference.referenceKey,
        moduleId: reference.moduleId,
        semanticVersion: reference.latestApprovedSemanticVersion,
      })
      ElMessage.success(t('templates.clauseAuthoring.bumpSuccess'))
      emitUpdated()
    } catch (error) {
      const key = resolveApiErrorMessageKey(error, 'templates.clauseAuthoring.error.saveReference')
      ElMessage.error(te(key) ? t(key) : t('templates.clauseAuthoring.error.saveReference'))
    } finally {
      bumping.value = false
    }
  }

  async function bumpAllOutdatedReferences(): Promise<void> {
    if (!editable.value || outdatedUnlockedReferences.value.length === 0) {
      return
    }
    try {
      await ElMessageBox.confirm(
        t('templates.clauseAuthoring.bumpAllConfirmMessage', {
          count: outdatedUnlockedReferences.value.length,
        }),
        t('templates.clauseAuthoring.bumpAllConfirmTitle'),
        {
          confirmButtonText: t('templates.clauseAuthoring.bumpAllConfirmAction'),
          cancelButtonText: t('common.cancel'),
          type: 'warning',
        },
      )
    } catch {
      return
    }

    bumping.value = true
    try {
      for (const reference of outdatedUnlockedReferences.value) {
        if (!reference.latestApprovedSemanticVersion) {
          continue
        }
        await panelDataStore.upsertContentModuleReference(templateId, reference.referenceKey, {
          referenceKey: reference.referenceKey,
          moduleId: reference.moduleId,
          semanticVersion: reference.latestApprovedSemanticVersion,
        })
      }
      ElMessage.success(t('templates.clauseAuthoring.bumpAllSuccess'))
      emitUpdated()
    } catch (error) {
      const key = resolveApiErrorMessageKey(error, 'templates.clauseAuthoring.error.saveReference')
      ElMessage.error(te(key) ? t(key) : t('templates.clauseAuthoring.error.saveReference'))
    } finally {
      bumping.value = false
    }
  }

  return {
    outdatedUnlockedReferences,
    hasOutdatedUnlockedReferences,
    bumpReference,
    bumpAllOutdatedReferences,
  }
}
