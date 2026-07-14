import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  evaluateComputeExpression,
  validateComputeExpression,
} from '@/api/templatesBindings'

const post = vi.fn()

vi.mock('@/api/http', () => ({
  http: {
    post: (...args: unknown[]) => post(...args),
  },
}))

describe('templatesBindings compute APIs', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('posts validate payload and unwraps envelope', async () => {
    post.mockResolvedValue({
      data: {
        metadata: {},
        result: { valid: true, message: null },
        error: null,
      },
    })
    const result = await validateComputeExpression('tpl-1', {
      expression: 'SPELL_AMOUNT(${principal})',
      knownVariableKeys: ['principal'],
    })
    expect(post).toHaveBeenCalledWith(
      '/templates/tpl-1/compute-expressions/validate',
      expect.objectContaining({ expression: 'SPELL_AMOUNT(${principal})' }),
    )
    expect(result.valid).toBe(true)
  })

  it('posts evaluate payload and unwraps envelope', async () => {
    post.mockResolvedValue({
      data: {
        metadata: {},
        result: {
          success: true,
          result: '壹佰元整',
          variableKey: 'principalCn',
          expressionSummary: 'SPELL_AMOUNT',
        },
        error: null,
      },
    })
    const result = await evaluateComputeExpression('tpl-1', {
      expression: 'SPELL_AMOUNT(${principal})',
      sampleVariables: { principal: 100 },
      locale: 'zh-CN',
    })
    expect(post).toHaveBeenCalledWith(
      '/templates/tpl-1/compute-expressions/evaluate',
      expect.objectContaining({ locale: 'zh-CN' }),
    )
    expect(result.result).toBe('壹佰元整')
  })
})
