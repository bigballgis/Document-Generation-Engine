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

export function parentPathOf(path: NodePath): NodePath | null {
  return path.length <= 1 ? null : path.slice(0, -1)
}

export function areSiblingPaths(left: NodePath, right: NodePath): boolean {
  if (left.length !== right.length || left.length === 0) {
    return false
  }
  const leftParent = parentPathOf(left)
  const rightParent = parentPathOf(right)
  if (leftParent === null && rightParent === null) {
    return true
  }
  if (leftParent === null || rightParent === null) {
    return false
  }
  return pathKey(leftParent) === pathKey(rightParent)
}

export function siblingCountAtPath(
  document: StructuredContentDocument,
  path: NodePath,
): number {
  const parentPath = parentPathOf(path)
  if (parentPath === null) {
    return document.nodes.length
  }
  const parent = getNodeAtPath(document, parentPath)
  return parent?.children?.length ?? 0
}

export function reorderBlockAtPath(
  document: StructuredContentDocument,
  path: NodePath,
  toIndex: number,
): StructuredContentDocument {
  const fromIndex = path[path.length - 1]
  if (fromIndex === undefined) {
    return document
  }
  const parentPath = path.slice(0, -1)

  function reorderSiblings(siblings: StructuredContentNode[]): StructuredContentNode[] {
    if (fromIndex < 0 || fromIndex >= siblings.length || fromIndex === toIndex) {
      return siblings
    }
    const next = [...siblings]
    const [moved] = next.splice(fromIndex, 1)
    if (!moved) {
      return siblings
    }
    const clampedTo = Math.max(0, Math.min(toIndex, next.length))
    next.splice(clampedTo, 0, moved)
    return next
  }

  if (parentPath.length === 0) {
    return { ...document, nodes: reorderSiblings(document.nodes) }
  }
  return updateNodeAtPath(document, parentPath, (node) => ({
    ...node,
    children: reorderSiblings(node.children ?? []),
  }))
}

function deepCloneNode(node: StructuredContentNode): StructuredContentNode {
  return JSON.parse(JSON.stringify(node)) as StructuredContentNode
}

export function duplicateBlockAtPath(
  document: StructuredContentDocument,
  path: NodePath,
): StructuredContentDocument {
  const node = getNodeAtPath(document, path)
  const index = path[path.length - 1]
  if (!node || index === undefined) {
    return document
  }
  const parentPath = path.slice(0, -1)
  const clone = deepCloneNode(node)

  function insertAfter(siblings: StructuredContentNode[]): StructuredContentNode[] {
    const next = [...siblings]
    next.splice(index + 1, 0, clone)
    return next
  }

  if (parentPath.length === 0) {
    return { ...document, nodes: insertAfter(document.nodes) }
  }
  return updateNodeAtPath(document, parentPath, (parent) => ({
    ...parent,
    children: insertAfter(parent.children ?? []),
  }))
}
