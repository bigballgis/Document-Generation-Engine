import { onBeforeUnmount, ref, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'

/** Loads a preview-run PDF blob by id when `enabled` (SUCCEEDED + pdfAvailable). */
export function usePreviewRunPdfBlob(options: {
  templateId: MaybeRefOrGetter<string>
  previewId: MaybeRefOrGetter<string | null | undefined>
  enabled: MaybeRefOrGetter<boolean>
}) {
  const { t } = useI18n()
  const panelDataStore = useTemplatePanelDataStore()

  const loading = ref(false)
  const errorMessage = ref<string | null>(null)
  const pdfBlob = ref<Blob | null>(null)
  let loadGeneration = 0

  async function loadPdfBlob() {
    const templateId = toValue(options.templateId)
    const previewId = toValue(options.previewId)
    const enabled = toValue(options.enabled)

    if (!previewId || !enabled) {
      pdfBlob.value = null
      errorMessage.value = null
      loading.value = false
      return
    }

    const generation = ++loadGeneration
    loading.value = true
    errorMessage.value = null

    try {
      const { blob } = await panelDataStore.downloadPreviewArtifact(templateId, previewId, 'pdf')
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
    () => [toValue(options.templateId), toValue(options.previewId), toValue(options.enabled)] as const,
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
    reloadPdf: loadPdfBlob,
  }
}
