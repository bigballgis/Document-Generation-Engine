<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  messageKey: string
}>()

const emit = defineEmits<{
  retry: []
}>()

const { t, te } = useI18n()

const title = computed(() => (te(props.messageKey) ? t(props.messageKey) : t('common.loadError')))
</script>

<template>
  <el-result icon="error" :title="title">
    <template #extra>
      <el-button type="primary" @click="emit('retry')">
        {{ t('common.retry') }}
      </el-button>
    </template>
  </el-result>
</template>
