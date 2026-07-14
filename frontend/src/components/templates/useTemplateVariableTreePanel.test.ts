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
  },
}))

const upsertVariable = vi.fn()
const deleteVariable = vi.fn()
vi.mock('@/stores/templates', () => ({
  useTemplatesStore: () => ({
    upsertVariable,
    deleteVariable,
  }),
}))

vi.mock('@/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({
    confirmAction: vi.fn().mockResolvedValue(true),
  }),
}))

const validateComputeExpression = vi.fn()
const evaluateComputeExpression = vi.fn()
vi.mock('@/api/templatesBindings', () => ({
  validateComputeExpression: (...args: unknown[]) => validateComputeExpression(...args),
  evaluateComputeExpression: (...args: unknown[]) => evaluateComputeExpression(...args),
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
    validateComputeExpression.mockResolvedValue({ valid: true, message: null })
    evaluateComputeExpression.mockResolvedValue({
      success: true,
      result: '壹佰元整',
      variableKey: 'principalCn',
      expressionSummary: 'SPELL_AMOUNT',
    })
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
})
