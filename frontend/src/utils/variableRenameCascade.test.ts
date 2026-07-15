import { describe, expect, it, vi } from 'vitest'
import type {
  AnchorBinding,
  CompositionRule,
  TestDataSet,
  VariableSchema,
} from '@/types/template'
import {
  analyzeVariableRenameImpact,
  buildVariableRenameTransforms,
  executeVariableRenameCascade,
  renameInStructuredContentJson,
  renameTestSetVariableKeys,
  replaceVariableRefsInExpression,
  validateRenameVariableKey,
} from '@/utils/variableRenameCascade'

function binding(partial: Partial<AnchorBinding> & Pick<AnchorBinding, 'anchorId'>): AnchorBinding {
  return {
    declaredContentType: 'RICH_TEXT',
    structuredContentJson: null,
    ...partial,
  }
}

function rule(
  partial: Partial<CompositionRule> & Pick<CompositionRule, 'ruleId' | 'conditionExpression'>,
): CompositionRule {
  return {
    targetAnchorId: 'body',
    ...partial,
  }
}

function variable(
  partial: Partial<VariableSchema> & Pick<VariableSchema, 'variableKey'>,
): VariableSchema {
  return {
    variableType: 'TEXT',
    required: true,
    defaultValue: null,
    description: null,
    computeExpression: null,
    ...partial,
  }
}

function dataSet(
  partial: Partial<TestDataSet> & Pick<TestDataSet, 'testDataSetId' | 'locked' | 'variables'>,
): TestDataSet {
  return {
    templateId: 'tpl-1',
    name: 'set',
    description: null,
    required: false,
    scenarioName: null,
    coverageTags: [],
    datasetVersion: 1,
    derivedFromId: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...partial,
  }
}

describe('replaceVariableRefsInExpression', () => {
  it('replaces ${oldKey} and dotted ${oldKey.path} without substring hits', () => {
    expect(
      replaceVariableRefsInExpression(
        '${customer} == true && ${customerName} != null && ${customer.id}',
        'customer',
        'party',
      ),
    ).toBe('${party} == true && ${customerName} != null && ${party.id}')
  })

  it('replaces bare identifiers with whole-token boundaries only', () => {
    expect(replaceVariableRefsInExpression('customer && customerName', 'customer', 'party')).toBe(
      'party && customerName',
    )
  })
})

describe('validateRenameVariableKey', () => {
  it('rejects blank and whitespace-only keys', () => {
    expect(validateRenameVariableKey('   ', 'old', ['old']).messageKey).toBe(
      'templates.authoring.rename.variableKeyRequired',
    )
  })

  it('rejects invalid morphology', () => {
    expect(validateRenameVariableKey('1bad', 'old', ['old']).messageKey).toBe(
      'templates.authoring.rename.variableKeyInvalid',
    )
  })

  it('rejects conflict with another existing key', () => {
    expect(validateRenameVariableKey('otherKey', 'oldKey', ['oldKey', 'otherKey']).messageKey).toBe(
      'templates.authoring.rename.variableKeyConflict',
    )
  })

  it('allows same key (metadata-only save)', () => {
    expect(validateRenameVariableKey('oldKey', 'oldKey', ['oldKey']).valid).toBe(true)
  })
})

describe('structured content + test-set rename', () => {
  it('renames variable key / loopVariable fields and conditionExpression refs', () => {
    const json = JSON.stringify({
      schemaVersion: '1.0',
      nodes: [
        {
          type: 'conditionBlock',
          conditionExpression: '${customer} == true',
          children: [
            { type: 'variable', key: 'customer' },
            { type: 'loopBlock', loopVariable: 'customer', children: [] },
            { type: 'variable', key: 'customerName' },
          ],
        },
      ],
    })
    const { changed, json: next } = renameInStructuredContentJson(json, 'customer', 'party')
    expect(changed).toBe(true)
    const parsed = JSON.parse(next) as {
      nodes: Array<{
        conditionExpression: string
        children: Array<{ key?: string; loopVariable?: string }>
      }>
    }
    expect(parsed.nodes[0].conditionExpression).toBe('${party} == true')
    expect(parsed.nodes[0].children[0].key).toBe('party')
    expect(parsed.nodes[0].children[1].loopVariable).toBe('party')
    expect(parsed.nodes[0].children[2].key).toBe('customerName')
  })

  it('renames unlocked test-set variable object keys and preserves values', () => {
    const { changed, variables } = renameTestSetVariableKeys(
      { customer: 'Acme', customerName: 'keep' },
      'customer',
      'party',
    )
    expect(changed).toBe(true)
    expect(variables).toEqual({ party: 'Acme', customerName: 'keep' })
  })
})

describe('analyze + build transforms', () => {
  const bindings = [
    binding({
      anchorId: 'a1',
      structuredContentJson: JSON.stringify({
        schemaVersion: '1.0',
        nodes: [{ type: 'variable', key: 'customer' }],
      }),
    }),
    binding({
      anchorId: 'a2',
      structuredContentJson: JSON.stringify({
        schemaVersion: '1.0',
        nodes: [{ type: 'variable', key: 'other' }],
      }),
    }),
  ]
  const rules = [
    rule({ ruleId: 'r1', conditionExpression: '${customer} == true' }),
    rule({ ruleId: 'r2', conditionExpression: '${other} == true' }),
  ]
  const variables = [
    variable({ variableKey: 'customer' }),
    variable({
      variableKey: 'label',
      variableType: 'COMPUTED',
      computeExpression: 'COALESCE(${customer}, "")',
    }),
    variable({ variableKey: 'customerName' }),
  ]
  const testDataSets = [
    dataSet({
      testDataSetId: 'u1',
      locked: false,
      variables: { customer: 1 },
    }),
    dataSet({
      testDataSetId: 'l1',
      locked: true,
      variables: { customer: 2 },
    }),
  ]

  it('summarizes impact including locked skips', () => {
    expect(
      analyzeVariableRenameImpact('customer', bindings, rules, variables, testDataSets),
    ).toEqual({
      bindingAnchorCount: 1,
      ruleCount: 1,
      unlockedTestSetCount: 1,
      lockedTestSetSkippedCount: 1,
      computeReferenceCount: 1,
    })
  })

  it('builds transforms that skip locked sets and leave substring keys alone', () => {
    const transforms = buildVariableRenameTransforms(
      'customer',
      'party',
      bindings,
      rules,
      variables,
      testDataSets,
    )
    expect(transforms.bindings).toHaveLength(1)
    expect(transforms.bindings[0].anchorId).toBe('a1')
    expect(transforms.rulesChanged).toBe(true)
    expect(transforms.rules.find((item) => item.ruleId === 'r1')?.conditionExpression).toBe(
      '${party} == true',
    )
    expect(transforms.computeUpdates).toHaveLength(1)
    expect(transforms.computeUpdates[0].payload.computeExpression).toBe('COALESCE(${party}, "")')
    expect(transforms.unlockedTestSetUpdates).toHaveLength(1)
    expect(transforms.unlockedTestSetUpdates[0].payload.variables).toEqual({ party: 1 })
    expect(transforms.lockedSkippedCount).toBe(1)
  })
})

describe('executeVariableRenameCascade', () => {
  it('upserts new key, cascades refs, deletes old key, refreshes once', async () => {
    const upsertVariable = vi.fn().mockResolvedValue(undefined)
    const deleteVariable = vi.fn().mockResolvedValue(undefined)
    const upsertBinding = vi.fn().mockResolvedValue(undefined)
    const saveRules = vi.fn().mockResolvedValue(undefined)
    const updateTestDataSet = vi.fn().mockResolvedValue(undefined)
    const refreshTemplate = vi.fn().mockResolvedValue(undefined)

    const result = await executeVariableRenameCascade({
      templateId: 'tpl-1',
      oldKey: 'customer',
      newKey: 'party',
      variablePayload: {
        variableKey: 'party',
        variableType: 'TEXT',
        required: true,
        defaultValue: null,
        description: null,
        computeExpression: null,
      },
      bindings: [
        binding({
          anchorId: 'a1',
          structuredContentJson: JSON.stringify({
            schemaVersion: '1.0',
            nodes: [{ type: 'variable', key: 'customer' }],
          }),
        }),
      ],
      rules: [rule({ ruleId: 'r1', conditionExpression: '${customer} == true' })],
      variables: [
        variable({ variableKey: 'customer' }),
        variable({
          variableKey: 'label',
          variableType: 'COMPUTED',
          computeExpression: '${customer}',
        }),
      ],
      testDataSets: [
        dataSet({ testDataSetId: 'u1', locked: false, variables: { customer: 'x' } }),
        dataSet({ testDataSetId: 'l1', locked: true, variables: { customer: 'y' } }),
      ],
      upsertVariable,
      deleteVariable,
      upsertBinding,
      saveRules,
      updateTestDataSet,
      refreshTemplate,
    })

    expect(upsertVariable.mock.calls[0]?.[1]).toBe('party')
    expect(upsertVariable.mock.calls.some((call) => call[1] === 'label')).toBe(true)
    expect(upsertBinding).toHaveBeenCalled()
    expect(saveRules).toHaveBeenCalled()
    expect(updateTestDataSet).toHaveBeenCalledWith(
      'tpl-1',
      'u1',
      expect.objectContaining({ variables: { party: 'x' } }),
    )
    expect(deleteVariable).toHaveBeenCalledWith('tpl-1', 'customer')
    expect(refreshTemplate).toHaveBeenCalledWith('tpl-1')
    expect(result.lockedSkippedCount).toBe(1)
  })

  it('allows rename with zero references', async () => {
    const upsertVariable = vi.fn().mockResolvedValue(undefined)
    const deleteVariable = vi.fn().mockResolvedValue(undefined)
    const refreshTemplate = vi.fn().mockResolvedValue(undefined)

    await executeVariableRenameCascade({
      templateId: 'tpl-1',
      oldKey: 'lonelyKey',
      newKey: 'soloKey',
      variablePayload: {
        variableKey: 'soloKey',
        variableType: 'TEXT',
        required: true,
      },
      bindings: [],
      rules: [],
      variables: [variable({ variableKey: 'lonelyKey' })],
      testDataSets: [],
      upsertVariable,
      deleteVariable,
      upsertBinding: vi.fn(),
      saveRules: vi.fn(),
      updateTestDataSet: vi.fn(),
      refreshTemplate,
    })

    expect(upsertVariable).toHaveBeenCalledWith('tpl-1', 'soloKey', expect.any(Object))
    expect(deleteVariable).toHaveBeenCalledWith('tpl-1', 'lonelyKey')
  })
})
