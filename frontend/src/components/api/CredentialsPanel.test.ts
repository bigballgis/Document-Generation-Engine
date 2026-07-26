import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it, vi } from 'vitest'
import CredentialsPanel from '@/components/api/CredentialsPanel.vue'
import en from '@/i18n/locales/en'
import type { ApiCredentialSummary } from '@/types/template'

const sampleCredential: ApiCredentialSummary = {
  credentialId: 'cred-1',
  externalId: 'EXT-CRED-001',
  status: 'ACTIVE',
  createdAt: '2026-06-23T10:00:00Z',
  revokedAt: null,
  expiresAt: '2026-12-20T10:00:00Z',
}

function mountPanel() {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  const credentialColumnFilters = { externalId: '', status: '', createdAt: '' }
  const currentPage = 1

  return mount(CredentialsPanel, {
    props: {
      credentials: [sampleCredential],
      credentialColumnFilters,
      currentPage,
      credentialStatusFilterOptions: [{ label: 'Active', value: 'ACTIVE' }],
      pageSize: 20,
      totalRows: 1,
      showCreateButton: true,
      formatDateTime: (value: string) => value,
      sortByCreatedAt: () => 0,
      'onUpdate:credentialColumnFilters': (value: Record<string, string>) => {
        Object.assign(credentialColumnFilters, value)
      },
      'onUpdate:currentPage': (value: number) => {
        currentPage.valueOf()
        void value
      },
    },
    global: { plugins: [i18n, ElementPlus] },
    attachTo: document.body,
  })
}

describe('CredentialsPanel', () => {
  it('renders credential rows and create action', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('EXT-CRED-001')
    // The create button is in a v-if block; check via element query
    expect(document.body.textContent).toContain('Create access key')
  })

  it('opens secret dialog when revealSecret is called', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const panel = wrapper.vm as unknown as { revealSecret: (id: string, secret: string) => void }
    panel.revealSecret('EXT-CRED-001', 'super-secret-value')
    await flushPromises()

    // el-dialog teleports to document.body; query from there
    const textarea = document.body.querySelector('textarea')
    expect(textarea).not.toBeNull()
    expect((textarea as HTMLTextAreaElement).value).toBe('super-secret-value')
    expect(document.body.textContent).toContain('Copy the secret now')
  })
})

  it('copies secret from dialog', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    })
    const wrapper = mountPanel()
    await flushPromises()
    const panel = wrapper.vm as unknown as {
      revealSecret: (id: string, secret: string) => void
    }
    panel.revealSecret('EXT-CRED-001', 'super-secret-value')
    await flushPromises()
    const buttons = Array.from(document.body.querySelectorAll('button'))
    const copyBtn = buttons.find((b) => b.textContent?.includes('Copy secret'))
    expect(copyBtn).toBeTruthy()
    await copyBtn!.click()
    await flushPromises()
    expect(writeText).toHaveBeenCalledWith('super-secret-value')
  })
