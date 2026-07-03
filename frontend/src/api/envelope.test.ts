import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import type { ApiEnvelope } from '@/types/session'
import { unwrapEnvelope } from '@/api/envelope'

const duplicatedUnwrapModules = [
  'templates.ts',
  'apiPolicy.ts',
  'masters.ts',
  'identity.ts',
  'audit.ts',
  'contentModules.ts',
  'collaboration.ts',
  'contract.ts',
  'riskPromptConfig.ts',
  'templateRiskPromptConfig.ts',
] as const

describe('unwrapEnvelope', () => {
  it('returns the result payload when present', () => {
    const envelope: ApiEnvelope<{ templateId: string }> = {
      metadata: { traceId: 'TRC-1' },
      result: { templateId: 'tpl-1' },
    }

    expect(unwrapEnvelope(envelope)).toEqual({ templateId: 'tpl-1' })
  })

  it('throws when the result payload is missing', () => {
    const envelope: ApiEnvelope<{ templateId: string }> = {
      metadata: { traceId: 'TRC-1' },
    }

    expect(() => unwrapEnvelope(envelope)).toThrow('API response missing result')
  })

  it('removes duplicated local unwrap helpers from shared api modules', () => {
    for (const moduleName of duplicatedUnwrapModules) {
      const source = readFileSync(resolve(process.cwd(), 'src/api', moduleName), 'utf8')

      expect(source).not.toContain('function unwrap<')
      expect(source).toContain("from '@/api/envelope'")
    }
  })
})
