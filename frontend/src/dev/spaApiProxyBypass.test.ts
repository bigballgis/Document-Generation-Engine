import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

import { bypassSpaApiRoutes } from './spaApiProxyBypass'

const thisDir = dirname(fileURLToPath(import.meta.url))
const frontendRoot = resolve(thisDir, '../..')
const repoRoot = resolve(frontendRoot, '..')

describe('bypassSpaApiRoutes (Vite hard-refresh)', () => {
  it('serves SPA index for /api/policies and /api/packages routes', () => {
    expect(bypassSpaApiRoutes('/api/policies')).toBe('/index.html')
    expect(bypassSpaApiRoutes('/api/policies/tpl-1')).toBe('/index.html')
    expect(bypassSpaApiRoutes('/api/packages/tpl-1/settings')).toBe('/index.html')
    expect(bypassSpaApiRoutes('/api/packages/tpl-1/settings?panel=domain')).toBe('/index.html')
  })

  it('does not bypass real management/runtime API paths', () => {
    expect(bypassSpaApiRoutes('/api/management/v1/templates')).toBeUndefined()
    expect(bypassSpaApiRoutes('/api/management/v1/session')).toBeUndefined()
    expect(bypassSpaApiRoutes('/api/v1/runtime/generate')).toBeUndefined()
  })
})

describe('nginx SPA /api exceptions (Critical #1 regression)', () => {
  it('docker nginx.conf try_files /api/policies and /api/packages before /api/ proxy', () => {
    const nginx = readFileSync(resolve(frontendRoot, 'nginx.conf'), 'utf8')
    expect(nginx).toMatch(/location\s+\^~\s+\/api\/policies\//)
    expect(nginx).toMatch(/location\s+\^~\s+\/api\/packages\//)
    expect(nginx).toMatch(/try_files\s+\$uri\s+\$uri\/\s+\/index\.html/)

    const policiesIdx = nginx.indexOf('location ^~ /api/policies/')
    const packagesIdx = nginx.indexOf('location ^~ /api/packages/')
    const apiProxyIdx = nginx.indexOf('location /api/ {')
    expect(policiesIdx).toBeGreaterThan(-1)
    expect(packagesIdx).toBeGreaterThan(-1)
    expect(apiProxyIdx).toBeGreaterThan(-1)
    expect(policiesIdx).toBeLessThan(apiProxyIdx)
    expect(packagesIdx).toBeLessThan(apiProxyIdx)
  })

  it('Helm frontend-nginx-configmap includes the same SPA exceptions', () => {
    const helm = readFileSync(
      resolve(repoRoot, 'deploy/helm/docgen/templates/frontend-nginx-configmap.yaml'),
      'utf8',
    )
    expect(helm).toMatch(/location\s+\^~\s+\/api\/policies\//)
    expect(helm).toMatch(/location\s+\^~\s+\/api\/packages\//)
    const policiesIdx = helm.indexOf('location ^~ /api/policies/')
    const packagesIdx = helm.indexOf('location ^~ /api/packages/')
    const apiProxyIdx = helm.indexOf('location /api/ {')
    expect(policiesIdx).toBeLessThan(apiProxyIdx)
    expect(packagesIdx).toBeLessThan(apiProxyIdx)
  })
})
