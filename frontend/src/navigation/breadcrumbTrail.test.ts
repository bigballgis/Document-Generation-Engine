import { describe, expect, it } from 'vitest'
import { buildBreadcrumbTrail } from '@/navigation/breadcrumbTrail'

describe('buildBreadcrumbTrail', () => {
  it('returns empty trail for top-level list pages', () => {
    expect(buildBreadcrumbTrail('/masters')).toEqual([])
    expect(buildBreadcrumbTrail('/templates')).toEqual([])
    expect(buildBreadcrumbTrail('/entitlement/groups')).toEqual([])
  })

  it('builds master package hub trail without nav group segment', () => {
    const trail = buildBreadcrumbTrail('/masters/master-1')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.items.masters',
      'masters.hub.breadcrumbLabel',
    ])
    expect(trail[0]?.path).toBe('/masters')
  })

  it('builds template package hub trail with list link', () => {
    const trail = buildBreadcrumbTrail('/templates/tpl-1')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.items.templates',
      'templates.packageHub.breadcrumbLabel',
    ])
    expect(trail[0]?.path).toBe('/templates')
  })

  it('builds template dev editor trail with hub link', () => {
    const trail = buildBreadcrumbTrail('/templates/tpl-1/dev/dev-2')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.items.templates',
      'templates.packageHub.breadcrumbLabel',
      'templates.devEditor.breadcrumbLabel',
    ])
    expect(trail[1]?.path).toBe('/templates/tpl-1')
  })

  it('builds master revision detail trail with hub link', () => {
    const trail = buildBreadcrumbTrail('/masters/master-1/revisions/revision-1')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.items.masters',
      'masters.hub.breadcrumbLabel',
      'masters.revision.breadcrumbLabel',
    ])
    expect(trail[1]?.path).toBe('/masters/master-1')
  })

  it('FOS-W2-5: builds API package settings trail via templates hub', () => {
    const trail = buildBreadcrumbTrail('/api/packages/tpl-1/settings')

    expect(trail.map((segment) => segment.labelKey)).toEqual([
      'nav.items.templates',
      'templates.packageHub.breadcrumbLabel',
      'apiPolicy.packageSettings.breadcrumbLabel',
    ])
    expect(trail[0]?.path).toBe('/templates')
    expect(trail[1]?.path).toBe('/templates/tpl-1')
  })
})
