import { nextTick, ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import type { VariableSchema } from '@/types/template'
import { useTemplateVariableTreePanel } from '@/components/templates/useTemplateVariableTreePanel'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}))

const upsertVariable = vi.fn()
const deleteVariable = vi.fn()
const fetchTemplate = vi.fn()
const selectedTemplate = ref({
  id: 'tpl-1',
  bindings: [] as Array<{
    anchorId: string
    declaredContentType: string
    structuredContentJson: string | null
  }>,
  rules: [] as Array<{ ruleId: string; conditionExpression: string; targetAnchorId: string }>,
})

vi.mock('@/stores/templates', () => ({
  useTemplatesStore: () => ({
    upsertVariable,
    deleteVariable,
    fetchTemplate,
    get selectedTemplate() {
      return selectedTemplate.value
    },
    set selectedTemplate(value: typeof selectedTemplate.value) {
      selectedTemplate.value = value
    },
    submitting: false,
  }),
}))

const confirmAction = vi.fn().mockResolvedValue(true)
vi.mock('@/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({
    confirmAction,
  }),
}))

const authorTemplates = ref(true)
vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => ({
    authorTemplates,
  }),
}))

const validateComputeExpression = vi.fn()
const evaluateComputeExpression = vi.fn()
const listTestDataSets = vi.fn()
const updateTestDataSet = vi.fn()
const upsertVariableApi = vi.fn()
const deleteVariableApi = vi.fn()
const upsertBindingApi = vi.fn()
const saveRulesApi = vi.fn()

vi.mock('@/api/templates', () => ({
  validateComputeExpression: (...args: unknown[]) => validateComputeExpression(...args),
  evaluateComputeExpression: (...args: unknown[]) => evaluateComputeExpression(...args),
  listTestDataSets: (...args: unknown[]) => listTestDataSets(...args),
  updateTestDataSet: (...args: unknown[]) => updateTestDataSet(...args),
  upsertVariable: (...args: unknown[]) => upsertVariableApi(...args),
  deleteVariable: (...args: unknown[]) => deleteVariableApi(...args),
  upsertBinding: (...args: unknown[]) => upsertBindingApi(...args),
  saveRules: (...args: unknown[]) => saveRulesApi(...args),
}))

function schema(partial: Partial<VariableSchema> & Pick<VariableSchema, 'variableKey'>): VariableSchema {
  return {
    variableType: 'TEXT',
    required: true,
    defaultValue: null,
    description: null,
    computeExpression: null,
    ...partial,
  }
}

describe('useTemplateVariableTreePanel compute paths', () => {
  const templateId = ref('tpl-1')
  const variables = ref<VariableSchema[]>([
    schema({ variableKey: 'principal', variableType: 'AMOUNT' }),
  ])
  const onUpdated = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    authorTemplates.value = true
    confirmAction.mockResolvedValue(true)
    validateComputeExpression.mockResolvedValue({ valid: true, message: null })
    evaluateComputeExpression.mockResolvedValue({
      success: true,
      result: '壹佰元整',
      variableKey: 'principalCn',
      expressionSummary: 'SPELL_AMOUNT',
    })
    listTestDataSets.mockResolvedValue([])
    selectedTemplate.value = { id: 'tpl-1', bindings: [], rules: [] }
  })

  it('sets client validation error for invalid COMPUTED expression', async () => {
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openAddVariable()
    panel.variableForm.variableKey = 'principalCn'
    panel.variableForm.variableType = 'COMPUTED'
    panel.variableForm.computeExpression = 'FOO(${principal})'
    await nextTick()
    expect(panel.computeValidationError.value).toBe(
      'templates.authoring.computeExpressionUnknownFunction',
    )
  })

  it('blocks save when client validation fails', async () => {
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openAddVariable()
    panel.variableForm.variableKey = 'principalCn'
    panel.variableForm.variableType = 'COMPUTED'
    panel.variableForm.computeExpression = 'FOO(${principal})'
    await panel.handleSaveVariable()
    expect(upsertVariable).not.toHaveBeenCalled()
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('validates on server then upserts COMPUTED variable', async () => {
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openAddVariable()
    panel.variableForm.variableKey = 'principalCn'
    panel.variableForm.variableType = 'COMPUTED'
    panel.variableForm.computeExpression = 'SPELL_AMOUNT(${principal})'
    await panel.handleSaveVariable()
    expect(validateComputeExpression).toHaveBeenCalledWith('tpl-1', expect.objectContaining({
      expression: 'SPELL_AMOUNT(${principal})',
    }))
    expect(upsertVariable).toHaveBeenCalled()
    expect(onUpdated).toHaveBeenCalled()
  })

  it('blocks save when server validation fails', async () => {
    validateComputeExpression.mockResolvedValue({ valid: false, message: 'bad expr' })
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openAddVariable()
    panel.variableForm.variableKey = 'principalCn'
    panel.variableForm.variableType = 'COMPUTED'
    panel.variableForm.computeExpression = 'SPELL_AMOUNT(${principal})'
    await panel.handleSaveVariable()
    expect(upsertVariable).not.toHaveBeenCalled()
    expect(panel.computeValidationError.value).toBe('bad expr')
  })

  it('blocks save when server validation throws', async () => {
    validateComputeExpression.mockRejectedValue(new Error('network'))
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openAddVariable()
    panel.variableForm.variableKey = 'principalCn'
    panel.variableForm.variableType = 'COMPUTED'
    panel.variableForm.computeExpression = 'SPELL_AMOUNT(${principal})'
    await panel.handleSaveVariable()
    expect(upsertVariable).not.toHaveBeenCalled()
    expect(ElMessage.error).toHaveBeenCalledWith('templates.error.saveVariable')
  })

  it('clears compute validation when type is not COMPUTED', async () => {
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openAddVariable()
    panel.variableForm.variableType = 'COMPUTED'
    panel.variableForm.computeExpression = 'FOO(${principal})'
    await nextTick()
    expect(panel.computeValidationError.value).not.toBeNull()
    panel.variableForm.variableType = 'TEXT'
    await nextTick()
    expect(panel.computeValidationError.value).toBeNull()
  })

  it('evaluates sample JSON and surfaces result', async () => {
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openEditVariable(
      schema({
        variableKey: 'principalCn',
        variableType: 'COMPUTED',
        computeExpression: 'SPELL_AMOUNT(${principal})',
      }),
    )
    panel.sampleJson.value = '{"principal":100}'
    await panel.handleSampleEvaluate()
    expect(evaluateComputeExpression).toHaveBeenCalled()
    expect(panel.sampleResult.value).toBe('壹佰元整')
    expect(panel.sampleError.value).toBeNull()
  })

  it('surfaces fieldErrors from sample evaluate failure', async () => {
    evaluateComputeExpression.mockRejectedValue({
      response: {
        data: {
          error: {
            message: 'fallback',
            fieldErrors: [{ message: 'compute failed' }],
          },
        },
      },
    })
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.sampleJson.value = '{"principal":100}'
    panel.variableForm.computeExpression = 'SPELL_AMOUNT(${principal})'
    await panel.handleSampleEvaluate()
    expect(panel.sampleError.value).toBe('compute failed')
  })

  it('rejects invalid sample JSON', async () => {
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.sampleJson.value = '{bad'
    await panel.handleSampleEvaluate()
    expect(panel.sampleError.value).toBe('templates.authoring.computeSampleJsonInvalid')
    expect(evaluateComputeExpression).not.toHaveBeenCalled()
  })

  it('includes piiCategory when saving a variable (CE-G03)', async () => {
    upsertVariable.mockResolvedValue(undefined)
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openAddVariable()
    panel.variableForm.variableKey = 'customerName'
    panel.variableForm.variableType = 'TEXT'
    panel.variableForm.piiCategory = 'PERSONAL_NAME'
    await panel.handleSaveVariable()
    expect(upsertVariable).toHaveBeenCalledWith(
      'tpl-1',
      'customerName',
      expect.objectContaining({
        variableKey: 'customerName',
        piiCategory: 'PERSONAL_NAME',
      }),
    )
  })
})

describe('useTemplateVariableTreePanel rename cascade', () => {
  const templateId = ref('tpl-1')
  const onUpdated = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    authorTemplates.value = true
    confirmAction.mockResolvedValue(true)
    listTestDataSets.mockResolvedValue([])
    upsertVariableApi.mockResolvedValue(undefined)
    deleteVariableApi.mockResolvedValue(undefined)
    upsertBindingApi.mockResolvedValue(undefined)
    saveRulesApi.mockResolvedValue(undefined)
    updateTestDataSet.mockResolvedValue(undefined)
    fetchTemplate.mockResolvedValue(undefined)
    selectedTemplate.value = {
      id: 'tpl-1',
      bindings: [
        {
          anchorId: 'a1',
          declaredContentType: 'RICH_TEXT',
          structuredContentJson: JSON.stringify({
            schemaVersion: '1.0',
            nodes: [{ type: 'variable', key: 'customer' }],
          }),
        },
      ],
      rules: [{ ruleId: 'r1', conditionExpression: '${customer} == true', targetAnchorId: 'a1' }],
    }
  })

  it('allows editing variableKey when opening edit dialog', () => {
    const variables = ref([schema({ variableKey: 'customer' })])
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openEditVariable(schema({ variableKey: 'customer' }))
    expect(panel.editingVariableKey.value).toBe('customer')
    expect(panel.variableForm.variableKey).toBe('customer')
    panel.variableForm.variableKey = 'borrowerLegalName'
    expect(panel.variableForm.variableKey).toBe('borrowerLegalName')
  })

  it('rejects conflicting rename keys without mutating', async () => {
    const variables = ref([
      schema({ variableKey: 'customer' }),
      schema({ variableKey: 'otherKey' }),
    ])
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openEditVariable(schema({ variableKey: 'customer' }))
    panel.variableForm.variableKey = 'otherKey'
    await panel.handleSaveVariable()
    expect(confirmAction).not.toHaveBeenCalled()
    expect(upsertVariableApi).not.toHaveBeenCalled()
    expect(ElMessage.error).toHaveBeenCalledWith('templates.authoring.rename.variableKeyConflict')
  })

  it('rejects blank rename keys', async () => {
    const variables = ref([schema({ variableKey: 'customer' })])
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openEditVariable(schema({ variableKey: 'customer' }))
    panel.variableForm.variableKey = '   '
    await panel.handleSaveVariable()
    expect(upsertVariableApi).not.toHaveBeenCalled()
    expect(ElMessage.error).toHaveBeenCalledWith('templates.authoring.rename.variableKeyRequired')
  })

  it('cancels rename when confirm is declined', async () => {
    confirmAction.mockResolvedValue(false)
    const variables = ref([schema({ variableKey: 'customer' })])
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openEditVariable(schema({ variableKey: 'customer' }))
    panel.variableForm.variableKey = 'party'
    await panel.handleSaveVariable()
    expect(confirmAction).toHaveBeenCalled()
    expect(upsertVariableApi).not.toHaveBeenCalled()
    expect(deleteVariableApi).not.toHaveBeenCalled()
  })

  it('cascades rename across bindings, rules, unlocked sets, and compute refs', async () => {
    listTestDataSets.mockResolvedValue([
      {
        testDataSetId: 'u1',
        locked: false,
        name: 'unlocked',
        description: null,
        variables: { customer: 'Acme' },
        required: false,
        scenarioName: null,
        coverageTags: [],
      },
      {
        testDataSetId: 'l1',
        locked: true,
        name: 'locked',
        description: null,
        variables: { customer: 'Frozen' },
        required: false,
        scenarioName: null,
        coverageTags: [],
      },
    ])
    const variables = ref([
      schema({ variableKey: 'customer' }),
      schema({
        variableKey: 'label',
        variableType: 'COMPUTED',
        computeExpression: '${customer}',
      }),
    ])
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openEditVariable(schema({ variableKey: 'customer' }))
    panel.variableForm.variableKey = 'party'
    await panel.handleSaveVariable()

    expect(confirmAction).toHaveBeenCalledWith(
      expect.objectContaining({
        titleKey: 'templates.authoring.rename.confirmTitle',
        messageParams: expect.objectContaining({
          oldKey: 'customer',
          newKey: 'party',
          bindingCount: 1,
          ruleCount: 1,
          unlockedCount: 1,
          lockedCount: 1,
          computeCount: 1,
        }),
      }),
    )
    expect(upsertVariableApi).toHaveBeenCalledWith(
      'tpl-1',
      'party',
      expect.objectContaining({ variableKey: 'party' }),
    )
    expect(upsertBindingApi).toHaveBeenCalled()
    expect(saveRulesApi).toHaveBeenCalled()
    expect(updateTestDataSet).toHaveBeenCalledWith(
      'tpl-1',
      'u1',
      expect.objectContaining({ variables: { party: 'Acme' } }),
    )
    expect(deleteVariableApi).toHaveBeenCalledWith('tpl-1', 'customer')
    expect(ElMessage.success).toHaveBeenCalledWith('templates.authoring.renameVariableSuccess')
    expect(ElMessage.warning).toHaveBeenCalledWith('templates.authoring.renameVariableLockedWarning')
    expect(onUpdated).toHaveBeenCalled()
  })

  it('renames with zero references after confirm', async () => {
    selectedTemplate.value = { id: 'tpl-1', bindings: [], rules: [] }
    const variables = ref([schema({ variableKey: 'lonelyKey' })])
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    panel.openEditVariable(schema({ variableKey: 'lonelyKey' }))
    panel.variableForm.variableKey = 'soloKey'
    await panel.handleSaveVariable()
    expect(upsertVariableApi).toHaveBeenCalledWith('tpl-1', 'soloKey', expect.any(Object))
    expect(deleteVariableApi).toHaveBeenCalledWith('tpl-1', 'lonelyKey')
  })

  it('hides write path when authorTemplates is false', async () => {
    authorTemplates.value = false
    const variables = ref([schema({ variableKey: 'customer' })])
    const panel = useTemplateVariableTreePanel({ templateId, variables, onUpdated })
    expect(panel.canWriteVariables.value).toBe(false)
    panel.openAddVariable()
    expect(panel.variableDialogOpen.value).toBe(false)
    panel.openEditVariable(schema({ variableKey: 'customer' }))
    panel.variableForm.variableKey = 'party'
    await panel.handleSaveVariable()
    expect(upsertVariableApi).not.toHaveBeenCalled()
  })
})
