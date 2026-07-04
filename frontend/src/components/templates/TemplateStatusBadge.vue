<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TemplateLifecycleStatus } from '@/types/template'

const props = defineProps<{
  status: TemplateLifecycleStatus
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
}>()

const { t } = useI18n()

const label = computed(() => {
  if (props.status === 'APPROVAL' && props.approvalSubState === 'PENDING_SUBMIT') {
    return t('templates.status.approvalPendingSubmit')
  }
  if (props.status === 'APPROVAL' && props.approvalSubState === 'PENDING_DECISION') {
    return t('templates.status.approvalPendingDecision')
  }
  return t(`templates.status.${props.status}`)
})

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
  <el-tag :type="tagType" effect="light" class="status-badge">{{ label }}</el-tag>
</template>
