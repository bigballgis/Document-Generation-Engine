import { computed, type ComputedRef, type Ref } from 'vue'
import type { MasterStyleCatalog, VariableSchema } from '@/types/template'
import { buildVariableOptionLabel } from '@/utils/variableDisplayName'
import type { ControlledStructuredContentEditorProps } from '@/composables/controlledStructuredContentEditorTypes'

export function useControlledStructuredContentCatalogOptions(options: {
  props: ControlledStructuredContentEditorProps
  styleCatalog: Ref<MasterStyleCatalog | null>
  te: (key: string) => boolean
  t: (key: string) => string
}) {
  const { props, styleCatalog, te, t } = options

  const styleOptions = computed(() => styleCatalog.value?.entries ?? [])

  const clauseReferenceOptions = computed(() =>
    (props.contentModuleReferenceKeys ?? []).map((referenceKey) => ({
      value: referenceKey,
      label: referenceKey,
    })),
  )

  const variableCatalog = computed(() => {
    if (props.variables?.length) {
      return props.variables
    }
    return (props.variableKeys ?? []).map(
      (variableKey): VariableSchema => ({
        variableKey,
        variableType: 'TEXT',
        required: false,
        defaultValue: null,
        enumValues: null,
        description: null,
      }),
    )
  })

  const variableSelectOptions = computed(() =>
    variableCatalog.value.map((variable) => ({
      value: variable.variableKey,
      label: buildVariableOptionLabel(variable),
    })),
  )

  const listVariableOptions = computed(() =>
    variableCatalog.value
      .filter((variable) => variable.variableType === 'LIST' || variable.variableType === 'OBJECT')
      .map((variable) => ({
        value: variable.variableKey,
        label: buildVariableOptionLabel(variable),
      })),
  )

  function styleLabel(styleKey: string): string {
    const key = `templates.structuredEditor.styleCatalog.keys.${styleKey}`
    return te(key) ? t(key) : styleKey
  }

  return {
    styleOptions,
    clauseReferenceOptions,
    variableSelectOptions,
    listVariableOptions,
    styleLabel,
  }
}

export type ControlledStructuredContentCatalogOptions = ReturnType<
  typeof useControlledStructuredContentCatalogOptions
>

export type StyleOptionsRef = ComputedRef<MasterStyleCatalog['entries']>
