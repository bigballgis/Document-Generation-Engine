import type {
  AnchorBinding,
  CompositionRule,
  CompositionRuleInput,
  TestDataSet,
  UpsertTestDataSetPayload,
  UpsertVariablePayload,
  VariableSchema,
} from '@/types/template'
import { toCompositionRuleInput } from '@/utils/mergeAnchorVisibilityRule'

/** Matches compute / condition `${path}` identifier roots (CE-K03 / F3). */
export const VARIABLE_KEY_PATTERN = /^[A-Za-z_][A-Za-z0-9_.-]*$/

export interface VariableRenameImpact {
  bindingAnchorCount: number
  ruleCount: number
  unlockedTestSetCount: number
  lockedTestSetSkippedCount: number
  computeReferenceCount: number
}

export interface VariableKeyValidation {
  valid: boolean
  messageKey?: string
}

export interface RenamedBinding {
  anchorId: string
  declaredContentType: string
  structuredContentJson: string
  pasteCleaningEvidence?: AnchorBinding['pasteCleaningEvidence']
}

export interface VariableRenameTransforms {
  bindings: RenamedBinding[]
  rules: CompositionRuleInput[]
  rulesChanged: boolean
  computeUpdates: Array<{ variableKey: string; payload: UpsertVariablePayload }>
  unlockedTestSetUpdates: Array<{ testDataSetId: string; payload: UpsertTestDataSetPayload }>
  lockedSkippedCount: number
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * Whole-token rename in expressions: `${oldKey}` / `${oldKey.path}` and bare
 * identifiers with non-identifier boundaries (never substring-hit `customerName`
 * when renaming `customer`).
 */
export function replaceVariableRefsInExpression(
  expression: string,
  oldKey: string,
  newKey: string,
): string {
  if (!expression || oldKey === newKey) {
    return expression
  }
  const escaped = escapeRegExp(oldKey)
  let result = expression.replace(
    new RegExp(`\\$\\{${escaped}(?=\\}|\\.)`, 'g'),
    `\${${newKey}`,
  )
  result = result.replace(
    new RegExp(`(?<![A-Za-z0-9_.-])${escaped}(?![A-Za-z0-9_.-])`, 'g'),
    newKey,
  )
  return result
}

export function expressionReferencesVariable(expression: string | null | undefined, key: string): boolean {
  if (!expression) {
    return false
  }
  return replaceVariableRefsInExpression(expression, key, `__rename_probe_${key}__`) !== expression
}

export function validateRenameVariableKey(
  newKeyRaw: string,
  oldKey: string,
  existingKeys: string[],
): VariableKeyValidation {
  const newKey = newKeyRaw.trim()
  if (!newKey) {
    return { valid: false, messageKey: 'templates.authoring.rename.variableKeyRequired' }
  }
  if (!VARIABLE_KEY_PATTERN.test(newKey)) {
    return { valid: false, messageKey: 'templates.authoring.rename.variableKeyInvalid' }
  }
  if (newKey !== oldKey && existingKeys.includes(newKey)) {
    return { valid: false, messageKey: 'templates.authoring.rename.variableKeyConflict' }
  }
  return { valid: true }
}

function renameStructuredNode(
  node: Record<string, unknown>,
  oldKey: string,
  newKey: string,
): boolean {
  let changed = false
  if (node.key === oldKey) {
    node.key = newKey
    changed = true
  }
  if (node.variableKey === oldKey) {
    node.variableKey = newKey
    changed = true
  }
  if (node.loopVariable === oldKey) {
    node.loopVariable = newKey
    changed = true
  }
  if (typeof node.conditionExpression === 'string') {
    const next = replaceVariableRefsInExpression(node.conditionExpression, oldKey, newKey)
    if (next !== node.conditionExpression) {
      node.conditionExpression = next
      changed = true
    }
  }
  const children = node.children
  if (Array.isArray(children)) {
    for (const child of children) {
      if (child && typeof child === 'object') {
        if (renameStructuredNode(child as Record<string, unknown>, oldKey, newKey)) {
          changed = true
        }
      }
    }
  }
  return changed
}

export function renameInStructuredContentJson(
  json: string | null | undefined,
  oldKey: string,
  newKey: string,
): { changed: boolean; json: string } {
  if (!json?.trim()) {
    return { changed: false, json: json ?? '' }
  }
  try {
    const parsed = JSON.parse(json) as Record<string, unknown>
    let changed = false
    const nodes = parsed.nodes
    if (Array.isArray(nodes)) {
      for (const node of nodes) {
        if (node && typeof node === 'object') {
          if (renameStructuredNode(node as Record<string, unknown>, oldKey, newKey)) {
            changed = true
          }
        }
      }
    }
    if (!changed) {
      // Fallback: whole-token replace across the serialized document string for
      // any `${oldKey}` occurrences outside the typed walk.
      const replaced = replaceVariableRefsInExpression(json, oldKey, newKey)
      if (replaced !== json) {
        return { changed: true, json: replaced }
      }
      return { changed: false, json }
    }
    return { changed: true, json: JSON.stringify(parsed) }
  } catch {
    const replaced = replaceVariableRefsInExpression(json, oldKey, newKey)
    return { changed: replaced !== json, json: replaced }
  }
}

export function renameTestSetVariableKeys(
  variables: Record<string, unknown>,
  oldKey: string,
  newKey: string,
): { changed: boolean; variables: Record<string, unknown> } {
  if (!(oldKey in variables)) {
    return { changed: false, variables }
  }
  const next: Record<string, unknown> = { ...variables }
  next[newKey] = next[oldKey]
  delete next[oldKey]
  return { changed: true, variables: next }
}

export function analyzeVariableRenameImpact(
  oldKey: string,
  bindings: AnchorBinding[],
  rules: CompositionRule[],
  variables: VariableSchema[],
  testDataSets: TestDataSet[],
): VariableRenameImpact {
  let bindingAnchorCount = 0
  for (const binding of bindings) {
    const { changed } = renameInStructuredContentJson(
      binding.structuredContentJson,
      oldKey,
      `__probe_${oldKey}__`,
    )
    if (changed) {
      bindingAnchorCount += 1
    }
  }

  let ruleCount = 0
  for (const rule of rules) {
    if (expressionReferencesVariable(rule.conditionExpression, oldKey)) {
      ruleCount += 1
    }
  }

  let unlockedTestSetCount = 0
  let lockedTestSetSkippedCount = 0
  for (const set of testDataSets) {
    if (!(oldKey in (set.variables ?? {}))) {
      continue
    }
    if (set.locked) {
      lockedTestSetSkippedCount += 1
    } else {
      unlockedTestSetCount += 1
    }
  }

  let computeReferenceCount = 0
  for (const variable of variables) {
    if (variable.variableKey === oldKey) {
      continue
    }
    if (expressionReferencesVariable(variable.computeExpression, oldKey)) {
      computeReferenceCount += 1
    }
  }

  return {
    bindingAnchorCount,
    ruleCount,
    unlockedTestSetCount,
    lockedTestSetSkippedCount,
    computeReferenceCount,
  }
}

export function buildVariableRenameTransforms(
  oldKey: string,
  newKey: string,
  bindings: AnchorBinding[],
  rules: CompositionRule[],
  variables: VariableSchema[],
  testDataSets: TestDataSet[],
): VariableRenameTransforms {
  const renamedBindings: RenamedBinding[] = []
  for (const binding of bindings) {
    const { changed, json } = renameInStructuredContentJson(
      binding.structuredContentJson,
      oldKey,
      newKey,
    )
    if (changed) {
      renamedBindings.push({
        anchorId: binding.anchorId,
        declaredContentType: binding.declaredContentType,
        structuredContentJson: json,
        pasteCleaningEvidence: binding.pasteCleaningEvidence,
      })
    }
  }

  let rulesChanged = false
  const nextRules = rules.map((rule) => {
    const nextExpression = replaceVariableRefsInExpression(rule.conditionExpression, oldKey, newKey)
    if (nextExpression !== rule.conditionExpression) {
      rulesChanged = true
      return {
        ...toCompositionRuleInput(rule),
        conditionExpression: nextExpression,
      }
    }
    return toCompositionRuleInput(rule)
  })

  const computeUpdates: VariableRenameTransforms['computeUpdates'] = []
  for (const variable of variables) {
    if (variable.variableKey === oldKey) {
      continue
    }
    const expression = variable.computeExpression ?? ''
    const nextExpression = replaceVariableRefsInExpression(expression, oldKey, newKey)
    if (nextExpression === expression) {
      continue
    }
    computeUpdates.push({
      variableKey: variable.variableKey,
      payload: {
        variableKey: variable.variableKey,
        variableType: variable.variableType,
        required: variable.required,
        defaultValue: variable.defaultValue ?? null,
        enumValues: variable.enumValues ?? null,
        description: variable.description ?? null,
        computeExpression: nextExpression || null,
      },
    })
  }

  const unlockedTestSetUpdates: VariableRenameTransforms['unlockedTestSetUpdates'] = []
  let lockedSkippedCount = 0
  for (const set of testDataSets) {
    if (!(oldKey in (set.variables ?? {}))) {
      continue
    }
    if (set.locked) {
      lockedSkippedCount += 1
      continue
    }
    const { variables: nextVariables } = renameTestSetVariableKeys(set.variables, oldKey, newKey)
    unlockedTestSetUpdates.push({
      testDataSetId: set.testDataSetId,
      payload: {
        name: set.name,
        description: set.description ?? undefined,
        variables: nextVariables,
        required: set.required,
        scenarioName: set.scenarioName ?? undefined,
        coverageTags: set.coverageTags,
      },
    })
  }

  return {
    bindings: renamedBindings,
    rules: nextRules,
    rulesChanged,
    computeUpdates,
    unlockedTestSetUpdates,
    lockedSkippedCount,
  }
}

export interface VariableRenameCascadeDeps {
  templateId: string
  oldKey: string
  newKey: string
  variablePayload: UpsertVariablePayload
  bindings: AnchorBinding[]
  rules: CompositionRule[]
  variables: VariableSchema[]
  testDataSets: TestDataSet[]
  upsertVariable: (templateId: string, variableKey: string, payload: UpsertVariablePayload) => Promise<unknown>
  deleteVariable: (templateId: string, variableKey: string) => Promise<unknown>
  upsertBinding: (
    templateId: string,
    anchorId: string,
    payload: {
      anchorId: string
      declaredContentType: string
      structuredContentJson: string
      pasteCleaningEvidence?: AnchorBinding['pasteCleaningEvidence'] | null
    },
  ) => Promise<unknown>
  saveRules: (templateId: string, rules: CompositionRuleInput[]) => Promise<unknown>
  updateTestDataSet: (
    templateId: string,
    testDataSetId: string,
    payload: UpsertTestDataSetPayload,
  ) => Promise<unknown>
  refreshTemplate: (templateId: string) => Promise<unknown>
}

export async function executeVariableRenameCascade(
  deps: VariableRenameCascadeDeps,
): Promise<{ lockedSkippedCount: number }> {
  const transforms = buildVariableRenameTransforms(
    deps.oldKey,
    deps.newKey,
    deps.bindings,
    deps.rules,
    deps.variables,
    deps.testDataSets,
  )

  await deps.upsertVariable(deps.templateId, deps.newKey, {
    ...deps.variablePayload,
    variableKey: deps.newKey,
  })

  for (const update of transforms.computeUpdates) {
    await deps.upsertVariable(deps.templateId, update.variableKey, update.payload)
  }

  for (const binding of transforms.bindings) {
    await deps.upsertBinding(deps.templateId, binding.anchorId, {
      anchorId: binding.anchorId,
      declaredContentType: binding.declaredContentType,
      structuredContentJson: binding.structuredContentJson,
      pasteCleaningEvidence: binding.pasteCleaningEvidence ?? null,
    })
  }

  if (transforms.rulesChanged) {
    await deps.saveRules(deps.templateId, transforms.rules)
  }

  // Drop oldKey from the schema before rewriting unlocked test-set JSON. Updating
  // sets while oldKey is still REQUIRED fails schema validation (missing required
  // field) even though the payload already uses newKey.
  await deps.deleteVariable(deps.templateId, deps.oldKey)

  for (const update of transforms.unlockedTestSetUpdates) {
    await deps.updateTestDataSet(deps.templateId, update.testDataSetId, update.payload)
  }

  await deps.refreshTemplate(deps.templateId)

  return { lockedSkippedCount: transforms.lockedSkippedCount }
}
