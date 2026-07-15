<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  detectDollarBracePrefix,
  filterVariableKeysForAutocomplete,
  insertVariableReference,
} from '@/utils/conditionExpressionAutocomplete'

const props = withDefaults(
  defineProps<{
    modelValue: string
    variableKeys: string[]
    readonly?: boolean
    placeholder?: string
    testId?: string
  }>(),
  {
    readonly: false,
    placeholder: '',
    testId: 'condition-expression-input',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  blur: []
}>()

const { t } = useI18n()
const inputRef = ref<{ textarea?: HTMLTextAreaElement; input?: HTMLInputElement } | null>(null)
const suggestionsOpen = ref(false)
const filterPrefix = ref('')
const caretIndex = ref(0)

const suggestions = computed(() =>
  filterVariableKeysForAutocomplete(props.variableKeys, filterPrefix.value),
)

function resolveNativeInput(): HTMLInputElement | HTMLTextAreaElement | null {
  const exposed = inputRef.value as unknown as {
    textarea?: HTMLTextAreaElement
    input?: HTMLInputElement
    $el?: HTMLElement
  } | null
  if (!exposed) {
    return null
  }
  if (exposed.textarea) {
    return exposed.textarea
  }
  if (exposed.input) {
    return exposed.input
  }
  const root = exposed.$el
  if (!root) {
    return null
  }
  return root.querySelector('textarea, input')
}

function syncCaretFromDom() {
  const native = resolveNativeInput()
  caretIndex.value = native?.selectionStart ?? props.modelValue.length
}

function openSuggestions(prefix = '') {
  if (props.readonly) {
    return
  }
  filterPrefix.value = prefix
  suggestionsOpen.value = true
}

function closeSuggestions() {
  suggestionsOpen.value = false
  filterPrefix.value = ''
}

function onInput(value: string) {
  emit('update:modelValue', value)
  void nextTick(() => {
    syncCaretFromDom()
    const detected = detectDollarBracePrefix(value, caretIndex.value)
    if (detected) {
      openSuggestions(detected.prefix)
    } else if (suggestionsOpen.value && !value.includes('${')) {
      closeSuggestions()
    }
  })
}

function applyVariable(variableKey: string) {
  syncCaretFromDom()
  const { nextValue, nextCaret } = insertVariableReference(
    props.modelValue,
    caretIndex.value,
    variableKey,
    { replaceDollarBracePrefix: true },
  )
  emit('update:modelValue', nextValue)
  closeSuggestions()
  void nextTick(() => {
    const native = resolveNativeInput()
    if (native) {
      native.focus()
      native.setSelectionRange(nextCaret, nextCaret)
      caretIndex.value = nextCaret
    }
  })
}

function onInsertVariableClick() {
  syncCaretFromDom()
  openSuggestions('')
}

function onBlur() {
  // Delay so suggestion click can register before close.
  window.setTimeout(() => {
    closeSuggestions()
    emit('blur')
  }, 150)
}
</script>

<template>
  <div class="condition-expression-input" data-testid="condition-expression-field">
    <div class="condition-expression-input__row">
      <el-input
        ref="inputRef"
        :model-value="modelValue"
        :readonly="readonly"
        :placeholder="placeholder"
        :data-testid="testId"
        @update:model-value="onInput(String($event))"
        @focus="syncCaretFromDom"
        @click="syncCaretFromDom"
        @keyup="syncCaretFromDom"
        @blur="onBlur"
      />
      <el-button
        v-if="!readonly"
        plain
        data-testid="insert-variable-button"
        @mousedown.prevent
        @click="onInsertVariableClick"
      >
        {{ t('templates.authoring.conditionExpression.insertVariable') }}
      </el-button>
    </div>
    <ul
      v-if="suggestionsOpen && !readonly"
      class="condition-expression-input__suggestions"
      data-testid="variable-autocomplete-list"
      role="listbox"
    >
      <li v-if="suggestions.length === 0" class="condition-expression-input__empty">
        {{ t('templates.authoring.conditionExpression.noSuggestions') }}
      </li>
      <li v-for="key in suggestions" :key="key" role="presentation">
        <button
          type="button"
          role="option"
          class="condition-expression-input__suggestion"
          :aria-selected="false"
          :data-testid="`variable-suggestion-${key}`"
          @mousedown.prevent
          @click="applyVariable(key)"
        >
          {{ key }}
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.condition-expression-input {
  width: 100%;
}

.condition-expression-input__row {
  display: flex;
  gap: var(--space-2, 8px);
  align-items: flex-start;
}

.condition-expression-input__row :deep(.el-input) {
  flex: 1;
}

.condition-expression-input__suggestions {
  list-style: none;
  margin: var(--space-1, 4px) 0 0;
  padding: var(--space-1, 4px) 0;
  max-height: 12rem;
  overflow: auto;
  border: 1px solid var(--color-border, #e4e7eb);
  border-radius: var(--radius-sm, 4px);
  background: var(--color-surface, #fff);
  box-shadow: var(--shadow-sm, 0 1px 2px rgb(0 0 0 / 8%));
}

.condition-expression-input__suggestion {
  display: block;
  width: 100%;
  border: 0;
  background: transparent;
  text-align: left;
  padding: var(--space-2, 8px) var(--space-3, 12px);
  cursor: pointer;
  color: var(--color-text, #1a1a1a);
  font: inherit;
}

.condition-expression-input__suggestion:hover {
  background: var(--color-surface-muted, #f5f7fa);
}

.condition-expression-input__empty {
  padding: var(--space-2, 8px) var(--space-3, 12px);
  color: var(--color-text-muted, #5c6670);
}
</style>
