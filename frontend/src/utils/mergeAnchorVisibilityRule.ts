import type { CompositionRule, CompositionRuleInput } from '@/types/template'

export function toCompositionRuleInput(rule: CompositionRule): CompositionRuleInput {
  return {
    ruleId: rule.ruleId,
    conditionExpression: rule.conditionExpression,
    targetAnchorId: rule.targetAnchorId,
    trueBranchRuleId: rule.trueBranchRuleId ?? undefined,
    falseBranchRuleId: rule.falseBranchRuleId ?? undefined,
  }
}

/**
 * Upsert or remove the per-anchor visibility composition rule while preserving
 * unrelated rules. Empty/disabled expressions remove the anchor's visibility rule.
 */
export function mergeAnchorVisibilityRule(
  existingRules: CompositionRule[],
  anchorId: string,
  enabled: boolean,
  expression: string,
): CompositionRuleInput[] {
  const others = existingRules.filter((rule) => rule.targetAnchorId !== anchorId)
  if (!enabled || !expression.trim()) {
    return others.map(toCompositionRuleInput)
  }
  const existing = existingRules.find((rule) => rule.targetAnchorId === anchorId)
  return [
    ...others.map(toCompositionRuleInput),
    {
      ruleId: existing?.ruleId ?? `visibility-${anchorId}`,
      conditionExpression: expression.trim(),
      targetAnchorId: anchorId,
    },
  ]
}
