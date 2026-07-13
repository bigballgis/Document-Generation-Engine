<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import CommandPaletteResults from '@/components/layout/CommandPaletteResults.vue'
import { useCommandPalette, type PaletteItem } from '@/composables/useCommandPalette'
import { useCommandPaletteDialogFocus } from '@/composables/useCommandPaletteDialogFocus'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const router = useRouter()
const sessionStore = useSessionStore()

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

const { dialogRef, inputRef } = useCommandPaletteDialogFocus({ open, focusNonce })

function onBackdropClick() {
  closePalette()
}

function onOptionHighlight(flatIndex: number) {
  highlightIndex.value = flatIndex
}

async function onOptionActivate(item: PaletteItem) {
  await activateItem(item)
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
      <CommandPaletteResults
        :groups="groups"
        :flat-items="flatItems"
        :highlight-index="highlightIndex"
        :show-no-match="showNoMatch"
        @highlight="onOptionHighlight"
        @activate="onOptionActivate"
      />
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

.command-palette__hint {
  margin: 0;
  padding: var(--space-2) var(--space-5);
  border-top: 1px solid var(--border-default);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
</style>
