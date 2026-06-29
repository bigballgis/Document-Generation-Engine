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

  it('builds master package hub trail with list link', () => {
    const trail = buildBreadcrumbTrail('/masters/master-1')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.groups.content',
      'nav.items.masters',
      'masters.hub.breadcrumbLabel',
    ])
    expect(trail[1]?.path).toBe('/masters')
  })

  it('builds master revision detail trail with hub link', () => {
    const trail = buildBreadcrumbTrail('/masters/master-1/revisions/revision-1')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.groups.content',
      'nav.items.masters',
      'masters.hub.breadcrumbLabel',
      'masters.revision.breadcrumbLabel',
    ])
    expect(trail[2]?.path).toBe('/masters/master-1')
  })
})
