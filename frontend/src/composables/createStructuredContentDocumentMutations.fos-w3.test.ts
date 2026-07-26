import { describe, expect, it } from 'vitest'
import { ref } from 'vue'
import { createStructuredContentDocumentMutations } from '@/composables/createStructuredContentDocumentMutations'
import { parseStructuredContent } from '@/utils/structuredContentNodes'
import type { NodePath } from '@/utils/structuredContentNodePath'

describe('createStructuredContentDocumentMutations FOS-W3', () => {
  function setup(
    json = JSON.stringify({
      schemaVersion: '1.0',
      nodes: [
        { type: 'paragraph', children: [{ type: 'textRun', value: 'one' }] },
        { type: 'paragraph', children: [{ type: 'textRun', value: 'two' }] },
        { type: 'paragraph', children: [{ type: 'textRun', value: 'three' }] },
      ],
    }),
  ) {
    const documentModel = ref(parseStructuredContent(json))
    const focusedPath = ref<NodePath | null>(null)
    const api = createStructuredContentDocumentMutations({
      documentModel,
      isReadonly: () => false,
      setPendingCoalesceKey: () => undefined,
      focusedPath,
    })
    return { documentModel, focusedPath, api }
  }

  it('FOS-W3-5: insertInline targets focused middle paragraph', () => {
    const { documentModel, focusedPath, api } = setup()
    focusedPath.value = [1]
    api.insertInline('variable', 'BodyText')
    expect(documentModel.value.nodes[0]?.children).toHaveLength(1)
    expect(documentModel.value.nodes[1]?.children?.some((c) => c.type === 'variable')).toBe(true)
    expect(documentModel.value.nodes[2]?.children).toHaveLength(1)
  })

  it('FOS-W3-3: applySelectedStyle updates only focused paragraph', () => {
    const { documentModel, focusedPath, api } = setup()
    focusedPath.value = [1]
    api.applySelectedStyle('BodyText', ['paragraph'])
    expect(documentModel.value.nodes[0]?.styleRef).toBeUndefined()
    expect(documentModel.value.nodes[1]?.styleRef).toBe('BodyText')
  })
})
