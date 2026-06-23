<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  title: string
  description?: string
}>()

const emit = defineEmits<{
  retry: []
}>()

const { t } = useI18n()
</script>

<template>
  <el-result icon="error" :title="title" :sub-title="description">
    <template #extra>
      <el-button type="primary" @click="emit('retry')">
        {{ t('common.retry') }}
      </el-button>
    </template>
  </el-result>
</template>
