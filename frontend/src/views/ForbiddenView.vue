<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const sessionStore = useSessionStore()

const traceId = computed(() => {
  const queryTraceId = route.query.traceId
  if (typeof queryTraceId === 'string' && queryTraceId.length > 0) {
    return queryTraceId
  }
  return sessionStore.lastDenyTraceId
})

function goHome() {
  if (sessionStore.authenticated) {
    router.push(sessionStore.defaultHomePath())
    return
  }
  router.push('/login')
}
</script>

<template>
  <div class="forbidden-page">
    <el-result icon="warning" :title="t('forbidden.title')" :sub-title="t('forbidden.message')">
      <template #extra>
        <p v-if="traceId" class="trace-id">
          {{ t('forbidden.referenceLabel') }}: {{ traceId }}
        </p>
        <el-button type="primary" @click="goHome">
          {{ t('forbidden.backToHome') }}
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped lang="scss">
.forbidden-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-bg);
}

.trace-id {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-family: monospace;
}
</style>
