import type { StructuredContentDocument, StructuredContentNode } from '@/utils/structuredContentNodes'

interface LegacyBlock {
  type?: string
  text?: string
  children?: LegacyBlock[]
  value?: string
  key?: string
}

interface LegacyStructuredContent {
  schemaVersion?: string
  nodes?: StructuredContentNode[]
  blocks?: LegacyBlock[]
}

function legacyBlockToNode(block: LegacyBlock): StructuredContentNode {
  const type = block.type ?? 'paragraph'
  if (type === 'paragraph' || type === 'sectionHeading') {
    const text = block.text ?? block.value ?? ''
    return {
      type,
      styleRef: type === 'sectionHeading' ? 'Heading1' : 'BodyText',
      children: [{ type: 'textRun', value: text }],
    }
  }
  if (block.text != null && block.text !== '') {
    return {
      type: 'paragraph',
      styleRef: 'BodyText',
      children: [{ type: 'textRun', value: block.text }],
    }
  }
  return {
    type,
    children: block.children?.map(legacyBlockToNode) ?? [],
  }
}

/**
 * Normalizes legacy {"blocks":[]} payloads to the v1 {schemaVersion,nodes} shape.
 */
export function normalizeLegacyStructuredContent(parsed: unknown): StructuredContentDocument {
  if (!parsed || typeof parsed !== 'object') {
    return { schemaVersion: '1.0', nodes: [] }
  }

  const candidate = parsed as LegacyStructuredContent

  if (Array.isArray(candidate.nodes)) {
    return {
      schemaVersion: candidate.schemaVersion ?? '1.0',
      nodes: candidate.nodes,
    }
  }

  if (Array.isArray(candidate.blocks)) {
    return {
      schemaVersion: candidate.schemaVersion ?? '1.0',
      nodes: candidate.blocks.map(legacyBlockToNode),
    }
  }

  return { schemaVersion: candidate.schemaVersion ?? '1.0', nodes: [] }
}

export function normalizeStructuredContentJson(json: string): StructuredContentDocument {
  try {
    return normalizeLegacyStructuredContent(JSON.parse(json))
  } catch {
    return { schemaVersion: '1.0', nodes: [] }
  }
}
