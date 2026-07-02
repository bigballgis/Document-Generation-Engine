<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
    showBack?: boolean
    backLabel?: string
  }>(),
  {
    description: undefined,
    showBack: false,
    backLabel: undefined,
  },
)

const emit = defineEmits<{
  back: []
}>()
</script>

<template>
  <header class="page-header">
    <div class="page-header__leading">
      <el-button
        v-if="showBack && backLabel"
        link
        type="primary"
        @click="emit('back')"
      >
        {{ backLabel }}
      </el-button>
      <div class="page-header__text">
        <h1>{{ title }}</h1>
        <p v-if="description">{{ description }}</p>
      </div>
    </div>
    <div v-if="$slots.meta" class="page-header__meta">
      <slot name="meta" />
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.page-header__leading {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
  min-width: 0;
  flex: 1;
}

.page-header__text {
  min-width: 0;

  h1 {
    margin: 0;
    font-size: var(--font-size-2xl);
    font-weight: 600;
    line-height: 1.25;
  }

  p {
    margin: var(--space-2) 0 0;
    font-size: var(--font-size-base);
    color: var(--text-secondary);
  }
}

.page-header__meta,
.page-header__actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}
</style>
