<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps<{
  title: string
  content: string
  placement?: 'top' | 'bottom' | 'left' | 'right'
}>()

const { t } = useI18n()
const visible = ref(false)
</script>

<template>
  <el-popover
    v-model:visible="visible"
    :title="title"
    :placement="placement ?? 'bottom-start'"
    :width="320"
    trigger="click"
    popper-class="context-help-popover"
  >
    <template #reference>
      <el-button
        class="context-help-trigger"
        :aria-label="t('common.contextHelp.triggerLabel')"
        circle
        size="small"
        type="primary"
        @click.stop
      >
        ?
      </el-button>
    </template>
    <p class="context-help-popover__content">{{ content }}</p>
  </el-popover>
</template>

<style scoped lang="scss">
.context-help-trigger {
  flex-shrink: 0;
  padding: 0.25rem;

  :deep(.el-icon) {
    vertical-align: middle;
  }
}
</style>

<style lang="scss">
.context-help-popover {
  &__content {
    margin: 0;
    font-size: 0.875rem;
    line-height: 1.5;
    color: var(--el-text-color-regular);
    white-space: pre-wrap;
  }
}
</style>
