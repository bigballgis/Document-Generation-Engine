/**
 * Structural peel: split locales/en.ts and locales/zh-CN.ts into domain modules.
 * Preserves exact object-literal text (no key/value rewrites).
 *
 * Usage: node frontend/scripts/peel-i18n-domains.mjs
 */
import { mkdirSync, readFileSync, writeFileSync, rmSync, existsSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const localesDir = resolve(__dirname, '../src/i18n/locales')
const domainsDir = join(localesDir, 'domains')

/** Nested templates.* keys grouped so each module stays well under 500 LOC. */
const TEMPLATE_GROUPS = {
  package: [
    'create',
    'list',
    'detail',
    'packageHub',
    'dependencies',
    'devEditor',
    'devWorkspace',
    'versionLines',
  ],
  releaseLifecycle: [
    'releaseDetail',
    'workflow',
    'status',
    'lifecycleStepper',
    'authoringPathGuide',
    'lifecycle',
    'governance',
    'versions',
    'metadata',
    'approvalMatrix',
    'documentBrandAllowList',
    'export',
  ],
  gates: [
    'import',
    'bindingGate',
    'submitGate',
    'publishGate',
    'publishSummary',
    'submitApprovalSummary',
    'testGenerate',
    'testPreview',
  ],
  authoring: ['authoring', 'structuredEditor', 'rules'],
  previewTest: [
    'preview',
    'previewHistory',
    'testDataSets',
    'coverage',
    'previewProgress',
    'batchTest',
    'batchTestHistory',
    'submitTestEligibility',
  ],
  clausesPolicy: [
    'contentModuleReferences',
    'clauseAuthoring',
    'changeDiff',
    'riskPrompt',
    'policy',
    'contract',
    'error',
    'deleteAction',
    'empty',
  ],
}

function findMatchingBrace(text, openIndex) {
  if (text[openIndex] !== '{') {
    throw new Error(`Expected '{' at ${openIndex}`)
  }
  let depth = 0
  let inSingle = false
  let inDouble = false
  let inTemplate = false
  let escape = false
  for (let i = openIndex; i < text.length; i++) {
    const ch = text[i]
    if (escape) {
      escape = false
      continue
    }
    if ((inSingle || inDouble || inTemplate) && ch === '\\') {
      escape = true
      continue
    }
    if (inSingle) {
      if (ch === "'") inSingle = false
      continue
    }
    if (inDouble) {
      if (ch === '"') inDouble = false
      continue
    }
    if (inTemplate) {
      if (ch === '`') inTemplate = false
      continue
    }
    if (ch === "'") {
      inSingle = true
      continue
    }
    if (ch === '"') {
      inDouble = true
      continue
    }
    if (ch === '`') {
      inTemplate = true
      continue
    }
    if (ch === '{') depth++
    else if (ch === '}') {
      depth--
      if (depth === 0) return i
    }
  }
  throw new Error('Unbalanced braces')
}

function extractTopLevelDomains(source) {
  const exportIdx = source.indexOf('export default')
  if (exportIdx < 0) throw new Error('export default not found')
  const rootOpen = source.indexOf('{', exportIdx)
  const rootClose = findMatchingBrace(source, rootOpen)
  const body = source.slice(rootOpen + 1, rootClose)

  const domains = []
  let i = 0
  while (i < body.length) {
    while (i < body.length && /\s/.test(body[i])) i++
    if (i >= body.length) break
    const keyMatch = body.slice(i).match(/^([a-zA-Z_][a-zA-Z0-9_]*)\s*:\s*/)
    if (!keyMatch) {
      throw new Error(`Expected domain key near: ${JSON.stringify(body.slice(i, i + 80))}`)
    }
    const name = keyMatch[1]
    i += keyMatch[0].length
    while (i < body.length && /\s/.test(body[i])) i++
    if (body[i] !== '{') {
      throw new Error(`Expected object for domain ${name}`)
    }
    const close = findMatchingBrace(body, i)
    const objectText = body.slice(i, close + 1)
    domains.push({ name, objectText })
    i = close + 1
    while (i < body.length && /[\s,]/.test(body[i])) i++
  }
  return domains
}

function extractNestedKeys(objectText, { allowIdentifierValues = false } = {}) {
  // objectText includes surrounding { }
  const inner = objectText.slice(1, -1)
  const keys = []
  let i = 0
  while (i < inner.length) {
    while (i < inner.length && /\s/.test(inner[i])) i++
    if (i >= inner.length) break
    const keyMatch = inner.slice(i).match(/^([a-zA-Z_][a-zA-Z0-9_]*)\s*:\s*/)
    if (!keyMatch) {
      throw new Error(`Expected nested key near: ${JSON.stringify(inner.slice(i, i + 80))}`)
    }
    const name = keyMatch[1]
    i += keyMatch[0].length
    while (i < inner.length && /\s/.test(inner[i])) i++
    if (inner[i] === '{') {
      const close = findMatchingBrace(inner, i)
      const objectBody = inner.slice(i, close + 1)
      keys.push({ name, kind: 'object', objectText: objectBody })
      i = close + 1
    } else if (allowIdentifierValues) {
      const idMatch = inner.slice(i).match(/^([a-zA-Z_][a-zA-Z0-9_]*)/)
      if (!idMatch) {
        throw new Error(`Expected identifier value for ${name}`)
      }
      keys.push({ name, kind: 'identifier', objectText: idMatch[1] })
      i += idMatch[1].length
    } else {
      throw new Error(`Expected object for nested key ${name}`)
    }
    while (i < inner.length && /[\s,]/.test(inner[i])) i++
  }
  return keys
}

function writeDomainFile(relPath, exportName, objectText, imports = []) {
  const abs = join(domainsDir, relPath)
  mkdirSync(dirname(abs), { recursive: true })
  const importBlock = imports.length ? `${imports.join('\n')}\n\n` : ''
  const content = `${importBlock}export const ${exportName} = ${objectText}\n`
  writeFileSync(abs, content, 'utf8')
  return abs
}

function peelLocale(localeFileName, { apiErrorImport, apiErrorSymbol, asConst }) {
  const sourcePath = join(localesDir, localeFileName)
  const source = readFileSync(sourcePath, 'utf8')
  const domains = extractTopLevelDomains(source)
  const localeTag = localeFileName.replace(/\.ts$/, '')

  const facadeImports = []
  const facadeEntries = []

  for (const domain of domains) {
    if (domain.name === 'api') {
      const nested = extractNestedKeys(domain.objectText, { allowIdentifierValues: true })
      const restParts = nested.filter((n) => n.name !== 'error' && n.kind === 'object')
      const restObject = `{\n${restParts
        .map((n) => `  ${n.name}: ${n.objectText},`)
        .join('\n')}\n}`
      const exportName = `apiMessages${localeTag === 'en' ? 'En' : 'ZhCn'}`
      const fileName = localeTag === 'en' ? 'apiMessages.en.ts' : 'apiMessages.zh-CN.ts'
      writeDomainFile(fileName, exportName, restObject)
      facadeImports.push(`import { ${exportName} } from './domains/${fileName.replace(/\.ts$/, '')}'`)
      facadeImports.push(apiErrorImport)
      facadeEntries.push(`  api: {\n    error: ${apiErrorSymbol},\n    ...${exportName},\n  }`)
      continue
    }

    if (domain.name === 'templates') {
      const nested = extractNestedKeys(domain.objectText)
      const byName = new Map(nested.map((n) => [n.name, n]))
      const groupExports = []
      for (const [groupName, keyNames] of Object.entries(TEMPLATE_GROUPS)) {
        const parts = keyNames.map((k) => {
          const found = byName.get(k)
          if (!found) throw new Error(`Missing templates.${k} in ${localeFileName}`)
          byName.delete(k)
          return `  ${k}: ${found.objectText},`
        })
        const exportName = `templates${groupName[0].toUpperCase()}${groupName.slice(1)}${localeTag === 'en' ? 'En' : 'ZhCn'}`
        const fileRel = `templates/${groupName}.${localeTag}.ts`
        writeDomainFile(fileRel, exportName, `{\n${parts.join('\n')}\n}`)
        groupExports.push({ groupName, exportName, fileRel })
      }
      if (byName.size > 0) {
        throw new Error(
          `Unassigned templates keys in ${localeFileName}: ${[...byName.keys()].join(', ')}`,
        )
      }
      const indexExport = `templates${localeTag === 'en' ? 'En' : 'ZhCn'}`
      const indexImports = groupExports
        .map(
          (g) =>
            `import { ${g.exportName} } from './${g.groupName}.${localeTag}'`,
        )
        .join('\n')
      const indexBody = `{\n${groupExports
        .map((g) => `  ...${g.exportName},`)
        .join('\n')}\n}`
      writeDomainFile(
        `templates/index.${localeTag}.ts`,
        indexExport,
        indexBody,
        indexImports.split('\n'),
      )
      facadeImports.push(
        `import { ${indexExport} } from './domains/templates/index.${localeTag}'`,
      )
      facadeEntries.push(`  templates: ${indexExport}`)
      continue
    }

    const exportName = `${domain.name}${localeTag === 'en' ? 'En' : 'ZhCn'}`
    const fileRel = `${domain.name}.${localeTag}.ts`
    writeDomainFile(fileRel, exportName, domain.objectText)
    facadeImports.push(`import { ${exportName} } from './domains/${domain.name}.${localeTag}'`)
    facadeEntries.push(`  ${domain.name}: ${exportName}`)
  }

  // Deduplicate apiError import if api handled it
  const uniqueImports = [...new Set(facadeImports)]
  const facade = `${uniqueImports.join('\n')}\n\nexport default {\n${facadeEntries.join(',\n')},\n}${asConst ? ' as const' : ''}\n`
  writeFileSync(sourcePath, facade, 'utf8')
  return { domainCount: domains.length, facadeLoc: facade.split(/\r?\n/).length }
}

function main() {
  if (existsSync(domainsDir)) {
    rmSync(domainsDir, { recursive: true, force: true })
  }
  mkdirSync(domainsDir, { recursive: true })

  const enResult = peelLocale('en.ts', {
    apiErrorImport: `import { apiErrorEn } from '@/i18n/catalogs/apiErrorEn'`,
    apiErrorSymbol: 'apiErrorEn',
    asConst: false,
  })
  const zhResult = peelLocale('zh-CN.ts', {
    apiErrorImport: `import { apiErrorZhCn } from '@/i18n/catalogs/apiErrorZhCn'`,
    apiErrorSymbol: 'apiErrorZhCn',
    asConst: true,
  })

  console.log('Peeled en:', enResult)
  console.log('Peeled zh-CN:', zhResult)
}

main()
