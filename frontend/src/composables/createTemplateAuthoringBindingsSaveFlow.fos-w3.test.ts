import { describe, expect, it, vi } from 'vitest'
import { nextTick, reactive, ref } from 'vue'
import { createTemplateAuthoringBindingsSaveFlow } from '@/composables/createTemplateAuthoringBindingsSaveFlow'
import type { StructuredBindingEditorExpose } from '@/composables/templateAuthoringBindingsTypes'
import { DEFAULT_STRUCTURED_CONTENT_JSON } from '@/utils/structuredContentNodes'

describe('createTemplateAuthoringBindingsSaveFlow FOS-W3-7', () => {
  it('invokes validateStructure and aborts save when invalid', async () => {
    const validateStructure = vi.fn().mockReturnValue([
      { messageKey: 'templates.structuredEditor.validation.unresolvedVariable', location: '0', blockPath: [0] },
    ])
    const structuredEditorRef = ref<StructuredBindingEditorExpose | null>({
      markPristine: vi.fn(),
      validateStructure,
    })
    const upsertBinding = vi.fn()
    const bindingForm = reactive({
      anchorId: 'A1',
      declaredContentType: 'TEXT' as const,
      structuredContentJson: DEFAULT_STRUCTURED_CONTENT_JSON,
    })
    const { saveBindingDraft } = createTemplateAuthoringBindingsSaveFlow({
      props: {
        templateId: 'tpl-1',
        masterId: 'm-1',
        variables: [],
        bindings: [],
        rules: null,
        contentModuleReferences: [],
      },
      structuredEditorRef,
      panelMode: ref('edit'),
      editingAnchorId: ref('A1'),
      visibilityEnabled: ref(false),
      visibilityExpression: ref(''),
      editorDirty: ref(true),
      structureRevision: ref(0),
      previewSyncedRevision: ref(0),
      editSnapshot: ref(null),
      suppressStructureBump: ref(false),
      bindingForm,
      pendingPasteEvidence: ref(null),
      pendingClearPasteEvidence: ref(false),
      draftDevVersionId: ref('dev-1'),
      expectedUpdatedAt: ref(null),
      sessionUsername: () => 'tester',
      upsertBinding,
      saveRules: vi.fn(),
    })

    await expect(saveBindingDraft()).rejects.toThrow('STRUCTURED_CONTENT_VALIDATION_FAILED')
    await nextTick()
    expect(validateStructure).toHaveBeenCalledOnce()
    expect(upsertBinding).not.toHaveBeenCalled()
  })
})
