import type { Ref } from 'vue'
import {
  appendChildBlockAtPath,
  canAddNestedBlockChildren,
  duplicateBlockAtPath,
  getNodeAtPath,
  pathKey,
  removeNodeAtPath,
  reorderBlockAtPath,
  updateNodeAtPath,
  type NodePath,
} from '@/utils/structuredContentNodePath'
import {
  applyStyleToParagraphs,
  createNodeTemplate,
  insertBlockNode,
  type ConfirmedNodeType,
  type StructuredContentDocument,
  type StructuredContentNode,
} from '@/utils/structuredContentNodes'

function isInlineHost(node: StructuredContentNode | null | undefined): boolean {
  return node?.type === 'paragraph' || node?.type === 'sectionHeading'
}

export function createStructuredContentDocumentMutations(options: {
  documentModel: Ref<StructuredContentDocument>
  isReadonly: () => boolean
  setPendingCoalesceKey: (value: string | null) => void
  focusedPath: Ref<NodePath | null>
}) {
  const { documentModel, isReadonly, setPendingCoalesceKey, focusedPath } = options

  function replaceNodeAtPath(path: NodePath, next: StructuredContentNode) {
    documentModel.value = updateNodeAtPath(documentModel.value, path, () => next)
  }

  function setFocusedPath(path: NodePath | null) {
    focusedPath.value = path
  }

  function insertBlock(type: ConfirmedNodeType, selectedStyleKey: string) {
    if (isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = insertBlockNode(documentModel.value, type, selectedStyleKey)
  }

  function insertNestedBlock(
    parentPath: NodePath,
    type: ConfirmedNodeType,
    selectedStyleKey: string,
  ) {
    if (isReadonly() || !canAddNestedBlockChildren(parentPath)) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = appendChildBlockAtPath(
      documentModel.value,
      parentPath,
      createNodeTemplate(type, selectedStyleKey),
    )
  }

  function applySelectedStyle(selectedStyleKey: string, applicableNodeTypes?: string[]) {
    if (!selectedStyleKey || isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = applyStyleToParagraphs(documentModel.value, selectedStyleKey, {
      focusedPath: focusedPath.value,
      applicableNodeTypes,
    })
  }

  function updateBlockField(path: NodePath, field: keyof StructuredContentNode, value: string) {
    const node = getNodeAtPath(documentModel.value, path)
    if (!node) {
      return
    }
    setFocusedPath(path)
    setPendingCoalesceKey(`field:${pathKey(path)}:${String(field)}`)
    replaceNodeAtPath(path, { ...node, [field]: value })
  }

  function updateInlineChild(
    path: NodePath,
    childIndex: number,
    nextChild: StructuredContentNode,
  ) {
    const node = getNodeAtPath(documentModel.value, path)
    if (!node) {
      return
    }
    setFocusedPath(path)
    setPendingCoalesceKey(`inline:${pathKey(path)}:${childIndex}`)
    const children = [...(node.children ?? [])]
    children[childIndex] = nextChild
    replaceNodeAtPath(path, { ...node, children })
  }

  function addInlineToBlock(path: NodePath, type: ConfirmedNodeType, selectedStyleKey: string) {
    const node = getNodeAtPath(documentModel.value, path)
    if (!node) {
      return
    }
    setFocusedPath(path)
    setPendingCoalesceKey(null)
    const children = [...(node.children ?? []), createNodeTemplate(type, selectedStyleKey)]
    replaceNodeAtPath(path, { ...node, children })
  }

  function removeBlock(path: NodePath) {
    if (isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = removeNodeAtPath(documentModel.value, path)
    if (focusedPath.value && pathKey(focusedPath.value) === pathKey(path)) {
      focusedPath.value = null
    }
  }

  function reorderBlock(path: NodePath, toIndex: number) {
    if (isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = reorderBlockAtPath(documentModel.value, path, toIndex)
  }

  function copyBlock(path: NodePath) {
    if (isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = duplicateBlockAtPath(documentModel.value, path)
  }

  function insertInline(type: ConfirmedNodeType, selectedStyleKey: string) {
    if (isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)

    // FOS-W3-5 — prefer focused host paragraph/heading.
    const focus = focusedPath.value
    if (focus && focus.length > 0) {
      const target = getNodeAtPath(documentModel.value, focus)
      if (isInlineHost(target)) {
        const children = [...(target!.children ?? []), createNodeTemplate(type, selectedStyleKey)]
        replaceNodeAtPath(focus, { ...target!, children })
        return
      }
    }

    const nodes = [...documentModel.value.nodes]
    if (!nodes.length) {
      nodes.push(createNodeTemplate('paragraph', selectedStyleKey))
    }
    // Prefer last top-level paragraph/heading host; otherwise append a new paragraph.
    let lastIndex = -1
    for (let i = nodes.length - 1; i >= 0; i--) {
      if (isInlineHost(nodes[i])) {
        lastIndex = i
        break
      }
    }
    if (lastIndex < 0) {
      nodes.push(createNodeTemplate('paragraph', selectedStyleKey))
      lastIndex = nodes.length - 1
    }
    const target = nodes[lastIndex]
    if (!target) {
      return
    }
    const children = [...(target.children ?? []), createNodeTemplate(type, selectedStyleKey)]
    nodes[lastIndex] = { ...target, children }
    documentModel.value = { ...documentModel.value, nodes }
    focusedPath.value = [lastIndex]
  }

  return {
    focusedPath,
    setFocusedPath,
    insertBlock,
    insertNestedBlock,
    insertInline,
    applySelectedStyle,
    updateBlockField,
    updateInlineChild,
    addInlineToBlock,
    removeBlock,
    reorderBlock,
    copyBlock,
  }
}
