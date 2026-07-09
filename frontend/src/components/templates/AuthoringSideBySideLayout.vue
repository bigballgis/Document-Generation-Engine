<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

const MD_BREAKPOINT_PX = 768

const { t } = useI18n()

const isStacked = ref(false)
const previewCollapsed = ref(false)
let mediaQuery: MediaQueryList | null = null

function syncLayout() {
  if (!mediaQuery) {
    return
  }
  isStacked.value = !mediaQuery.matches
  if (!isStacked.value) {
    previewCollapsed.value = false
  }
}

onMounted(() => {
  mediaQuery = window.matchMedia(`(min-width: ${MD_BREAKPOINT_PX}px)`)
  syncLayout()
  mediaQuery.addEventListener('change', syncLayout)
})

onUnmounted(() => {
  mediaQuery?.removeEventListener('change', syncLayout)
})

function togglePreviewCollapsed() {
  previewCollapsed.value = !previewCollapsed.value
}
</script>

<template>
  <div
    class="authoring-side-by-side"
    :class="{ 'authoring-side-by-side--stacked': isStacked }"
    data-testid="authoring-side-by-side-layout"
  >
    <div class="authoring-side-by-side__editor" data-testid="authoring-editor-pane">
      <slot name="editor" />
    </div>

    <div
      class="authoring-side-by-side__preview"
      :class="{ 'authoring-side-by-side__preview--collapsed': isStacked && previewCollapsed }"
      data-testid="authoring-preview-pane-slot"
    >
      <button
        v-if="isStacked"
        type="button"
        class="authoring-side-by-side__toggle"
        data-testid="authoring-preview-toggle"
        @click="togglePreviewCollapsed"
      >
        {{
          previewCollapsed
            ? t('templates.authoring.previewExpand')
            : t('templates.authoring.previewCollapse')
        }}
      </button>

      <div v-if="!isStacked || !previewCollapsed" class="authoring-side-by-side__preview-body">
        <slot name="preview" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.authoring-side-by-side {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-4);
  align-items: start;

  &--stacked {
    grid-template-columns: minmax(0, 1fr);
  }

  &__editor,
  &__preview {
    min-width: 0;
  }

  &__preview {
    border: 1px solid var(--border-color);
    border-radius: var(--radius-md);
    background: var(--surface-muted);
    padding: var(--space-3);
  }

  &__preview--collapsed {
    padding-bottom: var(--space-2);
  }

  &__preview-body {
    margin-top: var(--space-2);
  }

  &__toggle {
    display: inline-flex;
    align-items: center;
    width: 100%;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    background: var(--surface-card);
    color: var(--text-primary);
    font: inherit;
    font-weight: 600;
    cursor: pointer;
    transition: background-color var(--transition-base);

    &:hover {
      background: var(--brand-accent-soft);
    }

    &:focus-visible {
      outline: 2px solid var(--color-primary);
      outline-offset: 2px;
    }
  }
}
</style>
