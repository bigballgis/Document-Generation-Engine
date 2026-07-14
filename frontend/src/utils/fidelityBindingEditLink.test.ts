import { describe, expect, it } from 'vitest'
import { buildFidelityBindingEditLink } from '@/utils/fidelityBindingEditLink'

describe('buildFidelityBindingEditLink', () => {
  it('builds design bindings deep link with anchorId', () => {
    expect(
      buildFidelityBindingEditLink({
        templateId: 'tpl-1',
        devVersionId: 'ver-1',
        anchorId: 'BODY',
      }),
    ).toBe('/templates/tpl-1/dev/ver-1?workspaceTab=design&designTab=bindings&anchorId=BODY')
  })

  it('returns null when anchorId is missing', () => {
    expect(
      buildFidelityBindingEditLink({
        templateId: 'tpl-1',
        devVersionId: 'ver-1',
        anchorId: null,
      }),
    ).toBeNull()
  })
})
