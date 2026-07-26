import { createHash } from 'node:crypto'
import { existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'
import { collectLeafKeys, resolveLeafValue } from '@/i18n/collectLeafKeys'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'

const __dirname = dirname(fileURLToPath(import.meta.url))
const localesDir = __dirname
const domainsDir = join(localesDir, 'domains')
const baselineDir = join(localesDir, '__baselines__')

/** Soft budget signals (review targets — not a harder CI SoT than quality-gate baseline). */
const SOFT_LOC_TARGET = 500
const HARD_LOC_BAND = 800
/** Thin facade allowance (compose/re-export only). */
const FACADE_SOFT_LOC = 80

function countLoc(filePath: string): number {
  return readFileSync(filePath, 'utf8').split(/\r?\n/).length
}

function collectLeafEntries(obj: Record<string, unknown>): Array<[string, string]> {
  const keys = collectLeafKeys(obj)
  return keys.map((key) => {
    const value = resolveLeafValue(obj, key)
    if (typeof value !== 'string') {
      throw new Error(`Expected string leaf at ${key}`)
    }
    return [key, value]
  })
}

function fingerprint(entries: Array<[string, string]>): { keyCount: number; sha256: string } {
  const payload = entries.map(([k, v]) => `${k}\u0000${v}`).join('\n')
  return {
    keyCount: entries.length,
    sha256: createHash('sha256').update(payload, 'utf8').digest('hex'),
  }
}

function listTsFilesRecursive(dir: string): string[] {
  if (!existsSync(dir)) {
    return []
  }
  const out: string[] = []
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) {
      out.push(...listTsFilesRecursive(full))
    } else if (entry.isFile() && entry.name.endsWith('.ts') && !entry.name.endsWith('.test.ts')) {
      out.push(full)
    }
  }
  return out.sort()
}

describe('AI-SCALE i18n locale domain split (I18N-01…04)', () => {
  it('preserves English leaf key set and string values (fingerprint)', () => {
    const entries = collectLeafEntries(en as Record<string, unknown>)
    const fp = fingerprint(entries)
    const baselinePath = join(baselineDir, 'en.leaf-fingerprint.json')

    const updateBaselines = process.env.UPDATE_I18N_BASELINES === '1'
    if (!existsSync(baselinePath) || updateBaselines) {
      mkdirSync(baselineDir, { recursive: true })
      writeFileSync(
        baselinePath,
        `${JSON.stringify({ ...fp, keys: entries.map(([k]) => k) }, null, 2)}\n`,
        'utf8',
      )
      writeFileSync(
        join(baselineDir, 'en.leaf-values.json'),
        `${JSON.stringify(Object.fromEntries(entries), null, 2)}\n`,
        'utf8',
      )
    }

    const baseline = JSON.parse(readFileSync(baselinePath, 'utf8')) as {
      keyCount: number
      sha256: string
    }
    expect(fp.keyCount).toBe(baseline.keyCount)
    expect(fp.sha256).toBe(baseline.sha256)

    const valuesPath = join(baselineDir, 'en.leaf-values.json')
    if (existsSync(valuesPath)) {
      const baselineValues = JSON.parse(readFileSync(valuesPath, 'utf8')) as Record<string, string>
      for (const [key, value] of entries) {
        expect(baselineValues[key], `value drift at ${key}`).toBe(value)
      }
      expect(Object.keys(baselineValues).sort()).toEqual(entries.map(([k]) => k))
    }
  })

  it('preserves zh-CN leaf key set and string values (fingerprint)', () => {
    const entries = collectLeafEntries(zhCN as Record<string, unknown>)
    const fp = fingerprint(entries)
    const baselinePath = join(baselineDir, 'zh-CN.leaf-fingerprint.json')

    const updateBaselines = process.env.UPDATE_I18N_BASELINES === '1'
    if (!existsSync(baselinePath) || updateBaselines) {
      mkdirSync(baselineDir, { recursive: true })
      writeFileSync(
        baselinePath,
        `${JSON.stringify({ ...fp, keys: entries.map(([k]) => k) }, null, 2)}\n`,
        'utf8',
      )
      writeFileSync(
        join(baselineDir, 'zh-CN.leaf-values.json'),
        `${JSON.stringify(Object.fromEntries(entries), null, 2)}\n`,
        'utf8',
      )
    }

    const baseline = JSON.parse(readFileSync(baselinePath, 'utf8')) as {
      keyCount: number
      sha256: string
    }
    expect(fp.keyCount).toBe(baseline.keyCount)
    expect(fp.sha256).toBe(baseline.sha256)

    const valuesPath = join(baselineDir, 'zh-CN.leaf-values.json')
    if (existsSync(valuesPath)) {
      const baselineValues = JSON.parse(readFileSync(valuesPath, 'utf8')) as Record<string, string>
      for (const [key, value] of entries) {
        expect(baselineValues[key], `value drift at ${key}`).toBe(value)
      }
      expect(Object.keys(baselineValues).sort()).toEqual(entries.map(([k]) => k))
    }
  })

  it('keeps zh-CN covering every English leaf key (parity direction)', () => {
    // Matches localeRegistry.test.ts contract: zh-CN ⊇ en (allowlist empty).
    // Pre-existing zh-only keys are out of scope for this structure-only peel.
    const enKeys = collectLeafKeys(en as Record<string, unknown>)
    const missing = enKeys.filter(
      (key) => typeof resolveLeafValue(zhCN as Record<string, unknown>, key) !== 'string',
    )
    expect(missing).toEqual([])
  })

  it('exposes thin public facades at locales/en.ts and locales/zh-CN.ts', () => {
    const enFacade = join(localesDir, 'en.ts')
    const zhFacade = join(localesDir, 'zh-CN.ts')
    expect(existsSync(enFacade)).toBe(true)
    expect(existsSync(zhFacade)).toBe(true)

    const enLoc = countLoc(enFacade)
    const zhLoc = countLoc(zhFacade)
    expect(enLoc, `en.ts facade LOC ${enLoc} should stay thin`).toBeLessThanOrEqual(FACADE_SOFT_LOC)
    expect(zhLoc, `zh-CN.ts facade LOC ${zhLoc} should stay thin`).toBeLessThanOrEqual(FACADE_SOFT_LOC)

    const enSrc = readFileSync(enFacade, 'utf8')
    const zhSrc = readFileSync(zhFacade, 'utf8')
    // Facades compose via relative ./domains/* (public import path remains @/i18n/locales/en).
    expect(enSrc).toMatch(/from '\.\/domains\//)
    expect(zhSrc).toMatch(/from '\.\/domains\//)
  })

  it('keeps domain modules under soft size budgets', () => {
    const domainFiles = listTsFilesRecursive(domainsDir)
    expect(domainFiles.length, 'expected domain modules under locales/domains/').toBeGreaterThan(0)

    const oversizedHard: Array<{ file: string; loc: number }> = []
    const softWarnings: Array<{ file: string; loc: number }> = []

    for (const file of domainFiles) {
      const loc = countLoc(file)
      const rel = relative(resolve(localesDir, '../..'), file).replaceAll('\\', '/')
      if (loc > HARD_LOC_BAND) {
        oversizedHard.push({ file: rel, loc })
      } else if (loc > SOFT_LOC_TARGET) {
        softWarnings.push({ file: rel, loc })
      }
    }

    expect(
      oversizedHard,
      `Domain files exceed hard band ${HARD_LOC_BAND}: ${JSON.stringify(oversizedHard)}`,
    ).toEqual([])

    // Soft target: prefer ≤500; allow brief soft overrun only if still well under hard band.
    // Fail the suite on soft overrun so peel stays well under 500 as required by handoff.
    expect(
      softWarnings,
      `Domain files exceed soft target ${SOFT_LOC_TARGET}: ${JSON.stringify(softWarnings)}`,
    ).toEqual([])
  })
})
