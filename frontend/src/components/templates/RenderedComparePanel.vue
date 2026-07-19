<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import RenderedComparePane from '@/components/templates/RenderedComparePane.vue'
import type { PreviewRunSummary } from '@/types/template'

defineProps<{
  templateId: string
  runA: PreviewRunSummary
  runB: PreviewRunSummary
}>()

const { t } = useI18n()
</script>

<template>
  <section
    class="rendered-compare-panel"
    data-testid="rendered-compare-panel"
    :aria-label="t('templates.previewHistory.renderedCompare.title')"
  >
    <header class="rendered-compare-panel__header">
      <h3>{{ t('templates.previewHistory.renderedCompare.title') }}</h3>
      <p>{{ t('templates.previewHistory.renderedCompare.description') }}</p>
    </header>

    <div class="rendered-compare-panel__panes">
      <RenderedComparePane
        :template-id="templateId"
        :run="runA"
        pane-test-id="rendered-compare-pane-a"
      />
      <RenderedComparePane
        :template-id="templateId"
        :run="runB"
        pane-test-id="rendered-compare-pane-b"
      />
    </div>
  </section>
</template>

<style scoped lang="scss">
.rendered-compare-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);

  &__header {
    h3 {
      margin: 0 0 var(--space-1);
      font-size: var(--font-size-lg);
      color: var(--text-primary);
    }

    p {
      margin: 0;
      font-size: var(--font-size-sm);
      color: var(--text-muted);
    }
  }

  &__panes {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-4);
    align-items: start;

    @media (max-width: 960px) {
      grid-template-columns: 1fr;
    }
  }
}
</style>
