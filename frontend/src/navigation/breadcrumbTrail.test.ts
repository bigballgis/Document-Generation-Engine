import { describe, expect, it } from 'vitest'
import { buildBreadcrumbTrail } from '@/navigation/breadcrumbTrail'

describe('buildBreadcrumbTrail', () => {
  it('builds entitlement groups trail from path', () => {
    const trail = buildBreadcrumbTrail('/entitlement/groups')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.groups.entitlement',
      'nav.items.groups',
    ])
    expect(trail[1]?.path).toBe('/entitlement/groups')
  })

  it('builds template detail trail with list link', () => {
    const trail = buildBreadcrumbTrail('/templates/tpl-1')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.groups.content',
      'nav.items.templates',
      'nav.breadcrumb.detail',
    ])
    expect(trail[1]?.path).toBe('/templates')
  })
})
