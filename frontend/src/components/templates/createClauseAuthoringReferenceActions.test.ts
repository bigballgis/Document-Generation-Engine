import { describe, expect, it, vi, beforeEach } from 'vitest'
import { reactive, ref } from 'vue'
import { createClauseAuthoringReferenceActions } from '@/components/templates/createClauseAuthoringReferenceActions'
import type { ContentModuleSummary } from '@/types/contentModule'
import type { TemplateContentModuleReference } from '@/types/template'

vi.mock('@/api/contentModules', () => ({
  listAllContentModules: vi.fn(),
  getContentModule: vi.fn().mockResolvedValue({ versions: [] }),
}))

vi.mock('element-plus', () => ({
  ElMessage: { warning: vi.fn(), success: vi.fn(), error: vi.fn() },
}))

function buildActions(existingKeys: string[] = []) {
  const form = reactive({
    referenceKey: '',
    moduleId: '',
    semanticVersion: '',
  })
  const moduleOptions = ref<ContentModuleSummary[]>([
    {
      moduleId: 'mod-1',
      moduleCode: 'loan-disclosure',
      groupCode: 'RETAIL',
      name: 'Loan disclosure',
      reviewState: 'APPROVED',
      createdAt: '2026-07-01T00:00:00Z',
      updatedAt: '2026-07-01T00:00:00Z',
    },
    {
      moduleId: 'mod-empty',
      moduleCode: '***',
      groupCode: 'RETAIL',
      name: 'Empty code',
      reviewState: 'APPROVED',
      createdAt: '2026-07-01T00:00:00Z',
      updatedAt: '2026-07-01T00:00:00Z',
    },
  ])
  const versionOptions = ref([])
  const editingReferenceKey = ref<string | null>(null)
  const referenceDialogOpen = ref(false)
  const saving = ref(false)
  const references = ref<TemplateContentModuleReference[]>(
    existingKeys.map((referenceKey) => ({
      referenceKey,
      moduleId: 'mod-other',
      semanticVersion: '1.0.0',
      locked: false,
      outOfDate: false,
    })),
  )

  const actions = createClauseAuthoringReferenceActions({
    t: (key) => key,
    te: () => true,
    props: { templateId: 'tpl-1', groupCode: 'RETAIL', editable: true },
    emit: vi.fn(),
    panelDataStore: {
      fetchContentModuleReferences: vi.fn(),
      upsertContentModuleReference: vi.fn(),
    } as never,
    sessionStore: { session: { roles: ['DOCUMENT_AUTHOR'] } } as never,
    form,
    moduleOptions,
    versionOptions,
    editingReferenceKey,
    referenceDialogOpen,
    saving,
    references,
  })

  return { form, editingReferenceKey, actions, references }
}

describe('createClauseAuthoringReferenceActions auto referenceKey', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('BDD-BEI-012 auto-fills UPPER_SNAKE key on module select (create)', async () => {
    const { form, actions } = buildActions()

    await actions.handleModuleChange('mod-1')

    expect(form.moduleId).toBe('mod-1')
    expect(form.referenceKey).toBe('LOAN_DISCLOSURE')
    expect(actions.referenceKeyUserOverridden.value).toBe(false)
  })

  it('BDD-BEI-013 suggests _2 when base key exists', async () => {
    const { form, actions } = buildActions(['LOAN_DISCLOSURE'])

    await actions.handleModuleChange('mod-1')

    expect(form.referenceKey).toBe('LOAN_DISCLOSURE_2')
  })

  it('BDD-BEI-014 suggests _3 when base and _2 exist', async () => {
    const { form, actions } = buildActions(['LOAN_DISCLOSURE', 'LOAN_DISCLOSURE_2'])

    await actions.handleModuleChange('mod-1')

    expect(form.referenceKey).toBe('LOAN_DISCLOSURE_3')
  })

  it('BDD-BEI-016 does not clobber user-overridden key on module change', async () => {
    const { form, actions } = buildActions()

    await actions.handleModuleChange('mod-1')
    form.referenceKey = 'MY_CUSTOM_REF'
    actions.markReferenceKeyOverridden()

    await actions.handleModuleChange('mod-empty')

    expect(form.referenceKey).toBe('MY_CUSTOM_REF')
    expect(actions.referenceKeyUserOverridden.value).toBe(true)
  })

  it('BDD-BEI-016 reset restores auto-suggest on next module select', async () => {
    const { form, actions } = buildActions()

    await actions.handleModuleChange('mod-1')
    form.referenceKey = 'MY_CUSTOM_REF'
    actions.markReferenceKeyOverridden()
    actions.clearReferenceKeyOverride()

    expect(actions.referenceKeyUserOverridden.value).toBe(false)
    expect(form.referenceKey).toBe('LOAN_DISCLOSURE')

    await actions.handleModuleChange('mod-1')
    expect(form.referenceKey).toBe('LOAN_DISCLOSURE')
  })

  it('BDD-BEI-018 leaves key empty when moduleCode normalizes empty', async () => {
    const { form, actions } = buildActions()

    await actions.handleModuleChange('mod-empty')

    expect(form.referenceKey).toBe('')
  })

  it('BDD-BEI-017 edit path does not auto-replace locked key', async () => {
    const { form, editingReferenceKey, actions } = buildActions()
    editingReferenceKey.value = 'EXISTING_KEY'
    form.referenceKey = 'EXISTING_KEY'

    await actions.handleModuleChange('mod-1')

    expect(form.referenceKey).toBe('EXISTING_KEY')
  })
})
