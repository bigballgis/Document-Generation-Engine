import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive, ref } from 'vue'
import { createTemplateAuthoringBindingsSaveFlow } from '@/composables/createTemplateAuthoringBindingsSaveFlow'
import {
  buildStructuredDraftStorageKey,
  readStructuredDraft,
  writeStructuredDraft,
} from '@/utils/structuredContentDraftStorage'
import type { UpsertBindingPayload } from '@/types/template'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'
import {
  isBindingVersionConflict,
  presentBindingVersionConflict,
} from '@/utils/bindingVersionConflict'
import { ElMessageBox } from 'element-plus'

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      confirm: vi.fn(),
    },
  }
})

describe('CE-U21 binding save concurrency', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.mocked(ElMessageBox.confirm).mockReset()
  })

  it('BDD-CE-U21-DAC-006 sends expectedUpdatedAt from binding token on update', async () => {
    const upsertBinding = vi.fn().mockResolvedValue({
      anchorId: 'A',
      declaredContentType: 'TEXT',
      structuredContentJson: '{"ok":true}',
      updatedAt: '2026-07-17T12:00:01.000Z',
    })
    const bindingForm = reactive<UpsertBindingPayload>({
      anchorId: 'A',
      declaredContentType: 'TEXT',
      structuredContentJson: '{"ok":true}',
    })
    const expectedUpdatedAt = ref<string | null>('2026-07-17T12:00:00.000Z')
    const structuredEditorRef = ref({ markPristine: vi.fn() })

    const { saveBindingDraft } = createTemplateAuthoringBindingsSaveFlow({
      props: {
        templateId: 'tpl-1',
        masterId: 'm1',
        variables: [],
        bindings: [],
        rules: null,
        contentModuleReferences: [],
      },
      structuredEditorRef,
      panelMode: ref('edit'),
      editingAnchorId: ref('A'),
      visibilityEnabled: ref(false),
      visibilityExpression: ref(''),
      editorDirty: ref(true),
      structureRevision: ref(1),
      previewSyncedRevision: ref(0),
      editSnapshot: ref(null),
      suppressStructureBump: ref(false),
      bindingForm,
      pendingPasteEvidence: ref(null),
      pendingClearPasteEvidence: ref(false),
      draftDevVersionId: ref('dev-1'),
      expectedUpdatedAt,
      sessionUsername: () => 'author-1',
      upsertBinding,
      saveRules: vi.fn().mockResolvedValue([]),
    })

    await saveBindingDraft()

    expect(upsertBinding).toHaveBeenCalledWith(
      'tpl-1',
      'A',
      expect.objectContaining({
        expectedUpdatedAt: '2026-07-17T12:00:00.000Z',
      }),
    )
    expect(expectedUpdatedAt.value).toBe('2026-07-17T12:00:01.000Z')
  })

  it('BDD-CE-U21-DAC-009 omits expectedUpdatedAt on first create', async () => {
    const upsertBinding = vi.fn().mockResolvedValue({
      anchorId: 'A',
      declaredContentType: 'TEXT',
      structuredContentJson: '{"ok":true}',
      updatedAt: '2026-07-17T12:00:00.000Z',
    })
    const bindingForm = reactive<UpsertBindingPayload>({
      anchorId: 'A',
      declaredContentType: 'TEXT',
      structuredContentJson: '{"ok":true}',
    })
    const expectedUpdatedAt = ref<string | null>(null)

    const { saveBindingDraft } = createTemplateAuthoringBindingsSaveFlow({
      props: {
        templateId: 'tpl-1',
        masterId: 'm1',
        variables: [],
        bindings: [],
        rules: null,
        contentModuleReferences: [],
      },
      structuredEditorRef: ref({ markPristine: vi.fn() }),
      panelMode: ref('edit'),
      editingAnchorId: ref('A'),
      visibilityEnabled: ref(false),
      visibilityExpression: ref(''),
      editorDirty: ref(true),
      structureRevision: ref(1),
      previewSyncedRevision: ref(0),
      editSnapshot: ref(null),
      suppressStructureBump: ref(false),
      bindingForm,
      pendingPasteEvidence: ref(null),
      pendingClearPasteEvidence: ref(false),
      draftDevVersionId: ref('dev-1'),
      expectedUpdatedAt,
      sessionUsername: () => 'author-1',
      upsertBinding,
      saveRules: vi.fn().mockResolvedValue([]),
    })

    await saveBindingDraft()

    const payload = upsertBinding.mock.calls[0]?.[2] as UpsertBindingPayload
    expect(payload.expectedUpdatedAt).toBeUndefined()
  })

  it('BDD-CE-U21-DAC-005 success clears per-anchor draft and leaves sibling drafts', async () => {
    const keyA = buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1', 'A')
    const keyB = buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1', 'B')
    writeStructuredDraft(localStorage, keyA, {
      schemaVersion: 1,
      structureJson: '{"A":true}',
      draftUpdatedAt: '2026-07-17T10:00:00.000Z',
      anchorId: 'A',
    })
    writeStructuredDraft(localStorage, keyB, {
      schemaVersion: 1,
      structureJson: '{"B":true}',
      draftUpdatedAt: '2026-07-17T10:00:00.000Z',
      anchorId: 'B',
    })

    const upsertBinding = vi.fn().mockResolvedValue({
      anchorId: 'A',
      declaredContentType: 'TEXT',
      structuredContentJson: '{"ok":true}',
      updatedAt: '2026-07-17T12:00:01.000Z',
    })
    const bindingForm = reactive<UpsertBindingPayload>({
      anchorId: 'A',
      declaredContentType: 'TEXT',
      structuredContentJson: '{"ok":true}',
    })

    const { saveBindingDraft } = createTemplateAuthoringBindingsSaveFlow({
      props: {
        templateId: 'tpl-1',
        masterId: 'm1',
        variables: [],
        bindings: [],
        rules: null,
        contentModuleReferences: [],
      },
      structuredEditorRef: ref({ markPristine: vi.fn() }),
      panelMode: ref('edit'),
      editingAnchorId: ref('A'),
      visibilityEnabled: ref(false),
      visibilityExpression: ref(''),
      editorDirty: ref(true),
      structureRevision: ref(1),
      previewSyncedRevision: ref(0),
      editSnapshot: ref(null),
      suppressStructureBump: ref(false),
      bindingForm,
      pendingPasteEvidence: ref(null),
      pendingClearPasteEvidence: ref(false),
      draftDevVersionId: ref('dev-1'),
      expectedUpdatedAt: ref('2026-07-17T12:00:00.000Z'),
      sessionUsername: () => 'author-1',
      upsertBinding,
      saveRules: vi.fn().mockResolvedValue([]),
    })

    await saveBindingDraft()

    expect(readStructuredDraft(localStorage, keyA)).toBeNull()
    expect(readStructuredDraft(localStorage, keyB)?.structureJson).toBe('{"B":true}')
  })

  it('detects BINDING_VERSION_CONFLICT envelope for conflict UX', () => {
    const error = axiosEnvelopeError(409, 'api.error.template.bindingVersionConflict', {
      code: 'BINDING_VERSION_CONFLICT',
      category: 'CONFLICT',
      retryable: true,
      message: 'This binding was updated elsewhere.',
    })
    expect(isBindingVersionConflict(error)).toBe(true)
    expect(isBindingVersionConflict(new Error('other'))).toBe(false)
  })

  it('BDD-CE-U21-DAC-007 presentBindingVersionConflict offers Reload / Keep editing', async () => {
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as never)
    const t = ((key: string) => key) as (key: string) => string

    await expect(presentBindingVersionConflict(t)).resolves.toBe('reload')
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      'api.error.template.bindingVersionConflict',
      'templates.authoring.bindingVersionConflict',
      expect.objectContaining({
        confirmButtonText: 'templates.authoring.bindingVersionConflictReload',
        cancelButtonText: 'templates.authoring.bindingVersionConflictKeepEditing',
      }),
    )

    vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')
    await expect(presentBindingVersionConflict(t)).resolves.toBe('keep')
  })
})
