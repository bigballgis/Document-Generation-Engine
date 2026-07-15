<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ChangeDiffHumanReadableEntry, ChangeDiffSummary } from '@/types/template'

const props = defineProps<{
  changeDiffSummary: ChangeDiffSummary | null
  compact?: boolean
}>()

const { t } = useI18n()

const entries = computed<ChangeDiffHumanReadableEntry[]>(() => {
  const summary = props.changeDiffSummary
  if (!summary) {
    return []
  }
  if (summary.humanReadableEntries?.length) {
    return summary.humanReadableEntries
  }
  const fallback: ChangeDiffHumanReadableEntry[] = []
  const dimensions = summary.dimensions ?? []
  for (const dimension of dimensions) {
    for (const item of dimension.added ?? []) {
      fallback.push({ changeType: 'ADDED', path: item, summary: item })
    }
    for (const item of dimension.removed ?? []) {
      fallback.push({ changeType: 'REMOVED', path: item, summary: item })
    }
    for (const item of dimension.modified ?? []) {
      fallback.push({
        changeType: item.changeType || 'MODIFIED',
        path: item.key,
        summary: item.summary,
      })
    }
  }
  return fallback
})

const hasSemanticEntries = computed(() => entries.value.length > 0)
</script>

<template>
  <section class="change-diff-readable" data-testid="change-diff-human-readable">
    <h3 v-if="!compact">{{ t('templates.changeDiff.humanReadableTitle') }}</h3>
    <p v-if="!changeDiffSummary" class="change-diff-readable__empty">
      {{ t('templates.changeDiff.humanReadableUnavailable') }}
    </p>
    <p v-else-if="!hasSemanticEntries" class="change-diff-readable__empty">
      {{ t('templates.changeDiff.humanReadableEmpty') }}
    </p>
    <ul v-else class="change-diff-readable__list">
      <li v-for="(entry, index) in entries" :key="`${entry.path}-${entry.changeType}-${index}`">
        <el-tag size="small" class="change-diff-readable__tag">{{ entry.changeType }}</el-tag>
        <span>{{ entry.summary }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped lang="scss">
.change-diff-readable {
  margin-top: 0.75rem;

  h3 {
    margin: 0 0 0.5rem;
    font-size: 0.95rem;
  }
}

.change-diff-readable__empty {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.change-diff-readable__list {
  margin: 0;
  padding-left: 0;
  list-style: none;

  li {
    display: flex;
    align-items: flex-start;
    gap: 0.5rem;
    margin-bottom: 0.4rem;
    font-size: 0.9rem;
    line-height: 1.4;
  }
}

.change-diff-readable__tag {
  flex-shrink: 0;
  margin-top: 0.1rem;
}
</style>
