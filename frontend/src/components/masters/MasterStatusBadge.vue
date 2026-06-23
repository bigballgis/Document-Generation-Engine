<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MasterDocumentStatus } from '@/types/master'

const props = defineProps<{
  status: MasterDocumentStatus
}>()

const { t } = useI18n()

const tagType = computed(() => {
  switch (props.status) {
    case 'APPROVED':
      return 'success'
    case 'PENDING_REVIEW':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
})

const labelKey = computed(() => `masters.status.${props.status}`)
</script>

<template>
  <el-tag :type="tagType" effect="light">
    {{ t(labelKey) }}
  </el-tag>
</template>
