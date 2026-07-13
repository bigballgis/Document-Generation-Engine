import { ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useTemplatesStore } from '@/stores/templates'
import type { PasteCleaningEvidence, PasteCleaningSummary } from '@/types/template'
import { buildAcceptedPasteCleaningEvidence } from '@/utils/pasteCleaningEvidence'
import {
  DEFAULT_STRUCTURED_CONTENT_JSON,
  parseStructuredContent,
  serializeStructuredContent,
  type StructuredContentDocument,
} from '@/utils/structuredContentNodes'

export interface StructuredContentPasteFlowApi {
  pasteSummaryOpen: Ref<boolean>
  pasteSummary: Ref<PasteCleaningSummary | null>
  pasteBlocked: Ref<boolean>
  handlePasteFile: (event: Event) => Promise<void>
  acceptPaste: () => void
  cancelPaste: () => void
}

export function useStructuredContentPasteFlow(options: {
  templateId: () => string | undefined
  isReadonly: () => boolean
  documentModel: Ref<StructuredContentDocument>
  setPendingCoalesceKey: (value: string | null) => void
  emitPasteAccepted: (evidence: PasteCleaningEvidence) => void
}): StructuredContentPasteFlowApi {
  const { t } = useI18n()
  const templatesStore = useTemplatesStore()

  const pasteSummaryOpen = ref(false)
  const pasteSummary = ref<PasteCleaningSummary | null>(null)
  const pasteBlocked = ref(false)
  const pendingPasteJson = ref<string | null>(null)
  const prePasteSnapshot = ref(DEFAULT_STRUCTURED_CONTENT_JSON)

  async function handlePasteFile(event: Event) {
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]
    if (!file) {
      return
    }
    const html = await file.text()
    await runPasteClean(html)
    input.value = ''
  }

  async function runPasteClean(html: string) {
    const templateId = options.templateId()
    if (!html.trim() || options.isReadonly() || !templateId) {
      return
    }
    prePasteSnapshot.value = serializeStructuredContent(options.documentModel.value)
    try {
      const result = await templatesStore.pasteClean(templateId, {
        sourceHtml: html,
        prePasteStructuredContentJson: prePasteSnapshot.value,
      })
      pasteSummary.value = result.summary
      pasteBlocked.value = result.blocked
      pendingPasteJson.value = result.cleanedStructuredContentJson
      prePasteSnapshot.value = result.prePasteSnapshotJson
      pasteSummaryOpen.value = true
    } catch {
      ElMessage.error(t('templates.structuredEditor.error.pasteClean'))
    }
  }

  function acceptPaste() {
    if (pasteBlocked.value || !pasteSummary.value) {
      return
    }
    if (pendingPasteJson.value) {
      options.setPendingCoalesceKey(null)
      options.documentModel.value = parseStructuredContent(pendingPasteJson.value)
    }
    options.emitPasteAccepted(buildAcceptedPasteCleaningEvidence(pasteSummary.value))
    pendingPasteJson.value = null
    pasteSummary.value = null
    pasteBlocked.value = false
  }

  function cancelPaste() {
    options.setPendingCoalesceKey(null)
    options.documentModel.value = parseStructuredContent(prePasteSnapshot.value)
    pendingPasteJson.value = null
    pasteSummary.value = null
    pasteBlocked.value = false
  }

  return {
    pasteSummaryOpen,
    pasteSummary,
    pasteBlocked,
    handlePasteFile,
    acceptPaste,
    cancelPaste,
  }
}
