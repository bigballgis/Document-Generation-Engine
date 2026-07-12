import type { VariableSchema } from '@/types/template'
import { humanizeCamelCase, resolveFolderDisplayName, resolveVariableDisplayName } from '@/utils/variableDisplayName'

export interface VariableTreeNode {
  id: string
  label: string
  displayLabel: string
  technicalKey?: string
  isLeaf: boolean
  containerType?: 'LIST' | 'OBJECT'
  variable?: VariableSchema
  children?: VariableTreeNode[]
}

const LIST_FIELD_PREFIXES: Record<string, string[]> = {
  lenders: ['lender'],
  tranches: ['tranche'],
  fees: ['feeItem'],
  conditionsPrecedent: ['cpItem'],
  guarantors: ['guarantor'],
  securityPackages: ['securityPackage'],
  amortisationSchedule: ['amort'],
  facilityParticulars: ['particular'],
  hedgeProviders: ['hedgeProvider'],
  esgKpis: ['esgKpi'],
  pricingTiers: ['pricingTier'],
  milestones: ['milestoneDate'],
  parties: ['party'],
  covenants: ['covenant'],
  securedAssets: ['securedAsset'],
  legalReferences: ['legalClauseRef', 'legalDefinedTerm'],
  definedTerms: ['definedTerm'],
  representations: ['representation'],
  infoUndertakings: ['infoUndertaking'],
  eodTriggers: ['eodTrigger'],
  noticeParties: ['noticeParty'],
  benchmarkFallbacks: ['benchmarkFallback'],
  insurancePolicies: ['insurancePolicy'],
  obligors: ['obligor'],
  prepaymentEvents: ['prepaymentEvent'],
}

export function splitVariableKeySegments(variableKey: string): string[] {
  if (variableKey.includes('.')) {
    return variableKey.split('.').filter((segment) => segment.length > 0)
  }

  const itemMatch = variableKey.match(/^(.+Item)([A-Z].*)$/)
  if (itemMatch) {
    return [itemMatch[1], itemMatch[2]]
  }

  const boundary = variableKey.search(/(?<=[a-z0-9])(?=[A-Z])/)
  if (boundary > 0) {
    return [variableKey.slice(0, boundary), variableKey.slice(boundary)]
  }

  return [variableKey]
}

function resolveListContainerKey(
  variableKey: string,
  listVariables: VariableSchema[],
): string | null {
  for (const [listKey, prefixes] of Object.entries(LIST_FIELD_PREFIXES)) {
    if (!listVariables.some((variable) => variable.variableKey === listKey)) {
      continue
    }
    for (const prefix of prefixes) {
      if (variableKey.startsWith(prefix)) {
        return listKey
      }
    }
  }
  return null
}

function sortTreeNodes(nodes: VariableTreeNode[]): VariableTreeNode[] {
  return nodes
    .map((node) => ({
      ...node,
      children: node.children ? sortTreeNodes(node.children) : undefined,
    }))
    .sort((left, right) => {
      if (left.isLeaf !== right.isLeaf) {
        return left.isLeaf ? 1 : -1
      }
      return left.displayLabel.localeCompare(right.displayLabel)
    })
}

function appendLeafNode(level: VariableTreeNode[], variable: VariableSchema) {
  const segments = splitVariableKeySegments(variable.variableKey)
  let currentLevel = level
  let path = ''

  for (let index = 0; index < segments.length; index += 1) {
    const segment = segments[index]
    path = path ? `${path}.${segment}` : segment
    const isLeaf = index === segments.length - 1

    if (isLeaf) {
      currentLevel.push(createLeafNode(variable))
      return
    }

    let folder = currentLevel.find((node) => !node.isLeaf && node.label === segment)
    if (!folder) {
      folder = {
        id: path,
        label: segment,
        displayLabel: resolveFolderDisplayName(segment),
        isLeaf: false,
        children: [],
      }
      currentLevel.push(folder)
    }
    currentLevel = folder.children!
  }
}

function createLeafNode(variable: VariableSchema): VariableTreeNode {
  const segment = splitVariableKeySegments(variable.variableKey).at(-1) ?? variable.variableKey
  return {
    id: variable.variableKey,
    label: segment,
    displayLabel: resolveVariableDisplayName(variable),
    technicalKey: variable.variableKey,
    isLeaf: true,
    variable,
  }
}

function createContainerNode(variable: VariableSchema): VariableTreeNode {
  return {
    id: variable.variableKey,
    label: variable.variableKey,
    displayLabel: resolveVariableDisplayName(variable),
    technicalKey: variable.variableKey,
    isLeaf: false,
    containerType: variable.variableType === 'OBJECT' ? 'OBJECT' : 'LIST',
    variable,
    children: [],
  }
}

export function buildVariableSchemaTree(variables: VariableSchema[]): VariableTreeNode[] {
  const listVariables = variables.filter(
    (variable) => variable.variableType === 'LIST' || variable.variableType === 'OBJECT',
  )
  const listKeys = new Set(listVariables.map((variable) => variable.variableKey))
  const containers = new Map<string, VariableTreeNode>(
    listVariables.map((variable) => [variable.variableKey, createContainerNode(variable)]),
  )
  const root: VariableTreeNode[] = []
  const assigned = new Set<string>()

  for (const variable of variables) {
    if (listKeys.has(variable.variableKey)) {
      continue
    }

    const containerKey = resolveListContainerKey(variable.variableKey, listVariables)
    if (containerKey && containers.has(containerKey)) {
      containers.get(containerKey)!.children!.push(createLeafNode(variable))
      assigned.add(variable.variableKey)
    }
  }

  for (const container of containers.values()) {
    if (container.children!.length > 0 || container.variable) {
      root.push(container)
    }
  }

  for (const variable of variables) {
    if (listKeys.has(variable.variableKey) || assigned.has(variable.variableKey)) {
      continue
    }
    appendLeafNode(root, variable)
  }

  return sortTreeNodes(root)
}

export function filterVariableTree(nodes: VariableTreeNode[], query: string): VariableTreeNode[] {
  const normalized = query.trim().toLowerCase()
  if (!normalized) {
    return nodes
  }

  const result: VariableTreeNode[] = []
  for (const node of nodes) {
    if (node.isLeaf && node.variable) {
      const key = node.variable.variableKey.toLowerCase()
      const description = (node.variable.description ?? '').toLowerCase()
      const display = node.displayLabel.toLowerCase()
      if (key.includes(normalized) || description.includes(normalized) || display.includes(normalized)) {
        result.push(node)
      }
      continue
    }

    if (!node.children) {
      continue
    }

    const filteredChildren = filterVariableTree(node.children, normalized)
    const containerMatches =
      node.displayLabel.toLowerCase().includes(normalized) ||
      (node.technicalKey ?? '').toLowerCase().includes(normalized)

    if (filteredChildren.length > 0) {
      result.push({ ...node, children: filteredChildren })
    } else if (containerMatches) {
      result.push(node)
    }
  }

  return result
}

export function collectVariableTreeExpandKeys(nodes: VariableTreeNode[]): string[] {
  const keys: string[] = []
  for (const node of nodes) {
    if (!node.isLeaf) {
      keys.push(node.id)
      if (node.children) {
        keys.push(...collectVariableTreeExpandKeys(node.children))
      }
    }
  }
  return keys
}

export function humanizeVariableKeyForTest(value: string): string {
  return humanizeCamelCase(value)
}
