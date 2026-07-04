import { computed, defineComponent, ref, type Ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus, { ElMessage } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import type { TemplateDetail } from '@/types/template'
import {
  useTemplatePolicyCredentials,
  type UseTemplatePolicyCredentialsOptions,
} from '@/views/templates/useTemplatePolicyCredentials'
import { useTemplatesStore } from '@/stores/templates'

const routerPush = vi.fn()
const confirmAction = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/composables/useConfirmAction', () => ({
  useConfirmAction: () => ({ confirmAction }),
}))

const capabilityRefs = {
  manageApiPolicy: ref(true),
}

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => capabilityRefs,
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  }
})

function makeTemplate(overrides: Partial<TemplateDetail> = {}): TemplateDetail {
  return {
    id: 'tpl-1',
    externalId: 'TPL-001',
    groupCode: 'RETAIL',
    name: 'Test template',
    description: null,
    masterId: 'master-1',
    lifecycleStatus: 'PUBLISHED',
    releaseVersion: '1.0.0',
    devVersionId: 'dev-1',
    devVersionNumber: 1,
    variables: [],
    bindings: [],
    rules: [],
    createdAt: '2026-06-23T10:00:00Z',
    updatedAt: '2026-06-23T10:00:00Z',
    ...overrides,
  }
}

function mountPolicyCredentials(
  templateRef: Ref<TemplateDetail | null>,
  pinia: ReturnType<typeof createPinia>,
  overrides: Partial<Omit<UseTemplatePolicyCredentialsOptions, 'templateId' | 'template'>> = {},
) {
  const templateId = computed(() => templateRef.value?.id ?? 'tpl-1')
  const errorMessage = computed(() => '')

  const Comp = defineComponent({
    setup() {
      const policy = useTemplatePolicyCredentials({
        templateId,
        template: computed(() => templateRef.value),
        errorMessage,
        ...overrides,
      })
      return { policy }
    },
    template: '<div></div>',
  })

  const wrapper = mount(Comp, {
    global: { plugins: [pinia, createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus] },
  })

  return {
    wrapper,
    policy: (wrapper.vm as { policy: ReturnType<typeof useTemplatePolicyCredentials> }).policy,
    store: useTemplatesStore(),
  }
}

describe('useTemplatePolicyCredentials', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    routerPush.mockReset()
    confirmAction.mockReset()
    confirmAction.mockResolvedValue(true)
    capabilityRefs.manageApiPolicy.value = true
    vi.mocked(ElMessage.success).mockReset()
    vi.mocked(ElMessage.error).mockReset()
  })

  it('showPolicyPanel is true when template is PUBLISHED and user can manage API policy', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'PUBLISHED' }))
    const { policy, wrapper } = mountPolicyCredentials(templateRef, pinia)
    expect(policy.showPolicyPanel.value).toBe(true)
    wrapper.unmount()
  })

  it('showPolicyPanel is false when template is not PUBLISHED', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    const { policy, wrapper } = mountPolicyCredentials(templateRef, pinia)
    expect(policy.showPolicyPanel.value).toBe(false)
    wrapper.unmount()
  })

  it('canPolicy reflects manageApiPolicy capability', () => {
    const templateRef = ref(makeTemplate())
    capabilityRefs.manageApiPolicy.value = false
    const { policy, wrapper } = mountPolicyCredentials(templateRef, pinia)
    expect(policy.canPolicy.value).toBe(false)
    wrapper.unmount()
  })

  it('loadPolicyData fetches policy and credentials', async () => {
    const templateRef = ref(makeTemplate())
    const { policy, store, wrapper } = mountPolicyCredentials(templateRef, pinia)
    const fetchApiPolicy = vi.spyOn(store, 'fetchApiPolicy').mockResolvedValue()
    const fetchCredentials = vi.spyOn(store, 'fetchCredentials').mockResolvedValue()

    await policy.loadPolicyData()
    await flushPromises()

    expect(fetchApiPolicy).toHaveBeenCalledWith('tpl-1')
    expect(fetchCredentials).toHaveBeenCalledWith('tpl-1')
    expect(policy.policyLoadFailed.value).toBe(false)
    wrapper.unmount()
  })

  it('loadPolicyData sets policyLoadFailed when fetch fails', async () => {
    const templateRef = ref(makeTemplate())
    const { policy, store, wrapper } = mountPolicyCredentials(templateRef, pinia)
    vi.spyOn(store, 'fetchApiPolicy').mockRejectedValue(new Error('network'))

    await policy.loadPolicyData()
    await flushPromises()

    expect(policy.policyLoadFailed.value).toBe(true)
    wrapper.unmount()
  })

  it('openApiPolicyConsole navigates to API policy detail path', () => {
    const templateRef = ref(makeTemplate())
    const { policy, wrapper } = mountPolicyCredentials(templateRef, pinia)

    policy.openApiPolicyConsole()

    expect(routerPush).toHaveBeenCalledWith('/api/policies/tpl-1')
    wrapper.unmount()
  })

  it('displayedCredentialSecret prefers lastCreatedCredential secret', () => {
    const templateRef = ref(makeTemplate())
    const { policy, store, wrapper } = mountPolicyCredentials(templateRef, pinia)
    store.lastCreatedCredential = {
      externalId: 'ext-1',
      secret: 'created-secret',
      credentialId: 'cred-1',
      status: 'ACTIVE',
      createdAt: '2026-06-23T10:00:00Z',
    }
    store.lastRotatedCredential = {
      credentialId: 'cred-2',
      externalId: 'ext-2',
      secret: 'rotated-secret',
    }

    expect(policy.displayedCredentialSecret.value).toBe('created-secret')
    wrapper.unmount()
  })

  it('displayedCredentialSecret falls back to lastRotatedCredential secret', () => {
    const templateRef = ref(makeTemplate())
    const { policy, store, wrapper } = mountPolicyCredentials(templateRef, pinia)
    store.lastCreatedCredential = null
    store.lastRotatedCredential = { credentialId: 'cred-2', externalId: 'ext-2', secret: 'rotated-secret' }

    expect(policy.displayedCredentialSecret.value).toBe('rotated-secret')
    wrapper.unmount()
  })

  it('openCredentialSecretDialog sets dialog state', () => {
    const templateRef = ref(makeTemplate())
    const { policy, wrapper } = mountPolicyCredentials(templateRef, pinia)

    policy.openCredentialSecretDialog('ext-99', 'secret-value')

    expect(policy.credentialSecretExternalId.value).toBe('ext-99')
    expect(policy.credentialSecretValue.value).toBe('secret-value')
    expect(policy.credentialSecretDialogVisible.value).toBe(true)
    wrapper.unmount()
  })

  it('handleCreateCredential creates credential and opens secret dialog', async () => {
    const templateRef = ref(makeTemplate())
    const { policy, store, wrapper } = mountPolicyCredentials(templateRef, pinia)
    vi.spyOn(store, 'createCredential').mockResolvedValue({
      externalId: 'ext-new',
      secret: 'new-secret',
      credentialId: 'cred-new',
      status: 'ACTIVE',
      createdAt: '2026-06-23T10:00:00Z',
    })

    await policy.handleCreateCredential()
    await flushPromises()

    expect(store.createCredential).toHaveBeenCalledWith('tpl-1')
    expect(policy.credentialSecretExternalId.value).toBe('ext-new')
    expect(policy.credentialSecretValue.value).toBe('new-secret')
    expect(policy.credentialSecretDialogVisible.value).toBe(true)
    expect(ElMessage.success).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('handleRotateCredential confirms, rotates, and opens secret dialog', async () => {
    const templateRef = ref(makeTemplate())
    const { policy, store, wrapper } = mountPolicyCredentials(templateRef, pinia)
    vi.spyOn(store, 'rotateCredential').mockResolvedValue({
      credentialId: 'cred-1',
      externalId: 'ext-1',
      secret: 'rotated-secret',
      rotatedAt: '2026-06-23T11:00:00Z',
    })

    await policy.handleRotateCredential('cred-1', 'ext-1')
    await flushPromises()

    expect(confirmAction).toHaveBeenCalled()
    expect(store.rotateCredential).toHaveBeenCalledWith('tpl-1', 'cred-1')
    expect(policy.credentialSecretValue.value).toBe('rotated-secret')
    expect(ElMessage.success).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('handleRotateCredential skips rotate when confirmation is cancelled', async () => {
    confirmAction.mockResolvedValue(false)
    const templateRef = ref(makeTemplate())
    const { policy, store, wrapper } = mountPolicyCredentials(templateRef, pinia)
    const rotateSpy = vi.spyOn(store, 'rotateCredential')

    await policy.handleRotateCredential('cred-1', 'ext-1')
    await flushPromises()

    expect(rotateSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('handleRevokeCredential confirms and revokes credential', async () => {
    const templateRef = ref(makeTemplate())
    const { policy, store, wrapper } = mountPolicyCredentials(templateRef, pinia)
    vi.spyOn(store, 'revokeCredential').mockResolvedValue()

    await policy.handleRevokeCredential('cred-1')
    await flushPromises()

    expect(confirmAction).toHaveBeenCalled()
    expect(store.revokeCredential).toHaveBeenCalledWith('tpl-1', 'cred-1')
    expect(ElMessage.success).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('handleRevokeCredential skips revoke when confirmation is cancelled', async () => {
    confirmAction.mockResolvedValue(false)
    const templateRef = ref(makeTemplate())
    const { policy, store, wrapper } = mountPolicyCredentials(templateRef, pinia)
    const revokeSpy = vi.spyOn(store, 'revokeCredential')

    await policy.handleRevokeCredential('cred-1')
    await flushPromises()

    expect(revokeSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('resetPolicyCredentialsTransientState clears policyLoadFailed', () => {
    const templateRef = ref(makeTemplate())
    const { policy, wrapper } = mountPolicyCredentials(templateRef, pinia)
    policy.policyLoadFailed.value = true

    policy.resetPolicyCredentialsTransientState()

    expect(policy.policyLoadFailed.value).toBe(false)
    wrapper.unmount()
  })
})
