<script setup lang="ts">
import type { PaletteGroupView, PaletteItem } from '@/composables/useCommandPalette'

defineProps<{
  groups: PaletteGroupView[]
  flatItems: PaletteItem[]
  highlightIndex: number
  showNoMatch: boolean
}>()

const emit = defineEmits<{
  highlight: [flatIndex: number]
  activate: [item: PaletteItem]
}>()

function flatIndexForItem(item: PaletteItem, flatItems: PaletteItem[]): number {
  return flatItems.findIndex((entry) => entry.id === item.id)
}
</script>

<template>
  <div
    id="command-palette-listbox"
    class="command-palette__results"
    role="listbox"
    :aria-label="$t('commandPalette.resultsLabel')"
  >
    <template v-for="group in groups" :key="group.id">
      <section
        v-if="group.items.length > 0 || group.errorMessageKey || group.loading"
        class="command-palette__group"
        :data-testid="`command-palette-group-${group.id}`"
      >
        <h2 class="command-palette__group-title">{{ $t(group.labelKey) }}</h2>
        <p
          v-if="group.loading"
          class="command-palette__status"
          data-testid="command-palette-group-loading"
        >
          {{ $t('commandPalette.loading') }}
        </p>
        <p
          v-if="group.errorMessageKey"
          class="command-palette__error"
          role="alert"
          data-testid="command-palette-group-error"
        >
          {{ $t(group.errorMessageKey) }}
        </p>
        <button
          v-for="item in group.items"
          :id="`command-palette-option-${item.id}`"
          :key="item.id"
          type="button"
          class="command-palette__option"
          role="option"
          :data-testid="item.optionTestId ?? 'command-palette-option'"
          :data-kind="item.kind"
          :aria-selected="flatIndexForItem(item, flatItems) === highlightIndex"
          :class="{
            'command-palette__option--active': flatIndexForItem(item, flatItems) === highlightIndex,
          }"
          @mouseenter="emit('highlight', flatIndexForItem(item, flatItems))"
          @focus="emit('highlight', flatIndexForItem(item, flatItems))"
          @click="emit('activate', item)"
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
      {{ $t('commandPalette.noMatch') }}
    </p>
  </div>
</template>

<style scoped lang="scss">
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
</style>
