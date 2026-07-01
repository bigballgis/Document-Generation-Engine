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

  it('builds template package hub trail with list link', () => {
    const trail = buildBreadcrumbTrail('/templates/tpl-1')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.groups.content',
      'nav.items.templates',
      'templates.packageHub.breadcrumbLabel',
    ])
    expect(trail[1]?.path).toBe('/templates')
  })

  it('builds template dev editor trail with hub link', () => {
    const trail = buildBreadcrumbTrail('/templates/tpl-1/dev/dev-2')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.groups.content',
      'nav.items.templates',
      'templates.packageHub.breadcrumbLabel',
      'templates.devEditor.breadcrumbLabel',
    ])
    expect(trail[2]?.path).toBe('/templates/tpl-1')
  })

  it('builds template release detail trail with hub link', () => {
    const trail = buildBreadcrumbTrail('/templates/tpl-1/releases/1.0.0')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.groups.content',
      'nav.items.templates',
      'templates.packageHub.breadcrumbLabel',
      'templates.releaseDetail.breadcrumbLabel',
    ])
    expect(trail[2]?.path).toBe('/templates/tpl-1')
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
