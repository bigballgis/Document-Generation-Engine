<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ApprovalSubState } from '@/types/approvalMatrix'
import type { TemplateLifecycleStatus } from '@/types/template'

const props = defineProps<{
  status: TemplateLifecycleStatus
  approvalSubState?: ApprovalSubState | null
}>()

const { t } = useI18n()

const label = computed(() => {
  if (props.status === 'APPROVAL' && props.approvalSubState === 'PENDING_SUBMIT') {
    return t('templates.status.approvalPendingSubmit')
  }
  if (props.status === 'APPROVAL' && props.approvalSubState === 'PENDING_DECISION') {
    return t('templates.status.approvalPendingDecision')
  }
  if (props.status === 'APPROVAL' && props.approvalSubState === 'PENDING_LEGAL_DECISION') {
    return t('templates.status.approvalPendingLegalDecision')
  }
  if (props.status === 'APPROVAL' && props.approvalSubState === 'PENDING_COMPLIANCE_DECISION') {
    return t('templates.status.approvalPendingComplianceDecision')
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
