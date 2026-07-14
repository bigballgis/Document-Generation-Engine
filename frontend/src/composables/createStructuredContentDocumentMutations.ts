import type { Ref } from 'vue'
import {
  appendChildBlockAtPath,
  canAddNestedBlockChildren,
  getNodeAtPath,
  pathKey,
  removeNodeAtPath,
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

export function createStructuredContentDocumentMutations(options: {
  documentModel: Ref<StructuredContentDocument>
  isReadonly: () => boolean
  setPendingCoalesceKey: (value: string | null) => void
}) {
  const { documentModel, isReadonly, setPendingCoalesceKey } = options

  function replaceNodeAtPath(path: NodePath, next: StructuredContentNode) {
    documentModel.value = updateNodeAtPath(documentModel.value, path, () => next)
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

  function applySelectedStyle(selectedStyleKey: string) {
    if (!selectedStyleKey || isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = applyStyleToParagraphs(documentModel.value, selectedStyleKey)
  }

  function updateBlockField(path: NodePath, field: keyof StructuredContentNode, value: string) {
    const node = getNodeAtPath(documentModel.value, path)
    if (!node) {
      return
    }
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
  }

  function insertInline(type: ConfirmedNodeType, selectedStyleKey: string) {
    if (isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    const nodes = [...documentModel.value.nodes]
    if (!nodes.length) {
      nodes.push(createNodeTemplate('paragraph', selectedStyleKey))
    }
    const lastIndex = nodes.length - 1
    const target = nodes[lastIndex]
    if (!target) {
      return
    }
    const children = [...(target.children ?? []), createNodeTemplate(type, selectedStyleKey)]
    nodes[lastIndex] = { ...target, children }
    documentModel.value = { ...documentModel.value, nodes }
  }

  return {
    insertBlock,
    insertNestedBlock,
    insertInline,
    applySelectedStyle,
    updateBlockField,
    updateInlineChild,
    addInlineToBlock,
    removeBlock,
  }
}
