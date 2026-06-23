<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TemplateLifecycleStatus } from '@/types/template'

const props = defineProps<{
  status: TemplateLifecycleStatus
}>()

const { t } = useI18n()

const label = computed(() => t(`templates.status.${props.status}`))
const tagType = computed(() => {
  switch (props.status) {
    case 'PUBLISHED':
      return 'success'
    case 'TESTING':
    case 'APPROVAL':
    case 'PENDING_RELEASE':
      return 'warning'
    case 'STOPPED':
    case 'DEPRECATED':
      return 'info'
    default:
      return 'info'
  }
})
</script>

<template>
  <el-tag :type="tagType" effect="plain">{{ label }}</el-tag>
</template>
