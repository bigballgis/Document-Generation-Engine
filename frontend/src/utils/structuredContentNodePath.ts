import type { StructuredContentDocument, StructuredContentNode } from '@/utils/structuredContentNodes'

/** Max nesting depth for condition/loop child blocks (CE-U01). */
export const STRUCTURED_CONTENT_MAX_NEST_DEPTH = 3

export type NodePath = number[]

export function nodeDepthAtPath(path: NodePath): number {
  return path.length
}

/** Whether a container at `parentPath` may receive another nested block child. */
export function canAddNestedBlockChildren(parentPath: NodePath): boolean {
  return parentPath.length + 1 < STRUCTURED_CONTENT_MAX_NEST_DEPTH
}

export function pathKey(path: NodePath): string {
  return path.join('.')
}

/** DOM-safe path segment for data-testid attributes. */
export function pathTestId(path: NodePath): string {
  return path.join('-')
}

export function getNodeAtPath(
  document: StructuredContentDocument,
  path: NodePath,
): StructuredContentNode | null {
  if (path.length === 0) {
    return null
  }
  let node: StructuredContentNode | undefined = document.nodes[path[0]!]
  if (!node) {
    return null
  }
  for (let i = 1; i < path.length; i++) {
    node = node.children?.[path[i]!]
    if (!node) {
      return null
    }
  }
  return node
}

export function updateNodeAtPath(
  document: StructuredContentDocument,
  path: NodePath,
  updater: (node: StructuredContentNode) => StructuredContentNode,
): StructuredContentDocument {
  if (path.length === 0) {
    return document
  }

  function updateInChildren(
    children: StructuredContentNode[],
    pathIndex: number,
  ): StructuredContentNode[] {
    const idx = path[pathIndex]!
    if (pathIndex === path.length - 1) {
      const next = [...children]
      const current = next[idx]
      if (!current) {
        return children
      }
      next[idx] = updater(current)
      return next
    }
    const next = [...children]
    const current = next[idx]
    if (!current) {
      return children
    }
    next[idx] = {
      ...current,
      children: updateInChildren(current.children ?? [], pathIndex + 1),
    }
    return next
  }

  const topIdx = path[0]!
  const nodes = [...document.nodes]
  if (path.length === 1) {
    const current = nodes[topIdx]
    if (!current) {
      return document
    }
    nodes[topIdx] = updater(current)
  } else {
    const current = nodes[topIdx]
    if (!current) {
      return document
    }
    nodes[topIdx] = {
      ...current,
      children: updateInChildren(current.children ?? [], 1),
    }
  }
  return { ...document, nodes }
}

export function removeNodeAtPath(
  document: StructuredContentDocument,
  path: NodePath,
): StructuredContentDocument {
  if (path.length === 0) {
    return document
  }
  if (path.length === 1) {
    return {
      ...document,
      nodes: document.nodes.filter((_, index) => index !== path[0]),
    }
  }
  const parentPath = path.slice(0, -1)
  const removeIndex = path[path.length - 1]!
  return updateNodeAtPath(document, parentPath, (node) => ({
    ...node,
    children: (node.children ?? []).filter((_, index) => index !== removeIndex),
  }))
}

export function appendChildBlockAtPath(
  document: StructuredContentDocument,
  parentPath: NodePath,
  child: StructuredContentNode,
): StructuredContentDocument {
  return updateNodeAtPath(document, parentPath, (node) => ({
    ...node,
    children: [...(node.children ?? []), child],
  }))
}

export function isNestedContainerType(type: string): boolean {
  return type === 'conditionBlock' || type === 'loopBlock'
}
