<script setup lang="ts">
import DashboardStatCards from '@/components/dashboard/DashboardStatCards.vue'
import type { DashboardStatCard } from '@/composables/useDashboardStats'

defineProps<{
  stats: DashboardStatCard[]
  loading: boolean
  showStatsSection: boolean
}>()
</script>

<template>
  <DashboardStatCards v-if="showStatsSection" :stats="stats" :loading="loading" />
</template>
