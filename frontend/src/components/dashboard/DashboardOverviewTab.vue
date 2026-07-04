<script setup lang="ts">
import DashboardStatCards from '@/components/dashboard/DashboardStatCards.vue'
import CollaborationTimeoutConfigPanel from '@/components/collaboration/CollaborationTimeoutConfigPanel.vue'
import type { DashboardStatCard } from '@/composables/useDashboardStats'

defineProps<{
  stats: DashboardStatCard[]
  loading: boolean
  showStatsSection: boolean
  showTimeoutConfig: boolean
}>()
</script>

<template>
  <DashboardStatCards v-if="showStatsSection" :stats="stats" :loading="loading" />

  <CollaborationTimeoutConfigPanel v-if="showTimeoutConfig" />
</template>
