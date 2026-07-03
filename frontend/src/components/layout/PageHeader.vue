<script setup lang="ts">
import { computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    title: string
    description?: string
    helpText?: string
    showBack?: boolean
    backLabel?: string
  }>(),
  {
    description: undefined,
    helpText: undefined,
    showBack: false,
    backLabel: undefined,
  },
)

const emit = defineEmits<{
  back: []
}>()

const titleId = computed(() => `page-title-${props.title.replace(/\s+/g, '-').toLowerCase()}`)
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
        <div class="page-header__title-row">
          <h1 :id="titleId">{{ title }}</h1>
          <el-tooltip
            v-if="helpText"
            :content="helpText"
            placement="top-start"
            :show-after="200"
            popper-class="page-header-help-tooltip"
          >
            <button
              type="button"
              class="page-header__help"
              :aria-label="helpText"
              :aria-describedby="titleId"
            >
              <el-icon><InfoFilled /></el-icon>
            </button>
          </el-tooltip>
        </div>
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
}

.page-header__title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;

  h1 {
    margin: 0;
    font-size: var(--font-size-2xl);
    font-weight: 600;
    line-height: 1.25;
  }
}

.page-header__help {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-muted);
  cursor: help;
  font-size: 1rem;
  line-height: 1;

  &:hover,
  &:focus-visible {
    color: var(--brand-primary);
  }

  &:focus-visible {
    outline: 2px solid var(--brand-primary);
    outline-offset: 2px;
    border-radius: var(--radius-sm);
  }
}

.page-header__text p {
  margin: var(--space-2) 0 0;
  font-size: var(--font-size-base);
  color: var(--text-secondary);
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

<style lang="scss">
.page-header-help-tooltip {
  max-width: 360px;
  line-height: 1.5;
}
</style>
