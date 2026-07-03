import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const formatterTargets = [
  '../components/templates/TemplateTestDataSetPanel.vue',
  '../components/templates/TemplateReleaseVersionHistoryPanel.vue',
  '../views/masters/MasterRevisionDetailView.vue',
] as const

describe('locale formatter bypass regression', () => {
  it('avoids raw toLocaleString calls in the targeted UI surfaces', () => {
    for (const relativePath of formatterTargets) {
      const source = readFileSync(new URL(relativePath, import.meta.url), 'utf8')

      expect(source).not.toContain('toLocaleString(')
    }
  })
})
