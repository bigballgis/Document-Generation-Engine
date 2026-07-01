<script setup lang="ts">
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'

defineProps<{
  title: string
  helpTitle?: string
  helpContent?: string
}>()
</script>

<template>
  <div class="section-panel-header">
    <div class="section-panel-header__main">
      <h3 class="section-panel-header__title">{{ title }}</h3>
      <ContextHelpTrigger
        v-if="helpTitle && helpContent"
        :title="helpTitle"
        :content="helpContent"
      />
    </div>
    <div v-if="$slots.actions" class="section-panel-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.section-panel-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.75rem;

  &__main {
    display: flex;
    align-items: center;
    gap: 0.25rem;
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-size: 1rem;
    font-weight: 650;
    line-height: 1.4;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
  }
}
</style>
