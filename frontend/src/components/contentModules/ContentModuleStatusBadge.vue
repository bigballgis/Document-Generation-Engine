<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  ContentModuleLifecycleState,
  ContentModuleReviewState,
} from '@/types/contentModule'

const props = defineProps<{
  reviewState: ContentModuleReviewState
  lifecycleState?: ContentModuleLifecycleState
}>()

const { t } = useI18n()

const tagType = computed(() => {
  if (props.lifecycleState === 'DEPRECATED') {
    return 'info'
  }
  if (props.lifecycleState === 'STOPPED') {
    return 'warning'
  }
  switch (props.reviewState) {
    case 'APPROVED':
      return 'success'
    case 'SUBMITTED':
      return 'warning'
    default:
      return 'info'
  }
})

const label = computed(() => {
  if (props.lifecycleState === 'DEPRECATED') {
    return t('contentModules.lifecycle.DEPRECATED')
  }
  if (props.lifecycleState === 'STOPPED') {
    return t('contentModules.lifecycle.STOPPED')
  }
  return t(`contentModules.reviewState.${props.reviewState}`)
})
</script>

<template>
  <el-tag :type="tagType" effect="light">
    {{ label }}
  </el-tag>
</template>
