<script setup lang="ts">
import { computed, useSlots } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  titleKey: string
  descriptionKey?: string
}>()

const { t, te } = useI18n()
const slots = useSlots()

const title = computed(() => (te(props.titleKey) ? t(props.titleKey) : props.titleKey))
const description = computed(() =>
  props.descriptionKey && te(props.descriptionKey) ? t(props.descriptionKey) : '',
)
const hasActions = computed(() => Boolean(slots.actions))
</script>

<template>
  <el-empty :description="title">
    <p v-if="description" class="empty-description">{{ description }}</p>
    <div v-if="hasActions" class="empty-actions" data-testid="empty-state-actions">
      <slot name="actions" />
    </div>
  </el-empty>
</template>

<style scoped lang="scss">
.empty-description {
  margin: 0.5rem 0 0;
  color: var(--text-muted);
}

.empty-actions {
  margin-top: var(--space-4);
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: var(--space-3);
}
</style>
