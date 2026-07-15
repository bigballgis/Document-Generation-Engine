import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import en from '@/i18n/locales/en'
import TemplateTestDataSetEditDialog from '@/components/templates/TemplateTestDataSetEditDialog.vue'
import type { VariableSchema } from '@/types/template'

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue(true),
    },
  }
})

function schema(
  variableKey: string,
  variableType: VariableSchema['variableType'],
  overrides: Partial<VariableSchema> = {},
): VariableSchema {
  return {
    variableKey,
    variableType,
    required: false,
    defaultValue: null,
    enumValues: null,
    description: null,
    computeExpression: null,
    ...overrides,
  }
}

describe('TemplateTestDataSetEditDialog', () => {
  function mountDialog(variables: VariableSchema[], initialVariables: Record<string, unknown> = {}) {
    setActivePinia(createPinia())
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const form = {
      name: 'Sample set',
      description: '',
      scenarioName: '',
      required: false,
    }
    const wrapper = mount(TemplateTestDataSetEditDialog, {
      props: {
        modelValue: true,
        editingId: null,
        saving: false,
        form,
        coverageTagsText: '',
        variables,
        initialVariables,
      },
      global: {
        plugins: [createPinia(), i18n, ElementPlus],
      },
    })
    return { wrapper, form }
  }

  it('renders schema fields and skips compute (S1/S8)', async () => {
    const { wrapper } = mountDialog([
      schema('customerName', 'TEXT', { required: true }),
      schema('amount', 'AMOUNT'),
      schema('status', 'ENUM', { enumValues: 'ACTIVE,CLOSED' }),
      schema('principalCn', 'COMPUTED'),
    ])
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="schema-field-customerName"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="schema-field-amount"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="schema-field-status"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="schema-field-principalCn"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="generate-schema-skeleton"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="advanced-json-collapse"]').exists()).toBe(true)
  })

  it('blocks save when required field empty (S3)', async () => {
    const { wrapper } = mountDialog([schema('customerName', 'TEXT', { required: true })], {})
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="test-data-set-save"]').trigger('click')
    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.find('[data-testid="field-error-summary"]').exists()).toBe(true)
  })

  it('does not use Sample hardcode as create default (S5)', async () => {
    const { wrapper } = mountDialog([schema('customerName', 'TEXT', { defaultValue: 'Acme', required: true })])
    await wrapper.vm.$nextTick()
    const json = wrapper.find('[data-testid="advanced-json-editor"]').element as HTMLTextAreaElement
    // collapsed JSON may still be in DOM via collapse item
    const text = wrapper.html()
    expect(text).not.toContain('Sample')
    expect(json?.value ?? '').not.toContain('Sample')
  })

  it('generates a typed skeleton from schema defaults (S4)', async () => {
    const { wrapper } = mountDialog([
      schema('customerName', 'TEXT', { defaultValue: 'Acme', required: true }),
      schema('flag', 'BOOLEAN'),
    ])
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="generate-schema-skeleton"]').trigger('click')
    await wrapper.vm.$nextTick()
    const json = wrapper.find('[data-testid="advanced-json-editor"]').element as HTMLTextAreaElement
    expect(json?.value ?? '').toContain('Acme')
    expect(json?.value ?? '').toContain('false')
  })

  it('blocks save on invalid JSON syntax (S7)', async () => {
    const { wrapper } = mountDialog([schema('customerName', 'TEXT', { required: true })], {
      customerName: 'Acme',
    })
    await wrapper.vm.$nextTick()
    const editor = wrapper.find('[data-testid="advanced-json-editor"]')
    await editor.setValue('{not-json')
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="test-data-set-save"]').trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.find('[data-testid="field-error-summary"]').exists()).toBe(true)
  })

  it('blocks save on type mismatch (S2)', async () => {
    const { wrapper } = mountDialog(
      [schema('customerName', 'TEXT', { required: true }), schema('amount', 'AMOUNT')],
      { customerName: 'Acme', amount: 'not-a-number' },
    )
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="test-data-set-save"]').trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.find('[data-testid="field-error-summary"]').exists()).toBe(true)
  })

  it('emits save with stripped variables when payload is valid (S11 path)', async () => {
    const { wrapper } = mountDialog(
      [
        schema('customerName', 'TEXT', { required: true }),
        schema('principalCn', 'COMPUTED', { required: true }),
      ],
      { customerName: 'Acme', principalCn: 'ignored' },
    )
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="test-data-set-save"]').trigger('click')
    await wrapper.vm.$nextTick()
    const saveEvents = wrapper.emitted('save')
    expect(saveEvents).toBeTruthy()
    const payload = saveEvents![0]![0] as { variables: Record<string, unknown> }
    expect(payload.variables).toHaveProperty('customerName', 'Acme')
    expect(payload.variables).not.toHaveProperty('principalCn')
  })

  it('shows PII badge on PII-marked schema fields (BDD-CE-G03-012)', async () => {
    const { wrapper } = mountDialog([
      schema('customerName', 'TEXT', { piiCategory: 'PERSONAL_NAME', required: true }),
      schema('amount', 'AMOUNT'),
    ])
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="pii-badge-customerName"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="pii-badge-amount"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="pii-badge-customerName"]').text()).toContain('PII')
  })

  it('blocks save when PII values present without handling (BDD-CE-G03-013)', async () => {
    const { wrapper } = mountDialog(
      [schema('customerName', 'TEXT', { piiCategory: 'PERSONAL_NAME', required: true })],
      { customerName: 'Jane Doe' },
    )
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="pii-handling-group"]').exists()).toBe(true)
    ;(wrapper.vm as unknown as { clearPiiHandling: () => void }).clearPiiHandling()
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="test-data-set-save"]').trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.find('[data-testid="field-error-summary"]').exists()).toBe(true)
  })

  it('requires reason and secondary confirm for EXPLICIT_SENSITIVE (BDD-CE-G03-014)', async () => {
    const { wrapper } = mountDialog(
      [schema('customerName', 'TEXT', { piiCategory: 'PERSONAL_NAME', required: true })],
      { customerName: 'Jane Doe' },
    )
    await wrapper.vm.$nextTick()
    const api = wrapper.vm as unknown as {
      setPiiHandlingForTest: (value: string | null) => void
      setExplicitConfirmFieldsForTest: (reason: string, confirmed: boolean) => void
      explicitConfirmVisible: boolean
    }
    api.setPiiHandlingForTest('EXPLICIT_SENSITIVE')
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="test-data-set-save"]').trigger('click')
    await wrapper.vm.$nextTick()
    expect(api.explicitConfirmVisible).toBe(true)
    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.find('[data-testid="pii-explicit-confirm-dialog"]').exists()).toBe(true)

    await wrapper.find('[data-testid="pii-explicit-confirm-submit"]').trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('save')).toBeUndefined()

    api.setExplicitConfirmFieldsForTest('Approved synthetic-like sample for QA', true)
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="pii-explicit-confirm-submit"]').trigger('click')
    await wrapper.vm.$nextTick()
    const saveEvents = wrapper.emitted('save')
    expect(saveEvents).toBeTruthy()
    const payload = saveEvents![0]![0] as {
      variables: Record<string, unknown>
      piiHandling: string
      piiConfirmReason: string
      secondaryConfirmed: boolean
    }
    expect(payload.piiHandling).toBe('EXPLICIT_SENSITIVE')
    expect(payload.piiConfirmReason).toContain('QA')
    expect(payload.secondaryConfirmed).toBe(true)
    expect(payload.variables).toHaveProperty('customerName', 'Jane Doe')
  })

  it('emits SYNTHETIC handling when PII values saved with synthetic path', async () => {
    const { wrapper } = mountDialog(
      [schema('customerName', 'TEXT', { piiCategory: 'PERSONAL_NAME', required: true })],
      { customerName: 'SYNTH-001' },
    )
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="pii-handling-group"]').exists()).toBe(true)
    await wrapper.find('[data-testid="test-data-set-save"]').trigger('click')
    await wrapper.vm.$nextTick()
    const saveEvents = wrapper.emitted('save')
    expect(saveEvents).toBeTruthy()
    const payload = saveEvents![0]![0] as { piiHandling: string; variables: Record<string, unknown> }
    expect(payload.piiHandling).toBe('SYNTHETIC')
    expect(payload.variables.customerName).toBe('SYNTH-001')
  })

  it('maps server field errors onto the error summary (S9)', async () => {
    const { wrapper } = mountDialog([schema('customerName', 'TEXT', { required: true })], {
      customerName: 'Acme',
    })
    await wrapper.vm.$nextTick()
    await wrapper.setProps({
      serverFieldErrors: [{ field: 'customerName', reason: 'REQUIRED', message: 'required' }],
    })
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="field-error-summary"]').exists()).toBe(true)
    expect(wrapper.html()).toContain('customerName')
  })
})
