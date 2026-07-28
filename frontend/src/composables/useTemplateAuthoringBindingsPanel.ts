import { computed, onMounted, onUnmounted, ref, watch, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getMaster } from '@/api/masters'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import { registerAuthoringEditorContext } from '@/composables/authoringEditorContext'
import { useCapabilities } from '@/composables/useCapabilities'
import { useAuthoringEditorShortcuts } from '@/composables/useAuthoringEditorShortcuts'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import {
  BINDING_CONTENT_TYPES,
  type StructuredBindingEditorExpose,
  type TemplateAuthoringBindingsPanelProps,
} from '@/composables/templateAuthoringBindingsTypes'
import { useTemplateAuthoringBindingsEdit } from '@/composables/useTemplateAuthoringBindingsEdit'
import { useTemplatesStore } from '@/stores/templates'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import type { BindingValidationResult, PreviewRecord } from '@/types/template'
import type { MasterAnchor } from '@/types/master'

export type {
  BindingPanelMode,
  StructuredBindingEditorExpose,
  TemplateAuthoringBindingsPanelProps,
} from '@/composables/templateAuthoringBindingsTypes'

export function useTemplateAuthoringBindingsPanel(
  props: TemplateAuthoringBindingsPanelProps,
  emit: {
    (e: 'updated'): void
    (e: 'preview-refreshed', preview: PreviewRecord): void
  },
  structuredEditorRef: Ref<StructuredBindingEditorExpose | null>,
) {
  const { t, te } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const { authorTemplates } = useCapabilities()

  const masterAnchors = ref<MasterAnchor[]>([])
  const loadingMaster = ref(false)
  const validating = ref(false)
  const validationResult = ref<BindingValidationResult | null>(null)

  const edit = useTemplateAuthoringBindingsEdit(props, emit, structuredEditorRef, masterAnchors)

  // CE-U17 — Ctrl/Cmd+S / Ctrl/Cmd+P while Bindings panel is mounted; handlers only when editing.
  useAuthoringEditorShortcuts()
  let unregisterAuthoringContext: (() => void) | null = null
  watch(
    [() => edit.panelMode.value, authorTemplates],
    ([mode]) => {
      unregisterAuthoringContext?.()
      unregisterAuthoringContext = null
      if (mode !== 'edit') {
        return
      }
      unregisterAuthoringContext = registerAuthoringEditorContext({
        saveBinding: () => edit.handleSaveBinding(),
        refreshPreview: () => edit.handlePreviewRefresh(),
        canSave: () => authorTemplates.value,
        canRefresh: () => true,
        isSaving: () => templatesStore.submitting,
        isRefreshing: () => edit.previewRefreshing.value,
      })
    },
    { immediate: true },
  )
  onUnmounted(() => {
    unregisterAuthoringContext?.()
    unregisterAuthoringContext = null
  })

  const contentModuleReferenceKeys = computed(() =>
    props.contentModuleReferences.map((reference) => reference.referenceKey),
  )

  const { filters: bindingColumnFilters, filteredRows: filteredAnchorRows } = useDataTableFilters(
    edit.anchorRowsSource,
    [
      { key: 'anchorId', getValue: (row) => row.anchorId },
      { key: 'displayLabel', getValue: (row) => row.displayLabel },
      {
        key: 'declaredContentType',
        getValue: (row) => row.declaredContentType ?? '',
        matchMode: 'exact',
      },
      { key: 'validationStatus', getValue: (row) => row.validationStatus ?? '' },
    ],
  )

  const contentTypeFilterOptions = computed(() =>
    BINDING_CONTENT_TYPES.map((type) => ({ value: type, label: type })),
  )

  const configuredBindingCount = computed(() => props.bindings.length)

  onMounted(async () => {
    loadingMaster.value = true
    try {
      const master = await getMaster(props.masterId)
      masterAnchors.value = master.anchors
      openAnchorFromQueryIfPresent()
    } catch {
      ElMessage.error(t('templates.authoring.masterAnchorsLoadFailed'))
    } finally {
      loadingMaster.value = false
    }
  })

  function openAnchorFromQueryIfPresent() {
    const anchorId = typeof route.query.anchorId === 'string' ? route.query.anchorId : null
    if (!anchorId) {
      return
    }
    const row = edit.anchorRowsSource.value.find((candidate) => candidate.anchorId === anchorId)
    if (!row) {
      // FOS-W2-6 — do not fail silently on deep links.
      ElMessage.warning(t('templates.authoring.anchorDeepLinkNotFound', { anchorId }))
      return
    }
    edit.openEditPanel(row)
    const nextQuery = { ...route.query }
    delete nextQuery.anchorId
    void router.replace({ query: nextQuery })
  }

  function resolveValidationStatusLabel(status: string | undefined | null): string {
    if (!status) {
      return t('templates.authoring.validationUnknown')
    }
    const key = `templates.bindingGate.status.${status}`
    return te(key) ? t(key) : status
  }

  function resolveConfiguredLabel(row: MasterAnchorBindingRow): string {
    return row.configured
      ? t('templates.authoring.bindingConfigured')
      : t('templates.authoring.bindingNotConfigured')
  }

  async function handleValidateBindings() {
    validating.value = true
    validationResult.value = null
    try {
      const result = await templatesStore.validateBindings(props.templateId)
      validationResult.value = result
      emit('updated')
      if (result.summary.blocking) {
        ElMessage.warning(t('templates.authoring.bindingValidationBlocking'))
      } else {
        ElMessage.success(t('templates.authoring.bindingValidationSuccess'))
      }
    } catch (error) {
      const messageKey = resolveApiErrorMessageKey(error, 'templates.error.bindingValidation')
      ElMessage.error(te(messageKey) ? t(messageKey) : t('templates.error.bindingValidation'))
    } finally {
      validating.value = false
    }
  }

  return {
    contentTypes: BINDING_CONTENT_TYPES,
    templatesStore,
    panelMode: edit.panelMode,
    loadingMaster,
    validating,
    editingAnchorId: edit.editingAnchorId,
    validationResult,
    visibilityEnabled: edit.visibilityEnabled,
    visibilityExpression: edit.visibilityExpression,
    editSnapshot: edit.editSnapshot,
    bindingForm: edit.bindingForm,
    draftDevVersionId: edit.draftDevVersionId,
    bindingColumnFilters,
    filteredAnchorRows,
    contentTypeFilterOptions,
    configuredBindingCount,
    editingRow: edit.editingRow,
    contentModuleReferenceKeys,
    previewStale: edit.previewStale,
    previewRefreshing: edit.previewRefreshing,
    dirtyGuardDialogVisible: edit.dirtyGuardDialogVisible,
    dirtyGuardSaving: edit.dirtyGuardSaving,
    dirtyGuardShowSave: edit.dirtyGuardShowSave,
    dirtyGuardStay: edit.dirtyGuardStay,
    dirtyGuardDiscard: edit.dirtyGuardDiscard,
    dirtyGuardSave: edit.dirtyGuardSave,
    editingPasteResidueBlocked: edit.editingPasteResidueBlocked,
    resolveValidationStatusLabel,
    resolveConfiguredLabel,
    bindingHasPasteBlockers: edit.bindingHasPasteBlockers,
    pasteResidueItemLabel: edit.pasteResidueItemLabel,
    handlePasteAccepted: edit.handlePasteAccepted,
    clearPendingPasteResidue: edit.clearPendingPasteResidue,
    openEditPanel: edit.openEditPanel,
    expectedUpdatedAt: edit.expectedUpdatedAt,
    backToList: edit.backToList,
    handleSaveBinding: edit.handleSaveBinding,
    handlePreviewRefresh: edit.handlePreviewRefresh,
    handleEditorDirtyChange: edit.handleEditorDirtyChange,
    handleStructureChange: edit.handleStructureChange,
    handleValidateBindings,
  }
}
