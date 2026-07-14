import { computed, onBeforeUnmount, ref, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import type { PreviewRecord } from '@/types/template'

export function useInlinePdfPreview(options: {
  templateId: MaybeRefOrGetter<string>
  preview: MaybeRefOrGetter<PreviewRecord | null>
}) {
  const { t } = useI18n()
  const panelDataStore = useTemplatePanelDataStore()

  const loading = ref(false)
  const errorMessage = ref<string | null>(null)
  const pdfBlob = ref<Blob | null>(null)
  let loadGeneration = 0

  const canShowInlinePdf = computed(() => {
    const record = toValue(options.preview)
    return (
      record?.status === 'SUCCEEDED' &&
      (Boolean(record.pdfArtifactStorageKey) || Boolean(record.artifactStorageKey))
    )
  })

  async function loadPdfBlob() {
    const record = toValue(options.preview)
    const templateId = toValue(options.templateId)
    if (!record?.previewId || !canShowInlinePdf.value) {
      pdfBlob.value = null
      errorMessage.value = null
      return
    }

    const generation = ++loadGeneration
    loading.value = true
    errorMessage.value = null

    try {
      const { blob } = await panelDataStore.downloadPreviewArtifact(
        templateId,
        record.previewId,
        'pdf',
      )
      if (generation !== loadGeneration) {
        return
      }
      pdfBlob.value = blob
    } catch {
      if (generation !== loadGeneration) {
        return
      }
      pdfBlob.value = null
      errorMessage.value = t('templates.preview.inlinePdf.loadFailed')
    } finally {
      if (generation === loadGeneration) {
        loading.value = false
      }
    }
  }

  watch(
    () => [toValue(options.templateId), toValue(options.preview)?.previewId, canShowInlinePdf.value] as const,
    () => {
      void loadPdfBlob()
    },
    { immediate: true },
  )

  onBeforeUnmount(() => {
    loadGeneration++
    pdfBlob.value = null
  })

  return {
    loading,
    errorMessage,
    pdfBlob,
    canShowInlinePdf,
    reloadPdf: loadPdfBlob,
  }
}
