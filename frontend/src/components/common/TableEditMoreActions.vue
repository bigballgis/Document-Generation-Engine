<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  editLabel?: string
  moreLabel?: string
}>()

const emit = defineEmits<{
  edit: []
  command: [command: string]
}>()

const { t } = useI18n()
</script>

<template>
  <div class="table-edit-more-actions" data-testid="table-edit-more-actions">
    <el-button
      class="table-edit-more-actions__edit"
      link
      size="small"
      type="primary"
      @click="emit('edit')"
    >
      <slot name="edit">{{ editLabel ?? t('common.edit') }}</slot>
    </el-button>
    <el-dropdown
      trigger="click"
      class="table-edit-more-actions__more"
      @command="(command: string) => emit('command', command)"
    >
      <el-button link size="small">
        {{ moreLabel ?? t('common.more') }}
      </el-button>
      <template #dropdown>
        <slot name="more" />
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped lang="scss">
.table-edit-more-actions {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  white-space: nowrap;
  vertical-align: middle;
}

.table-edit-more-actions__edit,
.table-edit-more-actions__more {
  flex: 0 0 auto;
}
</style>
