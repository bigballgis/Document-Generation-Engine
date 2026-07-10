<script setup lang="ts">
import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'

const props = withDefaults(
  defineProps<{
    label: string
    subtitle?: string
    to?: RouteLocationRaw
    disabled?: boolean
  }>(),
  {
    disabled: false,
  },
)

const isLink = computed(() => Boolean(props.to) && !props.disabled)
</script>

<template>
  <div class="entity-link-cell">
    <router-link
      v-if="isLink"
      :to="to!"
      class="entity-link-cell__link"
    >
      {{ label }}
    </router-link>
    <span
      v-else
      class="entity-link-cell__text"
    >
      {{ label }}
    </span>
    <span
      v-if="subtitle"
      class="entity-link-cell__subtitle"
    >
      {{ subtitle }}
    </span>
  </div>
</template>

<style scoped lang="scss">
.entity-link-cell {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.entity-link-cell__link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;

  &:hover {
    color: var(--brand-primary-hover, var(--color-primary));
    text-decoration: underline;
  }

  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: var(--focus-ring-offset);
    border-radius: 2px;
  }
}

.entity-link-cell__text {
  color: var(--text-primary, #1a1a1a);
  font-weight: 500;
}

.entity-link-cell__subtitle {
  color: var(--text-muted);
  font-size: var(--font-size-sm, 12px);
  line-height: 1.4;
}
</style>
