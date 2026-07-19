import { describe, expect, it } from 'vitest'
import { buildTemplateListQuery } from '@/views/templates/templateListCatalogQuery'

describe('buildTemplateListQuery (IBL-E1)', () => {
  it('forwards exact locale filter with other catalog params', () => {
    expect(
      buildTemplateListQuery({
        searchQuery: 'letter',
        groupCode: 'RETAIL',
        statusFilter: 'DRAFT',
        localeFilter: 'en-US',
        activeWorkflowFilter: null,
        activeSortKey: 'nameAsc',
      }),
    ).toEqual({
      search: 'letter',
      groupCode: 'RETAIL',
      lifecycleStatus: 'DRAFT',
      approvalSubState: undefined,
      locale: 'en-US',
      sort: 'nameAsc',
    })
  })

  it('omits blank locale filter', () => {
    const query = buildTemplateListQuery({
      searchQuery: '',
      localeFilter: '   ',
      activeWorkflowFilter: null,
      activeSortKey: 'groupCodeAsc',
    })
    expect(query.locale).toBeUndefined()
  })
})
