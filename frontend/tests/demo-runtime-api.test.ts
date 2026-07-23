import { describe, expect, it } from 'vitest'

import { assertDocxArtifact, buildMinimalDocxArchive } from '@/utils/demoRuntimeArtifact'
import { DEMO_PUBLISH_EXTERNAL_IDS, DEMO_RUNTIME_MIN_DOCX_BYTES } from '@/utils/demoRuntimeRegistry'

describe('demoRuntimeRegistry', () => {
  it('defines 8 keep-set published demo external IDs', () => {
    expect(DEMO_PUBLISH_EXTERNAL_IDS).toEqual([
      'CORP-FOL-OFFER',
      'DEMO-CREDIT-LIMIT-CONFIRM',
      'DEMO-ANNUAL-REVIEW',
      'DEMO-FACILITY-RENEWAL',
      'DEMO-FACILITY-AMENDMENT',
      'DEMO-COMMITMENT-LETTER',
      'DEMO-FORMAL-DEMAND',
      'DEMO-COVENANT-WAIVER',
    ])
  })

  it('assigns a positive minBytes floor for every published demo', () => {
    for (const externalId of DEMO_PUBLISH_EXTERNAL_IDS) {
      expect(DEMO_RUNTIME_MIN_DOCX_BYTES[externalId]).toBeGreaterThan(0)
    }
  })

  it('uses calibrated size floors with FOL highest among keep-set', () => {
    expect(DEMO_RUNTIME_MIN_DOCX_BYTES['CORP-FOL-OFFER']).toBe(20_480)
    expect(DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-CREDIT-LIMIT-CONFIRM']).toBe(7_680)
    expect(DEMO_RUNTIME_MIN_DOCX_BYTES['DEMO-FACILITY-RENEWAL']).toBe(4_608)
    expect(DEMO_PUBLISH_EXTERNAL_IDS).not.toContain('DEMO-FULL-FLOW-LETTER')
  })
})

describe('assertDocxArtifact', () => {
  it('accepts a valid DOCX with required markers and size floor', () => {
    const docx = buildMinimalDocxArchive('Northgate Manufacturing CORP-CL-2026', 4_000)

    expect(() =>
      assertDocxArtifact(docx, {
        minBytes: 4_096,
        contentMarkers: ['Northgate Manufacturing', 'CORP-CL-2026'],
      }),
    ).not.toThrow()
  })

  it('rejects non-DOCX magic bytes', () => {
    expect(() =>
      assertDocxArtifact(Buffer.from('not-a-docx'), {
        minBytes: 1,
      }),
    ).toThrow(/not a valid DOCX/i)
  })

  it('rejects DOCX below the configured size floor', () => {
    const docx = buildMinimalDocxArchive('Harbour Logistics')

    expect(() =>
      assertDocxArtifact(docx, {
        minBytes: 8_192,
      }),
    ).toThrow(/DOCX too small/i)
  })

  it('rejects forbidden placeholder markers in document.xml', () => {
    const docx = buildMinimalDocxArchive('LOREM ipsum borrower text', 4_000)

    expect(() =>
      assertDocxArtifact(docx, {
        minBytes: 4_096,
      }),
    ).toThrow(/forbidden placeholder marker/i)
  })

  it('rejects missing content markers', () => {
    const docx = buildMinimalDocxArchive('generic bank letter body', 4_000)

    expect(() =>
      assertDocxArtifact(docx, {
        minBytes: 4_096,
        contentMarkers: ['Shanghai Apex'],
      }),
    ).toThrow(/missing expected content marker/i)
  })

  it('requires word/document.xml when requireDocumentXml is enabled', () => {
    const zipOnlyStyles = Buffer.from([0x50, 0x4b, 0x03, 0x04, ...Buffer.alloc(4_096, 0x20)])

    expect(() =>
      assertDocxArtifact(zipOnlyStyles, {
        minBytes: 4_096,
        requireDocumentXml: true,
      }),
    ).toThrow(/word\/document\.xml/i)
  })
})
