<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useCommandPalette, type PaletteItem } from '@/composables/useCommandPalette'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const router = useRouter()
const sessionStore = useSessionStore()

const dialogRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)

const visibleRoutes = computed(() => sessionStore.session?.visibleRoutes ?? [])
const roles = computed(() => sessionStore.session?.roles ?? [])
const capabilities = computed(() => sessionStore.session?.capabilities)

const palette = useCommandPalette({
  visibleRoutes,
  roles,
  capabilities,
  translate: (key) => t(key),
  navigate: (target) => {
    void router.push(target)
  },
})

const {
  open,
  query,
  highlightIndex,
  groups,
  flatItems,
  showNoMatch,
  focusNonce,
  openPalette,
  closePalette,
  setQuery,
  activateItem,
} = palette

watch(open, async (isOpen) => {
  if (!isOpen) {
    if (dialogRef.value) {
      dialogRef.value.removeEventListener('keydown', onDialogKeydown)
    }
    return
  }
  await nextTick()
  inputRef.value?.focus()
  dialogRef.value?.addEventListener('keydown', onDialogKeydown)
})

watch(focusNonce, async () => {
  if (!open.value) {
    return
  }
  await nextTick()
  inputRef.value?.focus()
})

onUnmounted(() => {
  dialogRef.value?.removeEventListener('keydown', onDialogKeydown)
})

function onBackdropClick() {
  closePalette()
}

function onOptionHighlight(flatIndex: number) {
  highlightIndex.value = flatIndex
}

async function onOptionActivate(item: PaletteItem) {
  await activateItem(item)
}

function collectFocusable(root: HTMLElement): HTMLElement[] {
  const nodes = root.querySelectorAll<HTMLElement>(
    'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
  )
  return Array.from(nodes).filter((el) => !el.hasAttribute('disabled') && el.tabIndex !== -1)
}

function onDialogKeydown(event: KeyboardEvent) {
  if (event.key !== 'Tab' || !dialogRef.value) {
    return
  }
  const focusables = collectFocusable(dialogRef.value)
  if (focusables.length === 0) {
    event.preventDefault()
    return
  }
  const first = focusables[0]!
  const last = focusables[focusables.length - 1]!
  const active = document.activeElement as HTMLElement | null
  if (event.shiftKey) {
    if (active === first || !dialogRef.value.contains(active)) {
      event.preventDefault()
      last.focus()
    }
  } else if (active === last) {
    event.preventDefault()
    first.focus()
  }
}

function flatIndexForItem(item: PaletteItem): number {
  return flatItems.value.findIndex((entry) => entry.id === item.id)
}

defineExpose({
  open,
  openPalette,
  closePalette,
  setQuery,
})
</script>

<template>
  <div v-if="open" class="command-palette-layer">
    <button
      type="button"
      class="command-palette-backdrop"
      data-testid="command-palette-backdrop"
      :aria-label="t('commandPalette.close')"
      @click="onBackdropClick"
    />
    <div
      ref="dialogRef"
      class="command-palette"
      role="dialog"
      aria-modal="true"
      tabindex="-1"
      :aria-label="t('commandPalette.dialogLabel')"
      data-testid="command-palette"
    >
      <input
        id="command-palette-input"
        ref="inputRef"
        class="command-palette__input"
        type="search"
        autocomplete="off"
        spellcheck="false"
        data-testid="command-palette-input"
        :aria-label="t('commandPalette.searchLabel')"
        :placeholder="t('commandPalette.placeholder')"
        :value="query"
        :aria-activedescendant="
          highlightIndex >= 0 && flatItems[highlightIndex]
            ? `command-palette-option-${flatItems[highlightIndex]!.id}`
            : undefined
        "
        aria-autocomplete="list"
        aria-controls="command-palette-listbox"
        @input="setQuery(($event.target as HTMLInputElement).value)"
      />
      <div
        id="command-palette-listbox"
        class="command-palette__results"
        role="listbox"
        :aria-label="t('commandPalette.resultsLabel')"
      >
        <template v-for="group in groups" :key="group.id">
          <section
            v-if="group.items.length > 0 || group.errorMessageKey || group.loading"
            class="command-palette__group"
            :data-testid="`command-palette-group-${group.id}`"
          >
            <h2 class="command-palette__group-title">{{ t(group.labelKey) }}</h2>
            <p
              v-if="group.loading"
              class="command-palette__status"
              data-testid="command-palette-group-loading"
            >
              {{ t('commandPalette.loading') }}
            </p>
            <p
              v-if="group.errorMessageKey"
              class="command-palette__error"
              role="alert"
              data-testid="command-palette-group-error"
            >
              {{ t(group.errorMessageKey) }}
            </p>
            <button
              v-for="item in group.items"
              :id="`command-palette-option-${item.id}`"
              :key="item.id"
              type="button"
              class="command-palette__option"
              role="option"
              data-testid="command-palette-option"
              :data-kind="item.kind"
              :aria-selected="flatIndexForItem(item) === highlightIndex"
              :class="{
                'command-palette__option--active': flatIndexForItem(item) === highlightIndex,
              }"
              @mouseenter="onOptionHighlight(flatIndexForItem(item))"
              @focus="onOptionHighlight(flatIndexForItem(item))"
              @click="onOptionActivate(item)"
            >
              <span class="command-palette__option-title">{{ item.title }}</span>
              <span class="command-palette__option-subtitle">{{ item.subtitle }}</span>
            </button>
          </section>
        </template>

        <p
          v-if="showNoMatch"
          class="command-palette__empty"
          data-testid="command-palette-no-match"
        >
          {{ t('commandPalette.noMatch') }}
        </p>
      </div>

      <p class="command-palette__hint">{{ t('commandPalette.hint') }}</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.command-palette-layer {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: var(--space-8) var(--space-4) var(--space-4);
}

.command-palette-backdrop {
  position: absolute;
  inset: 0;
  margin: 0;
  padding: 0;
  border: none;
  cursor: pointer;
  background: color-mix(in srgb, var(--text-primary) 42%, transparent);
}

.command-palette {
  position: relative;
  z-index: 1;
  width: min(640px, 100%);
  max-height: min(72vh, 560px);
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-lg);
  overflow: hidden;

  &:focus {
    outline: none;
  }
}

.command-palette__input {
  width: 100%;
  box-sizing: border-box;
  padding: var(--space-4) var(--space-5);
  border: none;
  border-bottom: 1px solid var(--border-default);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--font-size-lg);
  font-weight: 550;

  &:focus {
    outline: none;
  }

  &:focus-visible {
    box-shadow: inset 0 0 0 var(--focus-ring-width) var(--focus-ring-color);
  }

  &::placeholder {
    color: var(--text-secondary);
    font-weight: 400;
  }
}

.command-palette__results {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-2) 0;
}

.command-palette__group-title {
  margin: var(--space-2) var(--space-5) var(--space-1);
  font-size: var(--font-size-xs);
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
    color: var(--text-secondary);
}

.command-palette__option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.15rem;
  width: 100%;
  padding: var(--space-3) var(--space-5);
  border: none;
  background: transparent;
  text-align: left;
  font: inherit;
  color: var(--text-primary);
  cursor: pointer;

  &:hover,
  &--active {
    background: color-mix(in srgb, var(--brand-primary) 8%, var(--surface-card));
  }

  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: calc(-1 * var(--focus-ring-offset));
  }
}

.command-palette__option-title {
  font-weight: 600;
  line-height: 1.3;
}

.command-palette__option-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.3;
}

.command-palette__status,
.command-palette__error,
.command-palette__empty {
  margin: var(--space-2) var(--space-5);
  font-size: var(--font-size-sm);
}

.command-palette__error {
  color: var(--status-danger-text);
}

.command-palette__empty,
.command-palette__status {
  color: var(--text-secondary);
}

.command-palette__hint {
  margin: 0;
  padding: var(--space-2) var(--space-5);
  border-top: 1px solid var(--border-default);
  font-size: var(--font-size-xs);
    color: var(--text-secondary);
}
</style>
