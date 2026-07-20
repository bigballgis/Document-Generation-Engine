import { describe, expect, it } from 'vitest'
import { NAV_GROUPS } from '@/navigation/navGroupsCatalog'
import { NAV_ICON_MAP } from '@/navigation/navIcons'

describe('navIcons contract (BDD-SYS-NORM-W1-005 / N10)', () => {
  it('maps an icon for every remaining nav catalog item', () => {
    const missing = NAV_GROUPS.flatMap((group) => group.items)
      .map((item) => item.id)
      .filter((id) => NAV_ICON_MAP[id] == null)

    expect(missing).toEqual([])
  })

  it('does not define icons for retired D1 nav surfaces', () => {
    expect(NAV_ICON_MAP['document-brands']).toBeUndefined()
    expect(NAV_ICON_MAP['legal-entities']).toBeUndefined()
  })
})
