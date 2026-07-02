<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export type WorkspaceTabOption = {
  name: string
  labelKey: string
}

const props = defineProps<{
  modelValue: string
  tabs: WorkspaceTabOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()

const activeTab = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
})
</script>

<template>
  <section class="workspace-tab-shell">
    <div class="workspace-tab-shell__header">
      <el-tabs v-model="activeTab" class="workspace-tab-shell__tabs">
        <el-tab-pane
          v-for="tab in tabs"
          :key="tab.name"
          :label="t(tab.labelKey)"
          :name="tab.name"
        />
      </el-tabs>
      <div v-if="$slots.actions" class="workspace-tab-shell__actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="workspace-tab-shell__content">
      <slot :name="activeTab" />
    </div>
  </section>
</template>

<style scoped lang="scss">
.workspace-tab-shell {
  &__header {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    justify-content: space-between;
    gap: 0.75rem 1rem;
    margin-bottom: 1rem;
    border-bottom: 1px solid var(--border-color);
  }

  &__tabs {
    flex: 1 1 auto;
    min-width: 12rem;
    margin-bottom: -1px;

    :deep(.el-tabs__header) {
      margin-bottom: 0;
    }

    :deep(.el-tabs__nav-wrap::after) {
      display: none;
    }
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.5rem;
    padding-bottom: 0.5rem;
    margin-left: auto;
  }

  &__content {
    min-height: 4rem;
  }
}
</style>
