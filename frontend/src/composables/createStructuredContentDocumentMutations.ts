import type { Ref } from 'vue'
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

  function replaceBlock(index: number, next: StructuredContentNode) {
    const nodes = [...documentModel.value.nodes]
    nodes[index] = next
    documentModel.value = { ...documentModel.value, nodes }
  }

  function insertBlock(type: ConfirmedNodeType, selectedStyleKey: string) {
    if (isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = insertBlockNode(documentModel.value, type, selectedStyleKey)
  }

  function applySelectedStyle(selectedStyleKey: string) {
    if (!selectedStyleKey || isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = applyStyleToParagraphs(documentModel.value, selectedStyleKey)
  }

  function updateBlockField(index: number, field: keyof StructuredContentNode, value: string) {
    const node = documentModel.value.nodes[index]
    if (!node) {
      return
    }
    setPendingCoalesceKey(`field:${index}:${String(field)}`)
    replaceBlock(index, { ...node, [field]: value })
  }

  function updateInlineChild(
    blockIndex: number,
    childIndex: number,
    nextChild: StructuredContentNode,
  ) {
    const node = documentModel.value.nodes[blockIndex]
    if (!node) {
      return
    }
    setPendingCoalesceKey(`inline:${blockIndex}:${childIndex}`)
    const children = [...(node.children ?? [])]
    children[childIndex] = nextChild
    replaceBlock(blockIndex, { ...node, children })
  }

  function addInlineToBlock(
    blockIndex: number,
    type: ConfirmedNodeType,
    selectedStyleKey: string,
  ) {
    const node = documentModel.value.nodes[blockIndex]
    if (!node) {
      return
    }
    setPendingCoalesceKey(null)
    const children = [...(node.children ?? []), createNodeTemplate(type, selectedStyleKey)]
    replaceBlock(blockIndex, { ...node, children })
  }

  function removeBlock(index: number) {
    if (isReadonly()) {
      return
    }
    setPendingCoalesceKey(null)
    documentModel.value = {
      ...documentModel.value,
      nodes: documentModel.value.nodes.filter((_, nodeIndex) => nodeIndex !== index),
    }
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
    insertInline,
    applySelectedStyle,
    updateBlockField,
    updateInlineChild,
    addInlineToBlock,
    removeBlock,
  }
}
