<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import type { DashboardStatCard } from '@/composables/useDashboardStats'

defineProps<{
  stats: DashboardStatCard[]
  loading?: boolean
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

function navigate(path: string) {
  const hashIndex = path.indexOf('#')
  if (hashIndex >= 0) {
    const basePath = path.slice(0, hashIndex)
    const hash = path.slice(hashIndex)
    if (route.path === basePath) {
      document.querySelector(hash)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      return
    }
  }
  router.push(path)
}
</script>

<template>
  <section class="dashboard-stats">
    <header class="stats-header">
      <h2>{{ t('dashboard.stats.sectionTitle') }}</h2>
      <p>{{ t('dashboard.stats.sectionDescription') }}</p>
    </header>

    <el-skeleton v-if="loading" :rows="4" animated />

    <div v-else class="stats-grid">
      <el-card
        v-for="stat in stats"
        :key="stat.key"
        shadow="never"
        class="stat-card"
        :class="{ 'stat-card--highlight': stat.key === 'pendingActions' && stat.count > 0 }"
      >
        <p class="stat-count">{{ stat.count }}</p>
        <h3>{{ t(stat.titleKey) }}</h3>
        <p class="stat-description">{{ t(stat.descriptionKey) }}</p>
        <el-button type="primary" link @click="navigate(stat.path)">
          {{ t(stat.actionKey) }}
        </el-button>
      </el-card>
    </div>
  </section>
</template>

<style scoped lang="scss">
.dashboard-stats {
  margin-bottom: 2rem;
}

.stats-header {
  margin-bottom: 1rem;

  h2 {
    margin: 0 0 0.25rem;
    font-size: 1.25rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
}

.stat-card {
  border: 1px solid var(--border-color);

  &--highlight {
    border-color: color-mix(in srgb, var(--brand-primary) 35%, var(--border-color));
    background: color-mix(in srgb, var(--brand-primary) 6%, white);
  }
}

.stat-count {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  line-height: 1.1;
  color: var(--brand-primary);
}

.stat-card h3 {
  margin: 0.5rem 0 0.25rem;
  font-size: 1rem;
}

.stat-description {
  margin: 0 0 0.5rem;
  font-size: 0.875rem;
  color: var(--text-muted);
}
</style>
