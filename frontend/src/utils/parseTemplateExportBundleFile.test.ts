import { describe, expect, it } from 'vitest'
import {
  parseTemplateExportBundleFile,
  TEMPLATE_EXPORT_BUNDLE_FORMAT,
  TemplateExportBundleParseError,
} from '@/utils/parseTemplateExportBundleFile'

const sampleBundle = {
  format: TEMPLATE_EXPORT_BUNDLE_FORMAT,
  metadata: {
    templateId: '11111111-1111-1111-1111-111111111111',
    externalId: 'TPL-IMPORT',
    groupCode: 'RETAIL',
    name: 'Import sample',
    description: 'Sample',
    masterId: '22222222-2222-2222-2222-222222222222',
    lifecycleStatus: 'PUBLISHED',
    releaseVersion: '1.0.0',
    devVersionId: '33333333-3333-3333-3333-333333333333',
    devVersionNumber: 1,
    exportedAt: '2026-06-26T00:00:00Z',
  },
  variables: [],
  bindings: [],
  rules: [],
  contentModuleReferences: [],
  policySnapshot: null,
}

function createJsonFile(content: unknown, name = 'bundle.json'): File {
  const serialized = JSON.stringify(content)
  return {
    name,
    text: async () => serialized,
    arrayBuffer: async () => new TextEncoder().encode(serialized).buffer,
  } as unknown as File
}

describe('parseTemplateExportBundleFile', () => {
  it('parses a raw JSON bundle file', async () => {
    const file = createJsonFile(sampleBundle)
    const parsed = await parseTemplateExportBundleFile(file)
    expect(parsed.metadata.externalId).toBe('TPL-IMPORT')
  })

  it('parses an export envelope JSON file', async () => {
    const file = createJsonFile({ format: TEMPLATE_EXPORT_BUNDLE_FORMAT, bundle: sampleBundle }, 'bundle.json')
    const parsed = await parseTemplateExportBundleFile(file)
    expect(parsed.metadata.name).toBe('Import sample')
  })

  it('rejects unsupported file extensions', async () => {
    const file = new File(['not-json'], 'bundle.txt', { type: 'text/plain' })
    await expect(parseTemplateExportBundleFile(file)).rejects.toBeInstanceOf(TemplateExportBundleParseError)
  })

  it('parses a v2 JSON bundle file', async () => {
    const file = createJsonFile({
      ...sampleBundle,
      format: 'template-export-bundle-v2-json',
    })
    const parsed = await parseTemplateExportBundleFile(file)
    expect(parsed.format).toBe('template-export-bundle-v2-json')
    expect(parsed.metadata.externalId).toBe('TPL-IMPORT')
  })
})
