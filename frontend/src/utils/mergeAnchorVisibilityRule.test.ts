import { describe, expect, it } from 'vitest'
import type { CompositionRule } from '@/types/template'
import { mergeAnchorVisibilityRule, toCompositionRuleInput } from './mergeAnchorVisibilityRule'

function rule(
  partial: Pick<CompositionRule, 'ruleId' | 'conditionExpression' | 'targetAnchorId'> &
    Partial<CompositionRule>,
): CompositionRule {
  return {
    trueBranchRuleId: null,
    falseBranchRuleId: null,
    ...partial,
  }
}

describe('toCompositionRuleInput', () => {
  it('maps null branch ids to undefined', () => {
    expect(
      toCompositionRuleInput(
        rule({
          ruleId: 'r1',
          conditionExpression: 'x == 1',
          targetAnchorId: 'a1',
          trueBranchRuleId: 't1',
          falseBranchRuleId: null,
        }),
      ),
    ).toEqual({
      ruleId: 'r1',
      conditionExpression: 'x == 1',
      targetAnchorId: 'a1',
      trueBranchRuleId: 't1',
      falseBranchRuleId: undefined,
    })
  })
})

describe('mergeAnchorVisibilityRule', () => {
  const existing: CompositionRule[] = [
    rule({ ruleId: 'other', conditionExpression: 'keep', targetAnchorId: 'other-anchor' }),
    rule({ ruleId: 'visibility-a1', conditionExpression: 'old', targetAnchorId: 'a1' }),
  ]

  it('removes the anchor rule when disabled', () => {
    expect(mergeAnchorVisibilityRule(existing, 'a1', false, 'ignored')).toEqual([
      {
        ruleId: 'other',
        conditionExpression: 'keep',
        targetAnchorId: 'other-anchor',
        trueBranchRuleId: undefined,
        falseBranchRuleId: undefined,
      },
    ])
  })

  it('removes the anchor rule when expression is blank', () => {
    expect(mergeAnchorVisibilityRule(existing, 'a1', true, '   ')).toHaveLength(1)
  })

  it('updates an existing visibility rule in place', () => {
    const merged = mergeAnchorVisibilityRule(existing, 'a1', true, ' new ')
    expect(merged).toEqual([
      {
        ruleId: 'other',
        conditionExpression: 'keep',
        targetAnchorId: 'other-anchor',
        trueBranchRuleId: undefined,
        falseBranchRuleId: undefined,
      },
      {
        ruleId: 'visibility-a1',
        conditionExpression: 'new',
        targetAnchorId: 'a1',
      },
    ])
  })

  it('creates a default rule id when none exists', () => {
    const merged = mergeAnchorVisibilityRule(
      [rule({ ruleId: 'other', conditionExpression: 'keep', targetAnchorId: 'other-anchor' })],
      'a2',
      true,
      'flag',
    )
    expect(merged[1]).toEqual({
      ruleId: 'visibility-a2',
      conditionExpression: 'flag',
      targetAnchorId: 'a2',
    })
  })
})
