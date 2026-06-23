<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import ManagementShell from '@/components/layout/ManagementShell.vue'

const route = useRoute()
const useShell = computed(() => !route.meta.public)
</script>

<template>
  <ManagementShell v-if="useShell">
    <RouterView />
  </ManagementShell>
  <RouterView v-else />
</template>
