import type { NodePath } from '@/utils/structuredContentNodePath'
import { pathTestId } from '@/utils/structuredContentNodePath'
import type { StructuredContentDocument, StructuredContentNode } from '@/utils/structuredContentNodes'

export interface StructuredContentValidationIssue {
  blockPath: NodePath
  location: string
  messageKey: string
  code: string
}

const MESSAGE_KEY_UNRESOLVED_VARIABLE = 'generation.warning.fidelity.unresolvedVariable'
const MESSAGE_KEY_INVALID_CONDITION = 'generation.warning.fidelity.invalidConditionExpression'

const VARIABLE_REF_PATTERN = /\$\{([^}]+)\}/g

export function blockPathToLocation(path: NodePath): string {
  if (path.length === 0) {
    return 'document'
  }
  let location = `nodes[${path[0]}]`
  for (let i = 1; i < path.length; i++) {
    location += `.children[${path[i]}]`
  }
  return location
}

/** Parse backend-style locations (`nodes[0].children[1]`) into editor block paths. */
export function parseBlockLocation(location: string): NodePath | null {
  const trimmed = location.trim()
  if (!trimmed.startsWith('nodes[')) {
    return null
  }
  const segments = trimmed.match(/nodes\[(\d+)\]|children\[(\d+)\]/g)
  if (!segments?.length) {
    return null
  }
  const path: number[] = []
  for (const segment of segments) {
    const match = segment.match(/\[(\d+)\]/)
    if (!match?.[1]) {
      return null
    }
    path.push(Number.parseInt(match[1], 10))
  }
  return path
}

export function structuredBlockCardTestId(path: NodePath): string {
  return `structured-block-card-${pathTestId(path)}`
}

export function validateStructuredContentDocument(
  document: StructuredContentDocument,
  declaredVariableKeys: ReadonlySet<string>,
): StructuredContentValidationIssue[] {
  const issues: StructuredContentValidationIssue[] = []
  document.nodes.forEach((node, index) => {
    walkNode(node, [index], declaredVariableKeys, issues)
  })
  return issues
}

function walkNode(
  node: StructuredContentNode,
  path: NodePath,
  declaredVariableKeys: ReadonlySet<string>,
  issues: StructuredContentValidationIssue[],
): void {
  if (node.type === 'paragraph' || node.type === 'sectionHeading') {
    node.children?.forEach((child) => {
      if (child.type === 'variable') {
        const key = (child.key ?? '').trim()
        if (!key || !declaredVariableKeys.has(key)) {
          pushIssue(path, MESSAGE_KEY_UNRESOLVED_VARIABLE, 'UNRESOLVED_VARIABLE', issues)
        }
      }
    })
    return
  }

  if (node.type === 'conditionBlock') {
    validateConditionBlock(node, path, declaredVariableKeys, issues)
  }

  if (node.type === 'loopBlock') {
    const loopVariable = (node.loopVariable ?? node.key ?? '').trim()
    if (!loopVariable || !declaredVariableKeys.has(loopVariable)) {
      pushIssue(path, MESSAGE_KEY_UNRESOLVED_VARIABLE, 'UNRESOLVED_VARIABLE', issues)
    }
  }

  node.children?.forEach((child, index) => {
    walkNode(child, [...path, index], declaredVariableKeys, issues)
  })
}

function validateConditionBlock(
  node: StructuredContentNode,
  path: NodePath,
  declaredVariableKeys: ReadonlySet<string>,
  issues: StructuredContentValidationIssue[],
): void {
  const expression = (node.conditionExpression ?? node.key ?? '').trim()
  if (!expression) {
    pushIssue(path, MESSAGE_KEY_INVALID_CONDITION, 'INVALID_CONDITION_EXPRESSION', issues)
    return
  }
  for (const variableKey of extractVariableReferences(expression)) {
    if (!declaredVariableKeys.has(variableKey)) {
      pushIssue(path, MESSAGE_KEY_UNRESOLVED_VARIABLE, 'UNRESOLVED_VARIABLE', issues)
    }
  }
}

function extractVariableReferences(expression: string): string[] {
  const keys: string[] = []
  for (const match of expression.matchAll(VARIABLE_REF_PATTERN)) {
    const key = match[1]?.trim()
    if (key) {
      keys.push(key)
    }
  }
  return keys
}

function pushIssue(
  path: NodePath,
  messageKey: string,
  code: string,
  issues: StructuredContentValidationIssue[],
): void {
  issues.push({
    blockPath: [...path],
    location: blockPathToLocation(path),
    messageKey,
    code,
  })
}
