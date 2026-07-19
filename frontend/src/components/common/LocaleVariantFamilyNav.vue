<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { RouteLocationRaw } from 'vue-router'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import type { LocaleVariantSibling } from '@/api/localeVariantFamily'

defineProps<{
  currentLocale?: string | null
  siblings: LocaleVariantSibling[]
  loading?: boolean
  siblingLink: (id: string) => RouteLocationRaw | undefined
}>()

const { t } = useI18n()
</script>

<template>
  <section
    v-if="currentLocale || siblings.length > 0 || loading"
    class="locale-variant-family"
    data-testid="locale-variant-family-nav"
  >
    <h3 class="locale-variant-family__title">
      {{ t('localeVariantFamily.title') }}
    </h3>
    <p v-if="currentLocale" class="locale-variant-family__current">
      <span class="locale-variant-family__label">{{ t('localeVariantFamily.currentLocale') }}</span>
      <el-tag size="small" type="info" effect="plain" data-testid="locale-variant-current">
        {{ currentLocale }}
      </el-tag>
    </p>
    <el-skeleton v-if="loading" :rows="2" animated />
    <template v-else-if="siblings.length > 0">
      <p class="locale-variant-family__hint">{{ t('localeVariantFamily.siblingsHint') }}</p>
      <ul class="locale-variant-family__list">
        <li
          v-for="sibling in siblings"
          :key="sibling.id"
          class="locale-variant-family__item"
          data-testid="locale-variant-sibling"
        >
          <EntityLinkCell
            :label="sibling.name"
            :subtitle="`${sibling.code} · ${sibling.locale}`"
            :to="siblingLink(sibling.id)"
          />
          <el-tag size="small" effect="plain">{{ sibling.locale }}</el-tag>
        </li>
      </ul>
    </template>
    <p
      v-else-if="currentLocale"
      class="locale-variant-family__empty"
      data-testid="locale-variant-no-siblings"
    >
      {{ t('localeVariantFamily.noSiblings') }}
    </p>
  </section>
</template>

<style scoped lang="scss">
.locale-variant-family {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-color);
}

.locale-variant-family__title {
  margin: 0 0 var(--space-3);
  font-size: 1rem;
  font-weight: 600;
}

.locale-variant-family__current {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: 0 0 var(--space-3);
}

.locale-variant-family__label {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.locale-variant-family__hint,
.locale-variant-family__empty {
  margin: 0 0 var(--space-2);
  font-size: 0.85rem;
  color: var(--text-muted);
}

.locale-variant-family__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.locale-variant-family__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-2) 0;
}
</style>
