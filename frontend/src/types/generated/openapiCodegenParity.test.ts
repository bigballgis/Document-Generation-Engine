import { execSync } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('openapi-v1 codegen parity', () => {
  it('committed generated types match a fresh regen from docs/api/openapi-v1.yaml', () => {
    const frontendRoot = resolve(process.cwd())
    const specPath = resolve(frontendRoot, '../docs/api/openapi-v1.yaml')
    const committedPath = resolve(frontendRoot, 'src/types/generated/openapi-v1.ts')
    const tempDir = mkdtempSync(join(tmpdir(), 'openapi-codegen-parity-'))
    const tempOut = join(tempDir, 'openapi-v1.ts')

    try {
      execSync(`pnpm exec openapi-typescript "${specPath}" -o "${tempOut}"`, {
        cwd: frontendRoot,
        stdio: 'pipe',
      })

      const committed = readFileSync(committedPath, 'utf8')
      const regenerated = readFileSync(tempOut, 'utf8')

      expect(regenerated).toBe(committed)
    } finally {
      rmSync(tempDir, { recursive: true, force: true })
    }
  })
})
