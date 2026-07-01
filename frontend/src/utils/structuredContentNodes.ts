/**
 * Confirmed v1 structured-content node types (P18-T01 / ADR-0019).
 */
export const CONFIRMED_V1_NODE_TYPES = [
  'sectionHeading',
  'paragraph',
  'list',
  'conditionBlock',
  'loopBlock',
  'tableComponentRef',
  'textRun',
  'variable',
  'emphasis',
  'underline',
  'lineBreak',
  'contentModuleRef',
  'imageRef',
  'qrBarcodeRef',
  'sealRef',
  'attachmentListRef',
  'styleRef',
] as const

export type ConfirmedNodeType = (typeof CONFIRMED_V1_NODE_TYPES)[number]

export interface StructuredContentNode {
  type: string
  children?: StructuredContentNode[]
  value?: string
  key?: string
  conditionExpression?: string
  loopVariable?: string
  referenceKey?: string
  styleRef?: string
  imageRef?: string
  tableComponentRef?: string
}

export interface StructuredContentDocument {
  schemaVersion: string
  nodes: StructuredContentNode[]
}

export interface DisabledToolbarCapability {
  id: string
  labelKey: string
  reasonKey: string
}

/** Capabilities shown on the toolbar but not insertable in v1 (with i18n reason). */
export const DISABLED_TOOLBAR_CAPABILITIES: DisabledToolbarCapability[] = [
  {
    id: 'arbitraryHtml',
    labelKey: 'templates.structuredEditor.toolbar.arbitraryHtml',
    reasonKey: 'templates.structuredEditor.unavailable.arbitraryHtml',
  },
  {
    id: 'wordTable',
    labelKey: 'templates.structuredEditor.toolbar.wordTable',
    reasonKey: 'templates.structuredEditor.unavailable.wordTable',
  },
  {
    id: 'floatLayout',
    labelKey: 'templates.structuredEditor.toolbar.floatLayout',
    reasonKey: 'templates.structuredEditor.unavailable.floatLayout',
  },
]

export const DEFAULT_STRUCTURED_CONTENT_JSON = JSON.stringify({
  schemaVersion: '1.0',
  nodes: [{ type: 'paragraph', children: [{ type: 'textRun', value: '' }] }],
})

export function isConfirmedNodeType(type: string): type is ConfirmedNodeType {
  return (CONFIRMED_V1_NODE_TYPES as readonly string[]).includes(type)
}

import { normalizeLegacyStructuredContent } from '@/utils/structuredContentCompat'

export function parseStructuredContent(json: string): StructuredContentDocument {
  try {
    const parsed = JSON.parse(json)
    const normalized = normalizeLegacyStructuredContent(parsed)
    if (!normalized.nodes.length && parsed && typeof parsed === 'object') {
      const legacy = parsed as { nodes?: unknown; blocks?: unknown }
      if (!Array.isArray(legacy.nodes) && !Array.isArray(legacy.blocks)) {
        return JSON.parse(DEFAULT_STRUCTURED_CONTENT_JSON) as StructuredContentDocument
      }
    }
    return normalized
  } catch {
    return JSON.parse(DEFAULT_STRUCTURED_CONTENT_JSON) as StructuredContentDocument
  }
}

export function serializeStructuredContent(document: StructuredContentDocument): string {
  return JSON.stringify({
    schemaVersion: document.schemaVersion ?? '1.0',
    nodes: document.nodes,
  })
}

export function createNodeTemplate(type: ConfirmedNodeType, styleRef?: string): StructuredContentNode {
  switch (type) {
    case 'sectionHeading':
      return {
        type,
        styleRef: styleRef ?? 'Heading1',
        children: [{ type: 'textRun', value: '' }],
      }
    case 'paragraph':
      return {
        type,
        styleRef: styleRef ?? 'BodyText',
        children: [{ type: 'textRun', value: '' }],
      }
    case 'list':
      return {
        type,
        children: [
          {
            type: 'paragraph',
            children: [{ type: 'textRun', value: '' }],
          },
        ],
      }
    case 'conditionBlock':
      return { type, conditionExpression: '', children: [] }
    case 'loopBlock':
      return { type, loopVariable: '', children: [] }
    case 'tableComponentRef':
      return { type, tableComponentRef: '' }
    case 'textRun':
      return { type, value: '' }
    case 'variable':
      return { type, key: '' }
    case 'emphasis':
    case 'underline':
      return { type, children: [{ type: 'textRun', value: '' }] }
    case 'lineBreak':
      return { type }
    case 'contentModuleRef':
      return { type, referenceKey: '' }
    case 'imageRef':
      return { type, imageRef: '' }
    case 'qrBarcodeRef':
      return { type, referenceKey: '' }
    case 'sealRef':
      return { type, referenceKey: '' }
    case 'attachmentListRef':
      return { type, referenceKey: '' }
    case 'styleRef':
      return { type, styleRef: styleRef ?? 'BodyText' }
    default:
      return { type: 'paragraph', children: [{ type: 'textRun', value: '' }] }
  }
}

export function insertBlockNode(
  document: StructuredContentDocument,
  nodeType: ConfirmedNodeType,
  styleRef?: string,
): StructuredContentDocument {
  if (!isConfirmedNodeType(nodeType)) {
    return document
  }
  const blockTypes: ConfirmedNodeType[] = [
    'sectionHeading',
    'paragraph',
    'list',
    'conditionBlock',
    'loopBlock',
    'tableComponentRef',
  ]
  if (!blockTypes.includes(nodeType)) {
    return document
  }
  return {
    ...document,
    nodes: [...document.nodes, createNodeTemplate(nodeType, styleRef)],
  }
}

export function applyStyleToParagraphs(
  document: StructuredContentDocument,
  styleKey: string,
): StructuredContentDocument {
  return {
    ...document,
    nodes: document.nodes.map((node) => {
      if (node.type === 'paragraph' || node.type === 'sectionHeading') {
        return { ...node, styleRef: styleKey }
      }
      return node
    }),
  }
}
