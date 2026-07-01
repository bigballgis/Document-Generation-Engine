import { describe, expect, it } from 'vitest'
import {
  buildVariableSchemaTree,
  collectVariableTreeExpandKeys,
  filterVariableTree,
  humanizeVariableKeyForTest,
  splitVariableKeySegments,
} from '@/utils/variableSchemaTree'
import { resolveVariableDisplayName } from '@/utils/variableDisplayName'
import type { VariableSchema } from '@/types/template'

function varSchema(
  variableKey: string,
  variableType = 'TEXT',
  description: string | null = null,
): VariableSchema {
  return {
    variableKey,
    variableType,
    required: true,
    defaultValue: null,
    enumValues: [],
    description,
  }
}

describe('splitVariableKeySegments', () => {
  it('splits dot notation', () => {
    expect(splitVariableKeySegments('loan.facility.amount')).toEqual(['loan', 'facility', 'amount'])
  })

  it('splits camelCase keys', () => {
    expect(splitVariableKeySegments('borrowerLegalName')).toEqual(['borrower', 'LegalName'])
    expect(splitVariableKeySegments('cpItemDescription')).toEqual(['cpItem', 'Description'])
  })
})

describe('resolveVariableDisplayName', () => {
  it('prefers description and humanizes fallback keys', () => {
    expect(resolveVariableDisplayName(varSchema('borrowerLegalName', 'TEXT', 'Borrower legal name'))).toBe(
      'Borrower legal name',
    )
    expect(resolveVariableDisplayName(varSchema('borrowerLegalName'))).toBe('Borrower Legal Name')
  })
})

describe('buildVariableSchemaTree', () => {
  it('groups variables under shared prefixes with display labels', () => {
    const tree = buildVariableSchemaTree([
      varSchema('borrowerLegalName', 'TEXT', 'Borrower legal name'),
      varSchema('borrowerShortName'),
      varSchema('lenderName'),
    ])

    const borrower = tree.find((node) => node.label === 'borrower')
    expect(borrower?.displayLabel).toBe('Borrower')
    expect(borrower?.children?.[0]?.displayLabel).toBe('Borrower legal name')
    expect(borrower?.children?.[0]?.technicalKey).toBe('borrowerLegalName')
  })

  it('nests loop item fields under LIST containers', () => {
    const tree = buildVariableSchemaTree([
      varSchema('lenders', 'LIST', 'Syndicate lender roster'),
      varSchema('lenderName', 'TEXT', 'Lender legal name'),
      varSchema('lenderCommitment', 'AMOUNT', 'Lender commitment'),
    ])

    expect(tree).toHaveLength(1)
    expect(tree[0]?.containerType).toBe('LIST')
    expect(tree[0]?.displayLabel).toBe('Syndicate lender roster')
    expect(tree[0]?.children).toHaveLength(2)
    expect(
      tree[0]?.children?.some((child) => child.displayLabel === 'Lender legal name'),
    ).toBe(true)
  })
})

describe('filterVariableTree', () => {
  it('returns matching leaves and prunes empty folders', () => {
    const tree = buildVariableSchemaTree([
      varSchema('borrowerLegalName', 'TEXT', 'Borrower legal name'),
      varSchema('lenderName'),
    ])
    const filtered = filterVariableTree(tree, 'legal')
    expect(filtered).toHaveLength(1)
    expect(filtered[0]?.children?.[0]?.variable?.variableKey).toBe('borrowerLegalName')
  })
})

describe('collectVariableTreeExpandKeys', () => {
  it('collects folder node ids', () => {
    const tree = buildVariableSchemaTree([varSchema('borrowerLegalName'), varSchema('lenderName')])
    const keys = collectVariableTreeExpandKeys(tree)
    expect(keys).toContain('borrower')
    expect(keys).toContain('lender')
  })
})

describe('humanizeVariableKeyForTest', () => {
  it('title-cases camelCase segments', () => {
    expect(humanizeVariableKeyForTest('facilityCurrency')).toBe('Facility Currency')
  })
})
