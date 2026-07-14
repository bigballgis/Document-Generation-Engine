import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useClauseOutdatedBumpActions } from '@/components/templates/useClauseOutdatedBumpActions'

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
  },
  ElMessageBox: {
    confirm: vi.fn(),
  },
}))

const upsertContentModuleReference = vi.fn()

vi.mock('@/stores/templatePanelData', () => ({
  useTemplatePanelDataStore: () => ({
    upsertContentModuleReference,
  }),
}))

describe('useClauseOutdatedBumpActions', () => {
  const t = (key: string, params?: Record<string, unknown>) =>
    params ? `${key}:${JSON.stringify(params)}` : key
  const te = () => false

  const references = ref([
    {
      referenceKey: 'CLAUSE_A',
      moduleId: 'MOD-A',
      semanticVersion: '1.0.0',
      locked: false,
      outOfDate: true,
      latestApprovedSemanticVersion: '1.1.0',
    },
    {
      referenceKey: 'CLAUSE_B',
      moduleId: 'MOD-B',
      semanticVersion: '2.0.0',
      locked: true,
      outOfDate: true,
      latestApprovedSemanticVersion: '2.1.0',
    },
  ])
  const bumping = ref(false)

  beforeEach(() => {
    vi.clearAllMocks()
    upsertContentModuleReference.mockResolvedValue({})
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as never)
  })

  it('detects outdated unlocked references', () => {
    const actions = useClauseOutdatedBumpActions({
      t,
      te,
      templateId: 'tpl-1',
      editable: ref(true),
      references,
      bumping,
      emitUpdated: vi.fn(),
    })

    expect(actions.hasOutdatedUnlockedReferences.value).toBe(true)
    expect(actions.outdatedUnlockedReferences.value).toHaveLength(1)
  })

  it('bumps a single reference via upsertReference', async () => {
    const emitUpdated = vi.fn()
    const actions = useClauseOutdatedBumpActions({
      t,
      te,
      templateId: 'tpl-1',
      editable: ref(true),
      references,
      bumping,
      emitUpdated,
    })

    await actions.bumpReference(references.value[0])

    expect(upsertContentModuleReference).toHaveBeenCalledWith('tpl-1', 'CLAUSE_A', {
      referenceKey: 'CLAUSE_A',
      moduleId: 'MOD-A',
      semanticVersion: '1.1.0',
    })
    expect(emitUpdated).toHaveBeenCalled()
    expect(ElMessage.success).toHaveBeenCalled()
  })

  it('bumps all outdated references after confirmation', async () => {
    const emitUpdated = vi.fn()
    const actions = useClauseOutdatedBumpActions({
      t,
      te,
      templateId: 'tpl-1',
      editable: ref(true),
      references,
      bumping,
      emitUpdated,
    })

    await actions.bumpAllOutdatedReferences()

    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(upsertContentModuleReference).toHaveBeenCalledTimes(1)
    expect(emitUpdated).toHaveBeenCalled()
    expect(ElMessage.success).toHaveBeenCalled()
  })

  it('skips bump when user cancels confirmation', async () => {
    vi.mocked(ElMessageBox.confirm).mockRejectedValue(new Error('cancel'))

    const actions = useClauseOutdatedBumpActions({
      t,
      te,
      templateId: 'tpl-1',
      editable: ref(true),
      references,
      bumping,
      emitUpdated: vi.fn(),
    })

    await actions.bumpAllOutdatedReferences()

    expect(upsertContentModuleReference).not.toHaveBeenCalled()
  })

  it('skips single bump for locked or non-outdated references', async () => {
    const actions = useClauseOutdatedBumpActions({
      t,
      te,
      templateId: 'tpl-1',
      editable: ref(true),
      references,
      bumping,
      emitUpdated: vi.fn(),
    })

    await actions.bumpReference(references.value[1])
    expect(upsertContentModuleReference).not.toHaveBeenCalled()

    await actions.bumpReference({
      ...references.value[0],
      outOfDate: false,
      latestApprovedSemanticVersion: undefined,
    })
    expect(upsertContentModuleReference).not.toHaveBeenCalled()
  })

  it('skips bump-all when not editable', async () => {
    const actions = useClauseOutdatedBumpActions({
      t,
      te,
      templateId: 'tpl-1',
      editable: ref(false),
      references,
      bumping,
      emitUpdated: vi.fn(),
    })

    await actions.bumpAllOutdatedReferences()

    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(upsertContentModuleReference).not.toHaveBeenCalled()
  })

  it('shows error message when single bump upsert fails', async () => {
    upsertContentModuleReference.mockRejectedValue(new Error('save failed'))

    const actions = useClauseOutdatedBumpActions({
      t,
      te,
      templateId: 'tpl-1',
      editable: ref(true),
      references,
      bumping,
      emitUpdated: vi.fn(),
    })

    await actions.bumpReference(references.value[0])

    expect(ElMessage.error).toHaveBeenCalled()
    expect(bumping.value).toBe(false)
  })
})
